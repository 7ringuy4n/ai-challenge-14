#!/usr/bin/env python3
"""Verify member isolation over HTTP and compare protected PostgreSQL rows."""
from __future__ import annotations
import base64, http.cookiejar, json, os, subprocess, sys, urllib.error, urllib.request, uuid
from datetime import datetime, timezone
from pathlib import Path


def load_env(path: Path) -> dict[str, str]:
    out = {}
    for line in path.read_text().splitlines():
        if line.strip() and not line.lstrip().startswith('#') and '=' in line:
            k, v = line.split('=', 1); out[k.strip()] = v.strip()
    return out


class Client:
    def __init__(self, base: str, user: str, password: str, output: Path):
        self.base, self.user, self.password, self.output = base.rstrip('/'), user, password, output
        self.cookies = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookies))
        self.csrf = None

    def call(self, method: str, path: str, body=None, key=None):
        headers = {'Accept': 'application/json', 'X-Correlation-ID': f'cross-member-{uuid.uuid4().hex[:20]}'}
        headers['Authorization'] = 'Basic ' + base64.b64encode(f'{self.user}:{self.password}'.encode()).decode()
        if body is not None:
            headers['Content-Type'] = 'application/json'
        if key:
            headers['Idempotency-Key'] = key
        if self.csrf:
            headers[self.csrf['csrfHeaderName']] = self.csrf['csrfToken']
        data = json.dumps(body, separators=(',', ':')).encode() if body is not None else None
        req = urllib.request.Request(self.base + path, data=data, headers=headers, method=method)
        try:
            with self.opener.open(req, timeout=10) as response:
                status, raw, response_headers = response.status, response.read(), response.headers
        except urllib.error.HTTPError as error:
            status, raw, response_headers = error.code, error.read(), error.headers
        try:
            parsed = json.loads(raw) if raw else None
        except json.JSONDecodeError:
            parsed = {'body': '[non-JSON omitted]'}
        safe_headers = {k: ('[REDACTED]' if 'csrf' in k.lower() or k.lower() in {'set-cookie', 'authorization'} else v)
                        for k, v in headers.items()}
        safe_body = ({**parsed, 'csrfToken': '[REDACTED]'} if isinstance(parsed, dict) and 'csrfToken' in parsed else parsed)
        record = {'timestampUtc': datetime.now(timezone.utc).isoformat(), 'principal': self.user,
                  'request': {'method': method, 'path': path, 'headers': safe_headers, 'body': body},
                  'response': {'status': status, 'body': safe_body, 'correlationId': response_headers.get('X-Correlation-ID')},
                  'redactions': ['Authorization, session cookies and CSRF token values redacted.']}
        with self.output.open('a') as stream:
            stream.write(json.dumps(record, separators=(',', ':')) + '\n')
        return status, parsed

    def login(self):
        status, session = self.call('GET', '/api/v1/session')
        if status != 200 or not session.get('csrfToken'):
            raise RuntimeError(f'{self.user} session failed: HTTP {status}')
        self.csrf = session


def db_snapshot(claim_id: str) -> dict:
    safe_id = str(uuid.UUID(claim_id))
    sql = f"""SELECT json_build_object(
      'claim',(SELECT json_build_object('id',id,'member_id',member_id,'state',state,'version',version,'facts',facts,'updated_at',updated_at) FROM claims WHERE id='{safe_id}'),
      'claim_audit',(SELECT count(*) FROM claim_audit WHERE claim_id='{safe_id}'),
      'payment',(SELECT row_to_json(p) FROM payment_requests p WHERE claim_id='{safe_id}'),
      'outbox',(SELECT coalesce(json_agg(json_build_object('effect',effect,'attempts',attempts,'delivered_at',delivered_at) ORDER BY effect),'[]'::json) FROM local_outbox WHERE claim_id='{safe_id}')
    )::text"""
    command = ['docker', 'compose', '--env-file', os.environ.get('CLAIMS_ENV_FILE', '.env'),
               '-f', 'docker/compose.yml', 'exec', '-T', 'database', 'psql', '-U', 'claims_bootstrap',
               '-d', 'claims', '-Atc', sql]
    result = subprocess.run(command, check=True, text=True, capture_output=True)
    return json.loads(result.stdout.strip().splitlines()[-1])


def main() -> int:
    env = load_env(Path(os.environ.get('CLAIMS_ENV_FILE', '.env')))
    password = env.get('CLAIMS_DEMO_PASSWORD', '')
    if not password or password.startswith('CHANGE_ME_'):
        raise RuntimeError('Configured human password is unavailable.')
    root = Path(os.environ.get('CLAIMS_EVIDENCE_DIR', 'reports/verification/REQ-017-cross-member'))
    run_id = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    outdir = root / f'run-{run_id}'
    outdir.mkdir(parents=True, exist_ok=True)
    exchanges = outdir / 'http-exchanges.jsonl'
    base = os.environ.get('CLAIMS_API_BASE', f"http://127.0.0.1:{env.get('SERVER_PORT', '8080')}")
    alpha, beta = Client(base, 'member-alpha', password, exchanges), Client(base, 'member-beta', password, exchanges)
    alpha.login(); beta.login()
    alpha_key = 'xmember-' + uuid.uuid4().hex
    status, claim_a = alpha.call('POST', '/api/v1/claims', {'policyLimit': 750, 'currency': 'USD', 'facts': {'requestedAmount': 325}}, alpha_key)
    if status != 201:
        raise AssertionError(f'member-alpha claim creation got HTTP {status}')
    status, claim_b = beta.call('POST', '/api/v1/claims', {'policyLimit': 800, 'currency': 'USD', 'facts': {'requestedAmount': 350}}, 'xmember-' + uuid.uuid4().hex)
    if status != 201:
        raise AssertionError(f'member-beta claim creation got HTTP {status}')
    id_a, id_b = claim_a['id'], claim_b['id']
    before = db_snapshot(id_a)
    results = []
    def check(name: str, operation, allowed: set[int]):
        status, body = operation()
        print(f"running test case {len(results)+1}/7: {name}")
        if status not in allowed:
            raise AssertionError(f'{name}: expected HTTP {sorted(allowed)}, received {status}')
        results.append({'name': name, 'expectedStatus': sorted(allowed), 'actualStatus': status,
                        'result': 'PASS', 'responseCode': body.get('code') if isinstance(body, dict) else None})
        return status, body
    status, listing = beta.call('GET', '/api/v1/claims?page=0&size=100')
    if status != 200 or any(item['id'] == id_a for item in listing.get('items', [])):
        raise AssertionError('member-beta list exposed member-alpha claim')
    print('running test case 1/7: other member claim is absent from list')
    results.append({'name':'other member claim is absent from list','expectedStatus':[200],'actualStatus':status,'result':'PASS','claimHidden':True})
    check('member-beta can read its own claim', lambda: beta.call('GET', f'/api/v1/claims/{id_b}'), {200})
    check('cross-member claim detail is not disclosed', lambda: beta.call('GET', f'/api/v1/claims/{id_a}'), {404})
    check('cross-member audit is not disclosed', lambda: beta.call('GET', f'/api/v1/claims/{id_a}/audit'), {404})
    check('cross-member facts update is denied', lambda: beta.call('PUT', f'/api/v1/claims/{id_a}/facts', {'expectedVersion':0,'facts':{'requestedAmount':1}}, 'xmember-' + uuid.uuid4().hex), {404})
    check('cross-member approval transition is denied', lambda: beta.call('POST', f'/api/v1/claims/{id_a}/transitions', {'targetState':'APPROVED','expectedVersion':0,'reason':'unauthorized','facts':{}}, 'xmember-' + uuid.uuid4().hex), {404})
    check('member identity cannot invoke protected system transition', lambda: beta.call('POST', f'/api/v1/claims/{id_a}/system-transitions', {'targetState':'CLOSED','expectedVersion':0,'reason':'unauthorized','facts':{}}, 'xmember-' + uuid.uuid4().hex), {403})
    after = db_snapshot(id_a)
    if before['claim'] != after['claim'] or before['claim_audit'] != after['claim_audit'] or before['payment'] != after['payment'] or before['outbox'] != after['outbox']:
        raise AssertionError('denied requests changed the protected claim, claim audit, payment or outbox rows')
    report = {'status':'PASS','startedAtUtc':datetime.now(timezone.utc).isoformat(),'base':base,
              'members':{'alpha':{'username':'member-alpha','claimId':id_a},'beta':{'username':'member-beta','claimId':id_b}},
              'results':results,'databaseEvidence':{'targetClaimId':id_a,'before':before,'after':after,
              'invariant':'claim business row, claim audit count, payment request and outbox rows remained identical'},
              'redactions':['Authorization, cookie and CSRF secrets excluded/redacted; credentials are not written.'],
              'note':'Failure audit rows are permitted operational records; this comparison targets claim state and business side effects.'}
    (outdir / 'cross-member.json').write_text(json.dumps(report, indent=2) + '\n')
    print(f"Cross-member checks passed: {len(results)}/{len(results)}; HTTP evidence and DB snapshots saved.")
    return 0

if __name__ == '__main__':
    try: raise SystemExit(main())
    except Exception as exc:
        print(f'cross-member verification failed: {type(exc).__name__}: {exc}', file=sys.stderr)
        raise
