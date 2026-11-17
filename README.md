# Slimeverse: Idle Evolution RPG

A deep, premium idle monster-breeding RPG for Android. Collect expressive slimes, splice their genes, fuse hybrid species, evolve spectacular forms, and lead your squad through zones, dungeons and an endless tower — then Rebirth and Ascend for long-term power.

**A game by F7 Developer.** Built for players worldwide. No pay-to-win.

<p align="center">
  <img src="aso/icon_512.png" width="120" alt="Slimeverse icon"/>
</p>

## Highlights

- **Real genetics.** 8 gene loci, each with dominant/recessive alleles. Mendelian breeding, fusion mixing, and mutagen-driven mutation. Thousands of meaningful combinations — body shape, eyes, crown, aura, patterns and stats all derive from the genome.
- **Procedural species.** 16 families × 10 elements = 160 base species, 800 evolution forms, 4 secret hybrid families discovered through a fusion matrix.
- **Deep auto-battle.** Deterministic tick simulation with elemental affinities, skills, shields, heals, DoTs, stuns, crits and 25+ passive traits. Satisfying VFX: projectiles, bursts, floating damage, screen shake.
- **Massive progression.** 12 zones, endless tower, daily dungeons, timed expeditions, 4 living habitats, a 24-node research lab, element mastery, breeder talents, 60+ achievements, and a full evolution codex.
- **Multi-layer prestige.** Rebirth (Stardust) and Ascension (Aether Cores + the Aether element) with permanent perk trees.
- **True offline play.** Your nest keeps working up to 48h. Reliable autosave with atomic writes, backup, and export/import.
- **Premium UX.** Light fantasy theme by default (dark + auto available), playful bundled fonts, big readable text, native Android navigation insets respected.
- **Living animation.** Every screen has a living animated background (drifting clouds, parallax hills, themed particles). The Nest is an animated idle-farm terrarium with wandering, bouncing slimes and rising resource orbs; expeditions show slimes marching a trail; battles have projectiles, bursts, floating damage, screen shake and blinking expressive slimes.
- **Fair monetization.** AdMob banner/interstitial/optional rewarded with Google UMP consent, plus a one-time non-consumable **Remove Ads** ($4.99) via Play Billing with restore.

## Screenshots

See [`aso/`](aso/) for the Play Store feature graphic, icon, and device screenshots.

## Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin 2.1 |
| UI | Jetpack Compose (Material 3) + custom Canvas rendering |
| Rendering | Procedural vector slime renderer (no sprite assets) |
| Architecture | Pure-Kotlin game core + Android shell (MVVM-ish repository) |
| Persistence | kotlinx.serialization JSON, atomic save + backup |
| Ads | Google Mobile Ads 23.x + UMP 3.x |
| Billing | Play Billing 8.x |
| Min / Target SDK | 26 / 36 |
| Build | Gradle 8.14.3, AGP 8.13, R8 minify + resource shrink |

The entire game logic (`game/` package) is **pure Kotlin with no Android dependencies**, so genetics, battle, economy, offline and prestige are unit-tested on the JVM.

## Project structure

```
app/src/main/java/com/f7developer/slimeverse/
  game/        # pure-Kotlin core: elements, genes, species, skills, balance,
               # battle sim, zones, content, prestige, engine, state
  data/        # SaveManager, GameRepository (flows, tick + battle loops)
  ads/         # AdConfig, ConsentManager (UMP), AdManager, BannerAd
  billing/     # BillingManager (Remove Ads + restore)
  ui/
    theme/     # light/dark color schemes, typography
    render/    # SlimeRenderer, BattleScene (VFX)
    components/# shared premium UI kit
    screens/   # all game screens
```

## Build

Requires JDK 17+ and Android SDK (compileSdk 36). See [`docs/BUILD.md`](docs/BUILD.md).

```bash
./gradlew assembleDebug      # debug APK (test ad units)
./gradlew testDebugUnitTest  # JVM game-logic tests
./gradlew assembleRelease    # release APK (needs keystore.properties)
./gradlew bundleRelease      # release AAB for Play Store
```

## Configuration before publishing

1. **AdMob:** create an app + ad units, set the App ID in `AndroidManifest.xml` and the real unit IDs in `ads/AdConfig.kt` (`RELEASE_*`). Until then, builds use Google's official test units.
2. **Billing:** create the non-consumable product `remove_ads_permanent` at $4.99 in Play Console.
3. **Signing:** add `keystore.properties` (see `docs/BUILD.md`). Never commit your keystore.
4. **Privacy policy:** host a page covering AdMob/UMP/Billing data use and link it in Play Console.

## Documentation

- [`docs/GAME_DESIGN.md`](docs/GAME_DESIGN.md) — systems, formulas, balance
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — code architecture & data flow
- [`docs/BUILD.md`](docs/BUILD.md) — build, sign, test, release
- [`aso/metadata.md`](aso/metadata.md) — Play Store listing & ASO

## License

Code under the [MIT License](LICENSE). Slimeverse and F7 Developer branding/assets are proprietary to F7 Developer.