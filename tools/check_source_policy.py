#!/usr/bin/env python3
import re
from pathlib import Path
root = Path(__file__).resolve().parents[1]
source = root / "app/src/main"
failures = []
wallpaper_writers = []
for path in source.rglob("*.kt"):
    text = path.read_text()
    if ".setBitmap(" in text or ".setStream(" in text or "manager.clear(" in text: wallpaper_writers.append(path.name)
    if re.search(r'(?:AKIA[0-9A-Z]{16}|-----BEGIN (?:RSA |EC )?PRIVATE KEY-----|ghp_[A-Za-z0-9]{30,})',text): failures.append(f"Potential secret: {path}")
if wallpaper_writers != ["WallpaperDestinationController.kt"]: failures.append(f"Uncentralized wallpaper writers: {wallpaper_writers}")
controller = (source / "java/com/westkitty/dexwallpaper/wallpaper/WallpaperDestinationController.kt").read_text()
if "setBitmap(bitmap, null, false, flags)" not in controller or "DestinationPolicy.check" not in controller or "DestinationPolicy.verify" not in controller:
    failures.append("Explicit destination enforcement missing")
live = (source / "java/com/westkitty/dexwallpaper/wallpaper/DexWallpaperService.kt").read_text()
if "observeScreens" in live or "currentAssetId" in live or "dao.edit(" in live:
    failures.append("Live engine must use isolated immutable snapshots, never static Home records/mutable library edits")
if "LiveDestinationPolicy.source" not in live:
    failures.append("Live destination snapshot isolation missing")
manifest = (source / "AndroidManifest.xml").read_text()
for forbidden in ["MANAGE_EXTERNAL_STORAGE","READ_EXTERNAL_STORAGE","ACCESS_FINE_LOCATION","READ_CONTACTS","QUERY_ALL_PACKAGES","SCHEDULE_EXACT_ALARM"]:
    if forbidden in manifest: failures.append(f"Unexpected high-risk permission {forbidden}")
if failures: raise SystemExit("\n".join(failures))
print("PASS: one explicit wallpaper writer and isolated live sources; no broad storage, location, exact-alarm, or embedded-key patterns.")
