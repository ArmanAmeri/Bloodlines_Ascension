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

Three layers, all on the same 64×64 canvas (ring 4px from canvas left + bottom), exported
as separate PNGs. Draw order in game: panel → orb_back → liquid (code) → orb_front.

| File | Path | Size | Notes |
|---|---|---|---|
| Panel | `.../textures/gui/panel.png` | 64×64 | Bottommost, behind everything. No placeholder — add when ready. |
| Interior | `.../textures/gui/orb_back.png` | 64×64 | Behind the liquid: dark glass interior. Placeholder committed — overwrite. |
| Ring + deco | `.../textures/gui/orb_front.png` | 64×64 | Over the liquid: gold ring and filigree (waves tuck under the ring's inner edge). Placeholder committed — overwrite. |

(Full path prefix: `src/main/resources/assets/bloodlinesascension/`)

Liquid geometry wired in code (`BloodOrbHudLayer`): center (27, 37), radius 20 on the
64×64 canvas. If the ring's inner circle differs, say so — three constants to change.

## Coming up (paths reserved, not wired yet)

| File | Path | Size | Wired in |
|---|---|---|---|
| Skill slot frame | `.../textures/gui/skill_slot.png` | TBD (~24×24) | M2 skill hotbar |
| Character screen panel | `.../textures/gui/character/background.png` | ≤320×240 | M6 (skeleton maybe earlier) |
