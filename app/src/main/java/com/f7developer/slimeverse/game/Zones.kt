package com.f7developer.slimeverse.game

data class ZoneDef(
    val index: Int,
    val name: String,
    val subtitle: String,
    val bgTop: Long,
    val bgBottom: Long,
    val accent: Long,
    val families: List<Family>,
    val elements: List<Element>,
)

object Zones {
    val all: List<ZoneDef> = listOf(
        ZoneDef(0, "Dewdrop Meadow", "Where every breeder begins", 0xFFBDE8C8, 0xFFEAF7DC, 0xFF66BB6A, listOf(Family.BLOB, Family.SHROOM, Family.FIN), listOf(Element.BLOOM, Element.AQUA, Element.TERRA)),
        ZoneDef(1, "Cinder Hollows", "Embers never sleep", 0xFFFFC9A8, 0xFFFFE8D6, 0xFFFF7043, listOf(Family.FANG, Family.WISP, Family.BLOB), listOf(Element.EMBER, Element.UMBRA)),
        ZoneDef(2, "Coral Shallows", "Tides hide old genes", 0xFFA8DCF5, 0xFFE3F6FD, 0xFF29B6F6, listOf(Family.FIN, Family.SHARD, Family.WING), listOf(Element.AQUA, Element.FROST)),
        ZoneDef(3, "Stormcrag Peaks", "Lightning in the stone", 0xFFC5CAE9, 0xFFE8EAF6, 0xFFFFCA28, listOf(Family.WING, Family.MECH, Family.MANTIS), listOf(Element.VOLT, Element.GALE)),
        ZoneDef(4, "Rimebound Tundra", "Silence, frozen bright", 0xFFC8E6F5, 0xFFEFF8FD, 0xFF81D4FA, listOf(Family.SHARD, Family.GOLEM, Family.WISP), listOf(Element.FROST, Element.GALE)),
        ZoneDef(5, "Verdant Wilds", "The forest breathes", 0xFFB9DFB0, 0xFFE9F6E2, 0xFF66BB6A, listOf(Family.SHROOM, Family.MANTIS, Family.ROYAL), listOf(Element.BLOOM, Element.TERRA)),
        ZoneDef(6, "Duskwater Marsh", "Shadows drink the light", 0xFFC3B1E1, 0xFFEDE7F6, 0xFF7E57C2, listOf(Family.WISP, Family.PHANTOM, Family.FANG), listOf(Element.UMBRA, Element.AQUA)),
        ZoneDef(7, "Radiant Spire", "Dawn carved in glass", 0xFFFFE9A8, 0xFFFFF8E1, 0xFFFFD54F, listOf(Family.ROYAL, Family.SERAPH, Family.RUNE), listOf(Element.LUMEN, Element.VOLT)),
        ZoneDef(8, "Crystal Depths", "Pressure makes monarchs", 0xFFB2DFDB, 0xFFE0F2F1, 0xFF26C6DA, listOf(Family.SHARD, Family.GOLEM, Family.TITAN), listOf(Element.TERRA, Element.FROST, Element.AQUA)),
        ZoneDef(9, "Void Rift", "Where genes unravel", 0xFFB39DDB, 0xFFE1BEE7, 0xFFE040FB, listOf(Family.PHANTOM, Family.CHIMERA, Family.WISP), listOf(Element.UMBRA, Element.AETHER)),
        ZoneDef(10, "Celestial Garden", "Starlight takes root", 0xFFF8BBD0, 0xFFFCE4EC, 0xFFEC407A, listOf(Family.SERAPH, Family.WING, Family.ROYAL), listOf(Element.LUMEN, Element.AETHER, Element.BLOOM)),
        ZoneDef(11, "Aether Throne", "The end is a beginning", 0xFFE1BEE7, 0xFFF3E5F5, 0xFFE040FB, listOf(Family.TITAN, Family.CHIMERA, Family.SERAPH, Family.PHANTOM), listOf(Element.AETHER)),
    )

    const val STAGES = 10

    fun get(index: Int): ZoneDef = all[index.coerceIn(0, all.lastIndex)]
    fun isBoss(stage: Int): Boolean = stage == STAGES

    data class EnemySpec(
        val speciesId: String,
        val element: Element,
        val rarity: Rarity,
        val boss: Boolean,
        val level: Int,
        val name: String,
    )

    /** Deterministic enemy team for a stage. */
    fun enemies(zone: Int, stage: Int): List<EnemySpec> {
        val z = get(zone)
        val rng = kotlin.random.Random(zone * 1000L + stage * 7L + 13)
        val boss = isBoss(stage)
        val size = when {
            boss -> 3
            zone >= 8 -> 5
            zone >= 4 -> 4
            else -> 3
        }
        return (0 until size).map { i ->
            val fam = if (boss && i == 0) z.families.last() else z.families[rng.nextInt(z.families.size)]
            val el = z.elements[rng.nextInt(z.elements.size)]
            val rarity = when {
                boss && i == 0 -> Rarity.of((2 + zone / 3).coerceAtMost(6))
                zone >= 9 -> Rarity.roll(rng, 2)
                zone >= 5 -> Rarity.roll(rng, 1)
                else -> Rarity.roll(rng, 0)
            }
            val id = "${fam.name.lowercase()}.${el.name.lowercase()}"
            val tier = if (boss && i == 0) 2 else if (zone >= 7 && rng.nextDouble() < 0.4) 1 else 0
            val def = Species.form(if (tier == 0) id else "$id.t$tier" + "a")
            EnemySpec(id, el, rarity, boss && i == 0, Balance.enemyLevel(zone, stage), def.name)
        }
    }

    fun teamSize(zone: Int, stage: Int): Int = enemies(zone, stage).size
}