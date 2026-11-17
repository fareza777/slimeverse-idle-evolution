package com.f7developer.slimeverse.game

import kotlinx.serialization.Serializable

/** A living slime instance: species form + genome + progression. */
@Serializable
data class Slime(
    val uid: Long,
    val speciesId: String,
    val tier: Int = 0,
    val branch: String? = null,
    val element: Element,
    val secondary: Element? = null,
    val rarity: Rarity,
    val genome: Genome,
    var level: Int = 1,
    var xp: Long = 0,
    val bornAt: Long = 0,
    var nickname: String? = null,
    var habitat: Int = -1,
    var locked: Boolean = false,
    var fusionCount: Int = 0,
    val parents: List<Long> = emptyList(),
) {
    val formKey: String get() = if (tier == 0) speciesId else "$speciesId.t$tier${branch ?: "a"}"
    val def: SpeciesDef get() = Species.form(formKey)
    val displayName: String get() = nickname ?: def.name
    val traits: Set<Trait> get() = genome.traits()
    val prismatic: Boolean get() = genome.isPrismatic()
}

object Balance {
    const val MAX_LEVEL = 100
    const val EVOLVE_1_LEVEL = 20
    const val EVOLVE_2_LEVEL = 45

    fun levelGrowth(level: Int): Double {
        val l = (level - 1).toDouble()
        return 1.0 + 0.14 * l + 0.0025 * l * l
    }

    fun xpToNext(level: Int): Long = (40.0 * Math.pow(level.toDouble(), 1.6)).toLong()

    fun traitMods(traits: Set<Trait>): StatMods {
        var m = StatMods.ZERO
        if (Trait.SWIFT in traits) m += StatMods(spd = 0.15)
        if (Trait.GIANT in traits) m += StatMods(hp = 0.20, atk = 0.10, spd = -0.10)
        if (Trait.TINY in traits) m += StatMods(spd = 0.15, crit = 0.08, hp = -0.12)
        if (Trait.PRISMATIC in traits) m += StatMods(hp = 0.10, atk = 0.10, def = 0.10, spd = 0.10, crit = 0.10)
        return m
    }

    /** Final computed stats. ascensionMult and talentMods come from global progression. */
    fun statsOf(
        slime: Slime,
        ascensionMult: Double = 1.0,
        talentMods: StatMods = StatMods.ZERO,
        masteryMod: Double = 0.0,
    ): BaseStats {
        val def = slime.def
        val g = slime.genome.statMods() + traitMods(slime.traits) + talentMods
        val growth = levelGrowth(slime.level)
        val rm = slime.rarity.mult
        val am = ascensionMult * (1.0 + masteryMod)
        return BaseStats(
            hp = def.base.hp * growth * rm * (1 + g.hp) * am,
            atk = def.base.atk * growth * rm * (1 + g.atk) * am,
            def = def.base.def * growth * rm * (1 + g.def) * am,
            spd = def.base.spd * growth * rm * (1 + g.spd) * am,
            crit = (def.base.crit + g.crit * 100).coerceIn(0.0, 80.0),
        )
    }

    fun power(s: BaseStats): Long =
        (s.hp * 0.35 + s.atk * 3.2 + s.def * 2.4 + s.spd * 1.8 + s.crit * 5.0).toLong()

    fun powerOf(slime: Slime, ascensionMult: Double = 1.0, talentMods: StatMods = StatMods.ZERO, masteryMod: Double = 0.0): Long =
        power(statsOf(slime, ascensionMult, talentMods, masteryMod))

    /** Defense mitigation: 120/(120+def). */
    fun mitigation(def: Double): Double = 120.0 / (120.0 + def)

    fun damage(atk: Double, def: Double, skillMult: Double, elementMult: Double, variance: Double, crit: Boolean, critDmg: Double = 1.5): Double {
        val base = atk * skillMult * mitigation(def) * elementMult * variance
        return base * (if (crit) critDmg else 1.0)
    }

    // ---- Economy ----
    fun feedCost(level: Int): Long = (25.0 * Math.pow(level.toDouble(), 1.45)).toLong()
    fun feedXp(level: Int): Long = (xpToNext(level) * 0.18).toLong() + 10
    fun evolveEssence(tier: Int, rarity: Rarity): Long = when (tier) {
        0 -> (120 * rarity.mult).toLong()
        else -> (600 * rarity.mult).toLong()
    }
    fun evolveGold(tier: Int, rarity: Rarity): Long = when (tier) {
        0 -> (400 * rarity.mult).toLong()
        else -> (2500 * rarity.mult).toLong()
    }
    fun fusionGold(a: Slime, b: Slime): Long = (150.0 * (a.rarity.mult + b.rarity.mult) * (1 + (a.level + b.level) / 40.0)).toLong()
    fun breedGold(a: Slime, b: Slime): Long = (100.0 * (a.rarity.mult + b.rarity.mult)).toLong()
    fun breedEssence(a: Slime, b: Slime): Long = (30.0 * (a.rarity.mult + b.rarity.mult)).toLong()
    fun hatchMinutes(rarity: Rarity): Int = 5 + rarity.order * 12

    // ---- Enemy & zone scaling ----
    fun enemyLevel(zone: Int, stage: Int): Int = (zone * 10 + stage).coerceAtLeast(1)

    fun enemyStats(zone: Int, stage: Int, boss: Boolean, family: Family, element: Element, rarity: Rarity): BaseStats {
        val lvl = enemyLevel(zone, stage)
        val spread = family.spread
        val base = BaseStats(
            110.0 * (1 + spread.hp), 13.0 * (1 + spread.atk), 9.0 * (1 + spread.def),
            10.0 * (1 + spread.spd), 5.0 + spread.crit * 100,
        )
        val growth = levelGrowth(lvl)
        val zoneMult = 1.0 + zone * 0.10
        val bossMult = if (boss) 3.4 else 1.0
        val rm = rarity.mult
        return BaseStats(
            base.hp * growth * zoneMult * bossMult * rm,
            base.atk * growth * zoneMult * (if (boss) 1.5 else 1.0) * rm,
            base.def * growth * zoneMult * (if (boss) 1.4 else 1.0) * rm,
            base.spd * growth * (if (boss) 1.1 else 1.0),
            base.crit + zone * 0.8,
        )
    }

    fun stageGold(zone: Int, stage: Int, boss: Boolean): Long {
        val base = 18.0 * Math.pow(1.32, zone.toDouble()) * (1 + stage * 0.12)
        return (base * (if (boss) 6.0 else 1.0)).toLong()
    }

    fun stageEssence(zone: Int, stage: Int, boss: Boolean): Long {
        val base = 3.0 * Math.pow(1.28, zone.toDouble()) * (1 + stage * 0.10)
        return (base * (if (boss) 5.0 else 1.0)).toLong().coerceAtLeast(1)
    }

    fun stageXp(zone: Int, stage: Int, boss: Boolean): Long {
        val base = 14.0 * Math.pow(1.22, zone.toDouble()) * (1 + stage * 0.10)
        return (base * (if (boss) 4.0 else 1.0)).toLong()
    }

    /** Expected idle clears per minute at a stage the squad can beat comfortably. */
    fun idleClearsPerMinute(squadPower: Long, stagePower: Long): Double {
        if (stagePower <= 0) return 0.0
        val ratio = squadPower.toDouble() / stagePower
        return when {
            ratio >= 2.0 -> 5.0
            ratio >= 1.3 -> 3.5
            ratio >= 1.0 -> 2.2
            ratio >= 0.8 -> 1.0
            else -> 0.0
        }
    }

    fun stagePower(zone: Int, stage: Int, boss: Boolean, teamSize: Int): Long {
        val s = enemyStats(zone, stage, boss, Family.BLOB, Element.AQUA, Rarity.COMMON)
        return power(s) * teamSize
    }

    // ---- Prestige ----
    fun stardustGain(highestZone: Int, towerFloor: Int, totalPower: Long): Long {
        val z = maxOf(0, highestZone - 4)
        return (z * z * 8L + towerFloor * 2L + totalPower / 5000L).coerceAtLeast(0)
    }

    fun stardustMult(stardust: Long): Double = 1.0 + stardust * 0.02

    fun aetherGain(highestZone: Int, towerFloor: Int, rebirths: Int): Long {
        if (highestZone < 11) return 0
        return ((highestZone - 10) * 3L + towerFloor / 25L + rebirths).coerceAtLeast(1)
    }

    fun aetherMult(cores: Long): Double = 1.0 + cores * 0.10

    fun offlineCapHours(researchLevel: Int, ascensionPerks: Int): Int =
        (8 + researchLevel * 2 + ascensionPerks * 2).coerceAtMost(48)
}