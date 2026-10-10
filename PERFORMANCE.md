# Performance and measurement

## Policies implemented

- No hidden live-render frame loop; release hidden/surface-destroyed bitmaps.
- One engine-owned rendering thread and cancellable decode scope per live/preview instance.
- Static frames redraw on events; clock overlay only at visible minute boundaries.
- Decoding uses power-of-two sampling and a 16MP safety ceiling; output dimensions capped at 4,096 per axis for wallpaper and 16MP overall.
- Imported files are streamed in 64-KB chunks, limited to 80 MB, with bounded traversal.
- Shuffle queues contain at most one entry per eligible asset; recent history capped at 100; global receipts capped at 500.
- Unique periodic workers, minimum 15 minutes, battery-not-low constraint; screen-off and Battery Saver deferral. No foreground polling service.
- Thumbnails are disposable; originals and sidecars are protected.

## Measurable targets (not benchmark claims)

Hidden rendering: zero scheduled frames, no active decode job. Repeated change/orientation/engine recreation: bounded retained memory and no leaked surface/thread. Cached image switching latency: measure p50/p95 on Tab S9 Ultra; do not set a percentage battery claim without comparative runs. Large images: inspect peak Java/native heap and allocation pressure before raising decode caps.

## Actual evidence

No Android device, SDK, emulator, GPU profiler, or JDK is available in the Arena sandbox. Device memory/frame-time/power measurements **have not run**. JVM tests for 10/100/1,000-asset queue bounds are committed, pending CI. The Live data workspace exposes a point-in-time Java heap measurement (not total RAM/CPU/GPU metrics). No invented benchmark or battery-saving percentage is reported.

## Benchmark procedure

Use synthetic, locally generated images (10, 100, 1,000) and 30MP mixed-aspect photographs with rights. Run repeated next/apply/rotation, portrait/landscape, Activity and wallpaper-engine recreation. Record `dumpsys meminfo`, `gfxinfo` where applicable, Perfetto, renderer trace counters, visible/hidden time, worker execution delay, thermal state, and allocation retention. Also test corrupt/truncated/unsupported input, revoked SAF grants, and storage pressure. Keep generated datasets and personal artwork out of Git.

## GLES extension (device verification pending)

Actual ES2 shader preview/live pipeline with checked texture limits, at most two transition textures, program cleanup, display reference counting and one context-recovery attempt. Visibility/surface events release GPU/bitmap jobs. Adaptive frame caps consume Battery Saver, thermal and low-RAM status; reduced motion/safe mode have real rendering consumers. Measured frame/resource counters appear in Health; these are observations, not claimed benchmark wins. GPU pbuffer tests are committed but not executed at this checkpoint.
