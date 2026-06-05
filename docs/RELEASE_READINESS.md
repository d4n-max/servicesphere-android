# ServiceSphere Release Readiness

Last updated: 2026-06-05

## Current Status

ServiceSphere is not release-ready for production distribution yet, but the MVP is in a healthier internal QA state. Core offline data flows, branded UI shell, dashboard, forms, export/delete-data paths, PDF/share placeholders, and subscription gates are present. This pass focused on practical UX and crash-risk polish rather than new feature work.

## Closed Testing Prep Validation

- `./gradlew clean` passed on 2026-06-05 after stopping a stale Gradle daemon that held a Windows file lock.
- `./gradlew assembleDebug` passed on 2026-06-05.
- `./gradlew lintDebug` passed on 2026-06-05.
- Closed Testing release-prep validation did not run `bundleRelease` and did not generate an AAB.
- Existing Room/DataStore/export/share architecture was left intact.
- No database schema changes were made.
- No navigation or feature behavior changes were made for this release-prep pass.

## Required Before Beta

- Complete the remaining manual checklist in `docs/QA_CHECKLIST.md`.
- Smoke test first-run onboarding with "Start empty" and "Start with demo data".
- Manually test Create/Edit Job, Client, Quote, Invoice, Business Profile, Data Export, Delete Data, PDF preview/share, message share, photo proof, and signature flows.
- Triage all lint findings and decide which warnings are acceptable for beta.
- Add minimal ViewModel tests for validation-heavy flows before external users handle real business records.

## Release Blockers

- No complete manual emulator bug bash has been recorded for this pass yet.
- Validation-heavy flows need automated coverage before production use.
- Billing and PDF generation remain intentionally placeholder/mock services.
- Privacy Policy and Terms content must be finalized before store submission.
- Final app signing, package metadata, Play Store assets, and production icon review are still needed.
- Release AAB must be generated manually in Android Studio and signed with the intended Play upload key before Play Console upload.

## RevenueCat Manual Setup Checklist

- Add the public RevenueCat Android SDK key through `local.properties` or a private Gradle property named `REVENUECAT_API_KEY`.
- Do not commit private keys, signing keys, passwords, or `local.properties`.
- Configure the expected entitlement ID: `pro`.
- Configure Google Play products and RevenueCat packages for `servicesphere_pro_monthly` and `servicesphere_pro_yearly`.
- Verify the Closed Testing build remains on the Free plan when `REVENUECAT_API_KEY` is blank or missing.
- Verify the paywall shows the safe "Subscriptions are not configured for this build." message when RevenueCat is not configured.
- Test purchase and restore flows with Google Play license testers before wider rollout.

## Release Notes Draft

- Added ServiceSphere-branded offline-first field-service foundation.
- Added dashboard, client, job, quote, invoice, settings, export, and delete-data flows.
- Improved form keyboard handling across major create/edit screens.
- Hardened share behavior for devices without matching target apps.
- Added QA, bug backlog, and release readiness documentation.

## Internal Testing Status

Ready for Internal Testing: No.

ServiceSphere is close to internal testing prep, but a few manual and account-side tasks remain before an AAB should be uploaded to Google Play.

Remaining blockers:
- RevenueCat products need real setup or a confirmed safe fallback for the exact release build.
- Google Play subscriptions need setup if Pro subscriptions will be visible in the test release.
- Privacy Policy needs a hosted public URL.
- Terms of Use should be hosted or otherwise linked according to store requirements.
- Screenshots need final capture using realistic demo data.
- Feature graphic needs final 1024 x 500 export.
- Release AAB needs upload-key signing verification before Play Console upload.
- Physical device QA is recommended before adding external testers.
- Manual Play Console declarations still need completion: Data Safety, content rating, ads declaration, app access, target audience, and any financial features declaration shown by the console.
