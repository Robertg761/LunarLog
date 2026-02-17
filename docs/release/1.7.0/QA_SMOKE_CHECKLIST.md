# LunarLog 1.7.0 QA Smoke Checklist

## Setup
- Install release build (`1.7.0`, versionCode `12`).
- Use at least:
  - One device with biometric/device credential enabled.
  - One device profile without biometric capability (if available).

## Core flow checks
1. Onboarding
- First-run flow completes only after successful save.
- Invalid/failing save surfaces clear error message.

2. App Lock
- Enabling App Lock requires successful device auth.
- App re-locks based on selected timeout:
  - `Now`
  - `30s`
  - `2m`
- No silent unlock fallback when device auth unavailable.

3. Period domain operations
- Start period on today from Home Quick Log.
- End ongoing period with confirmation.
- Resume period ended today.
- Mark/unmark a historical day in day-detail screens.
- Edit period dates in Period Detail without creating overlap or invalid range.

4. Daily logging consistency
- Add/edit/delete log entries for a day.
- Re-open day and confirm values persist and display consistently.
- Verify no missing data after repeated edits.

5. Calendar and accessibility
- Calendar `Today` button returns to current month.
- TalkBack reads day states (today/period/fertile/predicted/ovulation) meaningfully.

6. Settings notifications
- Toggle cycle prediction alerts on/off.
- Toggle daily period reminders on/off and change reminder time.
- Confirm behavior persists across app restart.

7. Analysis
- Add new logs and verify analysis trends update without app restart.

## Pass criteria
- No blockers in above flows.
- No data loss or invalid cycle states observed.
- No critical crashes in smoke execution.

