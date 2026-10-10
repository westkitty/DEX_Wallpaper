# Backup and recovery

ZIP format v1: `manifest.json` plus optional `assets/<portable-id>.img` originals. JSON is capped at 8 MB/depth 16, archive at 6,000 entries/1GB expanded, images at 80 MB. Unknown paths, directories, duplicate entries, dangling memberships, invalid destinations, invalid scene formulas, unsupported versions, and hash mismatches are rejected before database mutation. Every included image is SHA-256 and decode verified. Image/scene metadata is merged in a Room transaction; wallpaper writes use the same repository maintenance lock. A filesystem interruption can leave unreferenced private copies; no source originals are deleted or arbitrary archive paths extracted.

Contains: artwork names/hash/dimensions/provenance/attribution/license/tags/favorites/exclusions, collection membership, orientation-specific editing sidecars, per-screen selection/interval/filter/queue state, rules, scene manifests/variables and workspace preferences. Does not contain remote credentials, signing keys, device SAF grants, private source URIs, or Android internal database files. Historical receipts, old scene revisions and provider credentials are not included yet (tracked limitations).

**Metadata-only is the default.** Original artwork export requires a clear opt-in and user-chosen SAF destination. Metadata-only restores show missing assets as quarantined until matching originals are re-imported. Exact hashes preserve identity; collisions are remapped. Restore pauses all imported screen schedules/rules and never calls a wallpaper setter. Users must review destinations and explicitly resume. Existing unrelated assets remain intact.

Editing sidecars are JSON subsets tied to SHA-256 rather than machine paths. Import applies only to existing matching artwork, preserving original image content. No new assets or broad permissions are required.

Sanitized diagnostic export is a separate format: only API/capability states and aggregate counts/storage totals. No names, paths, source URIs, location, artwork, personal health, network identifiers, or credentials.
