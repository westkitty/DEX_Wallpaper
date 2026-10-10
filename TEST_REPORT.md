# Verification report

## Initial foundation checkpoint — 2026-10-10

Executed in Arena:

- Repository identity/status/remote-head inspection: PASS; authorized repository has an existing initial README commit.
- `python3 tools/update_feature_matrix.py`: PASS; generated 276 individually tracked requirements.
- `python3 tools/validate_feature_matrix.py`: PASS; unique IDs, required fields, status/evidence references, totals, Markdown reconciliation.
- `python3 tools/check_source_policy.py`: PASS after correcting an overbroad `clear()` matcher (a queue clear is not a wallpaper write).
- Official wrapper SHA-256: `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`; matches Gradle v8.11.1 release.
- `git diff --check`: executed before checkpoint commits.

**Local Android build: NOT RUN.** `java`, `gradle`, `sdkmanager`, and `adb` are absent. A JDK download was attempted; the outbound policy blocks the GitHub release-assets redirect. Google Maven/SDK endpoints are also not available locally. No local compilation success is claimed.

**CI Android build: pending the first push.** The workflow runs `testDebugUnitTest lintDebug assembleDebug` on Ubuntu with JDK 17 and SDK 35, validates inventory/wrapper, and uploads the real debug APK and reports if successful.

Committed but not yet executed: 35 JVM test methods covering no-repeat/fair shuffle (10/100/1,000 assets), persistent queue determinism, filters, explicit destination safety, policy/OEM failures, rule priorities/conflicts/overnight windows, schedule math, crop placement/downsampling, and local dHash distance. Android integration tests cover import/deduplication/rendering/missing-file detection/corrupt inputs. Instrumentation requires an emulator/device.

## Mandatory device acceptance still outstanding

Install → actual dashboard → authorize folder → thumbnails → collection → shuffle/interval → Home-only preview/application → visible Home change → next → independent Lock preservation → process recreation persistence.

Also pending: Samsung Tab S9 Ultra portrait/landscape, DeX resizing, S Pen, keyboard focus, TalkBack, system picker destinations, real wallpaper engine preview/visibility/surface recreation, background scheduling/reboot, permission revocation, large-photo memory profiling. An emulator result alone does not certify OEM wallpaper isolation.

## CI repair checkpoint

First CI run: https://github.com/westkitty/DEX_Wallpaper/actions/runs/38083429516 — **FAIL**, at `android-actions/setup-android`, before Gradle or app compilation. Full-log download is blocked by the sandbox's `results-receiver.actions.githubusercontent.com` redirect restriction. Job/check APIs are accessible; no detailed SDK error is fabricated. Check annotations also report deprecated Node 20 / setup-java v4.

Repair: use the verified preinstalled Ubuntu 24.04 SDK 35 instead of a redundant SDK tool download; verify SDK files explicitly. Upgrade build actions to actual current releases read from GitHub and pin their SHAs. Emit bounded Gradle failure / test-count annotations to the accessible check API. This is an infrastructure repair, not evidence that the application compiles yet.

Local Python validator regression tests: **8 passed** (`python3 -m unittest discover -s tools/tests -v`).

## Foundation compiler/lint checkpoint

https://github.com/westkitty/DEX_Wallpaper/actions/runs/38083566334 — **FAIL** at `lintDebug`: `WallpaperColors.fromBitmap` requires API 27, but minSdk is 26. The corrected hosted-SDK setup and Kotlin compilation reached Android lint. Repair: explicitly guard color extraction/notification at API 27 and report that capability accurately. No lint suppression or feature deletion. Full test-count evidence is pending a successful verification run.

## Creative / recovery checkpoint (pending CI execution)

Added actual offline procedural pixels, shared-library generation, versioned layers, safe numeric formulas, time-scrubbed scene rendering, revision persistence, bounded metadata/original-image ZIP backup, sidecars, sanitized diagnostic export, bounded thumbnails and same-hash missing-image recovery. Added 21 further JVM test methods (formula/scene/cache/backup), and instrumented generation/scene/recovery/backup/Home–Lock identity/Activity recreation tests.

A separate API 35 emulator workflow is configured on implementation pushes and manual dispatch. It never requests sandbox root or uses Docker; it selects existing KVM access or software acceleration. The workflow emits exact test counts to check annotations. Until GitHub reports executed results, these tests are **NOT RUN**. Samsung visual wallpaper isolation remains a distinct physical-device acceptance item.

## First passing native build

**PASS** — https://github.com/westkitty/DEX_Wallpaper/actions/runs/38083970257 at `76c1f4288de4b71dbb115cd4c8849d6703c70efb`.

GitHub check annotation confirms **35 JVM tests, 0 failures/errors**, `lintDebug` and `assembleDebug` succeeded. Actual APK SHA-256: `19ab9ea678add1f56abce0a5cdd83fdf20e86895b2b30a5e71d2a91c3bc4340a`.

Uploaded debug APK artifact: https://github.com/westkitty/DEX_Wallpaper/actions/runs/38083970257/artifacts/11681253974 (`dex-wallpaper-debug`, 18,264,582 archive bytes; GitHub reports unexpired). Verification reports artifact: `11681184031`. This verifies the foundation commit, not uncommitted extensions or Samsung device behavior.

## Creative compiler repair

Run https://github.com/westkitty/DEX_Wallpaper/actions/runs/38084305918 — **FAIL**, `compileDebugKotlin`, CreativeWorkspaces.kt line 31. `Composition` collided with Compose runtime's `Composition` factory through wildcard imports. Repair: explicit `ArtComposition` alias for the procedural domain model. Improve CI diagnostics to retain adjacent compiler signatures and report available JVM XML results independently of a later lint failure. No failing test or feature is removed.

## Passing creative/recovery build

**PASS** — https://github.com/westkitty/DEX_Wallpaper/actions/runs/38084606094 at `8afef3122ecf0db5702d10107efcbcdaef16fdf9`.
GitHub confirms **56 JVM tests, 0 failures/errors**, Android lint and APK compilation, and the real Gradle `wrapper` task. APK SHA-256 `43ba7791bb1636b1d1474df46b039f7017ab29d659da595db4c6e336da24b718`; actual debug artifact https://github.com/westkitty/DEX_Wallpaper/actions/runs/38084606094/artifacts/11681394662 . Instrumentation run `38084606059` is still pending; no emulator success is claimed.

## Native surfaces / MotionGlass checkpoint

Added real ES2 rendering/SurfaceView previews/transitions/effects with resource accounting and adaptive policies; Home-only widget/tile/shortcuts; explicitly authorized external commands; DeskShift profile controls; packaged license reader; serial editor writes, undo and numeric precision. New command/profile/adaptive-policy tests and GPU readback tests are committed for the next CI run, not yet executed. The prior passing checkpoint remains available if these extensions fail validation.

## Native/compiler and emulator defects

Run https://github.com/westkitty/DEX_Wallpaper/actions/runs/38085741752 — **FAIL**, RenderTelemetry.kt conditional mixed Double and Long. Repair converts the last frame duration explicitly to Double.

Run https://github.com/westkitty/DEX_Wallpaper/actions/runs/38084606059 — **FAIL**, instrumentation process crashed; **0 reported executed test results**. Gradle reported 15 discovered tests, no passing result files. This is not counted as device success. Improve the instrumentation harness to capture AndroidRuntime crash evidence through check annotations, preserving all tests. Exact crash cause is not yet known; full log redirects remain sandbox-blocked.

## Verified native build and pending live-isolation repair

**PASS** — https://github.com/westkitty/DEX_Wallpaper/actions/runs/38086139810 at `f8c6e6b718c337e2dd69cfeaa85366a16595f1f5`.
Actual check evidence: **70 JVM tests, 0 failures/errors; lintDebug and assembleDebug**.
Debug APK artifact: https://github.com/westkitty/DEX_Wallpaper/actions/runs/38086139810/artifacts/11682677562 ; SHA-256 `fe70972bcfbd97acda58dace9527f2a79f33cd47a21edffe13b41990b446c64d`.
Verification report artifact: `11682797175`.

Device run https://github.com/westkitty/DEX_Wallpaper/actions/runs/38085741750 at `23dc9ed` has now **FAILED**, not passed. That ref contains the already repaired telemetry compiler defect; no passing device results are claimed. Manual workflow dispatch is denied by the integration's Actions permissions (HTTP 403); normal branch pushes work. The workflow is meaningfully extended to upload bounded crash/Gradle evidence, triggering another device run without changing main or asking for credentials.

Critical source review found that installed live engines watched the static Home record; Home-only static changes could therefore rebind live Lock content indirectly. The repair removes that observer and stores immutable per-destination live snapshots, gates updates on Android 14+ engine ownership, separates previews, refuses pending-preview replay, serializes ownership changes, and rejects stale preference/decode generations. Five new JVM isolation tests, an equal-ID static preflight regression, and a second actual EGL-context test are authored, **not yet executed**. Current repair: BUILD UNVERIFIED until its own CI result. Physical Samsung isolation and picker/lifecycle behavior remain unverified.

Local checks for this repair: 276-entry inventory validator, source policy including live/static separation, **8 Python validator tests**, Python syntax, Android resource XML parsing, and whitespace/staged inspection PASS. No local Java/SDK/adb is available. No failed tests have been deleted or disabled.

## Passing live-isolation build / optional source checkpoint

**PASS** — https://github.com/westkitty/DEX_Wallpaper/actions/runs/38087668672 at `ab0822cac0797038d1be41a0461d44d7f66934b5`.
Check annotations confirm **76 JVM tests, 0 failures/errors**, lintDebug and assembleDebug. APK SHA-256 `ca06f4bfe9d72440e5b173ded58d1255424cb56cd0c48725594b49d254752a9e`; artifact https://github.com/westkitty/DEX_Wallpaper/actions/runs/38087668672/artifacts/11682024693 ; reports `11683170074`.
The five live-isolation tests and equal-ID static policy regression really executed. This does **not** verify OS live picker outcomes, engine lifecycle or Samsung Lock pixels. Device run `38087668819` is in progress with the preserved crash harness; no success inferred.

Optional source extension: Room v3 additive migration, non-cascading collection upsert, native HTTPS/WebDAV/Nextcloud/RSS/numeric JSON configuration/sync/inspection/scene-binding UI, Keystore credential vault, same-library verified imports/provenance, pinned-network socket/DNS/policy checks, literal/DNS address safeguards, bounded retries/pacing/budgets, cancellation and atomic cache cleanup. Fourteen further JVM methods are authored (7 network policy, 4 XML, 3 numeric data), **90 total authored**; not yet executed for this extension. Real Keystore and v1→v3/reopen/membership tests are authored; no passing device evidence yet. No server, credential, physical-device or power acceptance is invented. Current source extension: BUILD UNVERIFIED until its own CI run.

## Source compiler repair and emulator infrastructure failure

Source run https://github.com/westkitty/DEX_Wallpaper/actions/runs/38088800616 at `c3235710923b33394f5736be87fe3acfbe3437ce` — **FAIL**, `compileDebugKotlin`, SyncCoordinator.kt line 48: OkHttp 4.12 `Dns` is not a Kotlin fun-interface; a lambda cannot implement it. Repair uses an explicit `object : Dns` with `lookup`, retaining pinned-network/address safeguards. No test or capability is removed. The source extension's 90 authored JVM methods have not executed in this failed compiler run.

Device run https://github.com/westkitty/DEX_Wallpaper/actions/runs/38087668819 at `ab0822c` — **FAIL before APK installation**, **0 reported instrumentation tests**. Installer error: `cmd: Can't find service: package`. Captured AndroidRuntime evidence shows system-server death / cascading `DeadSystemException`; “Lost network stack” is explicitly a secondary consequence, not an established root cause. This is **not** evidence of an application crash or passing app acceptance. Actual preserved device report artifact `11683876719` includes bounded Gradle/runtime evidence.

Existing Ubuntu KVM access was unavailable without privileges; software emulation was used. Do not apply root/chmod/udev workarounds. The next diagnostic run uses the verified official `macos-15-intel` runner label, requests existing HVF acceleration, disables snapshots and reduces guest RAM to 2GB. Crash-buffer capture is added for native/system failures. All tests are retained. This is a materially different infrastructure attempt, not an unchanged infinite retry; emulator availability remains unverified until it boots/executes.

## Historical optional source checkpoint (superseded below)

**PASS** — https://github.com/westkitty/DEX_Wallpaper/actions/runs/38089299045 at `daf52c1b2a64b9cade47a3236a3cb25e4efa1079`.
Actual annotations confirm **90 JVM tests, 0 failures/errors**, lintDebug, assembleDebug and wrapper task success. Fourteen source-policy/XML/numeric methods genuinely executed after the Dns repair. APK SHA-256 `e5616eb6f73536d2ef5c972ad337f645ab46513829903e2d65d80e022abf5db0`; artifact https://github.com/westkitty/DEX_Wallpaper/actions/runs/38089299045/artifacts/11683826676 ; reports `11683496817`.
Provider requests, network switching and Keystore/device behavior remain unverified. At this checkpoint accelerated device run `38089299077` was pending; its actual failure is recorded below.

At that checkpoint new hardening awaited its own run: capture/check unaffected wallpaper IDs/current DEX component after the live picker; refuse known Home-only ownership as a successful Both picker result; stop automatic writes after any attempted-but-unverified platform operation; return actual command failures to WorkManager/widgets/status; preserve the newest equal editor undo; apply the combined cache budget after local imports. Three further picker regression methods and three command-completion methods authored: **96 JVM methods total**, subsequently all executed at 33c9a2a. One post-write verification regression expands instrumentation to **15 authored methods**, with no passing result claim. No test is disabled or removed.

## Hardening build PASS / accelerated emulator unavailable

**PASS** — https://github.com/westkitty/DEX_Wallpaper/actions/runs/38089991851 at `33c9a2a5ab68aca5ccc4d0c48deff8898238136b`.
Actual check annotations: **96 JVM tests, 0 failures/errors**, lintDebug and assembleDebug PASS. App APK SHA-256 `4aada029c4bd26d97a1c0db359dfe54d898fa969fd12e14636e0f0ecf98e1366`; debug artifact https://github.com/westkitty/DEX_Wallpaper/actions/runs/38089991851/artifacts/11683352853 ; reports `11683882481`. The six new picker/command-completion methods really executed; device behavior is not inferred.

Accelerated device run https://github.com/westkitty/DEX_Wallpaper/actions/runs/38089299077 at `daf52c1` — **FAIL**, timeout waiting for emulator boot, before instrumentation/harness execution. No report files/test results were produced. Existing macOS HVF was requested without privileged modifications; actual failure cause beyond boot timeout is unknown. Neither this nor Ubuntu system death is an application acceptance result. No unchanged/infinite emulator retry, test removal, root or Docker workaround.

The ee24a43 CI improvement explicitly assembles the real instrumentation APK in ordinary Android CI and checks that the file exists; absence of JVM XML/results cannot be mislabeled PASS. Python validator regressions also run remotely. This separates test-code compilation from unavailable device boot. Runtime tests remain outstanding, including physical Samsung/DeX, actual SAF picker journeys and visible Home–Lock isolation.

## Instrumentation compilation independently verified

**PASS** — https://github.com/westkitty/DEX_Wallpaper/actions/runs/38090533888 at `ee24a435b0b59e8173f62b1b95f545742c80dd41`, job/check `114325863215` (4m5s).
Actual annotations confirm **96 JVM tests, 0 failures/errors**, lintDebug, assembleDebug and **assembleDebugAndroidTest** PASS. The eight Python validator regressions and source/inventory checks passed remotely. Wrapper verification/regeneration and all three artifact uploads succeeded.

- App APK: https://github.com/westkitty/DEX_Wallpaper/actions/runs/38090533888/artifacts/11684455076 ; SHA-256 `92fadd0fbc605a876e28faf456e938d0bf191952db76ac134363d748598b11f3`.
- Instrumentation APK: https://github.com/westkitty/DEX_Wallpaper/actions/runs/38090533888/artifacts/11684245354 ; SHA-256 `d613720072e1d5ab9c56ae48cc42c38050d39e47e7619bba4fa8205a78b03c30`.
- Reports artifact `11684375144`.

**15 native methods compiled; 0 known passed on a device.** Test APK existence/digest is compilation evidence only. The prior boot timeout/system-service failures are retained, not relabeled PASS. No physical Galaxy Tab S9 Ultra, actual picker/import/restart journey, visible unaffected-screen proof or power benchmark exists. Follow [DEVICE_ACCEPTANCE.md](docs/DEVICE_ACCEPTANCE.md) on a disposable test device; do not run wallpaper-modifying tests on a personal device without explicit consent. All 276 requirements/statuses remain unchanged (4/140/61/2/2/67).

Non-blocking build notice: pinned Gradle 8.11.1 is marked out-of-date by setup-gradle. No untested toolchain upgrade was substituted for this passing checkpoint. APKs are debug-signed, not production releases.

## Prior delivered HEAD independently rebuilt

**PASS** — https://github.com/westkitty/DEX_Wallpaper/actions/runs/38091097278 at `64906f780d3802c03cd18ca6ee1b3b91159ac1a8`, job/check `114327520587`, completed `2026-10-10T22:25:59Z`.
Actual annotations reconfirm **96 JVM tests, 0 failures/errors**, lintDebug, assembleDebug and assembleDebugAndroidTest; inventory/source/eight Python validator tests, wrapper tasks and three uploads PASS.

- App APK: https://github.com/westkitty/DEX_Wallpaper/actions/runs/38091097278/artifacts/11684116776 ; SHA-256 `42d418a40277609359d53bec3567a95509f072a5f7991690e405e8c6ef7cd145`.
- Instrumentation APK: https://github.com/westkitty/DEX_Wallpaper/actions/runs/38091097278/artifacts/11684416069 ; SHA-256 `db334a05823e2f0a9b7160881cdd0ccb851de0dae39e8ef156829ba92f576365`.
- Reports artifact `11684650804`. **15 compiled native methods, 0 known runtime passes**, no physical Samsung acceptance.

## Delivered handoff HEAD independently rebuilt

**PASS** — Android CI run `38091772969` at delivered HEAD `33c3ed0374341ba7a44e550a9859cfc79d717de0`. GitHub check `114329481827` confirms **96 JVM tests, 0 failures/errors**, lintDebug, assembleDebug, assembleDebugAndroidTest, feature/source checks, wrapper regeneration, and all three uploads PASS. This HEAD changes no Android source from the 64906f7 native checkpoint; this verifies the delivered baseline only.

- App artifact `11684582139`, actual APK SHA-256 `14436db301841dde21888e990e8cc6433c0e6b91f93a3d20d17c2453e2485b47`.
- Instrumentation APK artifact `11684083177`, SHA-256 `501122f9082570722f67584f608a45fa46ba6ba0c61d20d42199fde7e96412a1`.
- Reports artifact `11684152784`. The Gradle 8.11.1 out-of-date message is non-blocking.

## Arena continuation changes — Android CI PASS, device acceptance pending

This session continued the existing delivered `33c3ed0` implementation on the assigned `arena/43998994-dex-wallpaper` branch. The import path now persists/validates the selected destination collection before writing membership; document-tree traversal reads a bounded cursor and counts every file/folder against 5,000 entries / depth 16; a reached limit is explicitly reported instead of silently truncating; Compose-owned thumbnails and artwork previews release their bitmaps when disposed. Implementation statuses and the 276-row scope are preserved.

Added **4 JVM test methods** for the pure traversal budget and **1 repository instrumentation method** for import → collection → Home-only shuffle/interval → database reopen/Next, using a recording fake wallpaper platform. Android CI executed all 4 JVM budget tests; matrix row 038 now records `JVM_POLICY_PASS_DEVICE_PENDING`, while its overall implementation status stays `IMPLEMENTED_UNTESTED`. The repository journey is not an actual Android picker/WallpaperManager/OEM/visible-screen test.

**Android CI PASS** — run https://github.com/westkitty/DEX_Wallpaper/actions/runs/38092686248, implementation commit `28e0acc1f3d8b9e3f0dc2299398782ad3679cd30`, check/job `114332178631`. Actual annotations confirm **100 JVM tests, 0 failures/errors**, lintDebug, assembleDebug and assembleDebugAndroidTest, feature/source checks, Python checks, and artifact uploads PASS. The pure traversal-budget behavior ran; the test APK was built but no instrumentation methods ran on a device. App APK artifact `11684796718`, SHA-256 `6f64b50f88675f786b78bce9cf8b88a501bc6caf17d430c2eafb889c80f370a`; instrumentation APK artifact `11685175722`, SHA-256 `cdf094581c1b7fc73b22ebd96783402edec4e84dd71977c0ed25500d0b50c636`; reports artifact `11684856283`. This CI build compiles **16 native methods; 0 known runtime passes**.

Locally executed after the source edits: inventory validation PASS, source-policy PASS, **8 Python tests PASS**, all 8 Android XML files parse, and `git diff --check` PASS. This sandbox has no JDK/Java, Gradle, Android SDK, adb, or Kotlin compiler; Android CI is the Kotlin/build evidence. The implementation commit was normally pushed and `git ls-remote` independently matched its SHA before this verification report update. The 276 implementation-status totals remain **4 / 140 / 61 / 2 / 2 / 67 = 276**.

No device-workflow run executed against this branch/commit. The most recent actual device attempt `38089299077` was inspected via check annotations: “Timeout waiting for emulator to boot,” before test execution; no report files were uploaded. Its detailed runner log endpoint returned EOF. The unavailable log is not treated as app evidence, and no unchanged emulator attempt is made. See `docs/HANDOFF_PROMPT.md` for scope and `docs/DEVICE_ACCEPTANCE.md` for safe real-device acceptance.

## Continued library-path and thumbnail hardening — Android CI PASS, device acceptance pending

The source-bearing change `f4d795e7994ed50c9b321e7fef977dd0afee5a73` centralizes private library/cache file resolution in `LibraryFilePath`: it accepts only bounded direct-child names, canonicalizes against the owning directory and rejects traversal/symlink escapes. `AssetImporter.file` and backup restore now use that resolver. Thumbnail creation uses bounded mutex stripes selected by asset id, repairs missing, empty, or invalid-bounds cached previews, and publishes via unique same-directory atomic temporary files that are cleaned up on failure.

Added **3 pure JVM tests** for valid direct children, absolute/traversal/nested names, and symlink escape. Android CI run https://github.com/westkitty/DEX_Wallpaper/actions/runs/38094246090, check/job `114336744075`, confirms **103 JVM tests, 0 failures/errors**, lintDebug, assembleDebug and assembleDebugAndroidTest, inventory/source/Python checks and artifact uploads PASS. App APK artifact `11685990579`, SHA-256 `1a8c7ee64ba0cd9b6e2278c34e5c3fd54b004884a3bf86000519c0938549b3e1`; instrumentation APK artifact `11685721313`, SHA-256 `9688ebfbe795f747890f7cbf9acc595befeb85b5149479d433b1e6046bb6a7a6`; reports artifact `11685881016`. All **16 native methods compile; none ran on a device**. The latest device run remains boot-timeout `38089299077`; the device workflow list contains no run for this commit.

Matrix row 223 now records `JVM_POLICY_PASS_DEVICE_PENDING` for the passing pure path-policy tests, but remains `IMPLEMENTED_UNTESTED` for real app-private storage/import/cache/restore acceptance. The six implementation-status totals remain 4 / 140 / 61 / 2 / 2 / 67 = 276. Local Python, source-policy, matrix and XML checks pass; local Android/Kotlin builds are unavailable because no JDK/SDK is installed.
