#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
ENV_FILE="${ENV_FILE:-.env}"
if [[ ! -f "$ENV_FILE" && ( -z "${HOST_PORT:-}" || -z "${DEMO_PASSWORD:-}" || -z "${SYSTEM_PASSWORD:-}" ) ]]; then
  printf 'Set HOST_PORT, DEMO_PASSWORD, SYSTEM_PASSWORD or provide ENV_FILE.\n' >&2
  exit 1
fi
python3 - "$ENV_FILE" <<'PY'
import base64
import http.cookiejar
import json
import os
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path

# Configuration is parsed as data. Secrets never enter scenario output or logs.
values = {}
env_path = Path(sys.argv[1])
if env_path.is_file():
    for raw in env_path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip().strip("\"'")
port = os.environ.get("HOST_PORT", values.get("HOST_PORT", "18080"))
demo_password = os.environ.get("DEMO_PASSWORD", values.get("DEMO_PASSWORD", ""))
system_password = os.environ.get("SYSTEM_PASSWORD", values.get("SYSTEM_PASSWORD", ""))
if not port.isdecimal() or not 1 <= int(port) <= 65535:
    raise SystemExit("HOST_PORT must be an integer from 1 through 65535")
if not demo_password or not system_password or demo_password == system_password:
    raise SystemExit("Separate nonempty DEMO_PASSWORD and SYSTEM_PASSWORD values are required")
base_url = os.environ.get("BASE_URL", f"http://127.0.0.1:{port}").rstrip("/")
output_dir = Path(os.environ.get("SCENARIO_OUTPUT", "reports/scenarios"))
output_dir.mkdir(parents=True, exist_ok=True)

class HttpClient:
    def __init__(self, username=None, password=None):
        self.cookies = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookies))
        self.auth = None
        self.csrf_header = None
        self.csrf_token = None
        if username is not None:
            encoded = base64.b64encode(f"{username}:{password}".encode("utf-8")).decode("ascii")
            self.auth = f"Basic {encoded}"
            status, body, _ = self.request("GET", "/api/session")
            require(status == 200, f"{username} session returned HTTP {status}")
            require(bool(body.get("csrfToken")) and bool(body.get("csrfHeader")), "session has no CSRF material")
            self.csrf_token = body["csrfToken"]
            self.csrf_header = body["csrfHeader"]

    def request(self, method, path, body=None, include_auth=True, include_csrf=True):
        headers = {"Accept": "application/json"}
        if include_auth and self.auth:
            headers["Authorization"] = self.auth
        raw = None
        if body is not None:
            raw = json.dumps(body, separators=(",", ":")).encode("utf-8")
            headers["Content-Type"] = "application/json"
        if method not in ("GET", "HEAD") and include_csrf and self.csrf_token:
            headers[self.csrf_header] = self.csrf_token
        request = urllib.request.Request(base_url + path, data=raw, headers=headers, method=method)
        try:
            with self.opener.open(request, timeout=20) as response:
                content = response.read()
                parsed = json.loads(content) if content else {}
                return response.status, parsed, dict(response.headers)
        except urllib.error.HTTPError as error:
            content = error.read()
            try:
                parsed = json.loads(content) if content else {}
            except (UnicodeDecodeError, json.JSONDecodeError):
                parsed = {"body": "<non-json error body>"}
            return error.code, parsed, dict(error.headers)
        except (urllib.error.URLError, TimeoutError) as error:
            raise ScenarioFailure(f"request to {path} failed: {type(error).__name__}") from None

class ScenarioFailure(AssertionError):
    pass

def require(condition, message):
    if not condition:
        raise ScenarioFailure(message)

def human(username):
    return HttpClient(username, demo_password)

def system():
    return HttpClient("system", system_password)

def create(client, member="member-001"):
    status, claim, _ = client.request("POST", "/api/claims", {
        "memberId": member,
        "policyLimit": 1000.00,
        "currency": "USD",
        "requiredDocuments": ["identity", "receipt"],
    })
    require(status == 201, f"claim creation returned HTTP {status}")
    require(claim.get("state") == "SUBMITTED", "new claim was not SUBMITTED")
    return claim

def patch_facts(client, claim, updates):
    return client.request("PATCH", f"/api/claims/{claim['id']}/facts", {
        "expectedVersion": claim["version"], "updates": updates,
    })

def command(client, claim, target, key=None, notes="acceptance scenario"):
    body = {
        "target": target,
        "expectedVersion": claim["version"],
        "idempotencyKey": key or str(uuid.uuid4()),
        "notes": notes,
    }
    return client.request("POST", f"/api/claims/{claim['id']}/transitions", body)

def system_close(client, claim):
    return client.request("POST", f"/api/system/claims/{claim['id']}/close", {
        "expectedVersion": claim["version"],
        "idempotencyKey": str(uuid.uuid4()),
        "notes": "member acknowledged rejection",
    })

def audit(client, claim):
    status, rows, _ = client.request("GET", f"/api/claims/{claim['id']}/audit")
    require(status == 200, f"audit read returned HTTP {status}")
    return rows

def assert_audit_path(rows, expected_states):
    observed = [entry["toState"] for entry in rows]
    require(observed == expected_states, f"audit states differ: {observed}")
    sequences = [entry["sequence"] for entry in rows]
    require(all(a < b for a, b in zip(sequences, sequences[1:])), "audit sequence is not strictly increasing")

def assert_unchanged(client, claim, before):
    after = audit(client, claim)
    require(after == before, "failed operation changed the audit log")
    return after

def export(name, claim, rows, outcomes):
    path = output_dir / f"{name}.json"
    path.write_text(json.dumps({
        "scenario": name,
        "claim": claim,
        "audit": rows,
        "outcomes": outcomes,
    }, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

def patch_ok(client, claim, updates, description):
    status, updated, _ = patch_facts(client, claim, updates)
    require(status == 200, f"{description} facts update returned HTTP {status}")
    return updated

def transition_ok(client, claim, target, description):
    status, updated, _ = command(client, claim, target)
    require(status == 200 and updated.get("state") == target,
            f"{description} transition returned HTTP {status}")
    return updated

def documents_verified(claim):
    clerk = human("clerk")
    claim = patch_ok(clerk, claim, {"documents": {"identity": True, "receipt": True}}, "documents")
    return transition_ok(clerk, claim, "DOCUMENTS_VERIFIED", "document verification")

def start_assessment(claim):
    lead = human("lead")
    claim = patch_ok(lead, claim, {"assessorId": "assessor-acceptance"}, "assessor assignment")
    return transition_ok(lead, claim, "UNDER_ASSESSMENT", "assessment start")

def approve(claim):
    assessor = human("assessor")
    claim = patch_ok(assessor, claim, {
        "reportComplete": True, "assessmentAmount": 500.00, "assessmentCurrency": "USD",
    }, "assessment report")
    return transition_ok(assessor, claim, "APPROVED", "approval")

def complete_payment(claim):
    finance = human("finance")
    claim = transition_ok(finance, claim, "PAYMENT_INITIATED", "payment initiation")
    claim = patch_ok(finance, claim, {
        "paymentConfirmed": True, "paymentReference": "accept-" + str(uuid.uuid4()),
    }, "payment confirmation")
    return transition_ok(finance, claim, "CLOSED", "payment close")

def info_request(claim, index):
    assessor = human("assessor")
    claim = patch_ok(assessor, claim, {"missingInfoDescription": f"missing receipt {index}"}, "information request details")
    claim = transition_ok(assessor, claim, "PENDING_INFO", "information request")
    clerk = human("clerk")
    claim = patch_ok(clerk, claim, {"newInformation": f"receipt supplied {index}"}, "member supplied information")
    claim = transition_ok(clerk, claim, "DOCUMENTS_VERIFIED", "documents re-verification")
    return start_assessment(claim)

cases = [
    "happy5", "rejection4", "info-loop8", "invalid-transition", "wrong-role",
    "missing-documents-guard", "information-request-limit", "stale-version",
    "idempotent-replay", "cross-member-read", "spoofed-actor-body",
    "system-and-human-boundaries", "csrf-required", "unauthenticated-read",
]
current = "initialization"

def run_case(name, action):
    global current
    current = name
    try:
        print(f"RUN {cases.index(name)+1}/{len(cases)} {name}", flush=True)
        claim, rows, outcomes = action()
        export(name, claim, rows, outcomes)
        print(f"PASS {name}", flush=True)
    except Exception as error:
        print(f"FAIL {name}: {type(error).__name__}: {str(error)[:350]}", file=sys.stderr, flush=True)
        raise SystemExit(1) from None

def happy5():
    claim = documents_verified(create(human("clerk")))
    claim = start_assessment(claim)
    claim = approve(claim)
    claim = complete_payment(claim)
    rows = audit(human("finance"), claim)
    assert_audit_path(rows, ["DOCUMENTS_VERIFIED", "UNDER_ASSESSMENT", "APPROVED", "PAYMENT_INITIATED", "CLOSED"])
    return claim, rows, {"transitions": 5, "finalState": claim["state"]}

run_case("happy5", happy5)

def rejection4():
    claim = documents_verified(create(human("clerk")))
    claim = start_assessment(claim)
    assessor = human("assessor")
    claim = patch_ok(assessor, claim, {"reportComplete": True, "rejectionReason": "not covered"}, "rejection report")
    claim = transition_ok(assessor, claim, "REJECTED", "rejection")
    member = human("member-001")
    claim = patch_ok(member, claim, {"memberAcknowledged": True}, "member acknowledgement")
    close_status, claim, _ = system_close(system(), claim)
    require(close_status == 200 and claim.get("state") == "CLOSED", f"system close returned HTTP {close_status}")
    rows = audit(system(), claim)
    assert_audit_path(rows, ["DOCUMENTS_VERIFIED", "UNDER_ASSESSMENT", "REJECTED", "CLOSED"])
    require(rows[-1]["role"] == "system", "close audit actor is not system")
    return claim, rows, {"transitions": 4, "finalState": claim["state"]}

run_case("rejection4", rejection4)

def info_loop8():
    claim = documents_verified(create(human("clerk")))
    claim = start_assessment(claim)
    for index in (1, 2):
        claim = info_request(claim, index)
    claim = approve(claim)
    claim = complete_payment(claim)
    rows = audit(human("finance"), claim)
    expected = ["DOCUMENTS_VERIFIED", "UNDER_ASSESSMENT", "PENDING_INFO",
                "DOCUMENTS_VERIFIED", "UNDER_ASSESSMENT", "PENDING_INFO",
                "DOCUMENTS_VERIFIED", "UNDER_ASSESSMENT", "APPROVED",
                "PAYMENT_INITIATED", "CLOSED"]
    assert_audit_path(rows, expected)
    require(claim["informationRequests"] == 2, "information request counter did not equal two")
    return claim, rows, {"firstEightTransitions": expected[:8], "finalState": claim["state"], "informationRequests": claim["informationRequests"]}

run_case("info-loop8", info_loop8)

def invalid_transition():
    clerk = human("clerk"); claim = create(clerk); before = audit(clerk, claim)
    status, error, _ = command(clerk, claim, "APPROVED")
    require(status == 409 and error.get("code") == "INVALID_TRANSITION", f"expected invalid-transition 409; got HTTP {status} code {error.get('code')}")
    rows = assert_unchanged(clerk, claim, before)
    return claim, rows, {"httpStatus": status, "error": error}

run_case("invalid-transition", invalid_transition)

def wrong_role():
    clerk = human("clerk"); claim = create(clerk)
    claim = patch_ok(clerk, claim, {"documents": {"identity": True, "receipt": True}}, "documents")
    assessor = human("assessor"); before = audit(assessor, claim)
    status, error, _ = command(assessor, claim, "DOCUMENTS_VERIFIED")
    require(status == 403 and error.get("code") == "FORBIDDEN_ROLE", f"expected wrong-role 403; got HTTP {status}")
    rows = assert_unchanged(assessor, claim, before)
    return claim, rows, {"httpStatus": status, "error": error}

run_case("wrong-role", wrong_role)

def missing_documents_guard():
    clerk = human("clerk"); claim = create(clerk); before = audit(clerk, claim)
    status, error, _ = command(clerk, claim, "DOCUMENTS_VERIFIED")
    require(status == 422 and error.get("code") == "GUARD_FAILED", f"expected guard 422; got HTTP {status}")
    require("All required documents must be present and valid" in error.get("message", ""), "guard message was not preserved")
    rows = assert_unchanged(clerk, claim, before)
    return claim, rows, {"httpStatus": status, "error": error}

run_case("missing-documents-guard", missing_documents_guard)

def information_request_limit():
    claim = documents_verified(create(human("clerk"))); claim = start_assessment(claim)
    for index in (1, 2, 3): claim = info_request(claim, index)
    assessor = human("assessor"); claim = patch_ok(assessor, claim, {"missingInfoDescription": "fourth request"}, "fourth request details")
    before = audit(assessor, claim)
    status, error, _ = command(assessor, claim, "PENDING_INFO")
    exact = "Maximum information requests exceeded — escalate to team lead"
    require(status == 422 and error.get("code") == "INFORMATION_REQUEST_LIMIT", f"expected limit 422; got HTTP {status}")
    require(error.get("message") == exact, "information request limit message differs")
    rows = assert_unchanged(assessor, claim, before)
    require(claim["informationRequests"] == 3, "failed fourth request changed request count")
    return claim, rows, {"httpStatus": status, "error": error, "informationRequests": claim["informationRequests"]}

run_case("information-request-limit", information_request_limit)

def stale_version():
    clerk = human("clerk"); claim = create(clerk); old_version = claim["version"]
    claim = patch_ok(clerk, claim, {"documents": {"identity": True}}, "documents")
    before = audit(clerk, claim)
    body = {"target": "DOCUMENTS_VERIFIED", "expectedVersion": old_version,
            "idempotencyKey": str(uuid.uuid4()), "notes": "deliberately stale"}
    status, error, _ = clerk.request("POST", f"/api/claims/{claim['id']}/transitions", body)
    require(status == 409 and error.get("code") == "CONFLICT", f"expected stale-version 409; got HTTP {status}")
    rows = assert_unchanged(clerk, claim, before)
    return claim, rows, {"httpStatus": status, "error": error, "submittedVersion": old_version, "currentVersion": claim["version"]}

run_case("stale-version", stale_version)

def idempotent_replay():
    clerk = human("clerk"); claim = create(clerk)
    claim = patch_ok(clerk, claim, {"documents": {"identity": True, "receipt": True}}, "documents")
    key = str(uuid.uuid4())
    body = {"target": "DOCUMENTS_VERIFIED", "expectedVersion": claim["version"], "idempotencyKey": key, "notes": "same request"}
    status1, first, _ = clerk.request("POST", f"/api/claims/{claim['id']}/transitions", body)
    require(status1 == 200 and first.get("state") == "DOCUMENTS_VERIFIED", "first idempotent command failed")
    after_first = audit(clerk, first)
    status2, replay, _ = clerk.request("POST", f"/api/claims/{claim['id']}/transitions", body)
    require(status2 == 200 and replay == first, "replay did not return the original outcome")
    rows = audit(clerk, replay)
    require(rows == after_first, "replay appended another audit record")
    require(rows[-1]["sequence"] == first["version"], "audit/version evidence differs")
    return replay, rows, {"firstStatus": status1, "replayStatus": status2, "sameOutcome": True, "auditCount": len(rows)}

run_case("idempotent-replay", idempotent_replay)

def cross_member_read():
    claim = create(human("clerk"), member="member-002")
    member = human("member-001")
    before = audit(human("clerk"), claim)
    status, error, _ = member.request("GET", f"/api/claims/{claim['id']}")
    require(status == 403 and error.get("code") == "FORBIDDEN_ROLE", f"expected cross-member 403; got HTTP {status}")
    rows = assert_unchanged(human("clerk"), claim, before)
    return claim, rows, {"httpStatus": status, "error": error, "requester": "member-001", "claimMember": "member-002"}

run_case("cross-member-read", cross_member_read)

def spoofed_actor_body():
    clerk = human("clerk"); claim = create(clerk); before = audit(clerk, claim)
    body = {"target": "DOCUMENTS_VERIFIED", "expectedVersion": claim["version"],
            "idempotencyKey": str(uuid.uuid4()), "notes": "spoof probe", "actor": "system"}
    status, error, _ = clerk.request("POST", f"/api/claims/{claim['id']}/transitions", body)
    require(status == 400 and error.get("code") == "INVALID_REQUEST", f"expected spoof-body 400; got HTTP {status}")
    rows = assert_unchanged(clerk, claim, before)
    return claim, rows, {"httpStatus": status, "error": error}

run_case("spoofed-actor-body", spoofed_actor_body)

def system_and_human_boundaries():
    clerk = human("clerk"); claim = create(clerk); before = audit(clerk, claim)
    sys = system()
    status1, error1, _ = command(sys, claim, "DOCUMENTS_VERIFIED")
    require(status1 == 403 and error1.get("code") == "FORBIDDEN_ROLE", f"system used public path: expected 403, got {status1}")
    status2, error2, _ = clerk.request("POST", f"/api/system/claims/{claim['id']}/close", {
        "expectedVersion": claim["version"], "idempotencyKey": str(uuid.uuid4()), "notes": "human probe",
    })
    require(status2 == 403 and error2.get("code") == "FORBIDDEN", f"human used protected endpoint: expected 403, got {status2}")
    rows = assert_unchanged(clerk, claim, before)
    return claim, rows, {"systemPublicStatus": status1, "systemPublicError": error1,
                         "humanProtectedStatus": status2, "humanProtectedError": error2}

run_case("system-and-human-boundaries", system_and_human_boundaries)

def csrf_required():
    clerk = human("clerk"); claim = create(clerk); before = audit(clerk, claim)
    status, error, _ = clerk.request("PATCH", f"/api/claims/{claim['id']}/facts", {
        "expectedVersion": claim["version"], "updates": {"documents": {"identity": True}},
    }, include_csrf=False)
    require(status == 403 and error.get("code") == "FORBIDDEN", f"expected CSRF 403; got HTTP {status}")
    rows = assert_unchanged(clerk, claim, before)
    return claim, rows, {"httpStatus": status, "error": error}

run_case("csrf-required", csrf_required)

def unauthenticated_read():
    clerk = human("clerk"); claim = create(clerk); before = audit(clerk, claim)
    anonymous = HttpClient()
    status, error, _ = anonymous.request("GET", f"/api/claims/{claim['id']}", include_auth=False)
    require(status == 401 and error.get("code") == "UNAUTHENTICATED", f"expected unauthenticated 401; got HTTP {status}")
    rows = assert_unchanged(clerk, claim, before)
    return claim, rows, {"httpStatus": status, "error": error}

run_case("unauthenticated-read", unauthenticated_read)
print(f"PASS all {len(cases)} scenario cases; JSON claim/audit exports: {output_dir}")
PY
