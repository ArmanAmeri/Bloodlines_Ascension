# Bloodlines: Ascension

A NeoForge 1.21.1 vampire mod by ModishMonkee.

Eight ancient bloodlines. One long night. Bind yourself to a bloodline, feed to survive,
and rise through the ranks of the night — each bloodline with its own name, gifts, and price.

## Status

Early scaffold. First playable build targets two bloodlines:

- **The Dacien** — "The Suneaters"
- **Tharion** — "The Crimson Knights"

See [PROJECT_PLAN.md](PROJECT_PLAN.md) for the full roadmap, locked dependencies, and asset pipeline.

## Development

- Minecraft 1.21.1 / NeoForge 21.1.235 / Java 21
- `gradlew runClient` — dev client
- `gradlew runClient2` — second client (TestDummy) for multiplayer sync testing
- `gradlew runServer` — dedicated server (own `run-server/` dir, no client-only mods)
- `gradlew runData` — datagen into `src/generated/resources` (datagen is the source of truth for models, lang, tags, recipes)
