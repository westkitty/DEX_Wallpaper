# Privacy

No account, advertisements, analytics, user profiling, required backend or uploaded telemetry. The wallpaper core works without internet. Artwork, thumbnails, metadata, collections, edits, queues/history/rules/scenes and local renderer counters stay on device. Original source files are only read; protected private copies use Android credential-encrypted app storage. Uninstalling removes private copies.

SAF authorizes only chosen files/trees. Individually selected documents are copied into app-private storage without persistent document grants. A read-only folder grant remains only while a collection links that tree; stale or replaced grants are reconciled on startup and after import/rescan/restore. Backups exclude both grants and source-tree URIs. DEX does not enumerate unrelated storage or launcher icon positions, or access personal health, location, media notifications or calendars. Local battery/theme/network capabilities support explicitly configured behavior; no SSID or network identifier is collected.

## Optional trusted connections

Network requests happen only after the user saves an explicitly trusted HTTPS source and taps Synchronize. Image/WebDAV/Nextcloud/RSS/flat numeric JSON sources send the requested path and, when configured, Basic authorization only to the approved origin/path. The remote provider necessarily sees connection/request metadata; its own privacy/licensing rules apply. No redirects, embedded URL credentials/query tokens or TLS bypass. Private-LAN access is an explicit opt-in; loopback/link-local/metadata hosts stay forbidden. Network switches or policy/budget failures stop transfer. There is no feed polling, analytics endpoint or mandatory cloud service.

Credentials are AES-256-GCM encrypted with an Android Keystore key; encrypted files remain app-private and never enter Room, profiles, manual backups or diagnostics. Password inputs are unsaved/masked and cleared after source submission. No claim of hardware-backed Keystore on every device. Removing a connection deletes its credential/cache/configuration, not imported artwork. Provider/device credential behavior is not yet verified.

## Exports and backup

Android system backup is disabled to prevent uploading private artwork/credentials. Manual ZIP backup is implemented but device acceptance pending: optional original artwork may be private; choose a trusted destination. Credentials, source connections and URI grants are excluded. Sidecars/profiles contain metadata rather than original pixels/credentials. Sanitized diagnostic export contains counts/build/resource observations, not artwork, names, paths, source URLs, grants or passwords. Never upload personal backups to the public repository.

The repository/debug APK contains no user photos, signing keys, tokens or personal databases. Test fixtures are synthetic. Debug signing is development-only, not an authorized production release.
