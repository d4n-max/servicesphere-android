# Build and Release Guide

## A. Build Debug
Run:

```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

Debug APK path:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## B. Build Release Bundle
Do not generate the Closed Testing AAB from this Codex release-prep pass.

For the Closed Testing upload, generate the signed Android App Bundle manually in Android Studio:

```text
Build > Generate Signed App Bundle / APK > Android App Bundle
```

## C. Keystore Checklist
- Do not commit keystore files.
- Do not commit passwords.
- Store the keystore safely outside the repository, for example `C:\AndroidKeys\ServiceSphere\servicesphere-release-key.jks`.
- Store alias/password recovery details securely.
- Use Android Studio Generate Signed Bundle, or a Gradle signing config backed only by local/private properties.
- Verify the uploaded Play App Signing certificate and upload key process before release.

## D. Versioning
- `versionCode` must increase for each Play Console upload.
- `versionName` is user-facing. Example: `1.0.0`.
- Current project values are in `app/build.gradle.kts`.
- Every new upload to Google Play must use a higher versionCode than the previous uploaded bundle.

## E. Internal Testing
- Build signed release AAB.
- Upload AAB to Google Play Console Internal Testing.
- Add testers or tester group.
- Fill store listing.
- Complete Data Safety and policy declarations.
- Add screenshots and feature graphic.
- Add privacy policy URL.
- Add release notes.
- Roll out to internal testing.

## F. Release Signing Status
The current Gradle file does not define a private Play upload-key signing config with committed keystore credentials, which is correct for source control safety. That is OK for this project because the signed AAB will be generated manually from Android Studio.

Codex did not run `bundleRelease` for the Closed Testing release-prep pass and did not generate an AAB.

## G. Gradle Release Readiness
- applicationId: `com.servicesphere.app`
- minSdk: `26`
- targetSdk: `35`
- versionCode: `2`
- versionName: `1.0.0`
- release build type: explicit release config with `debuggable=false`, `minifyEnabled=false`, and `shrinkResources=false`.
- signing config: no committed release keystore, passwords, or signing config.
- debug app name: No debug-only app label found.
- debug-only Pro toggle: guarded by `BuildConfig.DEBUG` and not visible in release builds.
- hardcoded debug API keys: No committed RevenueCat key value found; `REVENUECAT_API_KEY` is read from Gradle/local properties.
- local file paths: No release-blocking local file paths found in Gradle config.
- permissions: Camera, Internet, and Post Notifications only; no broad storage permissions found.
