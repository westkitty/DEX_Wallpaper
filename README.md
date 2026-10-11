# DEX Wallpaper

**The Living Wall System** — a native, offline-first Android wallpaper instrument for tablets, phones, and resizable Samsung DeX windows. Kotlin, Jetpack Compose / Material 3, Room, DataStore, WorkManager, and a real `WallpaperService`. Application ID: `com.westkitty.dexwallpaper`. Apache-2.0.

## Development state

This is an engineering build, **not the completed 276-feature product**. See [FEATURE_MATRIX.md](FEATURE_MATRIX.md), [TEST_REPORT.md](TEST_REPORT.md), and [operational state](docs/OPERATIONAL_STATE.md) for actual coverage and verification. Unsupported integrations are identified in System health; they never simulate remote data or wallpaper actions.

## First usable journey

1. Install the debug APK from a successful **Android CI → dex-wallpaper-debug** artifact.
2. Open the actual Control center; no account or onboarding is required.
3. Use **Add images → Choose a folder** (or individual images) through Android's picker.
4. Open Library; create/select a collection before importing when desired. Imports create protected private copies and real thumbnails.
5. In Rotation lab choose **Shuffle**, an interval of at least 15 minutes, and automatic rotation if desired.
6. Leave the destination at **Home screen**. Select an image in Library, inspect it, and Apply, or use Next wallpaper.
7. Inspect Home and Lock. **If Android reports a shared/unverifiable Lock wallpaper, DEX refuses Home-only application. Set an independent static Lock wallpaper in system settings first.** It will not silently fall back to Both.
8. Reopen the app; collections, per-screen configuration, queue, history, and edits persist.

The shared non-destructive image pipeline supports fit/fill/stretch/original/fit-height, separate portrait/landscape framing, pinch/drag, brightness, contrast, saturation/grayscale, dimming, vignette, downsampled soft-focus blur, and manually calibrated icon-safe zones. Original files are not overwritten.

## Home and Lock safety

Static writes go through one controller and explicit `FLAG_SYSTEM`, `FLAG_LOCK`, or a deliberately selected Both destination. Home-only checks the independent Lock identity before and after writing. Unverifiable destinations fail closed. OEM-specific behavior must still be tested on the target Samsung device; no source-only check proves visible Lock preservation.

Live wallpaper uses the Android/OEM picker, which determines the final target; cancel if the requested single-screen destination is not offered. Motion controls edit a **preview draft**; explicitly commit an immutable image/edit/render snapshot to the selected installed destination. Installed live engines never read the static Home current-image record or mutable library edits. Android 14+ engine ownership evidence is required for Home/Lock-only live updates; shared/unknown owners fail closed. Android 8–13 only permits explicitly selected Both live configuration, not a claimed Home-only setup. Cancelled previews cannot replay on engine/process recreation. Picker completion also checks the unaffected wallpaper identity and actual installed DEX component; it does not accept a known Home-only assignment as Both. An OEM/user choice made inside the system picker cannot be safely rolled back by the app. Identity checks are not a substitute for visible Samsung verification. Different simultaneous live services are not assumed.

The live renderer uses actual OpenGL ES 2 textures/effects/transitions, with per-engine contexts and an event-driven Canvas safe fallback. Clock/battery overlays use minute-boundary Canvas frames and bypass shaders. Video/GIF/Vulkan and continuously layered live scenes remain outstanding. Safe mode and reduced motion are explicitly system-wide accessibility/resource policies, not per-destination image selections.

Latest source delivery: `90497678a8e6afb205ec97c68d00f1f1defb5578` on `arena/43998994-dex-wallpaper`. Android CI run **38098553115 PASS**: 112 JVM tests / 0 failures, lintDebug, app APK and instrumentation APK compilation. [Debug APK artifact](https://github.com/westkitty/DEX_Wallpaper/actions/runs/38098553115/artifacts/11686133189) (SHA-256 `1fbbb077437f047383fd8e3f2d6737797eb91b574b5e3e2f180c2fc93d57a3d5`); [compiled test APK](https://github.com/westkitty/DEX_Wallpaper/actions/runs/38098553115/artifacts/11686452807) (SHA-256 `ad1885064125913b0a639d3c2148e5846f0184c298be764d5f672dc352f78ea5`). **17 native methods compile; 0 known passing runtime results.** Library paths reject traversal and symlink escapes; thumbnails are bounded and atomically published. SAF grants retain read access only for selected folders linked to a collection; individual documents are copied without persistent grants, and stale grants are reconciled at startup/import/rescan/restore. The Library pins/unpins collections with stable pinned-first ordering, preserves that flag in backups, and provides transactional copy/move membership actions without rewriting protected originals or edit sidecars. Two pure grant-policy, two pin-policy, and five membership-policy tests pass. The repository journey and Room copy/move instrumentation test compile, but do not prove system-picker/OS wallpaper behavior or device acceptance. No device-workflow run is listed for this commit; the latest actual attempt (run 38089299077) timed out before emulator boot and executed no tests. Real SAF/Home-only/restart/Samsung/DeX acceptance remains outstanding; see [device acceptance procedure](docs/DEVICE_ACCEPTANCE.md).

## Continuation handoff

[Copy/paste prompt for the next Arena chat](docs/HANDOFF_PROMPT.md). Start from the existing feature branch, not `main`; the product is unfinished and all 276 requirements remain in the inventory.

## Build

- Android 8.0+ (API 26); compile/target API 35.
- JDK 17, Gradle 8.11.1, AGP 8.9.2, Kotlin 2.1.20.
- Android SDK platform 35 and build tools.

```sh
python3 tools/validate_feature_matrix.py
python3 tools/check_source_policy.py
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Create `local.properties` locally with `sdk.dir=...` if needed; never commit it. Gradle downloads require Google Maven / Maven Central. The Arena sandbox has neither JDK nor Android SDK and cannot access those hosts; GitHub Actions is the primary Android verification path.

The wrapper is the authentic official Gradle v8.11.1 wrapper (SHA-256 checked in CI); the distribution checksum is pinned from the official GitHub release. Dependencies and Actions are pinned. No production signing keys or credentials are included.

## Scheduling, sources, accessibility

WorkManager rotation is **approximate**, at least 15 minutes, independently configured for Home and Lock, and deferred while screen-off or Battery Saver. Both is manual only. No exact-alarm permission or clock-polling foreground service. Chronoscape evaluates time/charging/theme/battery predicates with deterministic priorities; simulation never writes wallpaper.

Folder scanning is explicit, bounded to 5,000 file/folder entries and depth 16; a reached limit is reported so narrower subfolders can be selected for the remainder. Hidden-folder inclusion remains an explicit opt-in. Only linked folders keep a persistent read grant; individual image picks do not, and stale grants are released on startup/import/rescan/restore. Image size limit: 80 MB each. SHA-256 deduplication, local perceptual similarity, protected originals, partial-transfer staging, and missing-file quarantine keep imports safe. A Syncthing folder can be authorized just like any other local folder.

Collections can be pinned or unpinned from the Library; pinned choices sort first and stay in the backup metadata. This does not change membership or per-destination rotation settings. From an artwork detail, you can also copy it to another collection (keep both memberships) or move it (confirm before removing the source membership). Both operations only change library links; they preserve the protected original and editing sidecar.

**Sync & sources** also provides explicit optional trusted HTTPS Image/WebDAV/Nextcloud/RSS/flat-numeric-JSON connections. Credentials are Keystore-encrypted, not included in backups. Choose the destination collection before saving a source. WebDAV/Nextcloud collection URLs must end with `/`; use an authorized server/app password and supply artwork rights. Transfers are manual, bounded and cancellable; no polling/backend/account is required. Sockets and DNS bind to the selected network; network changes abort. HTTPS redirects, embedded credentials/query tokens, path escapes and metadata/loopback hosts are refused. Private LAN requires opt-in and a platform-trusted certificate (no TLS bypass).

Feed/data snapshots can be inspected offline; numeric fields can be explicitly copied into a saved scene revision. This is **not continuous live weather/health/media**. Budgets cover successful response bodies, not total carrier/TLS overhead; WebDAV is Depth-1/100 new URLs per sync, not a recursive cloud client. Source behavior has not been tested against an actual provider/device yet.

Landscape uses a navigation sidebar and multi-pane control/editor layouts; narrow windows use a compact workspace menu. High contrast, density, keyboard navigation, and Material You are optional. Keyboard: Ctrl+Right/Left Next/Previous, Ctrl+L Library, Ctrl+E Edit, Ctrl+Enter Apply. Controls have text or accessibility labels; physical TalkBack/S Pen/DeX verification remains pending.

See [PERMISSIONS.md](PERMISSIONS.md), [PRIVACY.md](PRIVACY.md), and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
