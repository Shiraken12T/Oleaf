<p align="center">
  <img src="icon.png" alt="Oleaf logo" width="160">
</p>

# Oleaf

Oleaf is a client-side Fabric mod by Shiraken12T. It replaces Options → Video Settings with one screen for graphics presets, spatial upscaling, frame generation, and an FPS overlay.

Mod id: `oleaf`  
Config file: `config/oleaf.json`  
License: MIT

Oleaf is also available on CurseForge:

[![CurseForge](https://cf.way2muchnoise.eu/title/oleaf.svg)](https://www.curseforge.com/minecraft/mc-mods/oleaf)

## Graphics hub

Opening Video Settings opens the Oleaf screen. A preset fills the controls. Apply writes those values into Minecraft options. If Sodium is installed, Apply also writes the matching Sodium options. If OptimizedCore is installed, presets can write its options as well.

Presets:

- Low-End Laptop
- Balanced Survival
- PvP / Competitive
- Builder + Shaders

Controls on the hub include graphics quality, render distance, simulation distance, entity distance, max frame rate, VSync, brightness, particles, clouds, entity shadows, view bobbing, FOV, GUI scale, and fullscreen.

Advanced View opens the normal Minecraft video screen, and the Sodium or Iris screens when those mods are installed. A Shaders button is shown when Iris is present.

## Upscaling

Upscaling renders the world at a lower resolution and scales that image back up. The HUD stays at native resolution.

Filters:

- FSR 1: AMD FidelityFX Super Resolution 1.0, two passes (EASU, then RCAS). Shader header version is v1.20210629.
- SGSR 1: Qualcomm Snapdragon Game Super Resolution 1, one pass.
- Bicubic, bilinear, and nearest.

Quality modes and the internal world scale:

- Off / Native: 100%
- Ultra Quality: 77%
- Quality: 67%
- Balanced: 59%
- Performance: 50%
- Ultra Performance: 40%
- Custom: a percentage you set

FSR 1 has a sharpen control (RCAS). On 1.20.x and 1.21.x, Apply can rebuild an active Iris shader pack at the new resolution.

This is spatial upscaling of one frame. It is not FSR 2, FSR 3, or FSR 4.

## Frame generation

Frame generation inserts extra frames by blending the last two real frames. The world is not rendered again for those extra frames.

- Multipliers: 2x, 3x, 4x, 5x, and 6x
- Pace Frames: spaces the presented frames more evenly
- Adaptive FG: lowers the multiplier when a real frame takes too long
- Apply FG-Friendly Settings: turns VSync off and leaves Max FPS as it is

Fast camera or world movement can show ghosting, because the blend does not use motion vectors. A lower multiplier reduces that.

This is Oleaf's own blend. It is not AMD FSR 3 frame generation.

## FPS overlay

The overlay can show FPS, frame time, memory, ping, entity count, and the current FSR and frame generation state. Position, scale, and opacity are saved in `config/oleaf.json`.

Keybinds (Controls → Oleaf):

- Toggle Performance Overlay
- Toggle Overlay Edit Mode
- Reset Overlay Position

The same options are on the hub and in Mod Menu → Oleaf.

## Dependencies

Required:

- Fabric Loader
- Fabric API
- Mod Menu
- Cloth Config

Optional:

- Sodium
- Iris
- OptimizedCore

Oleaf does not include those mods. It only talks to them when they are already installed.

## Versions in this repository

| Folder | Minecraft | Mod Menu | Cloth Config | Java | GPU upscale and frame generation |
| --- | --- | --- | --- | --- | --- |
| repository root | 1.21.11 | 17.0.0 or later | 21.11.0 or later | 21 | Active |
| `versions/fabric-1.20.1` | 1.20.1 | 7.2.2 or later | 11.1.136 or later | 17 | Active |
| `versions/fabric-26.2` | 26.2 | 20.0.1 or later | 26.2.155 or later | 25 | Not active. Settings and the graphics screen still work. |

Each jar loads only on the Minecraft version it was built for. Mod Menu and Cloth Config versions follow that Minecraft version. A 1.21.11 jar does not load on 1.21.4, because 1.21.4 does not have Mod Menu 17 or Cloth Config 21.11, and the mixins target 1.21.11 methods.

## Source layout

Java package: `com.shiraken.optimizationcompanion`

- `config` — `oleaf.json` and presets
- `gui` — Mod Menu and Cloth Config
- `gui/graphics` — the video settings screen and its sub-screens
- `settings` — Apply, and the Sodium, Iris, and OptimizedCore bridges
- `upscale` — FSR 1, SGSR 1, and the other filters
- `framegen` — frame blending
- `hud` — the overlay
- `keybind` — overlay keys
- `mixin` — hooks into options, rendering, and the window

An older config named `config/optimization_companion.json` is copied to `config/oleaf.json` on first load if the new file is missing.

## Build

Open a terminal in the folder for the version you want (the repository root, `versions/fabric-1.20.1`, or `versions/fabric-26.2`) and run:

```
./gradlew build
```

On Windows:

```
gradlew.bat build
```

Use a JDK that matches the Java column in the table above.

The remapped jar is written to `build/libs/`. A packaging task may also copy it to a local `jars/<minecraft-version>/` folder. Built jars are not committed to this repository.

## Support

If you want to support Oleaf development:

[![Ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/shiraken12t)

## License

Oleaf is MIT. The full text is in `LICENSE`.

MIT allows use, copying, modification, and redistribution, including inside modpacks. Changing the license file is not required to use the mod, and the license cannot be used to forbid edits.

If you ship Oleaf, or a modified copy, in a mod, modpack, or fork, please keep a short credit to Shiraken12T in the mod list, pack credits, or README.

AMD FSR 1 shaders are MIT. SGSR 1 shaders are BSD-3-Clause. Notices for both are in `THIRD_PARTY_NOTICES.txt`. Those notices must stay with the shader files.
