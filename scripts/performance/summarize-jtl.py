#!/usr/bin/env python3
import csv,json,sys
from collections import defaultdict
from pathlib import Path
p=Path(sys.argv[1]); rows=list(csv.DictReader(p.open(newline='')))
valid=[r for r in rows if r.get('timeStamp') and r.get('elapsed')]
lat=sorted(int(r['elapsed']) for r in valid)
def pct(x): return lat[min(len(lat)-1,int((len(lat)-1)*x+0.999999))] if lat else None
start=min((int(r['timeStamp']) for r in valid),default=0); end=max((int(r['timeStamp'])+int(r['elapsed']) for r in valid),default=0)
errors=sum(r.get('success','').lower()!='true' for r in valid); secs=max((end-start)/1000,1e-9)
by=defaultdict(lambda:{'samples':0,'errors':0})
for r in valid:
 x=by[r.get('label','')];x['samples']+=1;x['errors']+=r.get('success','').lower()!='true'
print(json.dumps({'samples':len(valid),'errors':errors,'error_rate':errors/len(valid) if valid else None,'duration_seconds':secs,'rps':len(valid)/secs,'throughput_requests_per_second':len(valid)/secs,'latency_ms':{'p50':pct(.50),'p95':pct(.95),'p99':pct(.99)},'by_sampler':dict(by)},indent=2))
