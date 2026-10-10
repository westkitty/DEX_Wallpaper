# Permission inventory

| Manifest permission | Purpose / trigger | Fallback |
|---|---|---|
| `SET_WALLPAPER` | Normal permission for explicit static Home/Lock/Both writes | Fail closed on device policy or unsupported destination; previews/library remain usable |
| `RECEIVE_BOOT_COMPLETED` | Reconcile persisted approximate schedules after unlock/reboot or app update | WorkManager retains unique work; no direct-boot file access |
| `INTERNET` | Reserved for explicit optional source transport; unused by the initial offline core | All basic import/edit/rotate operations are offline |
| `ACCESS_NETWORK_STATE` | Local connectivity status / future network budgets | Offline status; no SSID/user-network identifiers collected |

`BIND_WALLPAPER` is a **service binding restriction**, not a user permission request. Only the OS may bind the exported wallpaper service. The launcher Activity is exported for supported Android entry. Recovery receiver is not exported.

Storage access uses `ACTION_OPEN_DOCUMENT` / `ACTION_OPEN_DOCUMENT_TREE` grants only. Persistable read access is requested for folders; individual images are copied privately and work even if enduring permission is refused. No `MANAGE_EXTERNAL_STORAGE`, broad media/storage, location, health, calendar, exact-alarm, notification-listener, microphone, or camera permission is requested.

Advanced unsupported features are capability-gated, not permission-gated behind fake switches. Optional permissions must be documented here when actual consumers are implemented. No privilege is requested merely to make a feature matrix appear complete.

Native surfaces: `BIND_QUICK_SETTINGS_TILE` restricts the exported tile service to the OS; it is not a permission request. Widget provider is non-exported and uses immutable self PendingIntents. The external command receiver is exported only to support opt-in Tasker/intent automation; default disabled, with separate explicit Lock/Both authorizations. No arbitrary code, paths, or component launches are accepted. No extra runtime permission is required by these action surfaces.
