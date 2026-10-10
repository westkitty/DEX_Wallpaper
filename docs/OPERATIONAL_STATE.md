# Durable operational state

Date: 2026-10-10 (UTC). Authorized repository: https://github.com/westkitty/DEX_Wallpaper.
Session-fixed branch: `arena/62a85de1-dex-wallpaper`. Main remains `db4ef366986c0b79a7c0ed71c8a50fd82c51ad02`; no PR/merge/force-push.

## Last independently verified delivery

`33c9a2a5ab68aca5ccc4d0c48deff8898238136b` was normally pushed and compared independently with `git ls-remote`.
Eleven prior session commits: `44399d6`, `6c3194a`, `76c1f42`, `40df6b6`, `8afef31`, `23dc9ed`, `f8c6e6b`, `ab0822c`, `c323571`, `daf52c1`, `33c9a2a`.
The following repair cannot record its own SHA before commit; update this checkpoint after remote verification.

## Actual execution evidence

- **Latest Android PASS at 33c9a2a**: run `38089991851`; **96 JVM tests, 0 failures/errors**, lintDebug/assembleDebug. App artifact `11683352853`, SHA-256 `4aada029c4bd26d97a1c0db359dfe54d898fa969fd12e14636e0f0ecf98e1366`; reports `11683882481`.

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

Next source checkpoint adds Room v3/configuration, Keystore credentials, manual trusted HTTPS/WebDAV/Nextcloud/RSS/flat numeric JSON transport and native source journeys. Its first build run `38088800616` FAILED on an actual OkHttp Dns lambda type mismatch; explicit Dns implementation is fixed and PASS at daf52c1. Its 14 new JVM methods really passed (90 total). v1→v3/reopen/collection-upsert and synthetic Keystore device tests remain unverified. Provider acceptance is still unavailable; no real credential was needed to implement safe setup.

## New hardening checkpoint (Android CI PASS, device pending)

Live picker records/checks before/after identities and actual DEX components, in addition to ownership. Known Home-only assignment is not reported as Both. Attempted-but-unverified writes stop automatic rotation. WorkManager command outputs propagate real errors; editor undo removes the latest matching index; combined cache pruning is used after local imports. Six new JVM methods bring total to 96; checkpoint `33c9a2a` CI `38089991851` completed PASS. Actual annotations confirm 96 tests/0 failures and app SHA `4aada029c4bd26d97a1c0db359dfe54d898fa969fd12e14636e0f0ecf98e1366`. One further instrumentation method brings authored total to 15; none is reported as passed yet.

## Inventory and material gaps

All 276 requirements retained. Totals: DONE_VERIFIED 4; IMPLEMENTED_UNTESTED 140; PARTIAL 61; BLOCKED_PLATFORM 2; BLOCKED_EXTERNAL 2; NOT_IMPLEMENTED 67. Reconcile against generated JSON after source updates. Verified statuses are limited to distinctly executed core algorithms/schema, not compilation or unrun device tests.

No physical Galaxy Tab S9 Ultra/DeX/S Pen/TalkBack, wallpaper visual isolation, power/GPU lifecycle, widget/tile or credentialed provider acceptance evidence. Video/GIF/Vulkan, continuous live layers/data and advanced scheduling/provider journeys remain incomplete. Shared/unverifiable static Lock state is intentionally refused for Home-only writes; it requires separate Lock setup. Earlier instrumentation crash remains unresolved; newest diagnostics show emulator system death before APK installation, not an app failure. Accelerated Intel macOS HVF diagnostic also failed at boot timeout, without root modifications. No actual app failure cause is inferred from either infrastructure failure. No test is skipped/deleted.

## Instrumentation compile / actual acceptance separation

The next standard CI checkpoint assembles/upload-checks the actual instrumentation APK in addition to app/JVM/lint evidence and runs all Python validator regressions. This is only test compilation, not runtime. Keep all 15 authored instrumentation methods and prior failures; device boot is unavailable in observed runner attempts.

## Highest-value next work

Check the instrumentation-compile checkpoint independently; preserve any real compiler failures. Device runner attempts did not produce app acceptance (software system death, accelerated boot timeout). Highest value is a stable Android test device / actual Galaxy Tab S9 Ultra: launch, real SAF individual/folder import, collection/shuffle/interval, Home-only preview/apply/next, restart, static/live mixed isolation, visibility/orientation/recovery. Verify unaffected visible pixels as well as IDs; do not run wallpaper-modifying instrumentation on a personal device without explicit consent. Provider/power/DeX/S Pen/TalkBack acceptance still needs execution. Never infer acceptance from a build.
