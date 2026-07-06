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
| JEI / EMI | optional, compile-time API | recipe viewer compat once we have recipes | deferred (copy Arsenal's setup when needed) |
| Veil | 1.21.1 (active) | post-processing / advanced rendering | **deferred — not in v1**, see §3 |

Rule: nothing else gets added without updating this table.

## 3. Veil assessment (requested research)

**What it is:** [Veil](https://github.com/FoundryMC/Veil) (FoundryMC) is a rendering library —
JSON-driven framebuffers and post-processing pipelines, full OpenGL shader support, shader
injection/modification, data-driven render types, dynamic lights, Quasar (data-driven particle
systems), and Necromancer (bone-based animation). ~312K downloads on
[Modrinth](https://modrinth.com/mod/veil), supports 1.21.1 on NeoForge and Fabric, actively
maintained (latest release July 2026, 1,300+ commits on the 1.21 branch).

**What it would buy us over vanilla + GeckoLib:**
- Real full-screen post-processing — vampire blood-sense vision, sun-scorch screen distortion,
  desaturation/red-tint worlds. Vanilla post shaders exist but are awkward and fight other mods.
- Quasar: particle systems defined in JSON instead of one hand-written particle class per effect
  (Arsenal needed a Java class per particle — `MushroomCloudParticle`, `LazerTrailParticle`, etc.).
- Dynamic lights and custom render pipelines for glowing blood magic.

**Costs and risks:**
- Becomes a *required runtime dependency for every player*, plus its ImGui companion lib.
- Documentation is thin — half the wiki pages currently don't load; learning happens by reading
  the example mod and source. That's slow, and we're optimizing for a fast first video.
- Compat surface: Veil needs specific Sodium versions for its buffer management; interaction with
  Iris/shader-pack setups (which you likely record with) is an open risk.
- Everything in the first two bloodlines' ability visuals is achievable with the toolkit already
  proven in Arsenal: custom `RenderType`s, GeckoLib render layers, custom particles, screen shake,
  full-screen overlay effects (flashbang), HUD mixins.

**Recommendation: skip Veil for v1, revisit at the polish milestone (M8).** Ship the first video
on vanilla + GeckoLib — zero new-library risk, known workflow. Adopt Veil later *if* a designed
effect genuinely needs post-processing (e.g. a full-screen blood-vision ability), and evaluate
Sodium/Iris interaction on your actual recording setup before committing. If you'd rather bet on
maximum visual ceiling now and accept the learning curve + dependency, say so and I'll wire it in
— but it should be a deliberate call, not a default.

## 4. UI approach (ranks + stats/skills menu)

**Recommendation: hand-rolled vanilla GUI, no UI library.**

- The skill tree / stat sheet is bespoke art-driven UI. Libraries (owo-lib is Fabric-first;
  LDLib and friends are heavy and geared to machine GUIs) don't save work for this — the effort
  is in layout, textures, and interaction code either way.
- Arsenal already has the precedent (`NukeTargetMapScreen`) plus keybind handling, HUD mixins,
  and the packet layer — all reusable patterns.
- Plan: a `client/screen/` package with a shared "vampire codex" style screen framework
  (tabbed: Character / Stats / Skills / Bloodline), driven by synced attachment data.
  Custom widgets for skill nodes + connecting lines; texture-atlas art from you (§6).

## 5. Roadmap 0 → 100

Sequenced for a fast first video (M1–M4), then the long game. Each milestone is playable/testable.

**M0 — Base (done).** Clean repo, build, datagen, CI-able jar.

**M1 — Vampire spine.** The systems every bloodline sits on:
- Player state attachment: bloodline id, rank, blood pool, allocated stats/skills. Synced to
  client; survives death/dimension change (rules TBD — open question Q1/Q2).
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

**M5 — Ranks.** Progression layered on bloodlines (design TBD — Q5): rank data in the attachment,
promotion triggers, rank-gated abilities/stats, rank UI.

**M6 — Character/Stats/Skills menu.** The §4 screen framework: stat sheet, allocation, skill
tree with your art. (Skeleton screen may land earlier as part of M2 if useful for the video.)

**M7 — Mobs.** Custom GeckoLib mobs (designs TBD — Q6): models/anims in Blockbench, spawning
rules, possibly bloodline-affiliated NPCs.

**M8 — World & polish.** Structures/worldgen if designed (Arsenal has the structure pipeline to
copy), Veil re-evaluation for a visual-overhaul pass, config options, performance.

**M9 — Bloodlines 3–8.** One at a time through the M3 pipeline, interleaved with videos.

## 6. Asset pipeline — what I need from you

Standard toolchain (same as Arsenal):

| Asset type | Tool | Format | Notes |
|---|---|---|---|
| Entity/mob/animated-item models | Blockbench (GeckoLib plugin) | `.geo.json` + `.animation.json` + texture PNG | keep the `.bbmodel` source in-repo (build excludes it) |
| Item/block textures | Aseprite / any pixel editor | PNG, **16×16** default (32×32 for hero items like guns were in Arsenal) | |
| UI/menu art | pixel editor, mock in Figma/paper first | PNG texture sheets, ≤256×256 per sheet | 9-slice-friendly panels where possible |
| Particle textures | pixel editor | PNG (single or sprite-sheet + `.mcmeta`) | |
| Sounds | any DAW / freesound | **OGG Vorbis**, mono for positional SFX | registered via datagen sound provider |
| Mod logo/icon | any | PNG (logo ~×2 wide banner for mods.toml; icon item texture 16×16) | |

**Concretely needed from you, in order:**
1. **Now (M1–M2):** nothing visual — but the *design docs*: blood/sun mechanics answers (§7) and
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

## 7. Open questions (need your answers, don't let me guess)

- **Q1 — Becoming a vampire:** are players vampires from world-start, or turned (item/ritual/bite)?
  Can you be human in this mod at all? Can you leave/switch a bloodline?
- **Q2 — Blood as a resource:** does blood replace hunger, sit alongside it, or act as mana?
  How is it gained (attacking? feeding on mobs/players? items?) and what happens at zero?
- **Q3 — Sun & weaknesses:** what does sunlight actually do (burn, debuff, power-off)? Per-bloodline
  differences (the Dacien are "Suneaters" — sun-immune? sun-powered?)? Other classic weaknesses
  (garlic, wood stakes, running water)?
- **Q4 — Choosing a bloodline:** how does a player join The Dacien vs Tharion — altar/ritual,
  found item, NPC, GUI choice on first night?
- **Q5 — Ranks:** what advances rank — XP-like grind, quest-ish deeds, blood consumed, boss kills?
  Are ranks per-bloodline (different names per line) or one shared ladder?
- **Q6 — Mobs:** roster? Hostile vampire hunters, feral vampires, thralls, bloodline NPCs?
- **Q7 — Multiplayer stance:** are bloodlines factions (PvP between lines?) or purely personal
  progression? Matters for M1 data design.
- **Q8 — Recording setup compat:** do you record with Iris/Sodium/shader packs? Decides how hard
  we test against them, and weighs on the Veil decision.
- **Q9 — Scale of first video:** target date / scope ceiling for M4, so M3 ability count can be
  sized to fit.

---

*Conventions carried from Arsenal: datagen is the source of truth for models/lang/tags/recipes;
run-dir isolation per run type; work directly on `main`; GPL-3.0.*
