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

## Character screen (wired — `CharacterScreen`)

Opened from a button in the survival inventory (beside the recipe-book toggle). Code
is live and composites the per-layer PNGs from `playerMenu.aseprite`.

Source is a shared **250×250** canvas; export each layer at full canvas size (transparent
where empty) so they all stack at the same origin. Draw order (back → front) mirrors the
Aseprite layer stack. Rendered at 1× for now (`SCALE` in `CharacterScreen`).

| Layer | Path | Notes |
|---|---|---|
| Background | `.../textures/gui/background.png` | Base panel, drawn first. |
| Frame | `.../textures/gui/frame.png` | Border/filigree over the background. |
| Crest | `.../textures/gui/crest.png` (+ `crest_hover.png`) | Central medallion. Hover state exists; hover/click wiring pending (needs the crest's bounds — see open questions). |
| Essence bar | `.../textures/gui/essencebar_silver.png` + `essencebar_gold.png` | Rank progress bar. Silver = Lesser ranks, gold = Noble/Elder (pureblood). Code picks the variant by rank category once that data is synced; currently draws silver. Bar **fill** overlay TBD. |
| Menu-button icon | `.../textures/gui/menu_button.png` | **20×36** (two stacked 20×18 states: normal on top, hover below). Optional — the inventory button shows a gold-framed blood placeholder until this lands. |

Not yet wired (present as Aseprite layers, no PNGs consumed yet): `buttons`, `milestoneMark`,
`separator`. Say how these should behave and I'll composite them in.

## Coming up (paths reserved, not wired yet)

| File | Path | Size | Wired in |
|---|---|---|---|
| Skill slot frame | `.../textures/gui/skill_slot.png` | TBD (~24×24) | M2 skill hotbar |
