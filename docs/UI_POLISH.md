# UI polish

This pass preserves the existing navigation, dose-event model and reminder scheduling behavior.

- **Daily medications:** separate cards, planned-dose progress and remaining counts, an explicit as-needed explanation, individually labeled dose times, and a full-width log action. The next reminder label only shows a future reminder on today's scheduled medications that is still eligible given the logged dose count. Remove actions identify the medication and time for accessibility.
- **Medication editor:** grouped details, schedule and reminders; full-width inputs; numeric keyboard and visible dose validation; reminder-count guidance; a single labeled switch target; wrapping reminder actions; and an emphasized save button. As-needed drafts are not blocked by an invalid, hidden daily-target field. Date and reminder controls respect saving state.
- **Logging:** scrollable fields with a fixed save/footer area, singular/plural save labels, saving feedback and an announced error surface. Existing draft preservation is retained.
- **Home:** the estimate explanation is grouped in a card. At 150% font scale and above, a flexible text card replaces the fixed-size cycle circle so text can grow vertically. The daily-summary icon uses the matching Material container foreground color.
- **History:** persistent search labeling, clear-filter and log-today empty-state actions, accurate symptom/mood filter wording, stronger date headings and two-line summaries. Loading/error states do not also claim there are no logs.
- **Settings:** medication name, schedule and management actions have distinct hierarchy; action rows wrap and as-needed medication no longer displays a daily dose target.

New copy is in `values/ui_polish.xml`; reusable form headings and error feedback are in `FormFeedback.kt`. Existing Compose device tests were updated for the fixed save footer and revised labels.

## Validation

Build, unit-test and lint results are recorded in `LOCAL_VERIFICATION.md`. Device checks remain necessary: 320dp and landscape layouts, 200% text, keyboard insets, TalkBack announcements/focus order, and real reminder delivery. The earlier cloud emulator aborted interactive tests with an Android system-process crash; the prior passing database/PDF tests do not verify these new layouts.
