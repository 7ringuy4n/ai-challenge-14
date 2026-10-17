#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
ENV_FILE="${ENV_FILE:-.env}"
if [[ -f "$ENV_FILE" ]]; then
  # Parse as data in Python; never source credential-bearing files.
  :
elif [[ -z "${HOST_PORT:-}" || -z "${DEMO_PASSWORD:-}" || -z "${SYSTEM_PASSWORD:-}" ]]; then
  printf 'Set HOST_PORT, DEMO_PASSWORD, SYSTEM_PASSWORD or provide ENV_FILE.\n' >&2
  exit 1
fi
python3 - "$ENV_FILE" <<'PY'
import base64, http.cookiejar, json, os, sys, urllib.error, urllib.request, uuid
from pathlib import Path

values = {}
path = Path(sys.argv[1])
if path.is_file():
    for line in path.read_text(encoding='utf-8').splitlines():
        line = line.strip()
        if not line or line.startswith('#') or '=' not in line: continue
        key, value = line.split('=', 1)
        values[key.strip()] = value.strip().strip('"\'')
port = os.environ.get('HOST_PORT', values.get('HOST_PORT', '18080'))
demo = os.environ.get('DEMO_PASSWORD', values.get('DEMO_PASSWORD', ''))
system = os.environ.get('SYSTEM_PASSWORD', values.get('SYSTEM_PASSWORD', ''))
if not port.isdecimal() or not 1 <= int(port) <= 65535: raise SystemExit('HOST_PORT must be between 1 and 65535')
if not demo or not system or demo == system: raise SystemExit('Separate DEMO_PASSWORD and SYSTEM_PASSWORD values are required')
base = os.environ.get('BASE_URL', f'http://127.0.0.1:{port}').rstrip('/')
out = Path(os.environ.get('SCENARIO_OUTPUT', 'reports/scenarios'))
out.mkdir(parents=True, exist_ok=True)

class Client:
    def __init__(self, user, password):
        self.jar = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))
        token = base64.b64encode(f'{user}:{password}'.encode()).decode()
        self.auth = 'Basic ' + token
        status, session = self.call('GET', '/api/session', auth=True)
        assert status == 200 and session.get('csrfToken')
        self.csrf, self.header = session['csrfToken'], session['csrfHeader']
    def call(self, method, path, body=None, auth=True, csrf=True):
        headers = {'Accept':'application/json'}
        if auth: headers['Authorization'] = self.auth
        data = None
        if body is not None:
            data = json.dumps(body).encode()
            headers['Content-Type'] = 'application/json'
        if method not in ('GET','HEAD') and csrf and hasattr(self, 'csrf'):
            headers[self.header] = self.csrf
        req = urllib.request.Request(base+path, data=data, headers=headers, method=method)
        try:
            with self.opener.open(req, timeout=15) as res: return res.status, json.loads(res.read())
        except urllib.error.HTTPError as e:
            try: body = json.loads(e.read())
            except Exception: body = {}
            return e.code, body

def client(name, password=demo): return Client(name, password)
def create(c, member='member-001'):
    status, claim = c.call('POST','/api/claims',{'memberId':member,'policyLimit':1000,'currency':'USD','requiredDocuments':['identity']})
    assert status == 201, f'create status {status}'
    return claim
def transition(c, claim, target, key=None, notes='scenario acceptance'):
    return c.call('POST',f"/api/claims/{claim['id']}/transitions",{'target':target,'expectedVersion':claim['version'],'idempotencyKey':key or str(uuid.uuid4()),'notes':notes})
def facts(c, claim, updates):
    return c.call('PATCH',f"/api/claims/{claim['id']}/facts",{'expectedVersion':claim['version'],'updates':updates})
def document_step(claim):
    c=client('clerk'); status,x=facts(c,claim,{'documents':{'identity':True}}); assert status==200; status,x=transition(c,x,'DOCUMENTS_VERIFIED'); assert status==200 and x['state']=='DOCUMENTS_VERIFIED'; return x
def audit(c, claim):
    status, rows=c.call('GET',f"/api/claims/{claim['id']}/audit"); assert status==200
    return rows
def export(name, claim, rows, outcomes):
    (out/f'{name}.json').write_text(json.dumps({'scenario':name,'claim':claim,'audit':rows,'outcomes':outcomes},indent=2)+'\n',encoding='utf-8')
def check_audit(rows, expected):
    assert [r['toState'] for r in rows] == expected, 'audit state sequence differs'
    assert [r['sequence'] for r in rows] == list(range(1,len(rows)+1)), 'audit sequence differs'

print('1/5 happy5')
c=client('clerk'); claim=create(c); claim=document_step(claim)
c=client('lead'); s,claim=facts(c,claim,{'assessorId':'assessor-01'}); assert s==200
s,claim=transition(c,claim,'UNDER_ASSESSMENT'); assert s==200
c=client('assessor'); s,claim=facts(c,claim,{'reportComplete':True,'assessmentAmount':500,'assessmentCurrency':'USD'}); assert s==200
s,claim=transition(c,claim,'APPROVED'); assert s==200
c=client('finance'); s,claim=transition(c,claim,'PAYMENT_INITIATED'); assert s==200
s,claim=facts(c,claim,{'paymentConfirmed':True,'paymentReference':'accept-'+str(uuid.uuid4())}); assert s==200
s,claim=transition(c,claim,'CLOSED'); assert s==200 and claim['state']=='CLOSED'
rows=audit(c,claim); check_audit(rows,['SUBMITTED','DOCUMENTS_VERIFIED','UNDER_ASSESSMENT','APPROVED','PAYMENT_INITIATED','CLOSED']); export('happy5',claim,rows,{'finalState':claim['state']})
print('2/5 rejection4')
c=client('clerk'); claim=create(c); claim=document_step(claim)
c=client('lead'); s,claim=facts(c,claim,{'assessorId':'assessor-01'}); assert s==200; s,claim=transition(c,claim,'UNDER_ASSESSMENT'); assert s==200
c=client('assessor'); s,claim=facts(c,claim,{'reportComplete':True,'rejectionReason':'not covered'}); assert s==200; s,claim=transition(c,claim,'REJECTED'); assert s==200
c=client('member-001'); s,claim=facts(c,claim,{'memberAcknowledged':True}); assert s==200
c=client('system',system); s,claim=c.call('POST',f"/api/system/claims/{claim['id']}/close",{'expectedVersion':claim['version'],'idempotencyKey':str(uuid.uuid4()),'notes':'acknowledged'})
assert s==200 and claim['state']=='CLOSED'; rows=audit(c,claim); check_audit(rows,['SUBMITTED','DOCUMENTS_VERIFIED','UNDER_ASSESSMENT','REJECTED','CLOSED']); export('rejection4',claim,rows,{'finalState':claim['state']})
print('3/5 info-loop8')
c=client('clerk'); claim=create(c); claim=document_step(claim)
c=client('lead'); s,claim=facts(c,claim,{'assessorId':'assessor-01'}); assert s==200; s,claim=transition(c,claim,'UNDER_ASSESSMENT'); assert s==200
c=client('assessor')
for n in range(2):
    _,claim=facts(c,claim,{'missingInfoDescription':f'missing evidence {n+1}'})
    s,claim=transition(c,claim,'PENDING_INFO'); assert s==200 and claim['state']=='PENDING_INFO'
    c=client('member-001'); s,claim=facts(c,claim,{'newInformation':f'provided evidence {n+1}'}); assert s==200
    c=client('clerk'); s,claim=transition(c,claim,'DOCUMENTS_VERIFIED'); assert s==200
    c=client('lead'); _,claim=facts(c,claim,{'assessorId':'assessor-01'}); s,claim=transition(c,claim,'UNDER_ASSESSMENT'); assert s==200
    c=client('assessor')
rows=audit(c,claim); expected=['SUBMITTED','DOCUMENTS_VERIFIED','UNDER_ASSESSMENT','PENDING_INFO','DOCUMENTS_VERIFIED','UNDER_ASSESSMENT','PENDING_INFO','DOCUMENTS_VERIFIED','UNDER_ASSESSMENT']; check_audit(rows,expected); export('info-loop8',claim,rows,{'informationRequests':claim['informationRequests']})
print('4/5 invalid SUBMITTED to APPROVED')
c=client('clerk'); claim=create(c); before=audit(c,claim); s,body=transition(c,claim,'APPROVED'); assert s==409, f'expected 409, got {s}'; after=audit(c,claim); assert after==before
export('invalid-transition',claim,after,{'httpStatus':s,'error':body})
print('5/5 wrong role')
c=client('assessor'); s,claim=facts(c,claim,{'reportComplete':True}); assert s==200
before=audit(c,claim); s,body=transition(c,claim,'DOCUMENTS_VERIFIED'); assert s==403, f'expected 403, got {s}'; after=audit(c,claim); assert after==before
export('wrong-role',claim,after,{'httpStatus':s,'error':body})
print('PASS five scenarios; exports contain claims and audit results')
PY
