package com.f7developer.slimeverse.game

enum class Rarity(val displayName: String, val mult: Double, val color: Long, val order: Int) {
    COMMON("Common", 1.00, 0xFF90A4AE, 0),
    UNCOMMON("Uncommon", 1.15, 0xFF66BB6A, 1),
    RARE("Rare", 1.35, 0xFF42A5F5, 2),
    EPIC("Epic", 1.60, 0xFFAB47BC, 3),
    LEGENDARY("Legendary", 1.95, 0xFFFFA726, 4),
    MYTHIC("Mythic", 2.35, 0xFFEF5350, 5),
    CELESTIAL("Celestial", 2.85, 0xFF26C6DA, 6);

    companion object {
        fun of(order: Int): Rarity = entries[order.coerceIn(0, 6)]
        fun roll(rng: kotlin.random.Random, floor: Int, bonus: Double = 0.0): Rarity {
            val weights = doubleArrayOf(46.0, 27.0, 15.0, 7.5, 3.2, 1.0, 0.3)
            for (i in 0..6) weights[i] = if (i < floor) 0.0 else weights[i] * (1.0 + bonus * i)
            val total = weights.sum()
            var roll = rng.nextDouble() * total
            for (i in 0..6) {
                roll -= weights[i]
                if (roll <= 0) return of(i)
            }
            return CELESTIAL
        }
    }
}

enum class Family(
    val displayName: String,
    val tag: String,
    val hybrid: Boolean,
    val nouns: List<String>,
    val spread: StatMods,
    val skill: String,
    val rarityFloor: Int,
) {
    BLOB("Blob", "wild", false, listOf("Blobling", "Blobbus", "Blobzilla"), StatMods(hp = 0.05), "bubble_burst", 0),
    FANG("Fang", "might", false, listOf("Fanglet", "Fangor", "Fangrex"), StatMods(atk = 0.12, spd = 0.03), "rend", 1),
    SHARD("Shard", "guard", false, listOf("Shardling", "Shardius", "Shardmonarch"), StatMods(def = 0.15, hp = 0.05), "crystal_ward", 2),
    WING("Wing", "swift", false, listOf("Flutter", "Skywing", "Stormseraph"), StatMods(spd = 0.15, crit = 0.04), "gale_slash", 1),
    ROYAL("Royal", "mind", false, listOf("Page", "Regent", "Sovereign"), StatMods(hp = 0.12, def = 0.05), "royal_decree", 2),
    MECH("Mech", "tech", false, listOf("Coglet", "Mecha", "Omegaframe"), StatMods(def = 0.10, atk = 0.08), "overdrive", 1),
    WISP("Wisp", "spirit", false, listOf("Wisp", "Soulflame", "Spiritlord"), StatMods(crit = 0.08, atk = 0.06), "soulflare", 2),
    RUNE("Rune", "mind", false, listOf("Runeling", "Runecarver", "Runetitan"), StatMods(atk = 0.08, def = 0.08), "ancient_bolt", 2),
    SHROOM("Shroom", "wild", false, listOf("Sporeling", "Mycelia", "Sporelord"), StatMods(hp = 0.15), "spore_cloud", 0),
    FIN("Fin", "swift", false, listOf("Fry", "Tidefin", "Leviathan"), StatMods(spd = 0.10, hp = 0.06), "tidal_mend", 0),
    MANTIS("Mantis", "might", false, listOf("Scyther", "Blademantis", "Reapermantis"), StatMods(atk = 0.14, crit = 0.06), "blade_dance", 2),
    GOLEM("Golem", "guard", false, listOf("Pebble", "Boulder", "Monolith"), StatMods(hp = 0.20, def = 0.12, spd = -0.10), "quake", 2),
    SERAPH("Seraph", "mind", true, listOf("Cherub", "Seraph", "Archseraph"), StatMods(hp = 0.12, atk = 0.08, def = 0.08), "sanctuary", 3),
    CHIMERA("Chimera", "might", true, listOf("Cub", "Chimera", "Hydrachar"), StatMods(atk = 0.18, crit = 0.06), "triple_maw", 3),
    TITAN("Titan", "guard", true, listOf("Minor", "Titan", "Worldbearer"), StatMods(hp = 0.25, def = 0.18, spd = -0.08), "bulwark_slam", 3),
    PHANTOM("Phantom", "spirit", true, listOf("Shade", "Phantom", "Eclipse"), StatMods(crit = 0.12, atk = 0.12, spd = 0.06), "void_rift", 3);

    val base: Boolean get() = !hybrid
}

object Families {
    val all: List<Family> = Family.entries.toList()
    val baseTwelve: List<Family> = all.filter { it.base }

    private val tagMatrix: Map<Pair<String, String>, Family> = buildMap {
        fun put(a: String, b: String, f: Family) {
            put(a to b, f); put(b to a, f)
        }
        put("might", "might", Family.CHIMERA)
        put("guard", "guard", Family.TITAN)
        put("swift", "swift", Family.PHANTOM)
        put("mind", "mind", Family.SERAPH)
        put("spirit", "spirit", Family.PHANTOM)
        put("tech", "tech", Family.MECH)
        put("wild", "wild", Family.BLOB)
        put("might", "guard", Family.GOLEM)
        put("might", "swift", Family.MANTIS)
        put("might", "mind", Family.FANG)
        put("might", "spirit", Family.CHIMERA)
        put("might", "tech", Family.MECH)
        put("might", "wild", Family.FANG)
        put("guard", "swift", Family.SHARD)
        put("guard", "mind", Family.RUNE)
        put("guard", "spirit", Family.TITAN)
        put("guard", "tech", Family.MECH)
        put("guard", "wild", Family.SHROOM)
        put("swift", "mind", Family.WING)
        put("swift", "spirit", Family.PHANTOM)
        put("swift", "tech", Family.WING)
        put("swift", "wild", Family.FIN)
        put("mind", "spirit", Family.SERAPH)
        put("mind", "tech", Family.RUNE)
        put("mind", "wild", Family.ROYAL)
        put("spirit", "tech", Family.WISP)
        put("spirit", "wild", Family.WISP)
        put("tech", "wild", Family.MECH)
    }

    /** Result family of fusing two families. Same family = purity. Hybrids follow special rules. */
    fun fusionResult(a: Family, b: Family, rng: kotlin.random.Random): Family {
        if (a == b) return a
        if (a.hybrid && b.hybrid) return Family.CHIMERA
        if (a.hybrid) return if (rng.nextDouble() < 0.40) a else b
        if (b.hybrid) return if (rng.nextDouble() < 0.40) b else a
        return tagMatrix[a.tag to b.tag] ?: Family.BLOB
    }

    /** All unordered base-family pairs, for the fusion codex. */
    val fusionPairs: List<Pair<Family, Family>> = buildList {
        for (i in baseTwelve.indices) for (j in i until baseTwelve.size) add(baseTwelve[i] to baseTwelve[j])
    }
}

data class BaseStats(val hp: Double, val atk: Double, val def: Double, val spd: Double, val crit: Double) {
    operator fun times(m: Double) = BaseStats(hp * m, atk * m, def * m, spd * m, crit * m)
}

data class SpeciesDef(
    val id: String,
    val family: Family,
    val element: Element,
    val tier: Int,
    val branch: String?,
    val name: String,
    val base: BaseStats,
    val skillId: String,
    val rarityFloor: Int,
) {
    val formKey: String get() = if (tier == 0) id else "$id.t$tier${branch ?: "a"}"
}

object Species {
    private val elementPrefix = mapOf(
        Element.AQUA to "Coral", Element.EMBER to "Cinder", Element.TERRA to "Moss",
        Element.VOLT to "Spark", Element.GALE to "Zephy", Element.FROST to "Rime",
        Element.BLOOM to "Petal", Element.UMBRA to "Dusk", Element.LUMEN to "Dawn",
        Element.AETHER to "Astra",
    )

    private val elementSpread = mapOf(
        Element.AQUA to StatMods(hp = 0.04), Element.EMBER to StatMods(atk = 0.06),
        Element.TERRA to StatMods(def = 0.06), Element.VOLT to StatMods(spd = 0.06),
        Element.GALE to StatMods(spd = 0.04, crit = 0.02), Element.FROST to StatMods(def = 0.04, hp = 0.03),
        Element.BLOOM to StatMods(hp = 0.06), Element.UMBRA to StatMods(crit = 0.05),
        Element.LUMEN to StatMods(atk = 0.03, def = 0.03), Element.AETHER to StatMods(hp = 0.05, atk = 0.05, def = 0.05),
    )

    private const val BASE_HP = 110.0
    private const val BASE_ATK = 13.0
    private const val BASE_DEF = 9.0
    private const val BASE_SPD = 10.0
    private const val BASE_CRIT = 5.0

    private fun applySpread(base: BaseStats, m: StatMods) = BaseStats(
        base.hp * (1 + m.hp), base.atk * (1 + m.atk), base.def * (1 + m.def),
        base.spd * (1 + m.spd), base.crit + m.crit * 100,
    )

    val all: List<SpeciesDef> = buildList {
        for (fam in Family.entries) {
            for (el in Element.entries) {
                val spread = fam.spread + elementSpread.getValue(el)
                val base = applySpread(BaseStats(BASE_HP, BASE_ATK, BASE_DEF, BASE_SPD, BASE_CRIT), spread)
                val floor = fam.rarityFloor + if (el == Element.AETHER) 2 else 0
                val prefix = elementPrefix.getValue(el)
                val id = "${fam.name.lowercase()}.${el.name.lowercase()}"
                add(SpeciesDef(id, fam, el, 0, null, "$prefix ${fam.nouns[0]}", base, fam.skill, floor))
                for (branch in listOf("a", "b")) {
                    val b1 = if (branch == "a") BaseStats(base.hp * 1.75, base.atk * 1.55, base.def * 1.65, base.spd * 1.12, base.crit + 2)
                    else BaseStats(base.hp * 1.55, base.atk * 1.80, base.def * 1.45, base.spd * 1.20, base.crit + 4)
                    add(SpeciesDef(id, fam, el, 1, branch, "$prefix ${fam.nouns[1]}", b1, fam.skill, floor + 1))
                    val b2 = if (branch == "a") BaseStats(base.hp * 3.1, base.atk * 2.5, base.def * 2.9, base.spd * 1.25, base.crit + 4)
                    else BaseStats(base.hp * 2.6, base.atk * 3.2, base.def * 2.4, base.spd * 1.40, base.crit + 8)
                    add(SpeciesDef(id, fam, el, 2, branch, "$prefix ${fam.nouns[2]}", b2, fam.skill, floor + 2))
                }
            }
        }
    }

    private val byForm = all.associateBy { it.formKey }
    private val byId = all.groupBy { it.id }

    fun form(key: String): SpeciesDef = byForm[key] ?: byForm.getValue("blob.aqua")
    fun base(id: String): SpeciesDef = byId.getValue(id).first { it.tier == 0 }
    fun tiers(id: String): List<SpeciesDef> = byId.getValue(id)
    fun exists(id: String): Boolean = byId.containsKey(id)

    fun evolveOptions(def: SpeciesDef): List<SpeciesDef> = when (def.tier) {
        0 -> tiers(def.id).filter { it.tier == 1 }
        1 -> tiers(def.id).filter { it.tier == 2 && it.branch == def.branch }
        else -> emptyList()
    }

    val baseSpeciesCount: Int = Family.entries.size * Element.entries.size
    val totalForms: Int = all.size
}