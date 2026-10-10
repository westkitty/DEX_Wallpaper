# Native action surfaces

All actions route through WorkManager → the shared repository → explicit-destination enforcement. No widget/tile/source writes wallpaper independently.

## Tasker / broadcast intents

Settings → **Allow external wallpaper commands** is off by default. Enabling it allows installed apps to send explicitly whitelisted commands; review this trust decision carefully. External Lock and Both targets require **separate explicit authorization switches**, off by default. Restoring a backup never enables external commands.

Tasker **Send Intent**:
- Action: `com.westkitty.dexwallpaper.COMMAND`
- Package: `com.westkitty.dexwallpaper`
- Receiver: `com.westkitty.dexwallpaper.integration.ExternalCommandReceiver`
- Target: Broadcast Receiver
- Extras: `command:NEXT`, `destination:HOME`

Commands: NEXT, PREVIOUS, PAUSE, RESUME, APPLY (`assetId`), SWITCH_COLLECTION (`collectionId`). Defaults to Home only; unknown destinations fail, never fall back to Both. Only local library IDs are accepted. No scripts, shell commands, arbitrary application components, URLs, or filesystem paths. IDs are shown in Library's inspector. This is an honest **Tasker Send Intent** integration, not a full Locale/Tasker plugin with its own plugin editor.

## Android surfaces

Quick Settings **Next wallpaper**: explicit Home-only action, with unlock request if locked. Home widget: Next/Previous-history not included; Next, Pause/Resume, Album (cycle saved collections), Select (cycle eligible images sequentially). All Home-only. No periodic widget timer; updates follow actions. Launcher long-press: Next Home wallpaper and Artwork library. Shortcut mutations are not replayed on Activity recreation.

## DeskShift profiles

Six real preset configurations: Work, Art, Gaming, Reading, Night, Minimal; custom named snapshots are saved privately. Profiles change the selected screen's configuration, workspace motion/clock policy, and the selected asset's treatment. They do **not** apply wallpaper automatically. Explicit import/export uses a bounded version-1 JSON document without artwork, credentials, device URIs, or an implicit wallpaper destination. Unknown transferred collection IDs retain the current collection. Profiles do not yet define every advanced integration or per-collection effect default.
