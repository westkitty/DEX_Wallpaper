# Durable operational state

Date: 2026-10-10 (UTC). Authorized repository: https://github.com/westkitty/DEX_Wallpaper.
Session-fixed branch: `arena/43998994-dex-wallpaper`. The source-bearing implementation commit `28e0acc1f3d8b9e3f0dc2299398782ad3679cd30` was normally pushed and independently matched via `git ls-remote`; its Android CI run `38092686248` passed. This report/matrix follow-up is documentation/evidence reconciliation only; `main` remains based at `db4ef366986c0b79a7c0ed71c8a50fd82c51ad02`. No PR/merge/force-push.

## Latest implementation delivery

The current session continued the existing delivered `33c3ed0374341ba7a44e550a9859cfc79d717de0` implementation on the Arena-assigned branch, then added bounded SAF traversal, import/collection validation, bitmap disposal and journey coverage. Source commit `28e0acc1f3d8b9e3f0dc2299398782ad3679cd30` is remote-verified; this operational-state update documents the evidence from that commit.

Android CI run **38092686248, PASS** (check/job `114332178631`): 100 JVM tests / 0 failures, lintDebug, assembleDebug, assembleDebugAndroidTest, inventory/source/Python checks and all uploads passed. App APK SHA-256 `6f64b50f88675f786b78bce9cf8b88a501bc6caf17d430c2eafb889c80f370a`, artifact `11684796718`; instrumentation APK SHA-256 `cdf094581c1b7fc73b22ebd96783402edec4e84dd71977c0ed25500d0b50c636`, artifact `11685175722`; reports artifact `11684856283`. These establish JVM behavior and APK/test APK compilation, not instrumentation runtime or device acceptance.

Thirteen prior application/session commits before the docs handoff: `44399d6`, `6c3194a`, `76c1f42`, `40df6b6`, `8afef31`, `23dc9ed`, `f8c6e6b`, `ab0822c`, `c323571`, `daf52c1`, `33c9a2a`, `ee24a43`, `64906f7`.

## Actual execution evidence

- **Latest Android PASS at 28e0acc**: run `38092686248`, job/check `114332178631`; **100 JVM tests, 0 failures/errors**, lintDebug/assembleDebug/**assembleDebugAndroidTest**. Inventory/source/Python checks and all three uploads PASS. App artifact `11684796718`, SHA-256 `6f64b50f88675f786b78bce9cf8b88a501bc6caf17d430c2eafb889c80f370a`; test APK `11685175722`, SHA-256 `cdf094581c1b7fc73b22ebd96783402edec4e84dd71977c0ed25500d0b50c636`; reports `11684856283`. **16 native methods compiled, 0 known device passes**. Four traversal-budget JVM methods executed; row 038's implementation remains unverified.
- Prior Android PASS at ee24a43: run `38090533888`, job/check `114325863215`; **96 JVM tests, 0 failures/errors**, lintDebug/assembleDebug/**assembleDebugAndroidTest**. Eight Python regressions/inventory/source/wrapper checks and three uploads PASS. App artifact `11684455076`, SHA-256 `92fadd0fbc605a876e28faf456e938d0bf191952db76ac134363d748598b11f3`; test APK `11684245354`, SHA-256 `d613720072e1d5ab9c56ae48cc42c38050d39e47e7619bba4fa8205a78b03c30`; reports `11684375144`. **15 native methods compiled, 0 known device passes**.
- Prior Android PASS at 33c9a2a: run `38089991851`; **96 JVM tests, 0 failures/errors**, lintDebug/assembleDebug. App artifact `11683352853`, SHA-256 `4aada029c4bd26d97a1c0db359dfe54d898fa969fd12e14636e0f0ecf98e1366`; reports `11683882481`.

- Prior Android PASS at daf52c1: run `38089299045`; **90 JVM tests, 0 failures/errors**, lintDebug/assembleDebug. APK artifact `11683826676`, SHA-256 `e5616eb6f73536d2ef5c972ad337f645ab46513829903e2d65d80e022abf5db0`; reports `11683496817`. Accelerated device `38089299077` FAILED at emulator boot timeout; no test execution or result artifacts.
- Prior Android PASS at ab0822c: run `38087668672`; **76 JVM tests, 0 failures/errors**, lintDebug/assembleDebug. APK artifact `11682024693`, SHA-256 `ca06f4bfe9d72440e5b173ded58d1255424cb56cd0c48725594b49d254752a9e`; reports `11683170074`. Device `38087668819` FAILED before APK install (package service missing / system death); 0 reported tests. Artifact `11683876719`.

- **Android PASS at f8c6e6b**: run https://github.com/westkitty/DEX_Wallpaper/actions/runs/38086139810 ; **70 JVM tests, 0 failures/errors**, lintDebug, assembleDebug.
- Real debug APK: https://github.com/westkitty/DEX_Wallpaper/actions/runs/38086139810/artifacts/11682677562 ; SHA-256 `fe70972bcfbd97acda58dace9527f2a79f33cd47a21edffe13b41990b446c64d`. Reports `11682797175`.
- Earlier creative/recovery checkpoint `8afef31`, run `38084606094`: PASS/56 JVM/lint/APK. Foundation `76c1f42`: PASS/35.
- **Device FAIL**, run `38084606059`: instrumentation process crashed, 15 discovered tests, **0 reported executed result files**; cause still unknown.
- Device `38085741750` at the old telemetry-defective `23dc9ed` also FAILED. No known passed instrumentation or physical Samsung result.
- Local toolchain absent. Do not retry blocked JDK/SDK downloads. GitHub Actions is the Android verification path. Manual dispatch is permissions-denied; normal push-triggered workflows work.

## Current safety repair (build/JVM verified, device pending)

Installed live engines now use independent immutable Home/Lock/shared snapshots and a separate preview draft, never static Home records or mutable library edits. A new explicit commit is required for source/effect changes. Android 14+ public engine ownership gates Home/Lock-only commits; unknown/shared/older ownership refuses those operations. OS picker assignment still needs user review. Engine creation never replays a cancelled/pending preview. Preference revisions and decode generations reject stale emissions/frames; per-engine serialized ownership reassignment preserves shared content on splitting into a single destination. EGL context ownership and frame coalescing are repaired.

Five pure live-isolation tests and equal-ID preflight regression PASS as part of 76 JVM tests; multi-context instrumented GPU test remains unverified. Local inventory/source/XML/Python/staged checks pass. The next device workflow preserves crash and Gradle evidence as artifacts; all failing tests are retained.

## Native capabilities implemented (runtime acceptance pending)

Responsive 12 Compose workspaces; SAF individual/folder imports, protected originals/EXIF/thumbnails/collections; non-destructive persistent precision editor/undo; fair shuffle/history/filtering; centralized static Home/Lock/Both preflight and independent approximate schedules/rules/forecasts; five real offline generators; static bounded layer/formula scene editor/export/revisions; backup/recovery/sidecars/sanitized diagnostics/cache budgets; native Home-only widget/tile/shortcuts and opt-in scoped commands; real GLES effects/transitions/local renderer counters; six/custom DeskShift profiles and packaged legal reader.

Delivered optional source checkpoint adds Room v3/configuration, Keystore credentials, manual trusted HTTPS/WebDAV/Nextcloud/RSS/flat numeric JSON transport and native source journeys. Its first build run `38088800616` FAILED on an actual OkHttp Dns lambda type mismatch; explicit Dns implementation is fixed and PASS at daf52c1. Its 14 new JVM methods really passed (90 total). v1→v3/reopen/collection-upsert and synthetic Keystore device tests remain unverified. Provider acceptance is still unavailable; no real credential was needed to implement safe setup.

## New hardening checkpoint (Android CI PASS, device pending)

Live picker records/checks before/after identities and actual DEX components, in addition to ownership. Known Home-only assignment is not reported as Both. Attempted-but-unverified writes stop automatic rotation. WorkManager command outputs propagate real errors; editor undo removes the latest matching index; combined cache pruning is used after local imports. Six new JVM methods bring total to 96; checkpoint `33c9a2a` CI `38089991851` completed PASS. Actual annotations confirm 96 tests/0 failures and app SHA `4aada029c4bd26d97a1c0db359dfe54d898fa969fd12e14636e0f0ecf98e1366`. One further instrumentation method brings authored total to 15; none is reported as passed yet.

## Inventory and material gaps

All 276 requirements retained. Totals: DONE_VERIFIED 4; IMPLEMENTED_UNTESTED 140; PARTIAL 61; BLOCKED_PLATFORM 2; BLOCKED_EXTERNAL 2; NOT_IMPLEMENTED 67. Four pure traversal-budget JVM tests executed successfully in Android CI, so row 038 verification_status is `JVM_POLICY_PASS_DEVICE_PENDING`; its implementation_status remains `IMPLEMENTED_UNTESTED` because actual DocumentsContract/SAF import is unverified. No other status changed. Verified statuses are limited to distinctly executed behavior, not compilation or unrun device tests.

No physical Galaxy Tab S9 Ultra/DeX/S Pen/TalkBack, wallpaper visual isolation, power/GPU lifecycle, widget/tile or credentialed provider acceptance evidence. Video/GIF/Vulkan, continuous live layers/data and advanced scheduling/provider journeys remain incomplete. Shared/unverifiable static Lock state is intentionally refused for Home-only writes; it requires separate Lock setup. Earlier instrumentation crash remains unresolved; newest diagnostics show emulator system death before APK installation, not an app failure. Accelerated Intel macOS HVF diagnostic also failed at boot timeout, without root modifications. No actual app failure cause is inferred from either infrastructure failure. No test is skipped/deleted.

## Instrumentation compile / actual acceptance separation

Standard CI at ee24a43 actually assembled/uploaded the instrumentation APK independently of unavailable emulator boot, in addition to app/JVM/lint evidence and all eight Python validator regressions. This is test compilation only, not runtime. All 15 native methods and prior failures are retained. Device boot was unavailable in observed runner attempts; no unchanged retry is queued. A Gradle 8.11.1 out-of-date notice is non-blocking, not a reason to claim the toolchain current.

## Current Arena continuation work

This session uses the assigned `arena/43998994-dex-wallpaper` branch and fast-forwarded its README-only starting checkout to the existing delivered `33c3ed0` implementation before edits. No repository or application was recreated. The current source changes harden the highest-priority library journey: collection selection is persisted before import; SAF folder scanning uses a `DocumentsContract` cursor and bounds all files/folders (not only recognized images) to 5,000 entries / depth 16 with an explicit user-facing truncation result; Compose-owned preview/thumbnail bitmaps are recycled when their UI owner leaves; a repository-level Home-only shuffle/interval/reopen journey is authored against a recording platform. Four pure traversal-budget JVM methods and one repository instrumentation method were added. The instrumented journey is deliberately not an actual SAF picker, WallpaperManager, OEM, or visible-pixel acceptance result.

Local checks after these edits: the 276-row inventory validator PASS; source-policy check PASS; all 8 Python validator tests PASS; all 8 Android XML resources/manifests parsed; `git diff --check` PASS. No Java/JDK, Gradle, Android SDK, adb, or Kotlin compiler exists in this Arena sandbox, so Android builds were not run locally. The pushed implementation commit `28e0acc1f3d8b9e3f0dc2299398782ad3679cd30` was independently matched by `git ls-remote` before this report update.

Android CI run `38092686248`, check/job `114332178631`, is **PASS**: 100 JVM tests / 0 failures, lintDebug, assembleDebug and assembleDebugAndroidTest, feature/source checks, Python checks and artifact uploads. App APK artifact `11684796718` (SHA-256 `6f64b50f88675f786b78bce9cf8b88a501bc6caf17d430c2eafb889c80f370a`); instrumentation APK artifact `11685175722` (SHA-256 `cdf094581c1b7fc73b22ebd96783402edec4e84dd71977c0ed25500d0b50c636`); reports artifact `11684856283`. CI compiled 16 native test methods but ran none on a device. The four pure traversal-budget JVM tests executed, supporting only row 038's `JVM_POLICY_PASS_DEVICE_PENDING` verification; its implementation remains untested as an end-to-end SAF journey.

No device-workflow run was associated with this branch/commit. The previous device check `38089299077` was independently inspected via GitHub check annotations: it failed with “Timeout waiting for emulator to boot,” before test execution, and uploaded no report files. `gh run view --log-failed` could not retrieve the runner log (results-receiver EOF); no additional cause is inferred. No manual/unchanged emulator retry is planned.

## Highest-value next work

Instrumentation compilation is now verified; keep runtime acceptance separate. See [DEVICE_ACCEPTANCE.md](DEVICE_ACCEPTANCE.md). Device runner attempts did not produce app acceptance (software system death, accelerated boot timeout). Highest value is a stable Android test device / actual Galaxy Tab S9 Ultra: launch, real SAF individual/folder import, collection/shuffle/interval, Home-only preview/apply/next, restart, static/live mixed isolation, visibility/orientation/recovery. Verify unaffected visible pixels as well as IDs; do not run wallpaper-modifying instrumentation on a personal device without explicit consent. Provider/power/DeX/S Pen/TalkBack acceptance still needs execution. Never infer acceptance from a build.

## Requested next-chat handoff

The user requested stage/commit/push and a takeover prompt. Existing implementation was clean and independently matched the remote before handoff preparation. [HANDOFF_PROMPT.md](HANDOFF_PROMPT.md) contains a self-contained copy/paste continuation contract, exact verified baseline, full remaining-scope references, priorities, safety invariants and failed approaches. Start the next Arena chat from the existing feature branch, **not README-only main**; respect any newly assigned Arena session branch. This handoff documentation does not alter app/test source or promote a requirement.
