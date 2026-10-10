# Architecture

One Android application module, small explicit packages, no empty framework layers.

- **core**: immutable destination/crop/queue/rule models, pure ShuffleGuard, Chronoscape, ScheduleMath, FrameSense, asset-integrity helpers. No Android dependency in the business algorithms. Pure logic is unit-testable.
- **data**: one Room database owns assets, collection membership, edits, per-screen configuration, persistent queues, receipts, and rules. Workspace-only preferences live in DataStore; preferences are not duplicated in Room. Imported originals are in credential-encrypted application-private files; thumbnails are expendable cache.
- **wallpaper**: bounded EXIF-aware decoding and non-destructive composition. One `WallpaperDestinationController` owns every static platform write. It takes explicit flags after fail-closed preflight, verifies unaffected screen identity, and stops automatic writing on irreversible identity mismatch.
- **worker**: one unique WorkManager periodic request per independent destination. No competing timers. Reboot/package recovery reconciles requests; WorkManager also persists its own work. No pre-unlock artwork access.
- **ui**: Compose state comes from lifecycle-aware ViewModel flows. SavedStateHandle retains workspace/destination/selection; edits persist as sidecars as they change. The responsive shell uses semantic controls and a desktop-sized multi-pane layout. Loading/cancellation/errors are explicit.
- **platform**: runtime capability states and truthful explanations. An unsupported advanced integration does not degrade offline artwork handling.

## Wallpaper operation

UI/widget/worker routes call the same repository. A process-wide mutex serializes wallpaper writes; selection returns a proposed queue, which is committed transactionally with the receipt only after a successful platform write. Unsuccessful writes do not consume the shuffle queue. Each screen has its own selected collection, mode, filters, settings, queue, and history. Explicit Both application updates the two current-artwork records but has no independent background schedule.

Rule evaluation receives immutable time/device snapshots. It has no direct access to wallpaper or networking; dry runs and forecasts are write-free. Equal priorities use a stable rule-ID tie breaker. Overlapping equal-priority different-collection rules are diagnosed rather than hidden.

## Rendering ownership

Each live wallpaper `Engine` (installed or preview) owns a coroutine scope, render HandlerThread, surface flags, decode job, and bitmap. Surface/visibility changes cancel loading and release frames. Invisible engines have no frame loop. Static scenes draw when artwork/settings/surface changes; the optional clock updates at minute boundaries while visible. Engine destruction cancels collectors and disposes resources on its owning thread. Android wallpaper-color callbacks use the rendered pixels.

`GlWallpaperRenderer` creates real EGL/ES2 contexts, uploads bounded textures, applies fragment effects, and blends crossfade/scale/slide transitions. A reference-counted EGL display and explicit current-context checks prevent one preview from terminating another engine. Telemetry is measured locally (not uploaded); frame/sensor/offset requests are coalesced by the adaptive cap. Canvas is the safe/context-loss/clock/battery fallback. Hidden engines release bitmaps/textures/contexts and sensors. No continuous animation exists for static content.

`LiveConfiguration` freezes source identity, image sidecar, and renderer options in DataStore. Preview, Home, Lock, and shared/unknown configurations are separate. Android 14+ public engine flags route installed instances; process-local ownership evidence gates Home/Lock-only commits. An explicit Both commit updates all assigned DEX instances. Older/unknown ownership cannot authorize Home-only changes. The service has **no static screen-record observer**; imports only trigger recovery of the already selected asset. Library edits require a new explicit snapshot commit. Safe/reduced-motion remain deliberately system-wide policies.

Picker preparation writes only a preview draft, never installed configuration. A reported successful picker result must match ownership before a scoped commit; cancellation clears the pending request. Engine creation never consumes pending previews. Flag reassignment is serialized on each engine thread; shared-to-single reassignments preserve existing content atomically unless a newer explicit Both commit superseded it. Preference revision stamps reject delayed old emissions; decode generation tokens prevent stale frames installing after cancellation. These lifecycle guarantees still require device execution, not just source review.

## Import and failure ownership

SAF grants are requested only for authorized trees/documents. Imports stream to an 80-MB-bounded `.partial` file, hash and decode-validate before rename, and transactionally add assets/membership. Exact duplicates link to the existing asset. Recursion and file counts are bounded. Source files are never edited or deleted. Unavailable source permissions are recoverable by choosing a source again; local private copies continue offline. Missing copies are quarantined instead of removed.

No silent destructive database migration is configured. Schema exports are produced by Room. Actual migration testing is required before schema upgrades. Backup, scene/network extensions, and recovery enhancements must reuse this shared state rather than build independent stores.
