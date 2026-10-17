#!/usr/bin/env python3
import os
from pathlib import Path
path=Path(os.environ.get('CLAIMS_ENV_FILE','.env'))
values={}
for line in path.read_text().splitlines():
    if '=' in line and not line.lstrip().startswith('#'):
        k,v=line.split('=',1); values[k.strip()]=v.strip()
secret=values.get('CLAIMS_DEMO_PASSWORD','')
if not secret or secret.startswith('CHANGE_ME_'):
    raise SystemExit('CLAIMS_DEMO_PASSWORD is not configured')
# Keep secrets out of command arguments and logs; the caller creates a mode-0600 temporary properties file.
import base64
for user in ('member-demo','clerk','lead','assessor','finance'):
    print(f"claims.auth.{user}=Basic " + base64.b64encode((user+':'+secret).encode()).decode())
print("claims.auth=Basic " + base64.b64encode(('member-demo:'+secret).encode()).decode())
