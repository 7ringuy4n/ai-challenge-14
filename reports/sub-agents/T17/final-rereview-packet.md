# T17 final focused rereview — T13 exception criterion reconciled

**Review type:** read-only final acceptance decision review. Do not edit files, run tests, commit, merge, or accept the project.
**Requirement:** REQ-20261017-020.

Review 01 confirmed hardware and proof-of-work evidence but identified T13/T16 running. Those tasks were reconciled as accepted with scoped evidence. Review 02 then found T13's criterion still required every original limit to pass. That second finding is preserved verbatim at `reports/sub-agents/T17/review-02.md`.

T13's current `acceptance` list and notes in `rules/workflow/tasks.json` now state: preserve all original measurements and the P95 564ms / monitored P95 638ms FAIL; only the REQ-020 local `HARDWARE_CONSTRAINED` exception can pass under the revised policy after evidence review; do not claim P95<=500ms passed; production-scale performance remains UNVERIFIED. T16 retains proof-of-work PASS with raw completeness PARTIAL/NOT_CAPTURED. T13 and T16 are accepted; T17 remains running. The workflow validator passes.

Please inspect current `rules/workflow/tasks.json`, `reports/acceptance/FINAL-ACCEPTANCE.md`, `reports/verification/REQ-017-current.json`, `reports/verification/REQ-017-requirement-matrix.json`, and reviews 01/02. Confirm whether T13's status and criterion are now unambiguous, original FAIL and raw PARTIAL remain accurate, the revised mandatory gates pass, and local conditional merge is justified after evidence is committed. Return `PASS`, `REWORK`, or `BLOCKED`, with any critical/high findings first. No production performance claim. Read-only; do not edit files or run tests. Raw host payloads remain `NOT_CAPTURED`.
