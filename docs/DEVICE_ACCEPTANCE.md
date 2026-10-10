# Real-device acceptance — not yet executed

This checklist is **outstanding work**, not test evidence. SAF grant-lifecycle source commit `a125671547ca7842fea137b4aee5aa5d5fe73232` passed Android CI run `38095787115`: 105 JVM tests pass, lint/app APK/instrumentation APK build pass; all 16 native instrumentation methods compile. No native runtime result is known to pass, and no device-workflow run is listed for this commit. The latest actual device attempt (`38089299077`) timed out before emulator boot; no tests ran. Neither result proves an app defect or acceptance. Preserve actual errors/results and update TEST_REPORT and individual matrix entries only after execution.

`LocalRotationJourneyTest` is a later repository-level test authored against a recording wallpaper platform. Even if its Android test APK compiles or that fake-platform method passes, it is **not** system-picker, `WallpaperManager`, visible-pixel, OEM, or physical-device acceptance. Consult that commit's actual Actions result for compilation/execution; do not count it as a Samsung journey pass.

## Safe test setup

Use a disposable emulator or dedicated Android test device. **WallpaperIsolationTest intentionally seeds both Home and Lock wallpapers.** Do not run the entire instrumentation suite on a personal device without explicit consent and a separate wallpaper backup. Never upload private photos, credentials, databases, raw provider responses or unsanitized `dumpsys` output to this public repository.

Retrieve the app and test APKs from the **same successful Actions run**. Latest verified run `38095787115` has app artifact `11685554177` (APK SHA-256 `0ef538f54c384eea8a75286d95149680b69bd80f57a844ca19f464cdeedd09a8`) and instrumentation artifact `11685654065` (APK SHA-256 `2f47479f4b43bbf31715e72ff593e2e989c5c46cefc24c1f41e535bfdbb8d0b8`). These are APK hashes, not artifact ZIP hashes. Debug signing is sufficient for acceptance; no release/signing account is involved.

On an authorized test device with the Android SDK available:

```sh
adb devices
adb install -r app-debug.apk
adb install -r -t app-debug-androidTest.apk
adb shell am instrument -w -r com.westkitty.dexwallpaper.test/androidx.test.runner.AndroidJUnitRunner
```

Record the **actual** installed run/SHA, Android version, device model, discovered/executed/passed/failed counts and failure traces locally. Installation or APK compilation is not an instrumentation pass. Keep evidence sanitized before committing. Instrumentation ID/component should be confirmed with `adb shell pm list instrumentation` rather than substituted if a device reports a different registration.

## Mandatory user journey (all NOT RUN)

1. Fresh-install launch: real Control Center opens without onboarding/account; destination is **Home only**. Verify all actions have consumers and unavailable operations explain the reason.
2. Seed a visibly distinct **independent** Lock wallpaper using explicit Lock selection/system settings. Retain its local screenshot/identity. If Home/Lock are shared or IDs unreadable, Home-only must refuse safely; this is a deliberate guard, not permission to write Both.
3. Android picker: import several lawful neutral test images individually, then from an authorized SAF folder. Confirm real thumbnails, collection membership and metadata. Cancel picker, deny access and revoke one grant: no fake success, recoverable explanation. Verify individual document picks leave no persistent grant; a linked folder retains read-only access through rescan/restart; failed or canceled folder imports do not retain an unreferenced grant; replacing a folder releases the old tree grant; and backup restore removes stale folder grants. Check grant flags/counts locally without writing private URI values into logs or the repository.
4. Choose collection, shuffle and an interval of at least 15 minutes in Rotation Lab. WorkManager timing is approximate; do not expect exact-alarm precision. Preview/apply explicitly **Home only**, then Next. Confirm the Home image actually changes and the Lock **visible image and identity** do not. IDs alone are not visual acceptance.
5. Restart the process and relaunch. Library, collection, draft edits, per-destination mode/interval/pause and history survive. Next/history/favorite/exclusion work with the same shared DB. Observe a scheduled change with screen on and adequate power; record delay, not a fabricated exact time.
6. Rotate landscape/portrait; test split-screen/resizable windows and Galaxy Tab S9 Ultra 2960×1848. Verify usable narrow navigation, no clipped Apply/import controls, and draft retention. Test DeX/mouse/keyboard/S Pen where hardware is available.

## Isolation, lifecycle and recovery (all NOT RUN)

- Home static + Lock live; Home live + independent Lock static; Home and Lock separately configured live on Android 14+ only when the platform exposes known ownership. Refuse unsupported/unknown/shared ownership. Explicit Both is a separate choice, not a fallback for Home-only.
- Cancel the live system picker; choose a different service; exercise a picker assigning only Home when Both was requested. Never report verified success without component/identity/ownership evidence. Inspect actual OS result timing and surface pixels.
- Hide/show, lock/unlock, switch launcher, rotate surfaces, background/foreground, screen off/Battery Saver, process death and memory pressure. Check hidden rendering/decoding stops, preview draft does not replay as installed content, snapshots remain independent, and stale frames/GL contexts are released.
- Missing/corrupt/large files, revoked SAF permission, low storage, decode failure and attempted-but-unverified wallpaper write: visible truthful error; no continuing automatic unsafe write. Backup/restore uses lawful synthetic data and preserves mappings/protected originals; verify migrations, cache budgets and sanitized export.
- Home-only widget/tile/shortcuts and scoped automation commands: actual success/failure status, never Both by default. Provider journeys are optional and require authorized endpoints; do not commit credentials. Check source allowlists, cancellation, DNS/network switching, budgets and Keystore restart behavior.
- TalkBack, large font, reduced motion/high contrast, keyboard focus/navigation, portrait and compact DeX. Performance measurements need device/run/context and actual data; build logs are not benchmarks.

All 276 requirements remain in FEATURE_MATRIX.md / feature_matrix.json. Do not promote a feature to DONE_VERIFIED based on this procedure or compilation alone.
