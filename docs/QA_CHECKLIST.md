# ServiceSphere QA Checklist

Last updated: 2026-06-18

## Build Verification

- [x] `./gradlew assembleDebug` passes after polish fixes.
- [x] `./gradlew clean` passes in final verification.
- [x] `./gradlew lintDebug` passes.
- [x] Debug APK installs on emulator/device.
- [x] App launches without immediate crash on emulator.

## App Shell

- [ ] Splash screen shows ServiceSphere branding.
- [ ] Onboarding can be completed.
- [ ] Business setup can be completed with required fields.
- [ ] Bottom navigation switches between Dashboard, Jobs, Clients, Invoices, and Settings.
- [ ] Shared top bar does not show a non-functional hamburger/menu button.
- [ ] First-run walkthrough appears after required business setup is complete.
- [ ] Walkthrough Skip marks it complete and lands safely on Dashboard.
- [ ] Walkthrough Back and Next move between steps correctly.
- [ ] Final walkthrough button says Go to Dashboard and does not show again after restart.
- [ ] Back navigation returns to the expected previous screen.
- [ ] Empty states render without overlapping controls.

## Dashboard

- [ ] Dashboard loads from Room without fake hardcoded counts.
- [ ] Metric cards show useful zero states when no data exists.
- [ ] Demo data appears only when seeded intentionally.
- [ ] Quick actions open the correct create flows.
- [ ] Dashboard returns cleanly after saving a client, job, quote, or invoice.

## Clients

- [ ] Client list empty state is visible when no clients exist.
- [ ] Create client validates required name.
- [ ] Email validation shows visible inline errors.
- [ ] Client save hides keyboard and navigates to detail/list as intended.
- [ ] Edit client loads existing values.
- [ ] Delete client requires confirmation and refreshes the list.

## Jobs

- [ ] Minimal job with title only saves.
- [ ] Job with address and estimated price saves.
- [ ] Job with scheduled date/time and no reminder saves.
- [ ] Reminder requires schedule when enabled.
- [ ] Invalid date/time and invalid price show visible errors.
- [ ] Keyboard hides on Done, Save, Cancel, selectors, and outside tap.
- [ ] Edit job follows the same validation and keyboard behavior.
- [ ] Job detail handles missing optional client, schedule, notes, photos, and signature.

## Quotes

- [ ] Create quote opens without requiring a client or job.
- [ ] Quote line item validation is visible.
- [ ] Tax, discount, and total calculations update correctly.
- [ ] Client, job, and status pickers dismiss the keyboard first.
- [ ] Save quote handles invalid dates and invalid amounts gracefully.
- [ ] Convert quote to invoice remains available from quote detail.

## Invoices

- [ ] Create invoice opens without requiring a client, job, or quote.
- [ ] Invoice line item validation is visible.
- [ ] Due date validation is visible.
- [ ] Payment status changes persist.
- [ ] Tax, discount, and total calculations update correctly.
- [ ] Client, job, quote, and status pickers dismiss the keyboard first.

## Photos, Signatures, PDF, Share

- [ ] Job photo picker works with granted permission.
- [ ] Missing photo/file URI failures show friendly errors.
- [ ] Signature capture saves and appears on job detail/PDF where supported.
- [ ] PDF preview/generation handles missing business profile fields.
- [ ] Share sheet handles missing target apps without crashing.
- [ ] Blank message share is blocked with a visible error.

## Settings and Data

- [ ] Business Profile saves valid values and shows validation errors.
- [ ] Currency & Tax saves valid currency/tax settings.
- [ ] Invoice Settings saves numbering and payment instructions.
- [ ] Data export creates JSON and CSV files.
- [ ] Export share opens Android share sheet.
- [ ] Delete Data confirmation clears local app data and reminders.
- [ ] Privacy Policy and Terms screens open.
- [ ] Rate ServiceSphere opens the Play Store listing or web fallback without crashing.
- [ ] Help & Support FAQ expands, collapses, and scrolls correctly.
- [ ] Replay walkthrough opens from Settings and returns safely without resetting app data.

## Visual QA

- [ ] Text does not overlap on small phone viewport.
- [ ] Action rows remain reachable above keyboard.
- [ ] Cards, chips, and buttons use ServiceSphere colors consistently.
- [ ] Long client/job/invoice names truncate or wrap cleanly.
- [ ] Light theme contrast is readable.
- [ ] Dark mode prepared screens do not use unreadable hardcoded colors.
