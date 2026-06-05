# App Icon Checklist

## Current Resource Audit
- Adaptive icon exists: Yes, `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` and fallback `app/src/main/res/mipmap-anydpi/ic_launcher.xml`.
- Round icon exists: Yes, `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` and fallback `app/src/main/res/mipmap-anydpi/ic_launcher_round.xml`.
- Play Store 512x512 icon exported: Yes, `play-store-assets/servicesphere-play-icon-512.png`.
- Icon visible on launcher: Verified during emulator launch smoke test.
- Icon not cropped: Verified visually on emulator launcher/app top bar; still review in Play Console asset preview.
- Icon uses ServiceSphere S mark: Resource uses ServiceSphere foreground/logo assets.
- Icon has no small text: Yes.
- Icon looks good on light/dark backgrounds: Pass for light app surfaces; review themed/dark launcher surfaces before production.

## Manifest Audit
- `android:icon` points to `@mipmap/ic_launcher`: Yes.
- `android:roundIcon` points to `@mipmap/ic_launcher_round`: Yes.
- App label is ServiceSphere: Yes, `@string/app_name` is `ServiceSphere`.

## Manual Export Checklist
- Upload `play-store-assets/servicesphere-play-icon-512.png` as the Play Console app icon.
- Check adaptive icon safe zone.
- Preview on circular, squircle, rounded square, and themed icon surfaces.
- Verify no important mark is cropped at small launcher sizes.
