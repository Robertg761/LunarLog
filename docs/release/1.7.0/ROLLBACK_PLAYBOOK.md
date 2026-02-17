# LunarLog 1.7.0 Rollback Playbook

## Trigger criteria (execute rollback if any true)
- Data integrity issues:
  - Period ranges become invalid (`endDate < startDate`).
  - Log edits appear to lose data or mismatch day aggregates.
- Security/privacy issues:
  - App Lock bypass without authentication.
  - Users unable to unlock despite valid system credentials.
- Stability issues:
  - Significant crash/ANR spike vs prior release.
- Severe UX regression:
  - Core flows blocked (cannot save period/log, onboarding dead-end).

## Immediate actions
1. Halt rollout progression in store console.
2. Freeze new release promotion.
3. Notify team channel with:
   - Trigger observed.
   - Impact scope.
   - First-known app version and device/OS pattern.
4. Triage:
   - Reproduce on latest release artifact.
   - Identify if issue is hotfixable.

## Rollback options
1. Store rollback:
- Roll staged rollout percentage back to 0% or stop rollout.
- Promote last known good version (1.6.1) if channel policy allows.

2. Hotfix forward:
- If fix is low-risk and fast, prepare `1.7.1` hotfix branch.
- Re-run Stage 6 verification commands.
- Deploy with smaller staged rollout.

## Data safety protocol
- Do not ship destructive migrations as emergency mitigation.
- Prefer app-level behavior guards and server/store rollout controls.
- Preserve local data and avoid automatic reset actions.

## Verification after rollback/hotfix
- Re-run smoke checklist in `QA_SMOKE_CHECKLIST.md`.
- Confirm:
  - App Lock behavior.
  - Period start/end/edit correctness.
  - Log save/edit consistency.
  - Notification settings respect user toggles.

