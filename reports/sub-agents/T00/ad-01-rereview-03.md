# AD-01 scoped rereview 03

**Verdict: PASS**

The previously reported ambiguities are resolved in `docs/challenge-14/FACTS-AND-PERSISTENCE.md`: stable keyed document identity and grammar; ID-and-field merge with omitted IDs/fields preserved; no recursive or list-index merge; null entry/field rejection; no deletion/removal/rename; member-only `present` edits on existing IDs; and explicit member prohibition on roster, `required`, or `valid` edits. Clerk add/edit authority is specified.

The lifecycle conflict from rereview-02 is explicit at line 20: `required` changes only in `SUBMITTED` and becomes immutable afterward, while clerk `present`/`valid` changes are allowed in both `SUBMITTED` and `PENDING_INFO`. No actionable ambiguity remains in the reviewed files. `CONTRACTS.md` links the addendum at lines 37–42; `application.yml` configures appeal period with `P30D` default at lines 19–22.

Read-only inspection only; no tests/builds run. Reviewer: `/root/ad01_rereview03` (independent, gpt-6-luna/medium selected; actual host model NOT_CAPTURED).
