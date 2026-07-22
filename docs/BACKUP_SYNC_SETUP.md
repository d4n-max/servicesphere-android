# Optional cloud backup and sync

ServiceSphere remains local-first. Signing in is optional; local records remain on the device when cloud sync is disabled or the user signs out.

## Firebase Console steps

1. Enable Google as a Firebase Authentication provider and register the production SHA-1/SHA-256 certificate fingerprints.
2. Create Firestore in production mode and deploy `firebase/firestore.rules`.
3. Create the default Storage bucket and deploy `firebase/storage.rules`.
4. Confirm the Android app's `google-services.json` contains an OAuth web client, which generates `default_web_client_id`.
5. Enable App Check/Play Integrity before production rollout and configure budget/quota alerts.

## App Check: staged configuration and rollout

The app installs an App Check provider before `ServiceLocator` creates any Firebase client:

- `debug`: `DebugAppCheckProviderFactory`, compiled through `debugImplementation` only.
- `release`: `PlayIntegrityAppCheckProviderFactory`, compiled through `releaseImplementation` only.

`verifyReleaseAppCheckProvider` checks that release's runtime classpath contains no `firebase-appcheck-debug` artifact and that its variant initializer references only Play Integrity. Do not add an App Check debug dependency to `implementation`.

### Emulator/debug token setup

1. Build and install the **debug** variant on the emulator; never use a release build for this flow.
2. Trigger a Firestore or Storage request (for example, a signed-in manual backup).
3. In Logcat, copy the debug secret printed by `DebugAppCheckProvider`. Treat it like a credential: do not paste it into source, issue trackers, screenshots, or CI logs.
4. Firebase Console → Security → App Check → Apps → ServiceSphere → Manage debug tokens: register that token.
5. Repeat the request. The app preflights `FirebaseAppCheck.getAppCheckToken(false)` without logging the token; Firestore and Storage then attach App Check automatically.
6. Revoke the token in the Firebase Console when the emulator is retired or the token may have been exposed.

For CI instrumentation tests, create a separate token in Firebase Console and store it only in the CI secret manager as `APP_CHECK_DEBUG_TOKEN_FROM_CI`. Pass it as `firebaseAppCheckDebugSecret`; never put the value in Gradle files or repository configuration.

### Play Integrity, monitoring, enforcement, and rollback

1. In Play Console, open **App integrity**, enable/link the Play Integrity API to the Firebase project's Google Cloud project, then register the Android app in Firebase App Check with Play Integrity.
2. Distribute a release build through Google Play internal testing. Verify successful Firestore and Storage backup/sync calls and check Firebase App Check metrics for verified traffic.
3. Keep enforcement **disabled** for Firestore and Storage while validating both debug-token traffic and Play-distributed traffic. Firebase Authentication is not used for backup/sync request protection in this app.
4. Only after metrics show the intended clients as verified, enable enforcement one product at a time: **Cloud Firestore first**, then **Cloud Storage**. Wait for the console propagation period and monitor failures after each change.
5. Rollback: Firebase Console → Security → App Check → product metrics → disable enforcement. This does not require a client release. Investigate provider configuration and keep sync/backup optional while enforcement is disabled.

Current enforcement readiness: **none**. Cloud Firestore and Cloud Storage are code-integrated and ready for monitoring only; enforcement requires the Firebase Console and Play internal-test verification above. Firebase Authentication and Analytics have no enforcement action in this feature.

## Data layout

Structured records: `users/{uid}/businesses/{businessId}/{entityType}/{entityId}`.
Attachments: `users/{uid}/businesses/{businessId}/attachments/{entityType}/{entityId}`.
Manual backup: `users/{uid}/businesses/{businessId}/backups/latest.json`.

Generated PDFs, exports, cache and share-sheet copies are excluded: PDFs can be regenerated from local records and their attachments; exports and cache are transient copies.

## QA essentials

- Verify local-only onboarding without Firebase sign-in.
- Create and edit records offline, restart, reconnect, and confirm the outbox completes.
- Verify a signed-out user cannot read another UID path in Firestore or Storage.
- Confirm a missing photo remains retryable and does not delete its local copy.
- Before Play Console submission, manually update Data safety disclosures for optional account, cloud files, and business-record storage. Do not claim end-to-end encryption.
