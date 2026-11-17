package com.f7developer.slimeverse.game

import kotlin.random.Random

enum class StatusType { BLEED, POISON, STUN, ATK_BUFF, DEF_BUFF, SPD_BUFF, REGEN, TAUNT, CRIT_BUFF }

data class Status(val type: StatusType, var turns: Int, val power: Double, val source: Int)

data class UnitVisual(
    val family: Family,
    val element: Element,
    val secondary: Element?,
    val tier: Int,
    val rarity: Rarity,
    val size: String,
    val temper: String,
    val hide: String,
    val aura: String,
    val eyes: String,
    val crown: String,
    val pattern: String,
    val core: String,
    val prismatic: Boolean,
    val seed: Int,
)

class BattleUnit(
    val index: Int,
    val name: String,
    val visual: UnitVisual,
    val stats: BaseStats,
    val traits: Set<Trait>,
    val skill: SkillDef,
    val isPlayer: Boolean,
    val row: Int, // 0 front, 1 back
    val slot: Int,
    val level: Int,
) {
    val maxHp: Double = stats.hp
    var hp: Double = stats.hp
    var shield: Double = 0.0
    var actionTimer: Double = 0.0
    var skillCooldown: Int = 1
    val statuses: MutableList<Status> = mutableListOf()
    var alive: Boolean = true
    var kills: Int = 0

    fun hasStatus(t: StatusType): Boolean = statuses.any { it.type == t }
    fun statusPower(t: StatusType): Double = statuses.filter { it.type == t }.sumOf { it.power }
    val atkNow: Double
        get() {
            var a = stats.atk * (1 + statusPower(StatusType.ATK_BUFF))
            if (Trait.BERSERKER in traits && hp < maxHp * 0.35) a *= 1.35
            return a
        }
    val defNow: Double get() = stats.def * (1 + statusPower(StatusType.DEF_BUFF))
    val spdNow: Double get() = stats.spd * (1 + statusPower(StatusType.SPD_BUFF))
    val critNow: Double get() = (stats.crit + statusPower(StatusType.CRIT_BUFF) * 100).coerceAtMost(85.0)
}

sealed class BattleEvent {
    data class Attack(val from: Int, val to: Int, val dmg: Double, val crit: Boolean, val element: Element, val killed: Boolean) : BattleEvent()
    data class SkillCast(val from: Int, val skillId: String, val targets: List<Int>, val amounts: List<Double>, val crits: List<Boolean>, val killed: List<Boolean>, val effect: String) : BattleEvent()
    data class Heal(val from: Int, val to: Int, val amount: Double) : BattleEvent()
    data class Shield(val from: Int, val to: Int, val amount: Double) : BattleEvent()
    data class StatusApplied(val to: Int, val type: StatusType, val turns: Int) : BattleEvent()
    data class Dot(val unit: Int, val dmg: Double, val type: StatusType) : BattleEvent()
    data class Death(val unit: Int) : BattleEvent()
    data class End(val win: Boolean) : BattleEvent()
}

enum class BattleResult { ONGOING, WIN, LOSE }

/**
 * Deterministic auto-battle. One tick = 0.1s of sim time.
 * Units act when their action timer (driven by SPD) reaches zero.
 */
class Battle(val units: List<BattleUnit>, seed: Long) {
    private val rng = Random(seed)
    var tick: Int = 0
        private set
    var result: BattleResult = BattleResult.ONGOING
        private set
    private val events = mutableListOf<BattleEvent>()

    val players: List<BattleUnit> = units.filter { it.isPlayer }
    val enemies: List<BattleUnit> = units.filter { !it.isPlayer }

    fun alivePlayers() = players.filter { it.alive }
    fun aliveEnemies() = enemies.filter { it.alive }

    private fun packLeaderBonus(unit: BattleUnit): Double =
        if (unit.isPlayer && players.any { it.alive && Trait.PACK_LEADER in it.traits }) 1.08 else 1.0

    private fun elementMult(attacker: BattleUnit, defender: BattleUnit): Double {
        val primary = Elements.multiplier(attacker.visual.element, defender.visual.element)
        val sec = defender.visual.secondary?.let { Elements.multiplier(attacker.visual.element, it) } ?: primary
        var m = minOf(primary, sec)
        if (m > 1.0 && Trait.ADAPTIVE in defender.traits) m = 1.0 + (m - 1.0) * 0.5
        return m
    }

    private fun pickTarget(attacker: BattleUnit, foes: List<BattleUnit>): BattleUnit? {
        val alive = foes.filter { it.alive }
        if (alive.isEmpty()) return null
        val taunted = alive.filter { it.hasStatus(StatusType.TAUNT) }
        val pool = taunted.ifEmpty { alive }
        val front = pool.filter { it.row == 0 }
        return (front.ifEmpty { pool }).minByOrNull { it.slot }
    }

    private fun applyDamage(target: BattleUnit, raw: Double, source: BattleUnit, isSkill: Boolean): Double {
        var dmg = raw
        if (isSkill && Trait.BULWARK in target.traits) dmg *= 0.8
        // Guardian intercept for back-row allies
        if (target.row == 1) {
            val guardian = (if (target.isPlayer) players else enemies).firstOrNull {
                it.alive && it.index != target.index && Trait.GUARDIAN in it.traits
            }
            if (guardian != null) {
                val share = dmg * 0.3
                dmg -= share
                absorb(guardian, share, source)
            }
        }
        absorb(target, dmg, source)
        if (Trait.THORNS in target.traits && source.alive && dmg > 0) {
            absorb(source, dmg * 0.12, target, thorns = true)
        }
        return dmg
    }

    private fun absorb(target: BattleUnit, dmg: Double, source: BattleUnit, thorns: Boolean = false) {
        var remaining = dmg
        if (target.shield > 0) {
            val blocked = minOf(target.shield, remaining)
            target.shield -= blocked
            remaining -= blocked
        }
        target.hp -= remaining
        if (!thorns && Trait.VAMPIRIC in source.traits && source.alive) {
            source.hp = minOf(source.maxHp, source.hp + remaining * 0.18)
        }
        if (target.hp <= 0 && target.alive) {
            target.alive = false
            target.hp = 0.0
            events.add(BattleEvent.Death(target.index))
            if (source.alive && source.index != target.index) {
                source.kills++
                if (Trait.OVERCLOCK in source.traits) {
                    source.statuses.add(Status(StatusType.SPD_BUFF, 2, 0.25, source.index))
                }
            }
        }
    }

    private fun rollHit(attacker: BattleUnit, defender: BattleUnit, power: Double, isSkill: Boolean, defIgnore: Boolean): Pair<Double, Boolean> {
        val crit = rng.nextDouble() * 100 < attacker.critNow
        val critDmg = 1.5 + if (Trait.VOIDHEART in attacker.traits) 0.2 else 0.0
        val def = if (defIgnore) 0.0 else defender.defNow
        val variance = 0.92 + rng.nextDouble() * 0.16
        val dmg = Balance.damage(
            attacker.atkNow * packLeaderBonus(attacker), def, power,
            elementMult(attacker, defender), variance, crit, critDmg,
        )
        return dmg to crit
    }

    private fun basicAttack(u: BattleUnit) {
        val foes = if (u.isPlayer) enemies else players
        val target = pickTarget(u, foes) ?: return
        val (dmg, crit) = rollHit(u, target, 1.0, false, false)
        val dealt = applyDamage(target, dmg, u, false)
        events.add(BattleEvent.Attack(u.index, target.index, dealt, crit, u.visual.element, !target.alive))
        if (Trait.POISONOUS in u.traits && target.alive && !immune(target)) {
            target.statuses.add(Status(StatusType.POISON, 3, 0.03, u.index))
            events.add(BattleEvent.StatusApplied(target.index, StatusType.POISON, 3))
        }
        if (Trait.STATIC in u.traits && rng.nextDouble() < 0.20) {
            val other = foes.filter { it.alive && it.index != target.index }.randomOrNull(rng)
            if (other != null) {
                val chain = applyDamage(other, dmg * 0.4, u, false)
                events.add(BattleEvent.Attack(u.index, other.index, chain, false, u.visual.element, !other.alive))
            }
        }
    }

    private fun immune(u: BattleUnit): Boolean = Trait.ZEN in u.traits

    private fun castSkill(u: BattleUnit) {
        val s = u.skill
        val foes = (if (u.isPlayer) enemies else players).filter { it.alive }
        val allies = (if (u.isPlayer) players else enemies).filter { it.alive }
        if (foes.isEmpty() && s.target !in listOf(SkillTarget.ALL_ALLIES, SkillTarget.LOWEST_ALLY, SkillTarget.SELF)) return
        val targets = mutableListOf<BattleUnit>()
        val amounts = mutableListOf<Double>()
        val crits = mutableListOf<Boolean>()
        val killed = mutableListOf<Boolean>()
        var effect = "damage"

        when (s.target) {
            SkillTarget.SINGLE_ENEMY, SkillTarget.FRONT_ENEMY -> pickTarget(u, foes)?.let { targets.add(it) }
            SkillTarget.ALL_ENEMIES -> targets.addAll(foes)
            SkillTarget.RANDOM_ENEMY_3 -> repeat(s.hits) { foes.randomOrNull(rng)?.let { targets.add(it) } }
            SkillTarget.LOWEST_ALLY -> allies.minByOrNull { it.hp / it.maxHp }?.let { targets.add(it) }
            SkillTarget.ALL_ALLIES -> targets.addAll(allies)
            SkillTarget.SELF -> targets.add(u)
        }

        val casts = if (Trait.ECHO in u.traits && rng.nextDouble() < 0.20) 2 else 1
        repeat(casts) { cast ->
            when {
                SkillEffect.HEAL in s.effects -> {
                    effect = "heal"
                    val healBase = u.atkNow * (if (s.statusPower > 0) s.statusPower else 0.25) * 2.2
                    for (t in targets) {
                        val amt = healBase * (if (Trait.SOFTCORE in t.traits) 1.15 else 1.0)
                        t.hp = minOf(t.maxHp, t.hp + amt)
                        amounts.add(amt); crits.add(false); killed.add(false)
                        events.add(BattleEvent.Heal(u.index, t.index, amt))
                    }
                }
                SkillEffect.SHIELD in s.effects && SkillEffect.DAMAGE !in s.effects -> {
                    effect = "shield"
                    for (t in targets) {
                        val amt = t.maxHp * s.statusPower
                        t.shield += amt
                        amounts.add(amt); crits.add(false); killed.add(false)
                        events.add(BattleEvent.Shield(u.index, t.index, amt))
                    }
                }
                else -> {
                    val perHit = if (s.target == SkillTarget.RANDOM_ENEMY_3) 1 else s.hits
                    for (t in targets) {
                        var total = 0.0
                        var anyCrit = false
                        repeat(perHit) {
                            val (dmg, crit) = rollHit(u, t, s.power, true, SkillEffect.DEF_IGNORE in s.effects)
                            total += applyDamage(t, dmg, u, true)
                            anyCrit = anyCrit || crit
                        }
                        amounts.add(total); crits.add(anyCrit); killed.add(!t.alive)
                        if (SkillEffect.LIFESTEAL in s.effects) {
                            u.hp = minOf(u.maxHp, u.hp + total * s.statusPower)
                        }
                    }
                }
            }
            if (cast == 0) applySkillStatuses(u, s, targets)
        }
        events.add(BattleEvent.SkillCast(u.index, s.id, targets.map { it.index }, amounts, crits, killed, effect))
        u.skillCooldown = s.cooldown
    }

    private fun applySkillStatuses(u: BattleUnit, s: SkillDef, targets: List<BattleUnit>) {
        val foes = (if (u.isPlayer) enemies else players)
        for (e in s.effects) {
            when (e) {
                SkillEffect.BLEED, SkillEffect.POISON -> for (t in targets) if (t.alive && !immune(t)) {
                    t.statuses.add(Status(if (e == SkillEffect.BLEED) StatusType.BLEED else StatusType.POISON, s.statusTurns, s.statusPower, u.index))
                    events.add(BattleEvent.StatusApplied(t.index, if (e == SkillEffect.BLEED) StatusType.BLEED else StatusType.POISON, s.statusTurns))
                }
                SkillEffect.STUN -> for (t in targets) if (t.alive && !immune(t) && Trait.STURDY !in t.traits && rng.nextDouble() < s.statusPower) {
                    t.statuses.add(Status(StatusType.STUN, s.statusTurns, 0.0, u.index))
                    events.add(BattleEvent.StatusApplied(t.index, StatusType.STUN, s.statusTurns))
                }
                SkillEffect.ATK_BUFF -> for (t in targets) if (t.alive) {
                    t.statuses.add(Status(StatusType.ATK_BUFF, s.statusTurns, s.statusPower, u.index))
                }
                SkillEffect.DEF_BUFF -> for (t in targets) if (t.alive) {
                    t.statuses.add(Status(StatusType.DEF_BUFF, s.statusTurns, s.statusPower, u.index))
                }
                SkillEffect.SPD_BUFF -> for (t in targets) if (t.alive) {
                    t.statuses.add(Status(StatusType.SPD_BUFF, s.statusTurns, s.statusPower, u.index))
                }
                SkillEffect.CRIT_BUFF -> if (u.alive) u.statuses.add(Status(StatusType.CRIT_BUFF, s.statusTurns, s.statusPower, u.index))
                SkillEffect.TAUNT -> if (u.alive) u.statuses.add(Status(StatusType.TAUNT, s.statusTurns, 0.0, u.index))
                SkillEffect.CLEANSE -> for (t in targets) {
                    t.statuses.removeAll { it.type in listOf(StatusType.BLEED, StatusType.POISON, StatusType.STUN) }
                }
                else -> {}
            }
        }
        // Self buff for overdrive-style skills handled via ATK_BUFF on self when target self included
        if (SkillEffect.ATK_BUFF in s.effects && s.target == SkillTarget.SINGLE_ENEMY && u.alive) {
            u.statuses.add(Status(StatusType.ATK_BUFF, s.statusTurns, s.statusPower, u.index))
            u.statuses.add(Status(StatusType.SPD_BUFF, s.statusTurns, s.statusPower, u.index))
        }
    }

    private fun tickStatuses(u: BattleUnit) {
        val it = u.statuses.iterator()
        while (it.hasNext()) {
            val st = it.next()
            when (st.type) {
                StatusType.BLEED, StatusType.POISON -> {
                    val dmg = u.maxHp * st.power
                    u.hp -= dmg
                    events.add(BattleEvent.Dot(u.index, dmg, st.type))
                    if (u.hp <= 0 && u.alive) {
                        u.alive = false; u.hp = 0.0
                        events.add(BattleEvent.Death(u.index))
                    }
                }
                StatusType.REGEN -> {
                    val amt = u.maxHp * st.power
                    u.hp = minOf(u.maxHp, u.hp + amt)
                    events.add(BattleEvent.Heal(u.index, u.index, amt))
                }
                else -> {}
            }
            st.turns--
            if (st.turns <= 0) it.remove()
        }
        if (Trait.REGENERATOR in u.traits && u.alive) {
            val amt = u.maxHp * 0.02
            u.hp = minOf(u.maxHp, u.hp + amt)
        }
        if (Trait.PHOTOSYNTHESIS in u.traits && u.alive) {
            val amt = u.maxHp * 0.03
            u.hp = minOf(u.maxHp, u.hp + amt)
        }
    }

    /** Advance one tick; returns the events produced. */
    fun tick(): List<BattleEvent> {
        if (result != BattleResult.ONGOING) return emptyList()
        events.clear()
        tick++
        for (u in units) {
            if (!u.alive) continue
            u.actionTimer -= u.spdNow * 0.12
            if (u.actionTimer <= 0) {
                u.actionTimer = 10.0
                tickStatuses(u)
                if (!u.alive) continue
                if (u.hasStatus(StatusType.STUN)) continue
                if (u.skillCooldown <= 0) castSkill(u) else {
                    basicAttack(u)
                    u.skillCooldown--
                }
            }
        }
        if (enemies.none { it.alive }) {
            result = BattleResult.WIN
            events.add(BattleEvent.End(true))
        } else if (players.none { it.alive }) {
            result = BattleResult.LOSE
            events.add(BattleEvent.End(false))
        } else if (tick > 900) {
            result = BattleResult.LOSE
            events.add(BattleEvent.End(false))
        }
        return events.toList()
    }

    /** Fast-forward without event collection (used for tests / offline validation). */
    fun resolve(maxTicks: Int = 900): BattleResult {
        while (result == BattleResult.ONGOING && tick < maxTicks) tick()
        return result
    }
}

object BattleFactory {
    fun visualOf(slime: Slime, seed: Int): UnitVisual {
        val p = slime.genome.phenotype()
        return UnitVisual(
            slime.def.family, slime.element, slime.secondary, slime.tier, slime.rarity,
            p.getValue(Locus.SIZE).visual, p.getValue(Locus.TEMPER).visual, p.getValue(Locus.HIDE).visual,
            p.getValue(Locus.AURA).visual, p.getValue(Locus.EYES).visual, p.getValue(Locus.CROWN).visual,
            p.getValue(Locus.PATTERN).visual, p.getValue(Locus.CORE).visual, slime.prismatic, seed,
        )
    }

    fun enemyVisual(spec: Zones.EnemySpec, seed: Int): UnitVisual {
        val rng = Random(seed * 31L + 7)
        val g = Genome.random(rng)
        val p = g.phenotype()
        val def = Species.base(spec.speciesId)
        return UnitVisual(
            def.family, spec.element, null, if (spec.boss) 2 else 0, spec.rarity,
            p.getValue(Locus.SIZE).visual, p.getValue(Locus.TEMPER).visual, p.getValue(Locus.HIDE).visual,
            p.getValue(Locus.AURA).visual, p.getValue(Locus.EYES).visual, p.getValue(Locus.CROWN).visual,
            p.getValue(Locus.PATTERN).visual, p.getValue(Locus.CORE).visual, g.isPrismatic(), seed,
        )
    }

    fun playerUnit(slime: Slime, index: Int, row: Int, slot: Int, ascensionMult: Double, talentMods: StatMods, masteryMod: Double): BattleUnit {
        val stats = Balance.statsOf(slime, ascensionMult, talentMods, masteryMod)
        val skill = Skills.scaled(slime.def.skillId, slime.tier, slime.branch, Trait.ELEMENTAL_MASTER in slime.traits)
        return BattleUnit(index, slime.displayName, visualOf(slime, slime.uid.toInt()), stats, slime.traits, skill, true, row, slot, slime.level)
    }

    fun enemyUnit(spec: Zones.EnemySpec, index: Int, slot: Int, zone: Int, stage: Int): BattleUnit {
        val def = Species.base(spec.speciesId)
        val tier = if (spec.boss) 2 else 0
        val form = Species.form(if (tier == 0) spec.speciesId else "${spec.speciesId}.t${tier}a")
        val stats = Balance.enemyStats(zone, stage, spec.boss, form.family, spec.element, spec.rarity)
        val skill = Skills.scaled(form.skillId, tier, null, false)
        val row = if (slot == 0 && spec.boss) 0 else if (slot < 2) 0 else 1
        return BattleUnit(index, spec.name, enemyVisual(spec, zone * 100 + stage * 10 + slot), stats, emptySet(), skill, false, row, slot, spec.level)
    }

    fun stageBattle(
        squad: List<Pair<Slime, Int>>, // slime to slot (0..4)
        zone: Int,
        stage: Int,
        seed: Long,
        ascensionMult: Double = 1.0,
        talentMods: StatMods = StatMods.ZERO,
        mastery: Map<Element, Double> = emptyMap(),
    ): Battle {
        val units = mutableListOf<BattleUnit>()
        squad.forEachIndexed { i, (slime, slot) ->
            val row = if (slot < 2) 0 else 1
            units.add(playerUnit(slime, i, row, slot, ascensionMult, talentMods, mastery[slime.element] ?: 0.0))
        }
        val specs = Zones.enemies(zone, stage)
        specs.forEachIndexed { i, spec ->
            units.add(enemyUnit(spec, 100 + i, i, zone, stage))
        }
        units.forEach { it.actionTimer = Random(seed + it.index).nextDouble() * 6.0 }
        return Battle(units, seed)
    }
}