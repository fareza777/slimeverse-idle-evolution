package com.f7developer.slimeverse.game

/** The ten elemental affinities. Aether is the prestige-tier element. */
enum class Element(val displayName: String, val color: Long, val glow: Long, val particle: ParticleStyle) {
    AQUA("Aqua", 0xFF29B6F6, 0xFF81D4FA, ParticleStyle.BUBBLE),
    EMBER("Ember", 0xFFFF7043, 0xFFFFAB91, ParticleStyle.EMBER),
    TERRA("Terra", 0xFFA9825F, 0xFFD7B899, ParticleStyle.LEAF),
    VOLT("Volt", 0xFFFFCA28, 0xFFFFE082, ParticleStyle.SPARK),
    GALE("Gale", 0xFF4DD0E1, 0xFFB2EBF2, ParticleStyle.SWIRL),
    FROST("Frost", 0xFF81D4FA, 0xFFE1F5FE, ParticleStyle.SNOW),
    BLOOM("Bloom", 0xFF66BB6A, 0xFFA5D6A7, ParticleStyle.LEAF),
    UMBRA("Umbra", 0xFF7E57C2, 0xFFB39DDB, ParticleStyle.WISP),
    LUMEN("Lumen", 0xFFFFD54F, 0xFFFFECB3, ParticleStyle.STAR),
    AETHER("Aether", 0xFFE040FB, 0xFFF8BBD0, ParticleStyle.STAR);

    val short: String get() = displayName.take(3)
}

enum class ParticleStyle { BUBBLE, EMBER, LEAF, SPARK, SWIRL, SNOW, WISP, STAR }

object Elements {
    /** Attacker element -> defenders it is strong against (1.5x). Inverse is 0.75x. */
    private val strongVs: Map<Element, List<Element>> = mapOf(
        Element.AQUA to listOf(Element.EMBER, Element.TERRA),
        Element.EMBER to listOf(Element.BLOOM, Element.FROST),
        Element.BLOOM to listOf(Element.AQUA, Element.GALE),
        Element.TERRA to listOf(Element.VOLT, Element.EMBER),
        Element.VOLT to listOf(Element.AQUA, Element.GALE),
        Element.GALE to listOf(Element.BLOOM, Element.TERRA),
        Element.FROST to listOf(Element.GALE, Element.BLOOM),
        Element.UMBRA to listOf(Element.LUMEN, Element.FROST),
        Element.LUMEN to listOf(Element.UMBRA, Element.VOLT),
        Element.AETHER to listOf(Element.UMBRA, Element.LUMEN),
    )

    fun multiplier(attacker: Element, defender: Element): Double {
        if (attacker == Element.AETHER) return if (defender == Element.AETHER) 1.0 else 1.25
        if (defender == Element.AETHER) return 1.0
        strongVs[attacker]?.let { if (defender in it) return 1.5 }
        strongVs[defender]?.let { if (attacker in it) return 0.75 }
        return 1.0
    }

    fun affinityLabel(attacker: Element, defender: Element): String = when (multiplier(attacker, defender)) {
        1.5 -> "Strong"
        0.75 -> "Weak"
        else -> "Neutral"
    }

    val all: List<Element> = Element.entries.toList()
    val baseNine: List<Element> = all.filter { it != Element.AETHER }
}