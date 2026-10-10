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
