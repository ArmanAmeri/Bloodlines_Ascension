# Art drop guide

`.aseprite` sources live here. Exported PNGs go into the mod resources at the exact
paths below — the code picks them up automatically (restart the client after adding
a file; plain F3+T works once a file already existed).

Palette: import [bloodlines_palette.gpl](bloodlines_palette.gpl) into Aseprite
(drag onto the palette panel).

## Export rules

- 1 GUI pixel = 1 texture pixel (export at 100%, never the zoomed view)
- Transparent background, no anti-aliasing
- State/animation variants: identical canvas size per variant

## Blood orb HUD

| File | Path | Size | Notes |
|---|---|---|---|
| Orb frame | `src/main/resources/assets/bloodlinesascension/textures/gui/blood_orb_frame.png` | 64×64 | Ring 4px from canvas left + bottom. Everything in one layer: interior (behind liquid), ring, filigree. A placeholder is committed — overwrite it. |

Liquid geometry wired in code (`BloodOrbHudLayer`): center (27, 37), radius 20 on the
64×64 canvas. If the ring's inner circle differs, say so — three constants to change.

## Coming up (paths reserved, not wired yet)

| File | Path | Size | Wired in |
|---|---|---|---|
| Skill slot frame | `.../textures/gui/skill_slot.png` | TBD (~24×24) | M2 skill hotbar |
| Character screen panel | `.../textures/gui/character/background.png` | ≤320×240 | M6 (skeleton maybe earlier) |
