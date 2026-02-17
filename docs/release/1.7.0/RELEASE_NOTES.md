# LunarLog 1.7.0 Release Notes

Release date: 2026-02-17

## Highlights
- Stronger period domain safety with explicit, validated period operations.
- Transactional log persistence to keep granular entries and daily aggregates consistent.
- Harder privacy posture for App Lock (no insecure auto-unlock fallback).
- Onboarding completion now waits for successful persistence.
- Better accessibility and usability in calendar and logging flows.
- Reactive analysis updates and safer full-text search query handling.

## User-visible improvements
- New cycle prediction alert toggle in Settings.
- App Lock timeout options in Settings (`Now`, `30s`, `2m`).
- Calendar now includes a quick `Today` action.
- Improved TalkBack descriptions for calendar day states.
- Confirmation prompt before ending an active period from Quick Log.
- Period logging save action now clearly communicates when date selection is required.

## Reliability and correctness
- Replaced generic period toggling with explicit domain actions:
  - `startPeriod`
  - `endOngoingPeriod`
  - `resumePeriodEndedOn`
  - `setPeriodDay`
  - `setPeriodRange`
  - `updateCycleDates`
- Added stronger cycle invariant checks to prevent invalid ranges and overlaps.
- Log writes now use transactional entry-based persistence, with aggregate rebuild from entries.

## Developer notes
- Version bumped to `1.7.0` (`versionCode 12`).
- Stage 6 operational artifacts are included under `docs/release/1.7.0/`.

