#!/usr/bin/env python3
"""Capture container CPU/memory and PostgreSQL/outbox snapshots during a load profile."""
import json, subprocess, sys, time
from datetime import datetime, timezone
out=sys.argv[1]
while True:
    stamp=datetime.now(timezone.utc).isoformat()
    stats=subprocess.run(['docker','stats','--no-stream','--format','{{json .}}'],capture_output=True,text=True)
    containers=[]
    if stats.returncode==0:
        for line in stats.stdout.splitlines():
            try:
                item=json.loads(line)
                containers.append({k:item.get(k) for k in ('Name','CPUPerc','MemUsage','MemPerc','NetIO','BlockIO','PIDs')})
            except json.JSONDecodeError: pass
    query="SELECT json_build_object('active_connections',(SELECT count(*) FROM pg_stat_activity WHERE datname=current_database()),'transactions_committed',xact_commit,'transactions_rolled_back',xact_rollback,'outbox_pending',(SELECT count(*) FROM local_outbox WHERE delivered_at IS NULL),'outbox_failed',(SELECT count(*) FROM local_outbox WHERE last_error IS NOT NULL),'outbox_delivered',(SELECT count(*) FROM local_outbox WHERE delivered_at IS NOT NULL),'outbox_oldest_pending_seconds',(SELECT COALESCE(EXTRACT(EPOCH FROM(clock_timestamp()-min(created_at))),0) FROM local_outbox WHERE delivered_at IS NULL)) FROM pg_stat_database WHERE datname=current_database();"
    db=subprocess.run(['docker','compose','--env-file','.env','-f','docker/compose.yml','exec','-T','database','psql','-U','claims_bootstrap','-d','claims','-Atc',query],capture_output=True,text=True)
    try: db_data=json.loads(db.stdout.strip()) if db.returncode==0 else {'status':'BLOCKED','returncode':db.returncode}
    except json.JSONDecodeError: db_data={'status':'PARSE_ERROR'}
    with open(out,'a') as f: f.write(json.dumps({'timestamp_utc':stamp,'containers':containers,'database':db_data})+'\n')
    time.sleep(5)
