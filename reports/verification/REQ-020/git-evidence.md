# REQ-020 Git decision and merge evidence

## Pre-commit verified state

- Current work branch: `develop`
- Existing main baseline: `521cb1427f910ebbd01129cc1a950c55b899ee87`
- Existing develop head before REQ-020 evidence commit: `8f6f5ad` (`docs: checkpoint REQ-017 remaining blockers`)
- Application source revision used for runtime, JMeter, and regression evidence: `0435e1b6f82e2c01b9b0068151e19eeb9d79cdd2`
- `git merge-base develop main` returned the existing main commit above; develop contains it. No branch reset, force operation, or remote push was performed.
- `origin` is configured, but the inspected local branch list had no `origin/*` tracking references; no fetch or push is part of this task.
- All staged REQ-020 changes are acceptance, audit, test and JMeter evidence/documentation. No application source files or build configuration changed.

The owner-authorized next steps are to commit the reviewed evidence on `develop`, merge `develop` into the existing `main` with a normal local merge, verify the resulting main head and working tree, and record the resulting hashes below. No push is authorized.

## Commit and merge result

- Evidence commit on `develop`: `45f447022dfbeee6d3d0d4a6609dd31f1b0a31d5`; checkpoint commit `730ca6fd6569a451a286b189f9dd69893d26c197` (parent `45f447022dfbeee6d3d0d4a6609dd31f1b0a31d5`). Before merge, `main` was `521cb1427f910ebbd01129cc1a950c55b899ee87`; `git merge-base develop main` returned that baseline. Working tree was clean.
- First attempted command: `git switch main && git merge --no-ff develop -m "merge: accept claims workflow with local performance exception"`; exit 128 after branch switch because committer identity was unset. No merge commit or configuration change resulted.
- Successful command: `git -c user.name='7ringuy4n' -c user.email='132858234+7ringuy4n@users.noreply.github.com' merge --no-ff develop -m "merge: accept claims workflow with local performance exception"`; exit 0, normal `ort` merge. Identity override applied only to this command.
- Merge commit: `18f97aa9c8296fdc107a02192aa4e8e62dd45d78` (commit time `2026-10-17T14:14:57+07:00`). Parent 1 is prior `main` baseline `521cb1427f910ebbd01129cc1a950c55b899ee87`; parent 2 is accepted `develop` head `730ca6fd6569a451a286b189f9dd69893d26c197`.
- `git merge-base main develop` after merge returned the complete develop head `730ca6fd6569a451a286b189f9dd69893d26c197`.
- `git branch -avv` showed local `main` at the merge and local `develop` at parent 2; no remote-tracking branches were present. `git remote -v` showed origin configured, but no remote operation was performed.
- A clean-tree check passed immediately after merge. The post-merge audit/evidence package is committed on `main` at `d0fd9dd` (`docs: record accepted local main merge`). It records the accepted policy, merge commit/parents, and latest audit. This final closeout commit records the package hash in the acceptance summary. No push.
