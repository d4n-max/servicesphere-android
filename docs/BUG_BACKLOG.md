# ServiceSphere Bug Backlog

Last updated: 2026-06-18

## Fixed In This Polish Pass

- Removed the non-functional hamburger icon from the shared app top bar because ServiceSphere uses bottom navigation rather than a drawer.
- Added a Settings "Rate ServiceSphere" action that opens the Google Play listing with a web fallback and friendly failure message.
- Added a lightweight Help & Support FAQ section in Settings for offline storage, offline use, export, quotes/invoices, photo proof/signatures, and support contact guidance.
- Added a post-setup guided walkthrough with local DataStore completion state and a Settings replay option.
- Form keyboard dismissal was inconsistent outside the Job form. Client, Quote, Invoice, Business Profile, Currency & Tax, Document Settings, and Business Setup screens now clear focus and hide the keyboard when users tap outside, save, cancel/back, or open key selectors.
- Quote and Invoice builder pickers could open while the keyboard still covered the lower screen. Client/job/quote/status picker actions now dismiss the keyboard first.
- Quote and Invoice line-item numeric fields did not have clear IME actions. Quantity advances, unit price completes, and discount/tax fields advance or complete more predictably.
- Missing Android share targets could surface through a brittle `error(...)` path. Share failures now return typed `IllegalStateException` failures for view models to display.

## High Priority

- Run full emulator smoke test after a clean install with both empty workspace and demo-data workspace.
- Verify all detail screens with stale/deleted IDs from deep links or back stack restoration. Earlier scan found non-null assertions in detail screens guarded by UI state; they should still be manually exercised.
- Add unit tests for JobFormViewModel validation: title-only save, price parsing, date/time parsing, reminder scheduling rules, and repository failure.
- Add focused tests for quote/invoice total calculation and date validation.

## Medium Priority

- Add explicit `KeyboardActions(onDone = ...)` to every final single-line field in settings/onboarding, not only the document builders.
- Improve date entry with a date picker or input mask for jobs, quotes, and invoices.
- Add more robust visual tests/screenshots for small devices and large font accessibility.
- Add repository-level tests for delete-data cleanup and export payload generation.
- Review warning/deprecation output with `./gradlew --warning-mode all`.
- Perform emulator walkthrough QA on rotation and very small phone viewports once `adb` is available.

## Low Priority

- Replace placeholder logo/business-logo surfaces with final production assets.
- Add stronger empty-state copy for first-run workflows after users choose "Start empty".
- Consider a shared `FormScaffold` composable to remove duplicated keyboard-dismiss plumbing.
- Add semantic content descriptions to more icon-only UI elements.
