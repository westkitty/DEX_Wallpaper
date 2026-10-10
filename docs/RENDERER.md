# MotionGlass renderer

An actual OpenGL ES 2.0 texture renderer is used by the visible live engine and a native `SurfaceView` effect preview. It compiles/links a local fixed shader (no remote code), checks `GL_MAX_TEXTURE_SIZE`, downsamples oversized textures, uses two bounded transition textures, and explicitly frees textures/programs/surfaces/contexts. The common EGL display is reference-counted across preview/installed engines; one closing preview cannot terminate another engine's display. Per-engine scopes/HandlerThreads are disposed independently.

Implemented shader modes: atmosphere/reverse, reeded/glass/reverse, color fill/reverse, sketch/reverse, frost/reverse, halftone/reverse, grain. Intensity is bounded. Transitions: off/crossfade/scale/slide, configured duration. Launcher offsets/touch and optional rotation-vector tilt produce bounded parallax; sensors are registered only while visible/GLES-active and released when hidden. Zoom is an optional 1×–1.05× slow scale animation. No continuous hidden rendering; static frames are event-driven.

Adaptive policy limits fps/effect intensity under Battery Saver, thermal severity and low-RAM; reduced motion disables transitions/zoom/parallax. Safe mode uses the CPU static pipeline. Unsupported GLES setup/texture/context operations fall back to Canvas after one context-recovery attempt. Vulkan and video/GIF decoders are not implemented. Clock/battery overlays currently use minute-event Canvas rendering and bypass shaders (clearly identified in the UI).

The preview is a real GPU surface, not a slideshow or mock Android interface. Preview errors display a CPU fallback rather than pretend the effect is active. Static WallpaperManager application uses the non-destructive CPU pipeline, not live shader effects. Live destination selection remains owned by Android/OEM, with a warning before picker handoff.

## Evidence and diagnostics

`RenderTelemetry` measures actual frame execution time and maintains live engine/context/texture counts and recovery attempts. System health snapshots measured values on request; no invented benchmark or battery-saving percentage. A pbuffer instrumentation test reads actual GPU pixels, checks crossfade colors, renders every effect, recreates contexts, and verifies resource-count recovery. Until the emulator workflow executes that test, GPU correctness remains unverified. SwiftShader/emulator evidence is not Samsung GPU/power evidence.
