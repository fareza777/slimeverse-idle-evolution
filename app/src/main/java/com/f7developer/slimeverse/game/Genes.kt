package com.f7developer.slimeverse.game

/** Passive abilities granted by genes, species, evolution or talents. Battle hooks live in BattleSim. */
enum class Trait(val displayName: String, val desc: String) {
    REGENERATOR("Regenerator", "Heals 2% max HP each turn."),
    BERSERKER("Berserker", "+35% ATK while below 35% HP."),
    VAMPIRIC("Vampiric", "Heals for 18% of damage dealt."),
    THORNS("Thorns", "Reflects 12% of damage taken."),
    BULWARK("Bulwark", "Takes 20% less damage from skills."),
    SWIFT("Swift", "+15% SPD."),
    LUCKY("Lucky", "+20% loot from battles."),
    SCHOLAR("Scholar", "+25% XP from battles."),
    ELEMENTAL_MASTER("Elemental Master", "+25% skill power."),
    GIANT("Giant", "+20% HP, +10% ATK, -10% SPD."),
    TINY("Tiny", "+15% SPD, +8% crit, -12% HP."),
    PRISMATIC("Prismatic", "+10% all stats. Shimmers with rainbow light."),
    STURDY("Sturdy", "Immune to stun."),
    POISONOUS("Poisonous", "Attacks apply poison (3% max HP/turn, 3 turns)."),
    STATIC("Static", "20% chance to chain 40% damage to another enemy."),
    PHOTOSYNTHESIS("Photosynthesis", "Heals 3% max HP each turn."),
    PACK_LEADER("Pack Leader", "Squad gains +8% ATK while this slime lives."),
    GUARDIAN("Guardian", "Intercepts 30% of damage aimed at back-row allies."),
    ECHO("Echo", "20% chance to cast skills twice."),
    METABOLIC("Metabolic", "Feeding costs 30% less."),
    FERTILE("Fertile", "Breeding costs 30% less."),
    ADAPTIVE("Adaptive", "Takes 15% less damage from strong elements."),
    OVERCLOCK("Overclock", "+25% SPD for 2 turns after a kill."),
    ZEN("Zen", "Immune to debuffs."),
    GEMTOUCH("Gemtouch", "+25% essence from battles."),
    VOIDHEART("Voidheart", "+20% crit damage."),
    SOFTCORE("Softcore", "+15% healing received."),
}

/** Stat modifiers are multiplicative bonuses expressed as fractions (0.1 = +10%). */
data class StatMods(
    val hp: Double = 0.0,
    val atk: Double = 0.0,
    val def: Double = 0.0,
    val spd: Double = 0.0,
    val crit: Double = 0.0,
) {
    operator fun plus(o: StatMods) = StatMods(hp + o.hp, atk + o.atk, def + o.def, spd + o.spd, crit + o.crit)

    companion object {
        val ZERO = StatMods()
    }
}

enum class Locus(val displayName: String, val desc: String) {
    SIZE("Size", "Body mass. Affects HP, ATK and speed."),
    TEMPER("Temper", "Personality. Shifts offense, defense or speed."),
    HIDE("Hide", "Skin type. Adds defense and body texture."),
    AURA("Aura", "Elemental shimmer. Grants passive power."),
    EYES("Eyes", "Eye shape. Affects crit and focus."),
    CROWN("Crown", "Headgear. Grants role bonuses."),
    PATTERN("Pattern", "Body markings. Rare patterns empower skills."),
    CORE("Core", "Inner core. Defines sustain and economy bonuses."),
}

/**
 * One allele of a gene. Dominance decides which allele is expressed in a pair:
 * higher dominance wins; ties express the first (maternal) allele.
 */
data class Allele(
    val id: String,
    val locus: Locus,
    val displayName: String,
    val dominance: Int,
    val mods: StatMods = StatMods.ZERO,
    val trait: Trait? = null,
    val visual: String = "",
    val rare: Boolean = false,
)

object Genes {
    val alleles: List<Allele> = listOf(
        // SIZE
        Allele("size.giant", Locus.SIZE, "Giant", 2, StatMods(hp = 0.25, atk = 0.10, spd = -0.10), Trait.GIANT, "giant"),
        Allele("size.standard", Locus.SIZE, "Standard", 1, visual = "standard"),
        Allele("size.tiny", Locus.SIZE, "Tiny", 0, StatMods(hp = -0.15, spd = 0.20, crit = 0.10), Trait.TINY, "tiny"),
        // TEMPER
        Allele("temper.fierce", Locus.TEMPER, "Fierce", 2, StatMods(atk = 0.15, def = -0.05), visual = "fierce"),
        Allele("temper.calm", Locus.TEMPER, "Calm", 1, StatMods(def = 0.10, hp = 0.05), visual = "calm"),
        Allele("temper.playful", Locus.TEMPER, "Playful", 0, StatMods(spd = 0.10, crit = 0.05), visual = "playful"),
        // HIDE
        Allele("hide.smooth", Locus.HIDE, "Smooth", 1, visual = "smooth"),
        Allele("hide.spiky", Locus.HIDE, "Spiky", 2, StatMods(def = 0.10), Trait.THORNS, "spiky"),
        Allele("hide.crystal", Locus.HIDE, "Crystalline", 2, StatMods(def = 0.15, spd = -0.05), visual = "crystal"),
        Allele("hide.fuzzy", Locus.HIDE, "Fuzzy", 0, StatMods(hp = 0.10), visual = "fuzzy"),
        // AURA (None is recessive to any aura)
        Allele("aura.none", Locus.AURA, "Dormant", 0, visual = "none"),
        Allele("aura.ember", Locus.AURA, "Ember Aura", 1, StatMods(atk = 0.05), visual = "ember"),
        Allele("aura.frost", Locus.AURA, "Frost Aura", 1, StatMods(def = 0.05), visual = "frost"),
        Allele("aura.volt", Locus.AURA, "Volt Aura", 1, StatMods(spd = 0.05), visual = "volt"),
        Allele("aura.bloom", Locus.AURA, "Bloom Aura", 1, StatMods(hp = 0.05), Trait.PHOTOSYNTHESIS, "bloom"),
        Allele("aura.umbra", Locus.AURA, "Umbra Aura", 1, StatMods(crit = 0.05), visual = "umbra"),
        Allele("aura.lumen", Locus.AURA, "Lumen Aura", 1, StatMods(hp = 0.03, atk = 0.03, def = 0.03), visual = "lumen"),
        Allele("aura.prismatic", Locus.AURA, "Prismatic", 2, StatMods(hp = 0.10, atk = 0.10, def = 0.10, spd = 0.10, crit = 0.10), Trait.PRISMATIC, "prismatic", rare = true),
        // EYES
        Allele("eyes.round", Locus.EYES, "Round", 1, visual = "round"),
        Allele("eyes.sleepy", Locus.EYES, "Sleepy", 0, StatMods(def = 0.05), visual = "sleepy"),
        Allele("eyes.star", Locus.EYES, "Starlit", 1, StatMods(crit = 0.06), visual = "star", rare = true),
        Allele("eyes.cyclops", Locus.EYES, "Cyclops", 2, StatMods(atk = 0.08, crit = 0.04), visual = "cyclops", rare = true),
        Allele("eyes.visor", Locus.EYES, "Visor", 2, StatMods(def = 0.06, spd = 0.04), visual = "visor", rare = true),
        // CROWN
        Allele("crown.none", Locus.CROWN, "Bare", 0, visual = "none"),
        Allele("crown.horns", Locus.CROWN, "Horns", 2, StatMods(atk = 0.08), visual = "horns"),
        Allele("crown.halo", Locus.CROWN, "Halo", 2, StatMods(hp = 0.05, atk = 0.05, def = 0.05), visual = "halo", rare = true),
        Allele("crown.antennae", Locus.CROWN, "Antennae", 1, StatMods(spd = 0.07), visual = "antennae"),
        Allele("crown.cap", Locus.CROWN, "Royal Cap", 1, StatMods(def = 0.08), visual = "cap"),
        Allele("crown.leaves", Locus.CROWN, "Leaf Sprout", 1, StatMods(hp = 0.06), visual = "leaves"),
        // PATTERN
        Allele("pattern.plain", Locus.PATTERN, "Plain", 1, visual = "plain"),
        Allele("pattern.spots", Locus.PATTERN, "Spots", 1, StatMods(hp = 0.03), visual = "spots"),
        Allele("pattern.stripes", Locus.PATTERN, "Stripes", 1, StatMods(atk = 0.03), visual = "stripes"),
        Allele("pattern.galaxy", Locus.PATTERN, "Galaxy", 0, StatMods(crit = 0.08), visual = "galaxy", rare = true),
        Allele("pattern.rune", Locus.PATTERN, "Runescript", 0, StatMods(atk = 0.05), Trait.ELEMENTAL_MASTER, "rune", rare = true),
        // CORE
        Allele("core.soft", Locus.CORE, "Soft", 1, visual = "soft"),
        Allele("core.gem", Locus.CORE, "Gem", 2, trait = Trait.GEMTOUCH, visual = "gem"),
        Allele("core.metal", Locus.CORE, "Metal", 2, StatMods(def = 0.12), visual = "metal"),
        Allele("core.void", Locus.CORE, "Void", 0, StatMods(atk = 0.12, crit = 0.08, hp = -0.08), Trait.VOIDHEART, "void", rare = true),
        Allele("core.heart", Locus.CORE, "Heart", 0, StatMods(hp = 0.08), Trait.SOFTCORE, "heart", rare = true),
    )

    private val byId = alleles.associateBy { it.id }
    private val byLocus = alleles.groupBy { it.locus }

    fun allele(id: String): Allele = byId[id] ?: byId.getValue("size.standard")
    fun pool(locus: Locus): List<Allele> = byLocus.getValue(locus)

    fun randomAllele(locus: Locus, rng: kotlin.random.Random, rareBoost: Double = 0.0): Allele {
        val pool = pool(locus)
        val weights = pool.map { a ->
            var w = when {
                a.rare -> 0.06 + rareBoost
                a.dominance == 2 -> 0.22
                a.dominance == 1 -> 0.30
                else -> 0.24
            }
            if (locus == Locus.AURA && a.id == "aura.none") w = 0.45
            w
        }
        val total = weights.sum()
        var roll = rng.nextDouble() * total
        for (i in pool.indices) {
            roll -= weights[i]
            if (roll <= 0) return pool[i]
        }
        return pool.last()
    }
}

/** Eight loci, two alleles each (maternal first). Expressed phenotype = dominant allele per locus. */
@kotlinx.serialization.Serializable
data class Genome(val pairs: Map<Locus, Pair<String, String>>) {

    fun expressed(locus: Locus): Allele {
        val (a, b) = pairs.getValue(locus)
        val aa = Genes.allele(a)
        val bb = Genes.allele(b)
        return if (bb.dominance > aa.dominance) bb else aa
    }

    fun phenotype(): Map<Locus, Allele> = Locus.entries.associateWith { expressed(it) }

    fun statMods(): StatMods = phenotype().values.fold(StatMods.ZERO) { acc, a -> acc + a.mods }

    fun traits(): Set<Trait> = phenotype().values.mapNotNull { it.trait }.toSet()

    fun isPrismatic(): Boolean = expressed(Locus.AURA).id == "aura.prismatic"

    fun rarityScore(): Int = phenotype().values.count { it.rare }

    companion object {
        fun random(rng: kotlin.random.Random, rareBoost: Double = 0.0): Genome =
            Genome(Locus.entries.associateWith { l ->
                Genes.randomAllele(l, rng, rareBoost).id to Genes.randomAllele(l, rng, rareBoost).id
            })

        /** Mendelian inheritance: one random allele per locus from each parent, then mutation. */
        fun breed(mother: Genome, father: Genome, rng: kotlin.random.Random, mutationChance: Double): Genome {
            val pairs = Locus.entries.associateWith { l ->
                val m = mother.pairs.getValue(l)
                val f = father.pairs.getValue(l)
                var a = if (rng.nextBoolean()) m.first else m.second
                var b = if (rng.nextBoolean()) f.first else f.second
                if (rng.nextDouble() < mutationChance) a = Genes.randomAllele(l, rng, 0.10).id
                if (rng.nextDouble() < mutationChance * 0.6) b = Genes.randomAllele(l, rng, 0.10).id
                a to b
            }
            return Genome(pairs)
        }

        /** Fusion mixing: pick one allele per locus from either parent, higher mutation rate. */
        fun fuse(a: Genome, b: Genome, rng: kotlin.random.Random, mutationChance: Double): Genome {
            val pairs = Locus.entries.associateWith { l ->
                val pa = a.pairs.getValue(l)
                val pb = b.pairs.getValue(l)
                var x = if (rng.nextBoolean()) pa.first else pa.second
                var y = if (rng.nextBoolean()) pb.first else pb.second
                if (rng.nextDouble() < mutationChance) x = Genes.randomAllele(l, rng, 0.16).id
                if (rng.nextDouble() < mutationChance) y = Genes.randomAllele(l, rng, 0.16).id
                x to y
            }
            return Genome(pairs)
        }
    }
}