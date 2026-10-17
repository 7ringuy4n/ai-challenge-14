#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd); cd "$root"
export PATH="$root/node_modules/.bin:$PATH"
if ! command -v newman >/dev/null 2>&1; then
  echo 'Newman is unavailable. Install the pinned dev dependency (npm ci) before running this collection.' >&2
  exit 127
fi
umask 077
tmp_env=$(mktemp)
trap 'rm -f "$tmp_env"' EXIT
python3 - "$tmp_env" <<'PY'
import json,sys
from pathlib import Path
values={}
for line in Path('.env').read_text().splitlines():
 if '=' in line and not line.lstrip().startswith('#'):
  k,v=line.split('=',1);values[k.strip()]=v.strip()
Path(sys.argv[1]).write_text(json.dumps({'id':'claims-local-runtime','name':'Claims local runtime','values':[{'key':'baseUrl','value':'http://127.0.0.1:'+values.get('SERVER_PORT','8080'),'enabled':True},{'key':'postmanUsername','value':'member-demo','enabled':True},{'key':'demoPassword','value':values['CLAIMS_DEMO_PASSWORD'],'enabled':True}]}))
PY
newman run postman/claims-workflow.postman_collection.json -e "$tmp_env" --reporters cli
