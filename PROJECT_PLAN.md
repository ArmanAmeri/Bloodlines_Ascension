# Bloodlines: Ascension — Project Plan

NeoForge 1.21.1 vampire mod. Eight bloodlines, ranks, an RPG stat/skill screen, custom mobs.
First public build ships with **two** bloodlines: **The Dacien** ("The Suneaters") and **Tharion** ("The Crimson Knights").

This document is the working plan agreed in the kickoff session (2026-07-06). It gets updated as
design details land. Bloodline lore, ability lists, rank names, and mob designs come from
ModishMonkee — nothing in here invents those.

---

## 1. Current state (Phase 1 — done)

Repo cleaned and rebuilt on Arsenal of Extinction's conventions:

```
src/main/java/com/modishmonkee/bloodlinesascension/
├── BloodlinesAscension.java      # main mod class, registry wiring
├── item/
│   ├── ModItems.java             # empty DeferredRegister, ready for content
│   └── ModCreativeModeTabs.java  # "Bloodlines: Ascension" tab (placeholder redstone icon)
└── datagen/
    ├── DataGenerators.java       # GatherDataEvent wiring
    ├── ModLanguageProvider.java  # en_us — datagen is source of truth for lang
    └── ModItemModelProvider.java
```

- Mod id `bloodlinesascension`, package `com.modishmonkee.bloodlinesascension` (matches Arsenal's
  no-underscore convention; the old template used `bloodlines_ascension`).
- Build: NeoForge 21.1.235, Parchment 1.21.1/2024.11.17 (template had a broken 1.21.11 value),
  Gradle 9.2.1, GeckoLib 4.7.6 wired in.
- Run-dir isolation ported from Arsenal: `run/` (client), `run-client2/` (TestDummy),
  `run-server/`, `run-data/`.
- `gradlew build` and `gradlew runData` verified green.

As systems come online the package layout grows to mirror Arsenal:
`entity/` (+ `client/`, `custom/`), `network/`, `effect/`, `particle/`, `sound/`, `event/`,
`client/` (keybinds, screens, HUD), `component/`, `damage/`, `mixin/`, `util/` — plus new
mod-specific packages `bloodline/`, `rank/`, and `skill/` when those systems start.

---

## 2. Locked tech stack

| Dependency | Version | Role | Status |
|---|---|---|---|
| NeoForge | 21.1.235 (MC 1.21.1) | platform | **locked** |
| Parchment | 1.21.1 / 2024.11.17 | mappings | **locked** |
| GeckoLib | 4.7.6 (neoforge-1.21.1) | animated entities, mobs, animated items | **locked** — already a required dep in `neoforge.mods.toml` |
| NeoForge Data Attachments | built-in | per-player vampire state (bloodline, rank, stats) | **locked** — no external lib needed |
| NeoForge `StreamCodec` payloads | built-in | client↔server sync, ability triggers (Arsenal `ModNetwork` pattern) | **locked** |
| Vanilla GUI (`Screen`/`GuiGraphics`) | built-in | stats/skills menu, rank UI, HUD overlays | **locked** — see §4 |
| Veil | 4.3.0 (neoforge-1.21.1) | shaders, post-processing, particle systems, dynamic lights — the VFX layer | **locked** — required dep, see §3 |
| JEI / EMI | optional, compile-time API | recipe viewer compat once we have recipes | deferred (copy Arsenal's setup when needed) |

Rule: nothing else gets added without updating this table.

## 3. Veil — adopted (decision 2026-07-06)

**What it is:** [Veil](https://github.com/FoundryMC/Veil) (FoundryMC) is a rendering library —
JSON-driven framebuffers and post-processing pipelines, full OpenGL shader support, shader
injection/modification, data-driven render types, dynamic lights, Quasar (data-driven particle
systems), and Necromancer (bone-based animation). ~312K downloads on
[Modrinth](https://modrinth.com/mod/veil), 1.21.1 NeoForge supported, actively maintained
(4.3.0 released July 2026).

**Decision:** locked in as a required dependency (`foundry.veil:veil-neoforge-1.21.1:4.3.0`).
Visual quality is the priority for this mod; Veil's post-processing and particle systems are the
ceiling-raiser (blood-sense vision, sun-scorch distortion, high-end ability VFX) and that ceiling
is worth the dependency cost.

**Division of labor — Veil does NOT replace GeckoLib.** They solve different problems and stack:
- **GeckoLib** owns models, rigs, and keyframed animation (entities, mobs, animated items) —
  the Blockbench workflow stays exactly as in Arsenal. We deliberately ignore Veil's Necromancer
  animation system: less mature, no established Blockbench pipeline, no reason to switch.
- **Veil** owns everything shader/VFX: post-processing screens, Quasar JSON particle systems
  (instead of one Java class per particle like Arsenal), dynamic lights, custom render types.
  A GeckoLib-animated mob can have Veil-driven aura/trail/light effects layered on top.

**Risks accepted, to manage actively:**
- Docs are thin (half the wiki doesn't load) — we learn from the example mod + source; budget
  extra time for the *first* Veil effect, it gets cheap after that.
- Compat: Veil couples to specific Sodium versions; test on the actual recording setup
  (Iris/Sodium/shader packs — Q8) *early*, not at video-crunch time.
- **Iris ↔ Veil dynamic lights (investigated 2026-07-07):**
  - *Baseline bug:* with Iris 1.8.14-beta.1 in `run/mods` — even with shaders *disabled* — spawning
    any Veil point light floods `GL_INVALID_OPERATION ... program texture usage` every frame and the
    light never renders. Iris's pipeline replacement defeats Veil's on-the-fly patching of the
    ALBEDO/NORMAL dynamic G-buffer samplers. Sodium 0.8.12-beta.2 alone is fine.
  - *Fix attempt — [iris-veil-compat](https://github.com/leon-o/iris-veil-compat) 0.3.0-beta*
    (`run/mods/irisveil-0.3.0.jar`, from the GitHub release; CurseForge is Cloudflare-walled).
    Its job: merge Veil shader code into the shaderpack's gbuffer programs via runtime AST patching.
    **Result: half-fixes it.** The `GL_INVALID_OPERATION` flood is *gone* (0 errors) — the pipeline
    bypass is genuinely bridged. **But** with Complementary Reimagined r5.7.1 + shaders on, the merged
    point-light shader fails to compile every frame: `gbuffers_veil_veil_light_point.vsh: 0(419):
    error C7011: implicit cast from "vec4" to "vec3"` (NVIDIA strict-GLSL). So the point light still
    doesn't draw *through this shaderpack*. Veil's own `point.vsh` source is clean — the bad cast is
    in the compat mod's merge with Complementary's gbuffer code.
  - **Still open (recording):** (a) try other shaderpacks — the C7011 may be Complementary-specific;
    (b) file the bug upstream at leon-o/iris-veil-compat (active, last release June 2026); (c) fall
    back to filming Veil-light scenes with shaders off. Compat mod kept in `run/mods` regardless — it
    removes the crippling flood, which is strictly better than bare Iris.
- Pin the Veil version; upgrade deliberately, never mid-milestone.

## 4. UI approach (ranks + stats/skills menu)

**Recommendation: hand-rolled vanilla GUI, no UI library.**

- The skill tree / stat sheet is bespoke art-driven UI. Libraries (owo-lib is Fabric-first;
  LDLib and friends are heavy and geared to machine GUIs) don't save work for this — the effort
  is in layout, textures, and interaction code either way.
- Arsenal already has the precedent (`NukeTargetMapScreen`) plus keybind handling, HUD mixins,
  and the packet layer — all reusable patterns.
- All screen art is **hand-drawn by ModishMonkee in Aseprite** — fully custom textures, not
  vanilla-styled widgets.

**Character screen — layout confirmed from mockup (2026-07-06):**
- Top center, overlapping the panel's upper edge: a round **medallion/emblem** (bloodline sigil
  and/or player portrait — TBD which).
- Beneath it, spanning the panel: a **segmented progress bar** with tick marks (rank progression —
  blood or pureblood progress depending on category).
- Left column: **basic stats** — Rank, Health, Damage, Armor, other basic info.
- Center: framed **player model preview** (live 3D render, like the inventory paper-doll).
- Right column: framed **advanced stats** panel ("for nerds" — detailed numbers).

Driven by synced attachment data; later grows tabs (Skills / Bloodline) as those systems land.

**In-game HUD — blood orb (design 2026-07-06, PoE-2-life-globe inspired):**
- A round **blood orb** anchored at a bottom corner of the screen near the hotbar (corner TBD),
  overlapping a rounded panel behind it. Liquid level = current blood.
- The liquid is **dynamic, not a static sprite**: a 1D spring-wave surface simulation on the CPU
  (reacts to gameplay — taking damage or spending blood makes it slosh) rendered through a custom
  Veil fragment shader (circle-masked, scrolling noise for the liquid body, bright meniscus line,
  glass highlights). Fallback if the shader path fights us: same wave sim rendered as a
  vertex-colored mesh with a scrolling texture — still animated, no custom shader.
- Orb glass/frame art: hand-drawn (Aseprite); liquid: procedural.
- **Skill activation — hold-to-swap hotbar (ModishMonkee design, 2026-07-06):** holding a
  dedicated key (e.g. the `<` key; rebindable) visually **replaces the vanilla hotbar with a
  skill hotbar** (rounded-square slots, ~4 in the reference sketch). While held, the number keys
  cast the corresponding skill instead of switching items; releasing the key restores the vanilla
  hotbar instantly. Slots show cooldown sweeps and "not enough blood" dimming; skills are
  assigned to slots from the skills screen. Slot art hand-drawn (Aseprite).
  Implementation: cancel the vanilla HOTBAR gui layer while active, render ours in its place,
  and lock the selected inventory slot so digit presses don't leak into item switching.
  Open — Q10: hold vs. toggle, slot count (fixed or rank-gated), exact default key.

## 5. Locked design: ranks, blood & pureblood (from ModishMonkee, 2026-07-06)

The vampire hierarchy, in categories. Players climb the ladder; two ranks exist only as mobs.

| Category | Rank(s) | Player? | Notes |
|---|---|---|---|
| Underlings | **Thrall** | no — mob only | Lowest of vampires, barely a vampire at all — a mindless servant/slave beast. Look inspired by gargoyles (stone statues that come alive). |
| Lessers | **Turned / Spawn** | yes — entry rank | *Turned*: the normal path — bitten by a vampire, becomes a vampire youngling. *Spawn*: born a vampire, not turned — exists as a mob; players can start as one **only** via a gamerule ("Be born as a Vampire Spawn"). |
| Lessers | **Knight** | yes | The bridge between the Lessers and the Nobles. |
| Nobles | **Baron → Count → Duke** | yes | The **pureblood** mechanic begins at Baron. |
| Elders | **Prince/Princess → Monarch** | yes | Monarch = the patriarch of the clan/bloodline. |
| — | **Progenitor** | never | The origin of each bloodline. Future plan: progenitors as defeatable bosses (maybe monarch bosses too — parked for later). |

**Blood vs. Pureblood:**
- **Blood** — the universal vampire resource. Powers the *general* vampire abilities that all
  lesser vampires share across every bloodline.
- **Pureblood** — bloodline-specific. Lore: the concentration of the progenitor's blood in your
  veins; it determines how much of the bloodline's *special* abilities you can wield. Exists only
  from Baron upward.
- **Progression loop:** gather blood to climb the Lessers up to Knight → from Baron on, gather
  pureblood to climb the Nobles and Elders.

Still open on ranks: see Q5 in §8 (how pureblood is gathered, what triggers a rank-up, numbers).

## 6. Roadmap 0 → 100

Sequenced for a fast first video (M1–M4), then the long game. Each milestone is playable/testable.

**M0 — Base (done).** Clean repo, build, datagen, CI-able jar.

**M1 — Vampire spine.** The systems every bloodline sits on:
- Player state attachment: bloodline id, rank (+ category), blood pool, pureblood pool
  (reserved, active from Baron), allocated stats/skills. Synced to client; survives
  death/dimension change (rules TBD — open question Q1/Q2).
- Blood resource mechanic (design TBD — Q2): gain/spend hooks, HUD element.
- Sun/weakness framework (design TBD — Q3): the "cost" side of being a vampire.
- Debug commands (`/bloodline set|get`, `/blood add`) for testing and filming.

**M2 — Bloodline framework (pluggable).** The extensibility requirement:
- `Bloodline` as a registry-backed definition (id, display name, theme color, ability set,
  passive modifiers, rank track). Adding bloodline #3–8 = one definition class + assets, no
  core-system edits.
- Ability framework: `Ability` objects with cooldowns, cast types (instant/held/toggle),
  server-side execution + client presentation split, keybinds (Arsenal keybind pattern),
  ability-bar HUD.
- Join/choose flow — mechanic TBD (Q4).

**M3 — The Dacien + Tharion content.** Built once you hand over their design docs:
- Abilities per your spec, GeckoLib visuals, sounds, particles.
- Bloodline-specific items (sigils, weapons, ritual items — whatever the designs call for).

**M4 — First video cut.** Balance pass, polish the two bloodlines, tab icon item, mod logo,
lang, recipes + JEI/EMI compat if items are craftable. **← ship video 1.**

**M5 — Ranks.** Implement the §5 ladder: rank/category logic in the attachment, blood-driven
rank-ups through the Lessers, pureblood mechanic from Baron up, rank-gated abilities, the
"Be born as a Vampire Spawn" gamerule, rank display in the character screen. Blocked on Q5
(pureblood sources, rank-up triggers, numbers).

**M6 — Character/Stats/Skills menu.** The §4 screen: confirmed mockup layout first (medallion,
rank progress bar, basic stats, player preview, advanced stats), skill tree tab later.
(Skeleton screen may land earlier as part of M2 if useful for the video.)

**M7 — Mobs.** Custom GeckoLib mobs: **Thrall** (gargoyle-inspired servant beast) and
**Vampire Spawn** are locked roster entries (§5); rest of roster TBD — Q6. Models/anims in
Blockbench, spawning rules, possibly bloodline-affiliated NPCs. Long-term: Progenitor bosses.

**M8 — World & polish.** Structures/worldgen if designed (Arsenal has the structure pipeline to
copy), full-mod visual polish pass (Veil post-processing on everything worth it), config options,
performance.

**M9 — Bloodlines 3–8.** One at a time through the M3 pipeline, interleaved with videos.

## 7. Asset pipeline — what I need from you

**Canonical palette (locked 2026-07-06)** — all art and rendering derives from these; defined in
`util/ModColors.java` and importable into Aseprite from `art/bloodlines_palette.gpl`:

| Name | Hex | Use |
|---|---|---|
| blood_black | `#0D0406` | blood-tinted black — backgrounds, voids, deepest liquid |
| blood_dark | `#6B0F16` | dark blood red — body color, dried/venous blood |
| blood_bright | `#C8202C` | bright blood red — highlights, meniscus, fresh blood |
| silver | `#C0C4CC` (+shadow `#7A7E88`) | frames, linings, lesser-rank trim |
| gold | `#D9A93F` (+shadow `#8F6B22`) | frames, linings, noble/elder-rank trim |

`.aseprite` sources live in `art/` alongside the palette.

Standard toolchain (same as Arsenal):

| Asset type | Tool | Format | Notes |
|---|---|---|---|
| Entity/mob/animated-item models | Blockbench (GeckoLib plugin) | `.geo.json` + `.animation.json` + texture PNG | keep the `.bbmodel` source in-repo (build excludes it) |
| Item/block textures | Aseprite / any pixel editor | PNG, **16×16** default (32×32 for hero items like guns were in Arsenal) | |
| UI/menu art | **Aseprite, hand-drawn** (mock layout rough first) | PNG texture sheets, ≤256×256 per sheet | 9-slice-friendly panels where possible |
| Particle textures | pixel editor | PNG (single or sprite-sheet + `.mcmeta`) | |
| Sounds | any DAW / freesound | **OGG Vorbis**, mono for positional SFX | registered via datagen sound provider |
| Mod logo/icon | any | PNG (logo ~×2 wide banner for mods.toml; icon item texture 16×16) | |

**Concretely needed from you, in order:**
1. **Now (M1–M2):** nothing visual — but the *design docs*: blood/sun mechanics answers (§8) and
   both bloodline ability lists. These block everything.
2. **M2:** HUD art direction — blood meter + ability bar style (sketch is enough, I can draft
   placeholder art you replace later).
3. **M3:** per-ability VFX direction; any bloodline item art (16×16 PNGs) and, if abilities spawn
   entities/projectiles, Blockbench GeckoLib models for them (I can do programmer-art placeholders
   so filming isn't blocked).
4. **M4:** mod logo PNG + creative-tab icon item texture.
5. **M6:** the big one — skill tree / stat screen art (panels, node frames, tab buttons). Mock it
   rough first; we lock layout before you paint finals.
6. **M7:** mob models + animations in Blockbench (GeckoLib format), one per mob design.

## 8. Open questions (need your answers, don't let me guess)

- **Q1 — Becoming a vampire:** *(partially answered: the normal path is being **bitten/turned**;
  gamerule allows starting as a born Spawn).* Still open: how does the first bite happen in
  gameplay (a vampire mob bites you? voluntary ritual with an NPC?)? Can players stay human?
  Can you leave/switch a bloodline?
- **Q2 — Blood as a resource:** does blood replace hunger, sit alongside it, or act as mana?
  How is it gained (attacking? feeding on mobs/players? items?) and what happens at zero?
- **Q3 — Sun & weaknesses:** what does sunlight actually do (burn, debuff, power-off)? Per-bloodline
  differences (the Dacien are "Suneaters" — sun-immune? sun-powered?)? Other classic weaknesses
  (garlic, wood stakes, running water)?
- **Q4 — Choosing a bloodline:** how does a player join The Dacien vs Tharion — altar/ritual,
  found item, NPC, GUI choice, or decided by who turned you?
- **Q5 — Rank-up mechanics:** *(ladder itself locked — §5).* How is pureblood gathered? What
  triggers a rank-up — hitting a threshold automatically, or a ritual/ceremony? Rough costs per
  rank? Do rank names differ per bloodline or is the ladder shared?
- **Q6 — Mobs:** *(Thrall + Vampire Spawn locked — §5; Progenitor bosses parked for later).*
  What else — vampire hunters, feral vampires, bloodline NPCs?
- **Q7 — Multiplayer stance:** are bloodlines factions (PvP between lines?) or purely personal
  progression? Matters for M1 data design.
- **Q8 — Recording setup compat:** *(answered: same dev mods as Arsenal — Iris 1.8.12,
  Sodium 0.6.13, JEI, Jade, etc. — copied into `run/mods` and boot-tested with Veil.)*
- **Q9 — Scale of first video:** target date / scope ceiling for M4, so M3 ability count can be
  sized to fit.
- **Q10 — HUD details:** which bottom corner for the blood orb? Is the character-screen medallion
  the same blood orb, or a separate sigil/portrait? Skill hotbar: hold or toggle? Slot count —
  fixed 4, or growing with rank? Default key (the physical `<` key only exists on some keyboard
  layouts — default comma and let you rebind?).

---

*Conventions carried from Arsenal: datagen is the source of truth for models/lang/tags/recipes;
run-dir isolation per run type; work directly on `main`; GPL-3.0.*
