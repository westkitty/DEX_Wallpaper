# DEX Wallpaper — 276-feature contract

Generated from `tools/feature_names.txt` and explicit `tools/feature_overrides.json`. Run `python3 tools/update_feature_matrix.py` after an evidence update.

`DONE_VERIFIED` means the specific behavior has referenced passing evidence. Device acceptance is never inferred from a successful build. Core algorithms and device integrations are tracked separately.

## Totals

- **DONE_VERIFIED**: 0
- **IMPLEMENTED_UNTESTED**: 74
- **PARTIAL**: 49
- **BLOCKED_PLATFORM**: 3
- **BLOCKED_EXTERNAL**: 2
- **NOT_IMPLEMENTED**: 148
- **TOTAL**: 276

## Individual requirements

| ID | Requirement | Implementation | Verification | UI / limitations |
|---|---|---|---|---|
| 001 | Home-screen-only wallpaper | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control center / Library — Device acceptance pending. Home-only refuses shared/unverifiable Lock. Both requires an explicit selection. |
| 002 | Lock-screen-only wallpaper | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control center / Library — Device acceptance pending. Home-only refuses shared/unverifiable Lock. Both requires an explicit selection. |
| 003 | Dual-screen wallpaper application | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control center / Library — Device acceptance pending. Home-only refuses shared/unverifiable Lock. Both requires an explicit selection. |
| 004 | Independent screen collections | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 005 | Synchronized screen collections | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 006 | Static wallpaper mode | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control center / Library — Device acceptance pending. Home-only refuses shared/unverifiable Lock. Both requires an explicit selection. |
| 007 | Live wallpaper mode | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 008 | Manual wallpaper selection | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control center / Library — Device acceptance pending. Home-only refuses shared/unverifiable Lock. Both requires an explicit selection. |
| 009 | Automatic timed rotation | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 010 | Custom rotation intervals | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 011 | Daily rotation | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 012 | Clock-time scheduling | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 013 | Day-of-week scheduling | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 014 | Unlock-triggered rotation | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 015 | Lock or screen-off-triggered rotation | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 016 | Location-triggered rotation | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 017 | Ambient-light-triggered rotation | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 018 | Custom light thresholds | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 019 | Automatic light/dark wallpaper selection | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 020 | Scheduled night mode | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 021 | Random wallpaper selection | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 022 | Sequential wallpaper selection | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 023 | Shuffle without repetition | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 024 | Collection-specific schedules | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 025 | Time-of-day playlists | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 026 | Theme-dependent playlists | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 027 | Favorites-only rotation | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 028 | Search-based rotation | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 029 | Local-folder rotation | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 030 | Manual next wallpaper | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control center / Library — Device acceptance pending. Home-only refuses shared/unverifiable Lock. Both requires an explicit selection. |
| 031 | Previous wallpaper access | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control center / Library — Device acceptance pending. Home-only refuses shared/unverifiable Lock. Both requires an explicit selection. |
| 032 | Pause and resume | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 033 | Start and stop rotation | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 034 | Restart behavior after reboot | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 035 | Default wallpaper restoration | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 036 | Collection-specific fallback wallpaper | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 037 | Import individual images | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 038 | Import complete folders | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 039 | Multiple source folders | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 040 | Automatic folder rescanning | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 041 | Manual folder synchronization | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 042 | User-authorized hidden-folder scanning | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 043 | Multiple albums | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 044 | Manual image collections | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 045 | Folder-backed collections | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 046 | Favorites | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 047 | Pinned collections | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 048 | Wallpaper tags | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 049 | Tag-filtered rotation | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 050 | Copy images between collections | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 051 | Move images between collections | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 052 | Multi-image selection and editing | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 053 | Thumbnail gallery | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 054 | Full-screen previews | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 055 | Swipe gallery navigation | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 056 | Pinch-to-zoom previews | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 057 | Wallpaper history | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 058 | Search history | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 059 | Saved online searches | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 060 | Wallpaper downloads | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 061 | Online wallpaper discovery | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 062 | Curated artwork collections | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 063 | Daily featured artwork | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 064 | Community artwork sources | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 065 | Wallpaper categories | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 066 | Licensed premium artwork integration | BLOCKED_EXTERNAL | NOT_RUN | System health / capability report — Requires authorized provider rights or a user-supplied compatible model; none supplied. |
| 067 | Cross-device favorites synchronization | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 068 | Muzei-compatible artwork sources | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 069 | Custom artwork-source plugins | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 070 | Nextcloud integration | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 071 | WebDAV integration | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 072 | Remote-image caching | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 073 | Wi-Fi-only downloads | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 074 | Online-search caching | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 075 | Interactive cropping | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 076 | Pinch-to-zoom editing | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 077 | Image panning | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 078 | Per-image crop settings | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 079 | Per-collection crop defaults | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 080 | Numeric positioning and zoom | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 081 | Fit-to-screen scaling | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 082 | Fill-screen scaling | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 083 | Stretch scaling | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 084 | Original-size positioning | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 085 | Fit-to-height scaling | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 086 | Left/center/right alignment | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 087 | Launcher wallpaper zoom correction | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 088 | Blurred-edge padding | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 089 | Sharp-edge padding | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 090 | EXIF orientation handling | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 091 | Tablet-aware scaling | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 092 | Foldable-screen support | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 093 | External-display adaptation | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 094 | On-device subject-aware cropping | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 095 | Custom object-detection models | BLOCKED_EXTERNAL | NOT_RUN | System health / capability report — Requires authorized provider rights or a user-supplied compatible model; none supplied. |
| 096 | Foreground subject isolation | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 097 | Background-only effects | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 098 | Multiple image formats | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 099 | Efficient image downsampling | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 100 | Image compression | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 101 | Non-destructive editing | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 102 | Persistent per-image edits | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 103 | Brightness adjustment | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 104 | Wallpaper dimming | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 105 | Blur | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 106 | Vignette | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 107 | Grayscale | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 108 | Color filters | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 109 | Contrast | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 110 | Saturation | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 111 | Film grain | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 112 | Crossfade transitions | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 113 | Scale transitions | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 114 | Slide transitions | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 115 | Atmosphere effects | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 116 | Reverse atmosphere | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 117 | Reeded glass | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 118 | Glass effects | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 119 | Reverse glass | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 120 | Color Fill effects | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 121 | Reverse Color Fill | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 122 | Canvas Sketch effects | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 123 | Reverse Canvas Sketch | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 124 | Frosted-glass effects | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 125 | Reverse Frosted effects | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 126 | Halftone effects | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 127 | Reverse Halftone | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 128 | Transition duration | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 129 | Effect intensity | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 130 | Independent transition enable/disable | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 131 | Scrolling effects | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 132 | Swipe parallax | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 133 | Gyroscope parallax | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 134 | Tilt-responsive motion | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 135 | Optional zoom animation | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 136 | GPU-accelerated rendering | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 137 | Vulkan support | BLOCKED_PLATFORM | NOT_RUN | System health / capability report — No Vulkan backend or proprietary Windows runtime. Direct boot intentionally excludes credential-encrypted artwork. Safe fallback only. |
| 138 | OpenGL ES support and fallback | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 139 | Real-time effect previews | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 140 | Wallpaper palette extraction | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 141 | Android dynamic wallpaper color integration | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 142 | Procedural wallpaper generation | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 143 | Geometric patterns | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 144 | Extensible predefined pattern library | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 145 | Custom generated palettes | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 146 | Procedural textures | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 147 | Random pattern variations | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 148 | Offline on-device procedural generation | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 149 | Resolution-aware generation | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 150 | Saved procedural favorites | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 151 | Video wallpapers | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 152 | GIF wallpapers | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 153 | Animated 2D scenes | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 154 | Animated 3D scenes | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 155 | Animated wallpaper playlists | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 156 | Documented lawful Wallpaper Engine content import | BLOCKED_PLATFORM | NOT_RUN | System health / capability report — No Vulkan backend or proprietary Windows runtime. Direct boot intentionally excludes credential-encrypted artwork. Safe fallback only. |
| 157 | Compatible portable wallpaper package import | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 158 | Mobile animation optimization | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 159 | Static motion-free alternatives | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 160 | Independent animation controls | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 161 | Visual wallpaper editor | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 162 | Layer-based composition | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 163 | Image overlays | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 164 | Custom typography | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 165 | Custom fonts | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 166 | Vector shapes | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 167 | SVG artwork | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 168 | Gradients and shadows | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 169 | Layer blending | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 170 | 3D-style text transforms | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 171 | Curved/transformed text | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 172 | Interactive hotspots where supported | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 173 | App-launch actions and supported shortcuts | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 174 | Touch-triggered animation | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 175 | Formula expressions | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 176 | Conditional logic | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 177 | Persistent variables | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 178 | Event-driven workflows | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 179 | Dynamic clocks | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 180 | Date and calendar displays | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 181 | Weather displays | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 182 | Battery indicators | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 183 | CPU and memory indicators | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 184 | Network-status indicators | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 185 | Music and artwork information | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 186 | Music-reactive visualization | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 187 | Authorized Health Connect data | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 188 | HTTP data retrieval | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 189 | RSS/XML parsing | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 190 | Location/map visualizations | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 191 | Weather-driven artwork | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 192 | Wind-field visualization | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 193 | Particle themes | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 194 | Color-generated themes | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 195 | Tasker integration | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 196 | Android broadcast commands | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 197 | External intents | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 198 | Quick Settings tiles | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 199 | Home screen widgets | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 200 | Widget album switching | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 201 | Widget wallpaper selection | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 202 | Launcher shortcuts | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 203 | Geographic triggers | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 204 | Device-state triggers | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 205 | Theme-switch automation | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 206 | External automation variables | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 207 | Programmable wallpaper actions | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 208 | Automation-triggered preset loading | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 209 | Image caching | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 210 | Preprocessed next-wallpaper buffers | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 211 | Missing-file detection | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 212 | Missing-image recovery | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 213 | Image relinking | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 214 | Persistent shuffle state | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 215 | Reboot recovery | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 216 | Battery Saver awareness | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 217 | Do Not Disturb awareness where permitted | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 218 | Visibility-aware rendering | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 219 | Static-frame rendering | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 220 | Configurable frame rates | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 221 | GPU resource cleanup | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 222 | Scoped storage access | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 223 | Local artwork storage | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 224 | Offline operation | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 225 | Encrypted remote credentials | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 226 | Internet-free offline core | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 227 | No advertisements or analytics | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 228 | Backup and restore | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 229 | Metadata-preserving backup | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 230 | Multilingual infrastructure | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 231 | Material You support | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 232 | Dark theme | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 233 | Tablet-optimized layouts | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 234 | Rendering diagnostics | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 235 | Direct-boot support where safely achievable | BLOCKED_PLATFORM | NOT_RUN | System health / capability report — No Vulkan backend or proprietary Windows runtime. Direct boot intentionally excludes credential-encrypted artwork. Safe fallback only. |
| 236 | License-compliant modifiable source | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 237 | Rule debugger | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 238 | Future schedule preview | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 239 | Rule conflict resolver | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 240 | Wallpaper decision receipt | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 241 | Fairness-aware shuffle | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 242 | Rule dry-run mode | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 243 | Collection eligibility explainer | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 244 | Manual override window | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 245 | Exact duplicate detection | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 246 | Near-duplicate detection | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 247 | Corrupt-asset quarantine | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 248 | Protected originals | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 249 | Editing sidecars | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 250 | Orientation-specific focal points | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 251 | Composition safe-zone editor | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 252 | A/B wallpaper comparison | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 253 | Samsung DeX-adaptive workspace | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 254 | S Pen precision controls | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 255 | Hardware keyboard shortcuts | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 256 | Accessible reduced-motion mode | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 257 | High-contrast workspace mode | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 258 | Customizable editor density | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 259 | Multi-orientation preview lab | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 260 | Interactive gallery inspector | IMPLEMENTED_UNTESTED | DEVICE_PENDING | Control / Library / Rotation / Editor / Rules — Code exists; Android compilation, unit execution, and device acceptance are not yet verified. Core tests are committed but not executed. |
| 261 | Storage budget dashboard | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 262 | Configurable cache limits | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 263 | Local performance profiler | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 264 | GPU context-loss recovery | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 265 | One-touch safe mode | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 266 | Sanitized diagnostic export | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 267 | Verified data migrations | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 268 | Profile import and export | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 269 | Asset provenance records | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
| 270 | Remote source allowlists | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 271 | Provider rate-limit control | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 272 | Network budget rules | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 273 | Versioned scene manifests | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 274 | Safe formula evaluation | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 275 | Scene history and revision comparison | NOT_IMPLEMENTED | NOT_RUN | Not exposed — Requirement retained; implementation and acceptance verification remain outstanding. |
| 276 | Adaptive render quality policy | PARTIAL | NOT_RUN | Relevant native workspace — Foundation or limited behavior only. Full individual acceptance and expanded capability remain outstanding. |
