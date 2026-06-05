# Permissions Audit

## Manifest Permissions

Final manifest permission list for Closed Testing:
- `android.permission.CAMERA`
- `android.permission.INTERNET`
- `android.permission.POST_NOTIFICATIONS`

### `android.permission.CAMERA`
Used to take job photos for Photo Proof. The app also uses the modern Android photo picker for gallery selection, so no gallery/storage permission is required.

Camera hardware is declared optional with:

```xml
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

### `android.permission.INTERNET`
Used for RevenueCat/subscription support when configured. Core clients, jobs, quotes, invoices, photos, signatures, PDFs, exports, and delete flows remain local/offline-first.

### `android.permission.POST_NOTIFICATIONS`
Used for job reminders on Android 13+. The app checks notification permission before posting reminder notifications.

## FileProvider
The FileProvider is not a runtime permission. It is used to safely share app-generated files such as PDFs, export files, and camera image URIs with other apps when the user chooses an action.

The provider authority uses the application ID pattern: `${applicationId}.fileprovider`.

`res/xml/file_paths.xml` includes app-specific paths for camera cache, job photos, PDFs, signatures, business assets, exports, and external app-specific photo/PDF folders. Code paths use `FileProvider.getUriForFile(...)` for camera image capture URIs, generated PDFs, and exported JSON/CSV files. No raw `file://` sharing was found for these share flows.

## Storage Permissions
No broad storage permissions are declared.

Not declared:
- `READ_EXTERNAL_STORAGE`
- `WRITE_EXTERNAL_STORAGE`
- `MANAGE_EXTERNAL_STORAGE`

This is correct for the current implementation because the app stores its own files in app-managed directories and uses Android's photo picker/share mechanisms.
