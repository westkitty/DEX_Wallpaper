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
