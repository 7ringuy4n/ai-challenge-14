#!/usr/bin/env python3
"""Exercise the authenticated HTTP claim lifecycle without printing credentials or payloads."""

from __future__ import annotations

import base64
import http.cookiejar
import json
import os
import sys
import time
import urllib.error
import urllib.request
import uuid
from pathlib import Path


def read_env(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


class Api:
    def __init__(self, base: str, passwords: dict[str, str]):
        self.base = base.rstrip("/")
        self.passwords = passwords
        self.cookies = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookies))

    def call(self, username: str | None, method: str, path: str, *, body=None, key=None, csrf=None):
        headers = {"Accept": "application/json"}
        if body is not None:
            headers["Content-Type"] = "application/json"
        if key:
            headers["Idempotency-Key"] = key
        if csrf:
            headers[csrf["csrfHeaderName"]] = csrf["csrfToken"]
        if username:
            auth = base64.b64encode(f"{username}:{self.passwords[username]}".encode()).decode()
            headers["Authorization"] = f"Basic {auth}"
        data = json.dumps(body, separators=(",", ":")).encode() if body is not None else None
        request = urllib.request.Request(self.base + path, data=data, headers=headers, method=method)
        try:
            with self.opener.open(request, timeout=10) as response:
                content = response.read()
                return response.status, json.loads(content) if content else None, response.headers
        except urllib.error.HTTPError as error:
            content = error.read()
            try:
                parsed = json.loads(content) if content else None
            except json.JSONDecodeError:
                parsed = None
            return error.code, parsed, error.headers


def main() -> int:
    env_path = Path(os.environ.get("CLAIMS_ENV_FILE", ".env"))
    if not env_path.is_file():
        print("Missing .env; run bash scripts/setup.sh first.", file=sys.stderr)
        return 2
    env = read_env(env_path)
    required = ("CLAIMS_DEMO_PASSWORD", "CLAIMS_SYSTEM_PASSWORD")
    if any(not env.get(name) or env[name].startswith("CHANGE_ME_") for name in required):
        print("Configured credentials are missing or still placeholders.", file=sys.stderr)
        return 2

    port = env.get("SERVER_PORT", "8080")
    api = Api(os.environ.get("CLAIMS_API_BASE", f"http://127.0.0.1:{port}"), {
        "clerk": env["CLAIMS_DEMO_PASSWORD"], "lead": env["CLAIMS_DEMO_PASSWORD"],
        "assessor": env["CLAIMS_DEMO_PASSWORD"], "finance": env["CLAIMS_DEMO_PASSWORD"],
        "member-demo": env["CLAIMS_DEMO_PASSWORD"], "system": env["CLAIMS_SYSTEM_PASSWORD"],
    })
    total = 20
    case = 0
    run_tag = uuid.uuid4().hex

    def check(name: str, actual: int, expected: int) -> None:
        nonlocal case
        case += 1
        print(f"running test case {case}/{total}: {name}")
        if actual != expected:
            raise RuntimeError(f"{name}: expected HTTP {expected}, received HTTP {actual}")

    try:
        status, _, _ = api.call(None, "GET", "/api/v1/claims?page=0&size=1")
        check("unauthenticated claims request is rejected", status, 401)

        status, csrf, _ = api.call("clerk", "GET", "/api/v1/session")
        check("authenticated session supplies CSRF token", status, 200)
        if not all(csrf.get(field) for field in ("csrfHeaderName", "csrfParameterName", "csrfToken")):
            raise RuntimeError("session did not return the expected CSRF contract")

        status, created, headers = api.call("member-demo", "POST", "/api/v1/claims", key=f"e2e-{run_tag}-create",
            csrf=csrf, body={"policyLimit": 500, "currency": "USD", "facts": {"requestedAmount": 125.50}})
        check("member creates claim", status, 201)
        claim_id = created["id"]
        if headers.get("Location", "").endswith(claim_id) is False:
            raise RuntimeError("create response Location does not identify the returned claim")

        status, replay, _ = api.call("member-demo", "POST", "/api/v1/claims", key=f"e2e-{run_tag}-create",
            csrf=csrf, body={"policyLimit": 500, "currency": "USD", "facts": {"requestedAmount": 125.50}})
        check("create idempotency replays the original claim", status, 201)
        if replay["id"] != claim_id:
            raise RuntimeError("idempotent create returned a different claim id")

        status, _, _ = api.call("member-demo", "GET", f"/api/v1/claims/{claim_id}")
        check("claim owner can read claim", status, 200)
        status, _, _ = api.call("member-demo", "GET", f"/api/v1/claims/{claim_id}/audit")
        check("claim owner can read audit", status, 200)

        status, _, _ = api.call("member-demo", "PUT", f"/api/v1/claims/{claim_id}/facts",
            key=f"e2e-{run_tag}-no-csrf", body={"expectedVersion": 0, "facts": {"documents": {}}})
        check("unsafe request without CSRF token is rejected", status, 403)

        status, _, _ = api.call("member-demo", "PUT", f"/api/v1/claims/{claim_id}/facts",
            key=f"e2e-{run_tag}-member-write-denied", csrf=csrf,
            body={"expectedVersion": 0, "facts": {"documents": {"invoice": {"present": True}}}})
        check("member cannot edit submitted document roster", status, 403)

        status, _, _ = api.call("clerk", "PUT", f"/api/v1/claims/{claim_id}/facts",
            key=f"e2e-{run_tag}-documents", csrf=csrf,
            body={"expectedVersion": 0, "facts": {"documents": {"invoice": {"required": True, "present": True, "valid": True}}}})
        check("document clerk records complete document", status, 200)

        status, transitioned, _ = api.call("clerk", "POST", f"/api/v1/claims/{claim_id}/transitions",
            key=f"e2e-{run_tag}-verify", csrf=csrf,
            body={"targetState": "DOCUMENTS_VERIFIED", "expectedVersion": 1, "reason": "Documents verified", "facts": {}})
        check("document clerk verifies claim", status, 200)
        if transitioned["claim"]["version"] != 2:
            raise RuntimeError("document verification did not advance the claim version")

        status, _, _ = api.call("lead", "PUT", f"/api/v1/claims/{claim_id}/facts",
            key=f"e2e-{run_tag}-assign", csrf=csrf,
            body={"expectedVersion": 2, "facts": {"assessorId": "assessor"}})
        check("team lead assigns assessor", status, 200)

        status, started, _ = api.call("lead", "POST", f"/api/v1/claims/{claim_id}/transitions",
            key=f"e2e-{run_tag}-start-assessment", csrf=csrf,
            body={"targetState": "UNDER_ASSESSMENT", "expectedVersion": 3, "reason": "Assigned", "facts": {}})
        check("team lead starts assessment", status, 200)

        status, _, _ = api.call("assessor", "PUT", f"/api/v1/claims/{claim_id}/facts",
            key=f"e2e-{run_tag}-report", csrf=csrf,
            body={"expectedVersion": 4, "facts": {"assessmentReport": {"complete": True}}})
        check("assigned assessor submits report", status, 200)

        status, approved, _ = api.call("assessor", "POST", f"/api/v1/claims/{claim_id}/transitions",
            key=f"e2e-{run_tag}-approve", csrf=csrf,
            body={"targetState": "APPROVED", "expectedVersion": 5, "reason": "Covered", "facts": {}})
        check("assessor approves and persists payment request", status, 200)

        status, _, _ = api.call("finance", "POST", f"/api/v1/claims/{claim_id}/transitions",
            key=f"e2e-{run_tag}-initiate-payment", csrf=csrf,
            body={"targetState": "PAYMENT_INITIATED", "expectedVersion": 6, "reason": "Dispatch", "facts": {}})
        check("finance dispatches payment via outbox", status, 200)

        status, _, _ = api.call("assessor", "POST", f"/api/v1/claims/{claim_id}/system-transitions",
            key=f"e2e-{run_tag}-forbidden-system", csrf=csrf,
            body={"targetState": "CLOSED", "expectedVersion": 7, "reason": "forbidden", "facts": {}})
        check("human identity cannot invoke system transition", status, 403)

        status, options, _ = api.call("finance", "GET", f"/api/v1/claims/{claim_id}/transitions")
        check("finance can inspect transition options", status, 200)
        close_available = next((option.get("available", False) for option in options
                                if option.get("transitionId") == "close-paid-claim"), False)
        attempts = 0
        while not close_available and attempts < 30:
            time.sleep(1)
            status, options, _ = api.call("finance", "GET", f"/api/v1/claims/{claim_id}/transitions")
            if status != 200:
                raise RuntimeError(f"payment confirmation poll returned HTTP {status}")
            close_available = next((option.get("available", False) for option in options
                                    if option.get("transitionId") == "close-paid-claim"), False)
            attempts += 1
        if not close_available:
            raise RuntimeError("payment outbox did not confirm within 30 seconds")
        status, closed, _ = api.call("finance", "POST", f"/api/v1/claims/{claim_id}/transitions",
            key=f"e2e-{run_tag}-close-paid", csrf=csrf,
            body={"targetState": "CLOSED", "expectedVersion": 7, "reason": "Payment confirmed", "facts": {}})
        check("finance closes claim after outbox confirmation", status, 200)
        if closed["claim"]["state"] != "CLOSED":
            raise RuntimeError("claim did not reach CLOSED")

        status, claim_list, _ = api.call("member-demo", "GET", "/api/v1/claims?page=0&size=20")
        check("member list endpoint returns visible claims", status, 200)
        if not any(item["id"] == claim_id for item in claim_list["items"]):
            raise RuntimeError("created claim is absent from the member-visible list")

        status, audit, _ = api.call("member-demo", "GET", f"/api/v1/claims/{claim_id}/audit")
        check("complete lifecycle exposes claim transition audit", status, 200)
        if len(audit["items"]) < 5:
            raise RuntimeError("expected transition audit records were not persisted")
    except (OSError, ValueError, KeyError, TypeError, RuntimeError) as error:
        print(f"Scenario failure after {case}/{total} cases: {error}", file=sys.stderr)
        return 1
    print(f"Completed {case}/{total} end-to-end API scenarios.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
