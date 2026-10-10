# Privacy

No account, advertisements, telemetry, analytics, user profiling, or required backend. The wallpaper core works without internet. Imported artwork, thumbnails, metadata, collection membership, edits, queue/history/rules are local. Original source files are only read; private protected copies are stored under the app's credential-encrypted files directory. Uninstalling removes these private copies.

Android SAF authorizes only selected files/trees. DEX does not enumerate unrelated storage, launcher icon positions, personal health data, precise location, media notifications, or calendars. Battery/theme/network status is used locally. No SSID or network identifier is collected.

Android system backup is disabled to avoid inadvertently uploading private artwork or future credentials. Manual backup is an outstanding requirement. No credentials are included in source, builds, or diagnostics. The initial foundation has no remote connectors; future explicit connectors must use Keystore encryption, trusted HTTPS endpoints, bounded requests, and sanitized exports.

The public repository and debug APK contain no user photos, signing keys, tokens, or personal databases. Generated benchmark data must not be committed. A debug APK is development-only and is not a production-signed release.
