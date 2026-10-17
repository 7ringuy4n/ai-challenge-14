# JMeter provisioning and profiles

- Installed distribution: Apache JMeter 5.6.3 at `/home/tringuyen/.local/jmeter` (present before this task; no root/network install required).
- Runtime: Java 21 at `/home/tringuyen/.local/java/jdk-21`.
- Reproducible invocation: `env JAVA_HOME=/home/tringuyen/.local/java/jdk-21 PATH=/home/tringuyen/.local/java/jdk-21/bin:/home/tringuyen/.local/jmeter/bin:$PATH /home/tringuyen/.local/jmeter/bin/jmeter -n -t performance/jmeter/profiles/<profile>.jmx -q performance/jmeter/profiles/<profile>.properties -l reports/performance/<profile>.jtl`.
- Seven executable profiles are generated for baseline, normal, peak, stress, spike, soak and concurrent claims. Configured threads/ramp/duration are in the corresponding `.properties` file. Defaults are planning settings, not approved capacity thresholds.
- `scripts/performance/run-profile.sh <profile>` also emits a JTL and HTML report and summarizes sample count, RPS, error rate and P50/P95/P99. It reads credentials from the local ignored `.env` through a temporary mode-0600 properties file and removes that file on exit.
- JMX parser/plan construction check: PASS (JMeter created the tree). Network execution: BLOCKED in default sandbox: JMeter cannot bind its local UDP status port (`Failed to create UDP port`) and direct service loopback is denied. No HTTP samples or performance result are claimed.
- Resource, DB, and outbox metrics collection still needs a permitted live run; this package does not claim those measurements yet.
