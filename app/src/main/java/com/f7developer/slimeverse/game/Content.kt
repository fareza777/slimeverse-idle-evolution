package com.f7developer.slimeverse.game

// ---------------- Habitats ----------------
enum class HabitatKind(val displayName: String, val desc: String, val produces: String, val color: Long, val favored: List<Element>) {
    POND("Dew Pond", "Calm waters breed gold.", "Gold", 0xFF29B6F6, listOf(Element.AQUA, Element.FROST, Element.BLOOM)),
    GROVE("Sun Grove", "Photosynthesis yields essence.", "Essence", 0xFF66BB6A, listOf(Element.BLOOM, Element.TERRA, Element.LUMEN)),
    GROTTO("Gene Grotto", "Deep crystals distill mutagen.", "Mutagen", 0xFF7E57C2, listOf(Element.UMBRA, Element.FROST, Element.TERRA)),
    SKYNEST("Sky Nest", "High winds carry research and eggs.", "Research", 0xFF4DD0E1, listOf(Element.GALE, Element.VOLT, Element.LUMEN));

    val baseRatePerHour: Double get() = when (this) {
        POND -> 60.0
        GROVE -> 8.0
        GROTTO -> 1.2
        SKYNEST -> 2.0
    }
}

object Habitats {
    val all = HabitatKind.entries.toList()
    fun capacity(level: Int): Int = 3 + (level - 1) / 3
    fun upgradeCost(level: Int): Long = (300.0 * Math.pow(1.9, (level - 1).toDouble())).toLong()
    fun rateMult(level: Int): Double = 1.0 + (level - 1) * 0.25
    fun slimeRate(kind: HabitatKind, slime: Slime, level: Int, power: Long): Double {
        val match = if (slime.element in kind.favored) 1.5 else 1.0
        return kind.baseRatePerHour * rateMult(level) * match * (1.0 + power / 1500.0)
    }
}

// ---------------- Dungeons ----------------
enum class DungeonKind(val displayName: String, val desc: String, val reward: String, val color: Long) {
    ESSENCE_MINE("Essence Mine", "Crystallized essence veins.", "Essence", 0xFF26C6DA),
    GENE_VAULT("Gene Vault", "Sealed genomes and mutagen.", "Mutagen", 0xFFAB47BC),
    GOLD_RUSH("Gold Rush", "A buried treasury of gold.", "Gold", 0xFFFFB300);
}

object Dungeons {
    val all = DungeonKind.entries.toList()
    const val TIERS = 5
    const val DAILY_KEYS = 3
    fun tierZone(tier: Int): Int = tier * 2 + 1
    fun unlocked(tier: Int, highestZone: Int): Boolean = highestZone >= tierZone(tier) - 1
    fun reward(kind: DungeonKind, tier: Int): Long = when (kind) {
        DungeonKind.ESSENCE_MINE -> (150.0 * Math.pow(2.1, tier.toDouble())).toLong()
        DungeonKind.GENE_VAULT -> (12.0 * Math.pow(1.9, tier.toDouble())).toLong()
        DungeonKind.GOLD_RUSH -> (900.0 * Math.pow(2.2, tier.toDouble())).toLong()
    }
    fun bonusEggChance(tier: Int): Double = 0.15 + tier * 0.10
}

// ---------------- Expeditions ----------------
data class ExpeditionDef(
    val id: String,
    val name: String,
    val desc: String,
    val minutes: Int,
    val minPower: Long,
    val gold: Long,
    val essence: Long,
    val mutagen: Long,
    val research: Long,
    val eggChance: Double,
    val favored: List<Element>,
)

object Expeditions {
    val all = listOf(
        ExpeditionDef("scout", "Meadow Scout", "A quick sweep of the shallows.", 15, 100, 120, 8, 0, 0, 0.05, listOf(Element.BLOOM, Element.AQUA)),
        ExpeditionDef("forage", "Essence Forage", "Gather crystallized essence.", 60, 400, 200, 45, 1, 0, 0.10, listOf(Element.TERRA, Element.BLOOM)),
        ExpeditionDef("deepdive", "Gene Deep-Dive", "Hunt rare alleles in the deep.", 240, 1500, 600, 90, 6, 2, 0.30, listOf(Element.UMBRA, Element.FROST)),
        ExpeditionDef("grandtour", "Grand Tour", "A long march for knowledge.", 480, 5000, 2200, 260, 10, 12, 0.50, listOf(Element.GALE, Element.LUMEN, Element.AETHER)),
    )
    fun get(id: String): ExpeditionDef = all.first { it.id == id }
    fun matchBonus(elements: List<Element>, favored: List<Element>): Double =
        1.0 + 0.25 * elements.count { it in favored }
}

// ---------------- Research ----------------
enum class ResearchBranch(val displayName: String, val color: Long) {
    BATTLE("Battle", 0xFFFF7043),
    BREEDING("Breeding", 0xFFEC407A),
    ECONOMY("Economy", 0xFFFFB300),
    AUTOMATION("Automation", 0xFF26C6DA),
}

data class ResearchNode(
    val id: String,
    val branch: ResearchBranch,
    val tier: Int,
    val name: String,
    val desc: String,
    val rpCost: Int,
    val goldCost: Long,
    val requires: String? = null,
)

object Research {
    val all: List<ResearchNode> = listOf(
        ResearchNode("b1", ResearchBranch.BATTLE, 1, "Sharp Claws", "+5% squad ATK.", 5, 300),
        ResearchNode("b2", ResearchBranch.BATTLE, 2, "Thick Hide", "+5% squad DEF.", 8, 800, "b1"),
        ResearchNode("b3", ResearchBranch.BATTLE, 3, "Quickstep", "+5% squad SPD.", 12, 2000, "b2"),
        ResearchNode("b4", ResearchBranch.BATTLE, 4, "Critical Eye", "+3% squad crit.", 18, 5000, "b3"),
        ResearchNode("b5", ResearchBranch.BATTLE, 5, "Battle Speed x2", "Unlock 2x battle speed.", 25, 12000, "b4"),
        ResearchNode("b6", ResearchBranch.BATTLE, 6, "Battle Speed x3", "Unlock 3x battle speed.", 40, 30000, "b5"),
        ResearchNode("r1", ResearchBranch.BREEDING, 1, "Fertile Grounds", "-10% breeding cost.", 5, 300),
        ResearchNode("r2", ResearchBranch.BREEDING, 2, "Mutation Lab", "+5% mutation chance.", 8, 800, "r1"),
        ResearchNode("r3", ResearchBranch.BREEDING, 3, "Gene Sequencer", "Preview inheritance odds.", 12, 2000, "r2"),
        ResearchNode("r4", ResearchBranch.BREEDING, 4, "Twin Ovum", "15% chance of twin eggs.", 18, 5000, "r3"),
        ResearchNode("r5", ResearchBranch.BREEDING, 5, "Breeding Queue +1", "Queue a second breed.", 25, 12000, "r4"),
        ResearchNode("r6", ResearchBranch.BREEDING, 6, "Breeding Queue +2", "Queue a third breed.", 40, 30000, "r5"),
        ResearchNode("e1", ResearchBranch.ECONOMY, 1, "Golden Touch", "+10% gold earned.", 5, 300),
        ResearchNode("e2", ResearchBranch.ECONOMY, 2, "Essence Filter", "+10% essence earned.", 8, 800, "e1"),
        ResearchNode("e3", ResearchBranch.ECONOMY, 3, "Hatchery Warmth", "-20% hatch time.", 12, 2000, "e2"),
        ResearchNode("e4", ResearchBranch.ECONOMY, 4, "Offline Vault", "+2h offline cap.", 18, 5000, "e3"),
        ResearchNode("e5", ResearchBranch.ECONOMY, 5, "Lucky Charm", "+10% battle loot.", 25, 12000, "e4"),
        ResearchNode("e6", ResearchBranch.ECONOMY, 6, "Market Access", "Release slimes for essence.", 40, 30000, "e5"),
        ResearchNode("a1", ResearchBranch.AUTOMATION, 1, "Auto-Feed", "Auto-feed the weakest squad slime.", 8, 500),
        ResearchNode("a2", ResearchBranch.AUTOMATION, 2, "Auto-Progress", "Advance stages automatically.", 12, 1500, "a1"),
        ResearchNode("a3", ResearchBranch.AUTOMATION, 3, "Auto-Evolve", "Evolve when affordable.", 18, 4000, "a2"),
        ResearchNode("a4", ResearchBranch.AUTOMATION, 4, "Auto-Fuse Rules", "Rule-based duplicate fusion.", 25, 10000, "a3"),
        ResearchNode("a5", ResearchBranch.AUTOMATION, 5, "Expedition Slot +1", "Run a third expedition.", 32, 20000, "a4"),
        ResearchNode("a6", ResearchBranch.AUTOMATION, 6, "Smart Squad", "One-tap optimal squad.", 45, 40000, "a5"),
    )
    private val byId = all.associateBy { it.id }
    fun get(id: String): ResearchNode = byId.getValue(id)
}

// ---------------- Talents ----------------
data class TalentNode(val id: String, val name: String, val desc: String, val maxLevel: Int, val costPerLevel: Int)

object BreederTalents {
    val all = listOf(
        TalentNode("t_gold", "Prosperity", "+4% gold per level.", 10, 1),
        TalentNode("t_essence", "Distillation", "+4% essence per level.", 10, 1),
        TalentNode("t_xp", "Mentorship", "+4% XP per level.", 10, 1),
        TalentNode("t_mut", "Gene Spark", "+2% mutation per level.", 5, 2),
        TalentNode("t_hatch", "Warm Hands", "-4% hatch time per level.", 5, 1),
        TalentNode("t_offline", "Night Shift", "+1h offline cap per level.", 8, 2),
        TalentNode("t_critdmg", "Lethality", "+5% crit damage per level.", 5, 2),
        TalentNode("t_skill", "Arcana", "+3% skill power per level.", 8, 2),
        TalentNode("t_heal", "Mending", "+4% healing per level.", 5, 1),
        TalentNode("t_loot", "Scavenger", "+3% loot per level.", 8, 1),
        TalentNode("t_breed", "Midwife", "-3% breed cost per level.", 5, 1),
        TalentNode("t_fusion", "Alchemist", "+2% fusion rarity odds per level.", 5, 2),
    )
    fun get(id: String): TalentNode = all.first { it.id == id }
    fun totalCost(id: String, level: Int): Int = get(id).costPerLevel * level
}

object Mastery {
    const val MAX = 10
    fun xpToLevel(level: Int): Long = (60.0 * Math.pow(1.5, level.toDouble())).toLong()
    fun statBonus(level: Int): Double = level * 0.03
}

// ---------------- Achievements ----------------
enum class StatKey {
    SLIMES_OWNED, SPECIES_DISCOVERED, FUSIONS, BREEDS, EVOLUTIONS, BATTLES_WON, BOSSES_SLAIN,
    TOWER_FLOOR, DUNGEONS_CLEARED, EXPEDITIONS_DONE, GOLD_EARNED, ESSENCE_EARNED, MUTATIONS,
    PRISMATIC_FOUND, LEGENDARY_OWNED, MYTHIC_OWNED, CELESTIAL_OWNED, REBIRTHS, ASCENSIONS,
    RESEARCH_DONE, TALENTS_LEARNED, EGGS_HATCHED, HYBRIDS_FOUND, FULL_CODEX_TIER, MAX_LEVEL_SLIME,
    PERFECT_GENOME, SQUAD_POWER, ZONES_CLEARED, OFFLINE_CLAIMED, ADS_WATCHED, FEEDS, RELEASES,
}

data class AchievementDef(
    val id: String,
    val name: String,
    val desc: String,
    val key: StatKey,
    val threshold: Long,
    val rewardGold: Long = 0,
    val rewardEssence: Long = 0,
    val rewardMutagen: Long = 0,
    val rewardTalent: Int = 0,
    val rewardEgg: Boolean = false,
)

object Achievements {
    val all: List<AchievementDef> = listOf(
        AchievementDef("own5", "Full Hands", "Own 5 slimes.", StatKey.SLIMES_OWNED, 5, rewardGold = 200),
        AchievementDef("own15", "Crowded Pond", "Own 15 slimes.", StatKey.SLIMES_OWNED, 15, rewardGold = 800, rewardTalent = 1),
        AchievementDef("own40", "Slime Rancher", "Own 40 slimes.", StatKey.SLIMES_OWNED, 40, rewardGold = 3000, rewardEgg = true),
        AchievementDef("own80", "Slime Baron", "Own 80 slimes.", StatKey.SLIMES_OWNED, 80, rewardGold = 12000, rewardTalent = 2),
        AchievementDef("disc10", "Curious Breeder", "Discover 10 species.", StatKey.SPECIES_DISCOVERED, 10, rewardEssence = 60),
        AchievementDef("disc30", "Field Scholar", "Discover 30 species.", StatKey.SPECIES_DISCOVERED, 30, rewardEssence = 250, rewardTalent = 1),
        AchievementDef("disc60", "Taxonomist", "Discover 60 species.", StatKey.SPECIES_DISCOVERED, 60, rewardEssence = 900, rewardEgg = true),
        AchievementDef("disc120", "Master of Forms", "Discover 120 species.", StatKey.SPECIES_DISCOVERED, 120, rewardEssence = 3000, rewardTalent = 2),
        AchievementDef("fuse1", "First splice", "Perform a fusion.", StatKey.FUSIONS, 1, rewardGold = 150),
        AchievementDef("fuse10", "Lab Regular", "Perform 10 fusions.", StatKey.FUSIONS, 10, rewardMutagen = 5, rewardTalent = 1),
        AchievementDef("fuse50", "Gene Artist", "Perform 50 fusions.", StatKey.FUSIONS, 50, rewardMutagen = 20, rewardEgg = true),
        AchievementDef("fuse150", "Master Splicer", "Perform 150 fusions.", StatKey.FUSIONS, 150, rewardMutagen = 60, rewardTalent = 2),
        AchievementDef("breed1", "Matchmaker", "Breed an egg.", StatKey.BREEDS, 1, rewardGold = 150),
        AchievementDef("breed20", "Lineage Keeper", "Breed 20 eggs.", StatKey.BREEDS, 20, rewardMutagen = 8, rewardTalent = 1),
        AchievementDef("breed60", "Dynasty", "Breed 60 eggs.", StatKey.BREEDS, 60, rewardMutagen = 25, rewardEgg = true),
        AchievementDef("evo1", "Metamorphosis", "Evolve a slime.", StatKey.EVOLUTIONS, 1, rewardEssence = 50),
        AchievementDef("evo10", "Growth Spurt", "Evolve 10 slimes.", StatKey.EVOLUTIONS, 10, rewardEssence = 300, rewardTalent = 1),
        AchievementDef("evo30", "Apex Breeder", "Evolve 30 slimes.", StatKey.EVOLUTIONS, 30, rewardEssence = 1200, rewardEgg = true),
        AchievementDef("win1", "First Blood", "Win a battle.", StatKey.BATTLES_WON, 1, rewardGold = 100),
        AchievementDef("win50", "Skirmisher", "Win 50 battles.", StatKey.BATTLES_WON, 50, rewardGold = 1000, rewardTalent = 1),
        AchievementDef("win250", "Warband", "Win 250 battles.", StatKey.BATTLES_WON, 250, rewardGold = 5000, rewardEgg = true),
        AchievementDef("win1000", "Legion", "Win 1000 battles.", StatKey.BATTLES_WON, 1000, rewardGold = 25000, rewardTalent = 2),
        AchievementDef("boss1", "Giant Slayer", "Slay a boss.", StatKey.BOSSES_SLAIN, 1, rewardEssence = 80),
        AchievementDef("boss12", "Zone Breaker", "Slay 12 bosses.", StatKey.BOSSES_SLAIN, 12, rewardEssence = 600, rewardTalent = 1),
        AchievementDef("tower10", "Climber", "Reach tower floor 10.", StatKey.TOWER_FLOOR, 10, rewardGold = 800),
        AchievementDef("tower30", "Mountaineer", "Reach tower floor 30.", StatKey.TOWER_FLOOR, 30, rewardEssence = 400, rewardTalent = 1),
        AchievementDef("tower60", "Skybound", "Reach tower floor 60.", StatKey.TOWER_FLOOR, 60, rewardMutagen = 15, rewardEgg = true),
        AchievementDef("tower100", "Stratosphere", "Reach tower floor 100.", StatKey.TOWER_FLOOR, 100, rewardMutagen = 50, rewardTalent = 3),
        AchievementDef("dun5", "Delver", "Clear 5 dungeons.", StatKey.DUNGEONS_CLEARED, 5, rewardEssence = 200),
        AchievementDef("dun25", "Spelunker", "Clear 25 dungeons.", StatKey.DUNGEONS_CLEARED, 25, rewardMutagen = 10, rewardTalent = 1),
        AchievementDef("exp5", "Wanderer", "Finish 5 expeditions.", StatKey.EXPEDITIONS_DONE, 5, rewardGold = 600),
        AchievementDef("exp25", "Pathfinder", "Finish 25 expeditions.", StatKey.EXPEDITIONS_DONE, 25, rewardGold = 3000, rewardTalent = 1),
        AchievementDef("gold10k", "Pocket Change", "Earn 10,000 gold total.", StatKey.GOLD_EARNED, 10000, rewardEssence = 100),
        AchievementDef("gold1m", "Treasury", "Earn 1,000,000 gold total.", StatKey.GOLD_EARNED, 1000000, rewardGold = 50000, rewardTalent = 2),
        AchievementDef("mut5", "Spark of Change", "Trigger 5 mutations.", StatKey.MUTATIONS, 5, rewardMutagen = 3),
        AchievementDef("mut25", "Chaotic Genes", "Trigger 25 mutations.", StatKey.MUTATIONS, 25, rewardMutagen = 12, rewardTalent = 1),
        AchievementDef("prism1", "Rainbow Child", "Find a Prismatic slime.", StatKey.PRISMATIC_FOUND, 1, rewardMutagen = 10, rewardEgg = true),
        AchievementDef("legend1", "Living Legend", "Own a Legendary slime.", StatKey.LEGENDARY_OWNED, 1, rewardEssence = 300, rewardTalent = 1),
        AchievementDef("mythic1", "Myth Made Flesh", "Own a Mythic slime.", StatKey.MYTHIC_OWNED, 1, rewardMutagen = 20, rewardEgg = true),
        AchievementDef("celest1", "Star-Touched", "Own a Celestial slime.", StatKey.CELESTIAL_OWNED, 1, rewardMutagen = 50, rewardTalent = 3),
        AchievementDef("rebirth1", "Born Again", "Rebirth once.", StatKey.REBIRTHS, 1, rewardTalent = 2),
        AchievementDef("rebirth5", "Cycle of Ages", "Rebirth 5 times.", StatKey.REBIRTHS, 5, rewardTalent = 4),
        AchievementDef("ascend1", "Ascendant", "Ascend once.", StatKey.ASCENSIONS, 1, rewardTalent = 5),
        AchievementDef("research10", "Lab Coat", "Complete 10 research.", StatKey.RESEARCH_DONE, 10, rewardGold = 2000, rewardTalent = 1),
        AchievementDef("research24", "Omniscient", "Complete all research.", StatKey.RESEARCH_DONE, 24, rewardMutagen = 30, rewardTalent = 3),
        AchievementDef("hatch10", "Nursery", "Hatch 10 eggs.", StatKey.EGGS_HATCHED, 10, rewardGold = 1000),
        AchievementDef("hatch50", "Hatchery", "Hatch 50 eggs.", StatKey.EGGS_HATCHED, 50, rewardEssence = 500, rewardTalent = 1),
        AchievementDef("hybrid1", "Impossible Child", "Discover a hybrid family.", StatKey.HYBRIDS_FOUND, 1, rewardMutagen = 8, rewardTalent = 1),
        AchievementDef("hybrid4", "Four Crowns", "Discover all 4 hybrid families.", StatKey.HYBRIDS_FOUND, 4, rewardMutagen = 40, rewardEgg = true),
        AchievementDef("maxlvl", "Peak Form", "Raise a slime to level 100.", StatKey.MAX_LEVEL_SLIME, 1, rewardEssence = 2000, rewardTalent = 2),
        AchievementDef("zone6", "Halfway There", "Clear 6 zones.", StatKey.ZONES_CLEARED, 6, rewardEssence = 400, rewardTalent = 1),
        AchievementDef("zone12", "World Cleared", "Clear all 12 zones.", StatKey.ZONES_CLEARED, 12, rewardMutagen = 30, rewardTalent = 3),
        AchievementDef("feed50", "Well Fed", "Feed slimes 50 times.", StatKey.FEEDS, 50, rewardGold = 800),
        AchievementDef("power50k", "Heavyweight", "Reach 50,000 squad power.", StatKey.SQUAD_POWER, 50000, rewardEssence = 800, rewardTalent = 1),
        AchievementDef("power500k", "Colossus", "Reach 500,000 squad power.", StatKey.SQUAD_POWER, 500000, rewardMutagen = 25, rewardTalent = 2),
        AchievementDef("offline10", "Patient Breeder", "Claim offline rewards 10 times.", StatKey.OFFLINE_CLAIMED, 10, rewardGold = 1500),
        AchievementDef("release10", "Letting Go", "Release 10 slimes.", StatKey.RELEASES, 10, rewardEssence = 200),
    )
    private val byId = all.associateBy { it.id }
    fun get(id: String): AchievementDef = byId.getValue(id)
}