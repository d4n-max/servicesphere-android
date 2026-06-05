# Google Play Data Safety Notes

These notes are guidance for manually completing the Google Play Console Data Safety form. They are not final Play Console answers; confirm against the exact release build, RevenueCat configuration, privacy policy, and any future integrations.

## Data Collected or Stored Locally
ServiceSphere lets users enter and store data locally on the device:
- Personal/contact info entered for clients
- Business profile information
- Job details, addresses, descriptions, and notes
- Quote and invoice records, including financial-like totals
- Photos added by the user for job proof
- Client signatures captured by the user
- Reminder data
- Generated PDFs and exported files

## Data Shared
- PDFs, messages, CSV files, and JSON backups are shared only when the user explicitly chooses to share them via Android's share sheet or another user action.
- Subscription purchase/status data may be handled by Google Play and RevenueCat if subscriptions are configured.

## Data Encrypted in Transit
- Local-only business records are not transmitted by ServiceSphere cloud sync because cloud sync is not currently implemented.
- Subscription-related network requests, if RevenueCat is configured, should use network transport handled by Google Play/RevenueCat SDKs.

## Data Deletion
- In-app local delete all data feature exists.
- Users can also uninstall the app to remove app-local data managed by Android.
- Previously shared files/messages outside the app may remain with the receiving app or recipient.

## Data Export
- In-app local export exists for JSON backup and CSV files.
- Exported files are user-controlled and may contain business/client/job data.

## Photos and Signatures
- Photos and signatures are user-generated local files.
- They are not automatically uploaded by ServiceSphere.

## Selling Data
ServiceSphere does not sell personal data.

