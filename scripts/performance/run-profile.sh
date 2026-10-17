#!/usr/bin/env bash
set -euo pipefail
umask 077
profile=${1:?usage: scripts/performance/run-profile.sh PROFILE [--no-setup]}
case "$profile" in baseline|normal|peak|stress|spike|soak|concurrent-claims) ;; *) echo "Unknown profile: $profile" >&2; exit 2;; esac
root=$(cd "$(dirname "$0")/../.." && pwd)
cd "$root"
: "${JAVA_HOME:=/home/tringuyen/.local/java/jdk-21}"
: "${JMETER_HOME:=/home/tringuyen/.local/jmeter}"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
mkdir -p reports/performance
out="reports/performance/${profile}-$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$out"
started_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)
threads=${JMETER_THREADS:-$(awk -F= '$1=="threads"{print $2} ' performance/jmeter/profiles/${profile}.properties)}
ramp=${JMETER_RAMP:-$(awk -F= '$1=="ramp"{print $2} ' performance/jmeter/profiles/${profile}.properties)}
duration=${JMETER_DURATION:-$(awk -F= '$1=="duration"{print $2} ' performance/jmeter/profiles/${profile}.properties)}
profile_sha256=$(sha256sum "performance/jmeter/profiles/${profile}.jmx" | awk '{print $1}')
java_version=$("$JAVA_HOME/bin/java" -version 2>&1 | sed -n 's/.*version "\([^"]*\)".*/\1/p' | head -n1)
if [[ -z "$(git status --porcelain)" ]]; then git_state=CLEAN; source_hash=$(git rev-parse HEAD); else git_state=DIRTY; source_hash=NOT_CAPTURED; fi
printf '{"jmeter":"Apache JMeter 5.6.3","java":"%s","profile":"%s","host":"127.0.0.1","port":8080,"threads":%s,"ramp_seconds":%s,"duration_seconds":%s,"started_utc":"%s","git_commit":"%s","git_worktree_state":"%s","exact_working_tree_source_hash":"%s","profile_sha256":"%s","approved_targets":{"p95_ms":500,"p99_ms":1000,"error_rate_max":0.01}}\n' "$java_version" "$profile" "$threads" "$ramp" "$duration" "$started_utc" "$(git rev-parse HEAD)" "$git_state" "$source_hash" "$profile_sha256" > "$out/run.json"
python3 scripts/performance/monitor.py "$out/system-metrics.jsonl" & monitor_pid=$!
trap 'kill "$monitor_pid" 2>/dev/null || true; wait "$monitor_pid" 2>/dev/null || true; rm -f "$out/credentials.properties"' EXIT
# Avoid retaining credentials or putting them on the process command line.
CLAIMS_ENV_FILE=${CLAIMS_ENV_FILE:-.env} python3 scripts/performance/read-secret.py > "$out/credentials.properties"
chmod 600 "$out/credentials.properties"
"$JMETER_HOME/bin/jmeter" -n -t "performance/jmeter/profiles/${profile}.jmx" \
  -q "performance/jmeter/profiles/${profile}.properties" \
  -q "$out/credentials.properties" ${JMETER_THREADS:+-Jthreads="$JMETER_THREADS"} ${JMETER_RAMP:+-Jramp="$JMETER_RAMP"} ${JMETER_DURATION:+-Jduration="$JMETER_DURATION"} -Jjmeterengine.nongui.port=0 -Jjmeterengine.nongui.maxport=0 \
  -l "$out/results.jtl" -j "$out/jmeter.log" -e -o "$out/html-report"
python3 scripts/performance/summarize-jtl.py "$out/results.jtl" > "$out/summary.json"
kill "$monitor_pid" 2>/dev/null || true
wait "$monitor_pid" 2>/dev/null || true
trap - EXIT
rm -f "$out/credentials.properties"
gzip -9 "$out/results.jtl" "$out/jmeter.log"
tar -czf "$out/html-report.tar.gz" -C "$out" html-report
rm -rf "$out/html-report"
echo "Results: $out"
