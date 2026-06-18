# App Icon Checklist

## Current Resource Audit
- Adaptive icon exists: Yes, `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` and fallback `app/src/main/res/mipmap-anydpi/ic_launcher.xml`.
- Round icon exists: Yes, `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` and fallback `app/src/main/res/mipmap-anydpi/ic_launcher_round.xml`.
- Play Store 512x512 icon exported: Yes, `play-store-assets/servicesphere-play-icon-512.png` and clean transparent-corner export `play-store-assets/servicesphere-play-icon-512-clean.png`.
- Adaptive foreground safe zone: Fixed 2026-06-18. The launcher foreground now uses the restored original ServiceSphere purple/indigo rounded mark with transparent padding.
- Launcher safe zone: Fixed 2026-06-18. The launcher-only foreground and legacy mipmap icons now center the ServiceSphere mark with transparent padding so the app drawer icon does not appear zoomed in.
- Exact launcher resource chain: `AndroidManifest.xml` uses `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`; the adaptive launcher XML files now use `@drawable/ic_launcher_servicesphere_padded`; that wrapper uses `@drawable/ic_launcher_servicesphere_padded_art`.
- Adaptive launcher scaling: Fixed 2026-06-18. `ic_launcher_servicesphere_padded.xml` uses `android:gravity="fill"` so Android scales the whole padded canvas instead of clipping the bitmap center; the optional monochrome launcher layer was removed so the app drawer uses the padded full-color path.
- Legacy density icons regenerated: Yes, mdpi through xxxhdpi `ic_launcher.png` and `ic_launcher_round.png` use the restored original-style mark at about 62% raw canvas size without baked white corners.
- Source app logo cleaned: Fixed 2026-06-18. `app/src/main/res/drawable/servicesphere_icon.png` now has true transparent corners for splash screen and in-app logo use.
- In-app and launcher icons checked separately: Yes. `servicesphere_icon.png` remains the in-app/splash source, while launcher resources use the dedicated padded `ic_launcher_servicesphere_padded` asset and regenerated mipmap PNGs.
- White-corner pixel audit: Passed for `servicesphere_icon.png`, adaptive foreground, legacy launcher PNGs, and Play Store 512 assets.
- Icon visible on launcher: Verified on emulator app drawer for `com.servicesphere.app` after safe-zone fix.
- Icon not cropped: Verified on emulator home screen/app drawer and Recents for `com.servicesphere.app`; still review in Play Console asset preview.
- App drawer safe-zone check: Verified after uninstalling the previous `com.servicesphere.app` build and reinstalling debug to avoid launcher icon cache.
- Home screen shortcut check: Verified after reinstall; the ServiceSphere mark is smaller, centered, and not pushed to the icon edge.
- No white corners: Verified on emulator home screen/app drawer, splash screen, Recents, and in-app top bar logo.
- Icon uses original ServiceSphere styling: Yes, restored from the previous purple/indigo rounded ServiceSphere mark rather than the newer simple vector S.
- Icon has no small text: Yes.
- Icon looks good on light/dark backgrounds: Pass for light app surfaces; review themed/dark launcher surfaces before production.
- Package audit: `app/build.gradle.kts` keeps `applicationId = "com.servicesphere.app"`, `namespace = "com.servicesphere"`, no `applicationIdSuffix`, and no product flavors. `versionCode` and `versionName` were not changed.
- Duplicate app audit: The test emulator had both `com.servicesphere.app` and an older stale `com.servicesphere` package installed. This is device/emulator state, not a current Gradle package change.

## Manifest Audit
- `android:icon` points to `@mipmap/ic_launcher`: Yes.
- `android:roundIcon` points to `@mipmap/ic_launcher_round`: Yes.
- App label is ServiceSphere: Yes, `@string/app_name` is `ServiceSphere`.
- Splash icon points to cleaned source: Yes, `windowSplashScreenAnimatedIcon` uses `@drawable/servicesphere_icon`.
- Launcher foreground points to dedicated padded asset: Yes, both `ic_launcher.xml` and `ic_launcher_round.xml` use `@drawable/ic_launcher_servicesphere_padded`.

## Manual Export Checklist
- Upload `play-store-assets/servicesphere-play-icon-512.png` as the Play Console app icon.
- Keep `play-store-assets/servicesphere-play-icon-512-clean.png` as the transparent-corner source/export reference.
- Check adaptive icon safe zone after the foreground padding update.
- Confirm splash screen and in-app logo render without white square corners.
- Confirm launcher/app drawer icon and in-app logo are checked independently after any icon change.
- Confirm only `com.servicesphere.app` is installed on fresh test devices. If an emulator shows a second ServiceSphere app, uninstall the stale `com.servicesphere` package.
- Preview on circular, squircle, rounded square, and themed icon surfaces.
- Verify no important mark is cropped at small launcher sizes.
