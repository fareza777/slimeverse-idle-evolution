# Architecture

## Guiding principle
All game rules live in a **pure-Kotlin core** (`game/`) with zero Android imports. The Android layer (`data/`, `ui/`, `ads/`, `billing/`) is a thin shell that drives the core, renders it, and persists it. This makes the entire simulation unit-testable on the JVM and keeps the UI replaceable.

## Layers

### 1. Game core (`game/`)
- `Elements.kt` — 10 elements + affinity multiplier chart.
- `Genes.kt` — `Locus` (8), `Allele` (dominance, stat mods, trait, visual key), `Genome` (8 loci × 2 alleles). Expression = higher dominance; ties express maternal. `Genome.breed` (Mendelian + mutation), `Genome.fuse` (mixing + higher mutation).
- `Species.kt` — `Family` (16, incl. 4 hybrids), `Rarity` (7), procedural `SpeciesDef` generation (160 base × 5 forms = 800), `Families.fusionResult` matrix, evolution options.
- `Skills.kt` — 16 skill archetypes with target/effect enums; tier/branch scaling.
- `Balance.kt` — all formulas: level growth, stats, power, damage mitigation, economy costs, enemy/zone scaling, idle clear rate, prestige gains, offline cap.
- `BattleSim.kt` — `BattleUnit`, `Status`, `BattleEvent` (sealed), deterministic `Battle.tick()` (SPD-driven action timers), `BattleFactory` builds stage/tower/dungeon battles.
- `Zones.kt` — 12 zones, deterministic enemy teams.
- `Content.kt` — habitats, dungeons, expeditions, research (24), talents (12), mastery, achievements (60+).
- `Prestige.kt` — rebirth upgrades, ascension perks, and all global multiplier aggregators.
- `GameState.kt` — `@Serializable` root state (resources, slimes, squad, eggs, breeds, expeditions, habitats, progress, codex, stats, settings).
- `GameEngine.kt` — orchestrator: every player action, loot application, achievement evaluation, automation passes, economy tick, offline computation, prestige resets, `newGame()`.

### 2. Android shell
- `data/SaveManager.kt` — JSON encode/decode, atomic write (tmp → rename) + rolling `.bak`, SAF export/import.
- `data/GameRepository.kt` — owns the `GameEngine`, exposes `StateFlow` revision counter, offline report, active battle, battle events (`SharedFlow`), outcome, toasts. Runs a 1 Hz economy/automation loop and a 10 Hz×speed battle loop on `Dispatchers.Default`. Autosaves every 30 s.
- `SlimeverseApp` — application singleton wiring repo + ads + consent + billing.
- `MainActivity` — edge-to-edge, consent gather → ads init, billing start, sync Remove Ads into state, save on pause.

### 3. UI (`ui/`)
- `theme/` — light (default) / dark / auto Material 3 schemes; Fredoka + Nunito variable fonts.
- `render/SlimeRenderer.kt` — procedural Canvas slime: family-specific harmonic body silhouette, element gradient, patterns, hide textures, expressive blinking eyes, crowns, wings, aura particles, squash/stretch. `SlimeView` composable wraps it with infinite animations.
- `render/BattleScene.kt` — arena: zone background, positioned units, event-driven VFX (projectiles, bursts, floating numbers, shake), outcome overlay.
- `components/` — resource pills, cards, buttons, badges, progress, dialogs.
- `screens/` — splash (Slimeverse → official F7 video), onboarding, main scaffold (bottom nav + banner), Nest, Collection, Detail, Fuse, Battle, Research, Talents, Codex, Ascension, Achievements, Settings, About.

## Data flow
```
User tap → Screen → repo.act { engine.action() } → engine mutates GameState
        → repo.bump() → rev StateFlow++ → Compose recomposes reading engine.state
Battle:  repo.startBattle → BattleFactory.stageBattle → battle loop ticks
        → battleEvents SharedFlow → BattleScene VFX; on end → engine.applyBattleWin
Offline: on init, now - lastSeen > 2min → engine.computeOffline → offline StateFlow → modal
```

## Determinism & testing
`Battle` and `GameEngine` accept injected `Random` and clock. Same seed → identical battle. JVM tests cover genetics inheritance (with a zero-mutation negative control), fusion matrix, battle determinism/termination, economy, offline cap, save round-trip, and content integrity.

## Persistence safety
- Schema version field + `migrate()` for forward compatibility.
- Atomic write prevents corruption on process death.
- Backup file recovered automatically if main is corrupt.
- `ignoreUnknownKeys` tolerates additive schema changes.