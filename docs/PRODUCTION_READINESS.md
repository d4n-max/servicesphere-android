# Production Readiness

## Current State
ServiceSphere is suitable for Google Play Internal Testing preparation. The app has offline-first local records, onboarding, business setup, clients, jobs, quotes, invoices, PDFs, photo proof, signatures, reminders, follow-up messages, free/pro gates, data export, and delete data flows.

## Prepared for Internal Testing Setup
- Debug build compiles.
- Closed Testing release bundle should be generated manually in Android Studio with a keystore stored outside the repository.
- Store listing draft is prepared.
- Screenshot and feature graphic plans are prepared.
- Privacy policy and terms drafts are prepared.
- Data safety notes are prepared.
- Permissions are documented.
- Internal testing checklist is prepared.
- Play Store 512 x 512 icon asset is exported at `play-store-assets/servicesphere-play-icon-512.png`.

## Not Yet Ready for Public Production Release
- Not ready to upload to Google Play until upload-key signing, hosted policy URLs, screenshots, feature graphic, and required Play Console declarations are complete.
- Privacy policy and terms must be reviewed and hosted publicly.
- RevenueCat products and Google Play subscriptions must be configured and tested.
- Signed release workflow must be finalized without committing secrets.
- Generated release AAB signing must be verified against the intended Play upload key.
- Store screenshots and feature graphic must be captured/exported.
- Physical device QA should be completed.
- Accessibility and PDF visual QA should be completed with real devices and realistic records.
- Play Console declarations must be completed manually.

## Operational Notes
- Do not add keystore files, passwords, or private API keys to source control.
- Keep exported screenshots free of private customer data.
- Confirm versionCode increments before each upload.
- Confirm local data delete/export behavior before wider testing.
