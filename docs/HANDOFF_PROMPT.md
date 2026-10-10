# Copy/paste handoff prompt — unfinished DEX Wallpaper mission

Start the next Arena chat with **westkitty/DEX_Wallpaper** and **arena/62a85de1-dex-wallpaper as its starting base**, not main. If Arena assigns a new session branch, work only on that assigned branch, preserving the existing implementation/history. Confirm that the baseline commit below is an ancestor before editing. This file records the last verified application checkpoint; subsequent documentation commits do not change its Android source.

---

Take over the unfinished implementation of **DEX Wallpaper / The Living Wall System**, a real native Android app in exactly **https://github.com/westkitty/DEX_Wallpaper**. Continue existing code; do not bootstrap/recreate the repository, build a website/PWA/mockup, or substitute a plan for implementation. Work autonomously: **BUILD → TEST → explicit STAGE → COMMIT → normal PUSH**, inspect actual GitHub Actions results and repair real failures. Ask only for unresolved external authorization, credentials/licenses or irreversible account decisions; never ask for tokens/passwords or an ordinary “continue.”

## 1. Establish the existing baseline before changes

- Previous delivery branch: `arena/62a85de1-dex-wallpaper`.
- Last independently remote-verified application/documentation checkpoint: **`64906f780d3802c03cd18ca6ee1b3b91159ac1a8`**. It contains the entire implementation, with 13 prior normally pushed commits. The handoff itself is a later documentation commit; inspect current remote history/status rather than assuming the checkpoint is still HEAD.
- `main` remained **`db4ef366986c0b79a7c0ed71c8a50fd82c51ad02`** and does **not** contain this implementation. No PR was opened/merged, no force-push or history rewrite occurred. At most one PR is allowed; do not merge without separate authorization.
- Inspect branch, status, local/remote history and concurrent edits. Preserve unrelated work. Do not switch/create/push an unassigned branch; the new Arena session's assigned branch takes precedence over the old branch name. Ensure the new session starts from the existing work, not the README-only main.
- Read **README.md, ARCHITECTURE.md, FEATURE_MATRIX.md, feature_matrix.json, TEST_REPORT.md, docs/OPERATIONAL_STATE.md, docs/DEVICE_ACCEPTANCE.md, PERMISSIONS.md, PERFORMANCE.md, PRIVACY.md, THIRD_PARTY_NOTICES.md, CONTRIBUTING.md**. The generated 276-row inventory is the exact remaining-work contract; this prompt is not a scope replacement.

## 2. Actual evidence — do not overclaim

**Android CI at 64906f7 PASS:** https://github.com/westkitty/DEX_Wallpaper/actions/runs/38091097278 ; job/check `114327520587`.

- **96 JVM tests executed, 0 failures/errors**; 8 Python inventory-validator tests; inventory/source policy checks; lintDebug; assembleDebug; **assembleDebugAndroidTest**; official wrapper verification/regeneration; all three uploads PASS.
- App APK: https://github.com/westkitty/DEX_Wallpaper/actions/runs/38091097278/artifacts/11684116776 . Actual APK SHA-256 **`42d418a40277609359d53bec3567a95509f072a5f7991690e405e8c6ef7cd145`**.
- Test APK: https://github.com/westkitty/DEX_Wallpaper/actions/runs/38091097278/artifacts/11684416069 . SHA-256 **`db334a05823e2f0a9b7160881cdd0ccb851de0dae39e8ef156829ba92f576365`**.
- Reports artifact `11684650804`. Hashes above identify APK files, not artifact ZIPs. Artifacts were not downloaded into the repository. Debug signing only, no production release authorized.
- **15 authored instrumentation methods compile; 0 known passing device runtime results.** Compilation is NOT launch/import/isolation/acceptance evidence.
- Latest device run **38089299077 FAILED** before emulator boot (macos-15-intel, existing HVF requested, boot timeout, no harness/results/artifacts). Previous Ubuntu **38087668819 FAILED before APK install**, missing package service/system-server death, 0 reported tests, preserved artifact `11683876719`. Actual cause beyond those observed failures is unknown; neither proves an app defect or success. Earlier device crashes remain in TEST_REPORT. Keep failures/tests.
- Previous sandbox had no Java/Android SDK/adb. JDK/SDK download routes and full-log redirects were blocked; manual workflow dispatch returned 403. Push-triggered CI and check-annotation/artifact APIs worked. Inspect the new environment once, but do not repeat unchanged downloads/dispatches/emulator attempts indefinitely. No root/chmod/udev/Docker workaround. A Gradle 8.11.1 out-of-date notice is non-blocking, not proof of a current toolchain.

## 3. Full scope remains unfinished

Exact inventory counts at the baseline:

| Status | Count |
|---|---:|
| DONE_VERIFIED | 4 |
| IMPLEMENTED_UNTESTED | 140 |
| PARTIAL | 61 |
| BLOCKED_PLATFORM | 2 |
| BLOCKED_EXTERNAL | 2 |
| NOT_IMPLEMENTED | 67 |
| TOTAL | 276 |

DONE_VERIFIED IDs are **023** shuffle without repetition, **241** fairness-aware shuffle, **273** versioned scene manifests, **274** safe formula evaluation, backed by distinct executed pure tests. No native integration is promoted from compilation. Re-audit each row against actual behavior/evidence; do not blanket-upgrade statuses or delete/renumber requirements.

Retain all groups: A001–036 destination/control/rotation; B037–074 import/library/sources; C075–102 crop/formats/non-destructive edits; D103–141 effects/transitions/GPU; E142–160 procedural/video/animated/lawful portable content; F161–194 programmable scenes/safe formulas/data; G195–208 native integrations/automation; H209–236 lifecycle/storage/offline/security/recovery/accessibility/legal; I237–244 rule/shuffle decisions; J245–252 integrity/duplicates/safe zones; K253–260 DeX/input/accessibility; L261–268 budgets/recovery/migrations/profiles; M269–276 source/scene/network safety and adaptive quality. Exact feature names are in `tools/feature_names.txt` and the matrix.

Application ID **com.westkitty.dexwallpaper**, label **DEX Wallpaper**. Kotlin/Compose Material3, Room/DataStore/coroutines/WorkManager/SAF, real WallpaperManager/WallpaperService. Offline core without account/backend/ads/telemetry. Primary hardware **Galaxy Tab S9 Ultra, 2960×1848 landscape**, also portrait, phones/tablets, split-screen and practical resizable Samsung DeX. Technical editorial near-black/charcoal/red/white UI, meaningful multi-pane tablet layouts and compact navigation, actual keyboard/mouse/stylus/TalkBack/large-font/reduced-motion/high-contrast support.

Keep 12 meaningful workspaces: Control Center, Library, Rotation Lab, Image Editor, Motion Studio, Generative Canvas, Scene Composer, Context Automation, Live Data, Sync and Sources, System Health, Settings. No inert toggles, fake previews/data, blanket error swallowing or hundreds of empty screens. Keep one shared library DB and integrated ShuffleGuard, Chronoscape, FrameSense, QuietCanvas, DeskShift, Ambient Desk, MotionGlass, Living Epochs, Wallpaper Bridge and Curator Engine. Living Epochs may accept user-provided Starsilk artwork/data; invent no Starsilk canon.

## 4. Non-negotiable safety

**Home and Lock are independent; default HOME ONLY.** Centralize every static write. Require explicit Home/Lock/Both and separate collections/settings/schedules. If a destination cannot be guaranteed/verified, fail safely; never silently fall back to Both. Shared/unreadable static Lock identity intentionally blocks Home-only until the user establishes independent Lock artwork.

Live picker/OEM final destinations are different from static writes. Existing immutable Home/Lock/shared live snapshots and separate preview draft must stay isolated from static records/mutable edits. Android 14+ public engine ownership gates single-destination live updates; shared/unknown/legacy owners must refuse unsafe requests. Picker completion checks component, before/after identities and ownership; known Home-only assignment is not Both. Do not assume arbitrary simultaneous Home/Lock live services or safe rollback of an OS picker choice. Unaffected IDs alone are not visible-pixel proof.

Keep one authoritative state and scheduler/writer, no competing timers. WorkManager intervals are at least 15 minutes and approximate/delay-prone. No unnecessary exact alarms/wakeups/polling. Correct per-engine visibility/surface/orientation/preview/process/GPU lifecycle, bounded decoding/cache/textures, hidden-render/decoder stop and cleanup, actual power-aware pacing. Do not invent launcher icon coordinates; safe zones are manually calibrated.

Optional permissions only on activation; SAF/scoped storage/protected originals; no broad file access. Public repo: no secrets/signing keys/private photos/user DBs/private paths/health/location/credentials. Keystore for remote credentials; validated HTTPS origins/allowlists/DNS/redirects/timeouts/backoff/budgets; bounded parsing/formulas, no arbitrary executable/binary imports. No unlicensed/proprietary/incompatible artwork or dependencies; keep license notices current.

## 5. What exists and where

Base: `app/src/main/java/com/westkitty/dexwallpaper/`.

- `ui/{Components,CoreWorkspaces,CreativeWorkspaces,SystemWorkspaces,DexViewModel,Theme,Workspace}.kt`, MainActivity/DexApplication: 12 native workspaces/navigation/state/actions. Do not guess nonexistent UI files.
- `data/{Database,Preferences,AssetImporter,WallRepository,BackupCodec,BackupRestoreCoordinator,ProfileRepository}.kt`: one Room v3 DB, additive migrations, SAF/private copies/metadata/thumbnails/edits, backup/recovery/profiles. Actual migrations/import/restore acceptance still pending.
- `wallpaper/{WallpaperDestinationController,BitmapPipeline,DexWallpaperService,GlWallpaperRenderer,LiveEngineRegistry,RenderTelemetry,ShaderPreviewView}.kt` plus `core/{DestinationPolicy,LiveConfiguration,RenderPolicy}.kt`: central static policy; attempted-but-unverified operations stop automatic writes; independent revisioned live snapshots, decode generations, GLES/Canvas fallback/resource policy. Real OEM/GPU/lifecycle acceptance pending.
- `worker/RotationScheduler.kt`, `integration/{AutomationController,DexWidgetProvider,NextWallpaperTile}.kt`, `core/AutomationCommand.kt`: persistent approximate rotations and Home-only native actions; real Outcome propagated to WorkManager/result/status, not unconditional success. Actual worker/widget/tile journeys pending.
- `creative/`, `core/{SafeFormula,SceneManifest,ShuffleGuard,Chronoscape,FrameSense,CachePolicy,VisualProfile}.kt`: offline generators, static layer/formula scene editing/export/revisions, fair queue/rules/framing/cache/profile logic. Video/GIF/Vulkan and continuously layered live scenes remain incomplete.
- `sources/SyncCoordinator.kt`, `security/CredentialVault.kt`, `core/{NetworkPolicy,XmlFeeds,NumericDataCodec}.kt`: optional manual HTTPS Image/WebDAV/Nextcloud/RSS/flat numeric JSON setup/sync/cache/provenance/numeric scene binding. Pure policies pass; real providers/Keystore/network switching/cancellation/budgets remain unverified. This is not a recursive account browser or continuously live weather/health/media client.
- Tests: `app/src/test/` (96 JVM), `app/src/androidTest/` (15 native methods including isolation, real Bitmap post-write verification failure, migrations, Keystore, GLES and navigation).
- CI: `.github/workflows/android-ci.yml`, `.github/workflows/android-device-tests.yml`; evidence harnesses `tools/run_android_checks.py`, `tools/run_android_instrumentation.py`. App + instrumentation compilation is separate from unavailable boot/runtime.

## 6. Immediate priorities — implementation, not another plan

1. Inspect/check the current baseline and latest actual CI. Run local inventory/source/Python checks. Repair any real regression; do not remove tests/features to obtain green checks.
2. Highest-value acceptance is **real dashboard launch → Android individual/folder picker → real thumbnails → persistent collection → shuffle + interval → Home-only preview/apply → Next changes Home without changing Lock → restart preserves state**. Follow `docs/DEVICE_ACCEPTANCE.md`; obtain actual safe test-device evidence if available. WallpaperIsolationTest intentionally seeds both wallpapers: do NOT run on a personal device without explicit consent. Test pixels and identities, cancellation/revoked grants/corrupt/missing files and restart.
3. Then mixed static/live Home–Lock isolation, ownership/picker result timing/shared-to-single reassignments/revisions/stale frames; visibility/orientation/process/GPU recovery; worker/widget/tile failures; import/cache/backup/migration boundaries; optional provider/Keystore/network behavior; Galaxy/DeX/S Pen/TalkBack/large-font/input/power/performance. Record actual outcomes, not invented benchmarks.
4. If stable hardware is unavailable, state that honestly and keep making meaningful code/test progress from the 67 NOT_IMPLEMENTED and 61 PARTIAL rows, dependency-aware and end-to-end. Do not waste the session on repeated infrastructure attempts or documentation-only activity. Examples outstanding include daily/clock/day schedules and event-triggered rotation, animated formats, advanced scene/data/scheduling/provider journeys. Keep platform/external blocks truthful; ask only where actual outside authorization is needed. Prioritize completed verified journeys if all 276 cannot fit; no scope downgrade or “entire product complete” claim.

## 7. Delivery discipline

```sh
python3 tools/validate_feature_matrix.py
python3 tools/check_source_policy.py
python3 -m unittest discover -s tools/tests -v
# After deliberate per-row evidence/status edits in tools/feature_overrides.json:
python3 tools/update_feature_matrix.py
python3 tools/validate_feature_matrix.py
# With a real JDK/SDK available:
python3 tools/run_android_checks.py build
```

Before each cohesive commit: inspect status/full diff, run applicable checks and `git diff --check`, explicitly stage intended files, review the full staged diff and public-data/secrets scan. Do not stage caches/SDK/configs/build output/artifacts. Keep all required root docs and `docs/OPERATIONAL_STATE.md` truthful. Matrix fields per ID: id/name/subsystem/source_file/ui_location/implementation_status/verification_status/test_reference/permissions/api_constraints/dependencies/limitations. DONE_VERIFIED needs distinct passing behavior evidence, not a compile or authored test.

Use local git for status/diff/commit/push, authenticated gh for GitHub checks/PRs/issues/releases. Normally push only the Arena-assigned session branch; independently compare `git rev-parse HEAD` with `git ls-remote`/API. Never force-push, rewrite public history, delete branches/repo, merge unrelated work or bypass protections. No production release without authorization. Inspect real Actions annotations/artifacts; report pending/failure/not-run honestly. If authentication fails, request reconnection in Arena, not credentials.

Final report must be brief/factual: exact repo/assigned branch/remote-verified SHA/clean-or-dirty state/session commits/push result/PR if any; actual Android build/Actions state and APK location; all six counts totaling 276; executed tests; implemented capabilities vs unverified runtime; material defects/gaps and highest-value next action. Do not finish with a fabricated acceptance or ask the user to “continue.”
