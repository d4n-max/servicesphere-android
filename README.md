# ServiceSphere

**An offline-first Android workspace for solo tradespeople and small field-service teams to manage clients, work, quotes, and invoices from one device.**

[View on Google Play](https://play.google.com/store/apps/details?id=com.servicesphere.app)

## Overview

ServiceSphere is a native Android app for service professionals who need a practical record of client work while away from a desk. It brings client records, jobs, quotes, invoices, notes, photos, signatures, reminders, and business settings into one mobile workflow.

The primary audience is a solo tradesperson, technician, contractor, handyman, or small service business. The product is intentionally useful without an account or network connection: core records are stored locally, while cloud account, backup, and sync capabilities are optional.

## Screenshots

No verified runtime screenshots are currently tracked in `docs/screenshots/`. The existing files in `design/stitch/` are design exports, so this README deliberately does not present them as shipped-app captures. The verified Google Play listing above contains the current public store screenshots.

When adding repository screenshots, place privacy-safe captures made from the running app in `docs/screenshots/` and link them here. Do not use customer data in those captures.

## Key Features

- Client, job, quote, and invoice creation, editing, and linked record navigation.
- A quote-to-job-to-invoice workflow with duplicate-safe conversions and activity timelines.
- Dashboard and calendar views for upcoming work, follow-ups, and overdue invoices.
- Job notes, checklists, photo proof, customer signatures, message templates, PDF/share flows, and reminders.
- Local JSON/CSV export and local data deletion controls.
- Optional Firebase sign-in, backup, and sync foundations; the app remains usable offline without them.
- A free-plan policy and RevenueCat integration boundary for subscription-gated features.

## Architecture

The app is a single native Android module built with Kotlin and Jetpack Compose. UI screens and ViewModels coordinate navigation and state; repository classes provide the data boundary; Room stores the offline-first domain model; and DataStore holds preferences. `WorkflowRepository` uses Room transactions for related document conversions. Android WorkManager handles scheduled reminder work. Optional integrations are isolated around Firebase, RevenueCat, and platform sharing/review APIs rather than being required for the local workflow.

## Tech Stack

- Kotlin, Android SDK (min SDK 26; compile/target SDK 36)
- Jetpack Compose and Material 3
- Navigation Compose, Lifecycle ViewModels, Kotlin coroutines
- Room + KSP for local persistence and schema export
- DataStore for preferences; WorkManager for reminders
- Firebase Analytics, Authentication, Firestore, Storage, and App Check integration points
- RevenueCat SDK for subscription integration
- JUnit unit tests; AndroidX test/Espresso instrumentation-test setup
- Gradle Kotlin DSL with a version catalog

## Main Product Flow

1. A user starts with an empty workspace or sample data, then completes the basic business setup.
2. They add a client and create a job, with details, notes, checklist items, photos, reminders, or a signature when needed.
3. They create a quote, convert accepted work into a job, and create an invoice from the related work.
4. Linked records and the client/job timeline preserve the history; users can export local records or explicitly share generated content through Android.

## Product Status

The app has a live Google Play listing. This repository should still be read as an actively developed MVP, not as a claim that every integration is production-complete.

The local offline workflow, linked document flow, and unit-test coverage are implemented. Repository release-readiness notes identify remaining work before relying on every optional feature in production: signed-release validation, physical-device and accessibility QA, final policy hosting and declarations, subscription configuration/testing, and Firebase/App Check/cloud-flow verification. Cloud-data deletion and complete restore are not represented as production-ready functionality.

## My Role

I owned the product end to end: problem framing for field-service workflows, UX and information architecture, Kotlin/Compose implementation, Room data modeling and migrations, workflow design, local export/privacy controls, integration boundaries, test coverage, QA documentation, release preparation, and store-facing assets.

AI-assisted tools were used as part of development. Product decisions, architecture, implementation review, integration, testing, and release ownership remained mine.

## Local Setup

### Prerequisites

- Android Studio with an Android SDK capable of compiling API 36
- JDK 11 (the project targets JVM 11)
- A device or emulator running Android 8.0/API 26 or later for app testing

### Run locally

```powershell
git clone <your-repository-url>
cd servicesphere
.\gradlew.bat assembleDebug
```

Open the project in Android Studio or install the generated debug APK from `app/build/outputs/apk/debug/` on an emulator/device.

`local.properties` is ignored by Git. A RevenueCat key, if used for local testing, is supplied as `REVENUECAT_API_KEY` through `local.properties` or a private Gradle property. The application has a safe unconfigured subscription state when that value is absent. Never commit signing files, private keys, passwords, or local environment files.

## Verification and Testing

The repository includes focused JUnit coverage for document lifecycle rules, conversion timelines, today/dashboard calculations, sync primitives, free-plan limits, App Check policy, review prompts, handyman templates, and analytics first-event handling. It also includes an Android instrumentation-test source set.

Run the existing automated checks with:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
git diff --check
```

Automated checks do not replace real-device validation of camera, notifications, PDF rendering, sharing targets, accessibility, subscriptions, Firebase/App Check, or cloud recovery.

## Known Limitations

- Optional cloud authentication, backup, and sync require Firebase-side configuration and verification before being exposed as a production promise.
- Billing requires private RevenueCat and Google Play configuration plus purchase/restore testing.
- Release AAB signing and Play Console declarations are external release steps, not committed source configuration.
- The release-readiness documents identify further physical-device, accessibility, PDF, and full manual-flow QA work.
- The repository does not yet contain verified runtime screenshots in `docs/screenshots/`.

## Privacy and Security

Core records are designed to be stored locally. Client, job, and document data can be sensitive; screenshots, exports, and shared files should be handled as such. Optional Firebase services process data only when the user enables the associated account/cloud functionality. The project does not claim end-to-end encryption.

The repository ignores local properties, environment files, signing materials, generated packages, build output, exports, PDFs, job photos, and signatures. The tracked Firebase Android configuration should be maintained with least-privilege project settings and restricted API-key policies; it is not a substitute for server-side secret storage.

## License

No license file is currently included. Until a license is added, visitors should not assume permission to reuse the source code or assets beyond reviewing the project.
