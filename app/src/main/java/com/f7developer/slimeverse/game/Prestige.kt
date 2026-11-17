package com.f7developer.slimeverse.game

data class RebirthUpgrade(val id: String, val name: String, val desc: String, val maxLevel: Int, val baseCost: Long)

object RebirthUpgrades {
    val all = listOf(
        RebirthUpgrade("rb_gold", "Golden Memory", "+8% gold per level.", 10, 5),
        RebirthUpgrade("rb_essence", "Essence Memory", "+8% essence per level.", 10, 5),
        RebirthUpgrade("rb_xp", "Wisdom", "+8% XP per level.", 10, 5),
        RebirthUpgrade("rb_stats", "Ancestral Power", "+4% all stats per level.", 15, 8),
        RebirthUpgrade("rb_mut", "Unstable Genes", "+2% mutation per level.", 5, 10),
        RebirthUpgrade("rb_hatch", "Quickening", "-5% hatch time per level.", 5, 8),
        RebirthUpgrade("rb_egg", "Heirloom Egg", "Start each rebirth with a Rare+ egg per level.", 3, 20),
        RebirthUpgrade("rb_keys", "Dungeon Memory", "+1 daily dungeon key per level.", 2, 25),
    )
    fun get(id: String): RebirthUpgrade = all.first { it.id == id }
    fun cost(id: String, level: Int): Long = (get(id).baseCost * Math.pow(1.7, level.toDouble())).toLong()
    fun level(state: GameState, id: String): Int = state.rebirthUpgrades[id] ?: 0
}

data class AscensionPerk(val id: String, val name: String, val desc: String, val cost: Long)

object AscensionPerks {
    val all = listOf(
        AscensionPerk("ap_stats", "Aether Body", "+15% all stats, permanently.", 3),
        AscensionPerk("ap_aether", "Aether Bloodline", "Aether-element eggs appear in the wild.", 5),
        AscensionPerk("ap_offline", "Timeless", "+4h offline cap.", 4),
        AscensionPerk("ap_auto", "Living Lab", "All automation unlocked.", 6),
        AscensionPerk("ap_habitat", "Sanctuary", "Habitat output +50%.", 4),
        AscensionPerk("ap_mut", "Radiant Genes", "+10% mutation chance.", 5),
        AscensionPerk("ap_keys", "Keymaster", "+2 daily dungeon keys.", 4),
        AscensionPerk("ap_expedition", "Wayfarer", "+1 expedition slot.", 5),
        AscensionPerk("ap_talent", "Awakened Mind", "+5 talent points now and per ascension.", 3),
        AscensionPerk("ap_hatch", "Starlight Incubator", "-30% hatch time.", 4),
        AscensionPerk("ap_gold", "Midas Aura", "+25% gold.", 3),
        AscensionPerk("ap_start", "Head Start", "Begin ascensions at zone 3 with a leveled squad.", 6),
    )
    fun get(id: String): AscensionPerk = all.first { it.id == id }
    fun has(state: GameState, id: String): Boolean = id in state.ascensionPerks
}

object Prestige {
    fun ascensionMult(state: GameState): Double =
        Balance.aetherMult(state.lifetimeAether) * (if (AscensionPerks.has(state, "ap_stats")) 1.15 else 1.0)

    fun stardustMult(state: GameState): Double = Balance.stardustMult(state.lifetimeStardust)

    fun globalStatMods(state: GameState): StatMods {
        val rb = RebirthUpgrades.level(state, "rb_stats") * 0.04
        return StatMods(hp = rb, atk = rb, def = rb, spd = rb)
    }

    fun goldMult(state: GameState): Double {
        var m = stardustMult(state)
        m *= 1.0 + RebirthUpgrades.level(state, "rb_gold") * 0.08
        m *= 1.0 + (state.talents["t_gold"] ?: 0) * 0.04
        if ("e1" in state.research) m *= 1.10
        if (AscensionPerks.has(state, "ap_gold")) m *= 1.25
        return m
    }

    fun essenceMult(state: GameState): Double {
        var m = stardustMult(state)
        m *= 1.0 + RebirthUpgrades.level(state, "rb_essence") * 0.08
        m *= 1.0 + (state.talents["t_essence"] ?: 0) * 0.04
        if ("e2" in state.research) m *= 1.10
        return m
    }

    fun xpMult(state: GameState): Double {
        var m = 1.0 + RebirthUpgrades.level(state, "rb_xp") * 0.08
        m *= 1.0 + (state.talents["t_xp"] ?: 0) * 0.04
        return m
    }

    fun lootMult(state: GameState): Double {
        var m = 1.0 + (state.talents["t_loot"] ?: 0) * 0.03
        if ("e5" in state.research) m *= 1.10
        return m
    }

    fun mutationChance(state: GameState): Double {
        var m = 0.08
        if ("r2" in state.research) m += 0.05
        m += (state.talents["t_mut"] ?: 0) * 0.02
        m += RebirthUpgrades.level(state, "rb_mut") * 0.02
        if (AscensionPerks.has(state, "ap_mut")) m += 0.10
        return m.coerceAtMost(0.6)
    }

    fun hatchMult(state: GameState): Double {
        var m = 1.0
        if ("e3" in state.research) m *= 0.8
        m *= 1.0 - (state.talents["t_hatch"] ?: 0) * 0.04
        m *= 1.0 - RebirthUpgrades.level(state, "rb_hatch") * 0.05
        if (AscensionPerks.has(state, "ap_hatch")) m *= 0.7
        return m.coerceAtLeast(0.2)
    }

    fun offlineCapHours(state: GameState): Int {
        var h = 8
        if ("e4" in state.research) h += 2
        h += (state.talents["t_offline"] ?: 0)
        if (AscensionPerks.has(state, "ap_offline")) h += 4
        return h.coerceAtMost(48)
    }

    fun expeditionSlots(state: GameState): Int {
        var s = 2
        if ("a5" in state.research) s++
        if (AscensionPerks.has(state, "ap_expedition")) s++
        return s
    }

    fun breedQueueCap(state: GameState): Int {
        var c = 1
        if ("r5" in state.research) c++
        if ("r6" in state.research) c++
        return c
    }

    fun dailyKeys(state: GameState): Int {
        var k = Dungeons.DAILY_KEYS
        k += RebirthUpgrades.level(state, "rb_keys")
        if (AscensionPerks.has(state, "ap_keys")) k += 2
        return k
    }

    fun battleSpeedMax(state: GameState): Int = when {
        "b6" in state.research -> 3
        "b5" in state.research -> 2
        else -> 1
    }

    fun automationUnlocked(state: GameState): Boolean = AscensionPerks.has(state, "ap_auto")

    fun talentGoldMult(state: GameState) = goldMult(state)
    fun critDmgBonus(state: GameState): Double = (state.talents["t_critdmg"] ?: 0) * 0.05
    fun skillPowerBonus(state: GameState): Double = (state.talents["t_skill"] ?: 0) * 0.03
    fun healBonus(state: GameState): Double = (state.talents["t_heal"] ?: 0) * 0.04
    fun breedCostMult(state: GameState): Double {
        var m = 1.0
        if ("r1" in state.research) m *= 0.9
        m *= 1.0 - (state.talents["t_breed"] ?: 0) * 0.03
        return m
    }
    fun fusionRarityBonus(state: GameState): Double = (state.talents["t_fusion"] ?: 0) * 0.02

    fun researchStatMods(state: GameState): StatMods = StatMods(
        atk = if ("b1" in state.research) 0.05 else 0.0,
        def = if ("b2" in state.research) 0.05 else 0.0,
        spd = if ("b3" in state.research) 0.05 else 0.0,
        crit = if ("b4" in state.research) 0.03 else 0.0,
    )

    fun masteryOf(state: GameState, el: Element): Double =
        Mastery.statBonus(state.masteryLevel[el] ?: 0)
}