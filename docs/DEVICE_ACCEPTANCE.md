# Real-device acceptance — not yet executed

This checklist is **outstanding work**, not test evidence. At the `ee24a43` checkpoint, 96 JVM tests pass and all 15 instrumentation methods compile. No known native runtime result passes. Ubuntu failed before APK install; accelerated macOS timed out before boot. Neither failure proves an app defect or acceptance. Preserve any new actual error/result and update TEST_REPORT and the individual matrix entries only after execution.

## Safe test setup

Use a disposable emulator or dedicated Android test device. **WallpaperIsolationTest intentionally seeds both Home and Lock wallpapers.** Do not run the entire instrumentation suite on a personal device without explicit consent and a separate wallpaper backup. Never upload private photos, credentials, databases, raw provider responses or unsanitized `dumpsys` output to this public repository.

Retrieve the app and test APKs from the **same successful Actions run**. The verified `38090533888` app SHA-256 is `92fadd0fbc605a876e28faf456e938d0bf191952db76ac134363d748598b11f3`; test SHA-256 is `d613720072e1d5ab9c56ae48cc42c38050d39e47e7619bba4fa8205a78b03c30`. These are APK hashes, not artifact ZIP hashes. Debug signing is sufficient for acceptance; no release/signing account is involved.

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
3. Android picker: import several lawful neutral test images individually, then from an authorized SAF folder. Confirm real thumbnails, collection membership and metadata. Cancel picker, deny access and revoke one grant: no fake success, recoverable explanation.
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
