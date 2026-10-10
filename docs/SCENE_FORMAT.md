# Portable scenes — schema 1

Scenes are UTF-8 JSON, at most 256 KB and nesting depth 16, with up to 64 uniquely identified layers and 64 finite numeric variables. Supported geometry: 64–8192 per dimension, at most 16MP. Imported manifests never contain executable code, file paths, network URLs, binary plugins, or external fonts. Image references are **shared-library asset IDs**; referenced artwork must already exist on the device. Missing references are omitted visibly in the composition and must be re-linked by the author. Schema versions other than 1 fail closed.

Fields: `schemaVersion`, `title`, `width`, `height`, `background` (#RRGGBB or #AARRGGBB), `durationSeconds` (1–3600), `animate`, `epoch`, `sequenceIndex`, `variables`, `layers`. `epoch` / `sequenceIndex` provide neutral Living Epochs ordering, not proprietary canon. The user supplies authoritative artwork and sequence information.

Layer kinds: RECTANGLE, CIRCLE, TEXT, IMAGE, CLOCK, BATTERY. Position/size are normalized. Each layer has `id`, `x`, `y`, `width`, `height`, `color`, `text`, `assetId`, `opacity`, `rotation`, `visibleWhen`, `xFormula`, `font` (sans-serif/serif/monospace), `blend` (NORMAL/MULTIPLY/SCREEN/ADD). Layers draw back-to-front in array order. The editor supports layering, position, rotation, size, opacity, typography, formulas, time scrubbing, static library export, persistence, and 20 retained revisions per scene. Scene JSON is visible/editable in the manifest inspector.

## Formula language

A numeric bounded parser, **not JavaScript/Lua/Kotlin evaluation**. Input <=512 characters, recursion <=32, <=2048 steps, finite magnitude <=1e9. `+ - * / % ^`, comparisons, `&& || !`, parentheses. Allowed functions: sin, cos, abs, sqrt, floor, ceil, min, max, clamp, if (up to three arguments). Variables: persisted numeric scene variables plus `time`, `hour`, `battery`, `charging`; constants `pi`, `e`. Unknown functions, assignment, properties/reflection, paths, I/O, divide-by-zero and nonfinite results fail. All arguments evaluate eagerly; do not depend on short-circuiting to hide an invalid operation.

No lawfully unsupported Wallpaper Engine Windows runtime is reimplemented. Compatible **declarative DEX manifests** can be authored on Windows and imported as text when document import is enabled. Binary Windows packages, untrusted libraries and external code are never executed. Animated live scene playback, native SVG/video, advanced text curves, event workflows and full profile packages remain separately tracked in the matrix.
