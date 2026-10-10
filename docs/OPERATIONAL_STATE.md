# Durable operational state

Date: 2026-10-10 (UTC). Authorized repository: https://github.com/westkitty/DEX_Wallpaper.
Session-fixed branch: `arena/62a85de1-dex-wallpaper`. Main remains `db4ef366986c0b79a7c0ed71c8a50fd82c51ad02`; no PR/merge/force-push.

## Last independently verified delivery

`f8c6e6b718c337e2dd69cfeaa85366a16595f1f5` was normally pushed and compared independently with `git ls-remote`.
Seven prior session commits: `44399d6`, `6c3194a`, `76c1f42`, `40df6b6`, `8afef31`, `23dc9ed`, `f8c6e6b`.
The following repair cannot record its own SHA before commit; update this checkpoint after remote verification.

## Actual execution evidence

- **Android PASS at f8c6e6b**: run https://github.com/westkitty/DEX_Wallpaper/actions/runs/38086139810 ; **70 JVM tests, 0 failures/errors**, lintDebug, assembleDebug.
- Real debug APK: https://github.com/westkitty/DEX_Wallpaper/actions/runs/38086139810/artifacts/11682677562 ; SHA-256 `fe70972bcfbd97acda58dace9527f2a79f33cd47a21edffe13b41990b446c64d`. Reports `11682797175`.
- Earlier creative/recovery checkpoint `8afef31`, run `38084606094`: PASS/56 JVM/lint/APK. Foundation `76c1f42`: PASS/35.
- **Device FAIL**, run `38084606059`: instrumentation process crashed, 15 discovered tests, **0 reported executed result files**; cause still unknown.
- Device `38085741750` at the old telemetry-defective `23dc9ed` also FAILED. No known passed instrumentation or physical Samsung result.
- Local toolchain absent. Do not retry blocked JDK/SDK downloads. GitHub Actions is the Android verification path. Manual dispatch is permissions-denied; normal push-triggered workflows work.

## Current safety repair (awaiting its own Android build)

Installed live engines now use independent immutable Home/Lock/shared snapshots and a separate preview draft, never static Home records or mutable library edits. A new explicit commit is required for source/effect changes. Android 14+ public engine ownership gates Home/Lock-only commits; unknown/shared/older ownership refuses those operations. OS picker assignment still needs user review. Engine creation never replays a cancelled/pending preview. Preference revisions and decode generations reject stale emissions/frames; per-engine serialized ownership reassignment preserves shared content on splitting into a single destination. EGL context ownership and frame coalescing are repaired.

Five pure live-isolation tests plus an equal-ID preflight regression and multi-context instrumented GPU test are authored, not executed. Local inventory/source/XML/Python/staged checks pass. The next device workflow preserves crash and Gradle evidence as artifacts; all failing tests are retained.

## Native capabilities implemented (runtime acceptance pending)

Responsive 12 Compose workspaces; SAF individual/folder imports, protected originals/EXIF/thumbnails/collections; non-destructive persistent precision editor/undo; fair shuffle/history/filtering; centralized static Home/Lock/Both preflight and independent approximate schedules/rules/forecasts; five real offline generators; static bounded layer/formula scene editor/export/revisions; backup/recovery/sidecars/sanitized diagnostics/cache budgets; native Home-only widget/tile/shortcuts and opt-in scoped commands; real GLES effects/transitions/local renderer counters; six/custom DeskShift profiles and packaged legal reader.

Core schema remains Room v2 in this safety commit. Trusted HTTPS/WebDAV/Nextcloud/RSS/numeric-JSON source transport, Keystore credentials, Room v3 and native source journeys are in-progress separately; they are not claimed as delivered by the above SHA.

## Inventory and material gaps

All 276 requirements retained. Totals: DONE_VERIFIED 4; IMPLEMENTED_UNTESTED 133; PARTIAL 57; BLOCKED_PLATFORM 2; BLOCKED_EXTERNAL 2; NOT_IMPLEMENTED 78. Verified statuses are limited to distinctly executed core algorithms/schema, not compilation or unrun device tests.

No physical Galaxy Tab S9 Ultra/DeX/S Pen/TalkBack, wallpaper visual isolation, power/GPU lifecycle, widget/tile or credentialed provider acceptance evidence. Video/GIF/Vulkan, continuous live layers/data and advanced scheduling/provider journeys remain incomplete. Shared/unverifiable static Lock state is intentionally refused for Home-only writes; it requires separate Lock setup. Device instrumentation crash remains unresolved.

## Highest-value next work

Inspect the new CI/compiler/lint result and actual AndroidRuntime crash evidence; repair real failures and recheck. Finish/review trusted-source transport and reconcile its individual inventory entries. Execute launch/import/Home-only/restart/live-isolation journeys on an emulator and Galaxy Tab S9 Ultra, preserving the unaffected screen's visible content as well as identity. Never infer acceptance from a build.
