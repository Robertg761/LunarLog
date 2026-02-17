# LunarLog 1.7.0 Staged Rollout Plan

Release date target: 2026-02-17
Owner: Mobile team

## Rollout phases
1. Internal validation (0%)
- Build: Release APK from `main`.
- Audience: Internal testers only.
- Duration: 4-8 hours.
- Exit criteria:
  - App launches and navigates all primary tabs.
  - App Lock, period logging, onboarding, settings toggles, and update checks behave correctly.
  - No blocker crashes in manual smoke.

2. Limited production (10%)
- Audience: 10% staged rollout in store channel.
- Duration: 24 hours.
- Observe:
  - Crash rate trend.
  - ANR trend.
  - User support signals (app lock failures, log-save failures, period-state confusion).
- Exit criteria:
  - No severe user-impact regressions.
  - No spike in stability issues relative to last release.

3. Broad rollout (50%)
- Audience: 50% staged rollout.
- Duration: 24 hours.
- Exit criteria:
  - Stability metrics remain within expected band.
  - No high-severity functional regressions.

4. Full rollout (100%)
- Audience: all users.
- Duration: complete when prior gates pass.

## Monitoring focus (1.7.0-specific)
- App lock accessibility failures or lock-loop behavior.
- Incorrect period state updates for historical dates.
- Missing/incorrect daily logs after edits.
- Calendar interaction accessibility regressions.
- Notification opt-in/out behavior mismatch.

## Stop conditions
- Any high-severity data integrity regression.
- Any security/privacy regression in lock behavior.
- Crash or ANR spike beyond release baseline.
- Wide user reports of log loss or period corruption.

## Communication
- Publish release notes from `CHANGELOG.md`.
- Keep internal incident channel active during first 48h.

