package com.f7developer.slimeverse.game

enum class SkillTarget { SINGLE_ENEMY, ALL_ENEMIES, RANDOM_ENEMY_3, LOWEST_ALLY, ALL_ALLIES, SELF, FRONT_ENEMY }

enum class SkillEffect { DAMAGE, HEAL, SHIELD, ATK_BUFF, DEF_BUFF, SPD_BUFF, BLEED, POISON, STUN, CLEANSE, LIFESTEAL, DEF_IGNORE, TAUNT, CRIT_BUFF }

data class SkillDef(
    val id: String,
    val name: String,
    val desc: String,
    val cooldown: Int,
    val power: Double,
    val target: SkillTarget,
    val effects: List<SkillEffect>,
    val hits: Int = 1,
    val statusTurns: Int = 2,
    val statusPower: Double = 0.0,
)

object Skills {
    val all: List<SkillDef> = listOf(
        SkillDef("bubble_burst", "Bubble Burst", "Bursts bubbles across all enemies.", 3, 0.85, SkillTarget.ALL_ENEMIES, listOf(SkillEffect.DAMAGE)),
        SkillDef("rend", "Rend", "Savages one enemy and causes bleeding.", 2, 1.7, SkillTarget.SINGLE_ENEMY, listOf(SkillEffect.DAMAGE, SkillEffect.BLEED), statusTurns = 3, statusPower = 0.04),
        SkillDef("crystal_ward", "Crystal Ward", "Shields the whole squad.", 4, 0.0, SkillTarget.ALL_ALLIES, listOf(SkillEffect.SHIELD), statusPower = 0.22),
        SkillDef("gale_slash", "Gale Slash", "Three lightning-fast slashes.", 2, 0.62, SkillTarget.SINGLE_ENEMY, listOf(SkillEffect.DAMAGE), hits = 3),
        SkillDef("royal_decree", "Royal Decree", "Heals and emboldens all allies.", 4, 0.0, SkillTarget.ALL_ALLIES, listOf(SkillEffect.HEAL, SkillEffect.ATK_BUFF), statusPower = 0.20, statusTurns = 2),
        SkillDef("overdrive", "Overdrive", "Self ATK/SPD surge, then a heavy strike.", 3, 1.4, SkillTarget.SINGLE_ENEMY, listOf(SkillEffect.DAMAGE, SkillEffect.ATK_BUFF, SkillEffect.SPD_BUFF), statusPower = 0.25, statusTurns = 2),
        SkillDef("soulflare", "Soulflare", "Spirit fire that pierces defense.", 2, 1.5, SkillTarget.SINGLE_ENEMY, listOf(SkillEffect.DAMAGE, SkillEffect.DEF_IGNORE)),
        SkillDef("ancient_bolt", "Ancient Bolt", "A runic bolt that may stun.", 3, 1.55, SkillTarget.SINGLE_ENEMY, listOf(SkillEffect.DAMAGE, SkillEffect.STUN), statusTurns = 1, statusPower = 0.35),
        SkillDef("spore_cloud", "Spore Cloud", "Poisons all enemies and regenerates self.", 3, 0.7, SkillTarget.ALL_ENEMIES, listOf(SkillEffect.DAMAGE, SkillEffect.POISON), statusTurns = 3, statusPower = 0.035),
        SkillDef("tidal_mend", "Tidal Mend", "Heals the weakest ally and cleanses.", 3, 0.0, SkillTarget.LOWEST_ALLY, listOf(SkillEffect.HEAL, SkillEffect.CLEANSE), statusPower = 0.30),
        SkillDef("blade_dance", "Blade Dance", "Two blades that sharpen crit.", 2, 0.95, SkillTarget.SINGLE_ENEMY, listOf(SkillEffect.DAMAGE, SkillEffect.CRIT_BUFF), hits = 2, statusPower = 0.15, statusTurns = 2),
        SkillDef("quake", "Quake", "Slams the ground, taunting foes.", 4, 0.9, SkillTarget.ALL_ENEMIES, listOf(SkillEffect.DAMAGE, SkillEffect.TAUNT), statusTurns = 2),
        SkillDef("sanctuary", "Sanctuary", "Great heal plus a squad shield.", 5, 0.0, SkillTarget.ALL_ALLIES, listOf(SkillEffect.HEAL, SkillEffect.SHIELD), statusPower = 0.28),
        SkillDef("triple_maw", "Triple Maw", "Three bites at random enemies.", 2, 0.8, SkillTarget.RANDOM_ENEMY_3, listOf(SkillEffect.DAMAGE), hits = 3),
        SkillDef("bulwark_slam", "Bulwark Slam", "AoE slam that fortifies the squad.", 4, 0.85, SkillTarget.ALL_ENEMIES, listOf(SkillEffect.DAMAGE, SkillEffect.DEF_BUFF), statusPower = 0.25, statusTurns = 2),
        SkillDef("void_rift", "Void Rift", "A rift that devours defense and drains life.", 3, 0.95, SkillTarget.ALL_ENEMIES, listOf(SkillEffect.DAMAGE, SkillEffect.DEF_IGNORE, SkillEffect.LIFESTEAL), statusPower = 0.25),
    )

    private val byId = all.associateBy { it.id }
    fun get(id: String): SkillDef = byId.getValue(id)

    /** Tier and branch scaling: tier multiplies power, branch b adds status potency. */
    fun scaled(id: String, tier: Int, branch: String?, elementalMaster: Boolean): SkillDef {
        val s = get(id)
        val tm = when (tier) { 1 -> 1.5; 2 -> 2.2; else -> 1.0 }
        val bm = if (branch == "b") 1.15 else 1.0
        val em = if (elementalMaster) 1.25 else 1.0
        return s.copy(
            power = s.power * tm * em,
            statusPower = s.statusPower * tm * bm,
            cooldown = if (tier == 2) maxOf(1, s.cooldown - 1) else s.cooldown,
        )
    }
}