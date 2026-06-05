# Internal Testing Checklist

## Pre-upload
- `assembleDebug` passes.
- `assembleRelease` passes if used.
- Signed AAB generated.
- App opens on emulator.
- App opens on physical device if available.
- No debug-only Pro toggle in release.
- RevenueCat key configured or safe fallback confirmed.
- App icon correct.
- App label correct.
- versionCode incremented.
- versionName set.

## Core Smoke Tests
- Onboarding.
- Business setup.
- Create client.
- Create job.
- Reminder no reminder.
- Create quote.
- Convert quote to invoice.
- Mark invoice paid.
- Generate PDF.
- Add photo.
- Capture signature.
- Send message.
- Export data.
- Delete data.
- Paywall Maybe Later.

## Google Play
- App name.
- Short description.
- Full description.
- Screenshots.
- Feature graphic.
- Privacy policy URL.
- Data safety.
- Content rating.
- Target audience.
- Ads declaration.
- App access.
- Health apps declaration if shown.
- Financial features declaration if shown.
- Release notes.

## Before uploading AAB
- Clean build passes.
- Release build configuration checked.
- versionCode/versionName checked.
- App icon checked.
- App label checked.
- Permissions checked.
- RevenueCat fallback checked.
- Privacy policy prepared.
- Data Safety notes prepared.
- Screenshots/feature graphic prepared.
- Keystore stored outside repo.
- AAB will be generated manually from Android Studio.
