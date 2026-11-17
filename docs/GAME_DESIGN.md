# Game Design & Balance

## Core loop
Collect → Fuse/Breed → Mutate → Evolve → Build Squad → Auto-Battle → Unlock → Ascend.

## Genetics
- 8 loci: Size, Temper, Hide, Aura, Eyes, Crown, Pattern, Core.
- Each locus has 2 alleles. Expressed = higher `dominance` (tie → maternal). Recessive alleles persist and can surface in offspring.
- Alleles carry stat mods, an optional trait, and a visual key consumed by the renderer.
- **Breeding**: one random allele per locus from each parent; mutation chance rerolls alleles. Parents kept; produces an egg (gestation 2 min + hatch time).
- **Fusion**: parents consumed; child genome mixes alleles with higher mutation; family from the fusion matrix; element = primary parent (+ secondary if different); rarity ≥ max(parents) with upgrade odds; level = avg/4.
- **Mutation chamber**: spend 3 mutagen to reroll one locus (rare boost).
- Prismatic aura = shiny, +10% all stats.

## Species & evolution
- 16 families (12 base + Seraph/Chimera/Titan/Phantom hybrids) × 10 elements = 160 base species.
- Each species: tier 0 → tier 1 (branch A "Bulwark" or B "Striker") → tier 2. 800 forms total.
- Evolution gates: level 20 (t1), level 45 (t2); costs essence + gold scaled by rarity; rarity +1 on evolve.
- Fusion matrix maps family tag pairs to result families; same-family = purity (+rarity odds). 78 base pairs discoverable in the codex.

## Stats & combat
- `levelGrowth(l) = 1 + 0.14(l-1) + 0.0025(l-1)²` (≈35× at level 100).
- Final stat = base × growth × rarity × (1 + gene/trait/talent mods) × ascension × mastery.
- Damage = ATK × skillMult × 120/(120+DEF) × elementMult × variance × crit.
- Element chart: each element strong vs 2 (1.5×), weak vs inverse (0.75×); Lumen↔Umbra mutual; Aether deals 1.25× to all.
- Battle: SPD-driven action timers (10 Hz sim). Skills on cooldown; basic attacks otherwise. Front row targeted first; taunt overrides; Guardian intercepts back-row damage.
- 25+ traits with battle hooks (regen, berserk, vampiric, thorns, bulwark, swift, lucky, scholar, echo, static, poison, stun-immune, etc.).

## Economy (interconnected, non-grindy)
- **Gold**: battles, habitats (Pond), expeditions, Gold Rush dungeon. Spent on feed/fuse/breed/evolve/research/habitat upgrades.
- **Essence**: battles, Grove, Essence Mine. Spent on evolve/breed/research.
- **Mutagen**: Grotto, Gene Vault, bosses, achievements. Spent on mutation/fusion boost.
- **Research Points**: Sky Nest, expeditions, bosses, dungeons. Spent on research tree.
- **Stardust / Aether Cores**: prestige currencies → permanent multipliers + perk trees.
- **Talent Points**: breeder levels + achievements → breeder talents.
- Multipliers aggregate from research, talents, rebirth upgrades, ascension perks, traits (Lucky/Gemtouch/Scholar).

## Progression systems
- 12 zones × 10 stages (boss at 10). Auto-progress unlock. Idle farm at current stage.
- Endless Tower: floor → zone/stage mapping; milestone rewards every 10, egg every 25.
- 3 dungeons × 5 tiers, daily keys (base 3, upgradeable).
- 4 expedition types (15m/1h/4h/8h), element-match + power bonuses, 2–4 slots.
- 4 habitats (Gold/Essence/Mutagen/Research), capacity & rate scale with level and assigned slime power/element.
- Research: 4 branches × 6 nodes (Battle, Breeding, Economy, Automation) with prerequisites.
- Mastery: per-element level 0–10 (+3% stats/level) from battle XP.
- Achievements: 60+ across collection/fusion/battle/tower/economy/prestige.

## Prestige
- **Rebirth** (clear Zone 6): reset zones + levels, keep collection/codex/research/talents. Gain Stardust = f(zones, tower, power). Permanent +2%/Stardust gold & essence; spend on 8 rebirth upgrades.
- **Ascension** (clear Zone 12 + Tower 50): keep only Celestial slimes + codex/perks. Gain Aether Cores; +10%/core all stats; 12 ascension perks incl. Aether element, automation unlock, +offline cap.

## Offline
- Cap 8h base → up to 48h (research/talents/perks).
- Idle clears/min from squadPower vs stagePower ratio; loot × multipliers.
- Habitats accumulate; eggs hatch; breed jobs complete; expeditions auto-claim.
- Welcome-back modal; optional rewarded ad doubles rewards.

## Automation
Auto-feed, auto-evolve, auto-progress, auto-fuse rules (max copies, rarity/prismatic/locked/squad protection), breeding queue (1–3), smart squad, loadout presets (3).

## Monetization (fair)
- Banner (adaptive, above bottom nav, never over UI/nav bar).
- Interstitial only at natural breaks (post-battle continue), ≥3 min apart, hidden with Remove Ads.
- Optional rewarded: double offline rewards.
- Remove Ads: non-consumable $4.99, restore via queryPurchases. No pay-to-win.