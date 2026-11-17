package com.f7developer.slimeverse.game

import kotlin.random.Random

data class OfflineReport(
    val elapsedMs: Long,
    val cappedHours: Int,
    val gold: Long,
    val essence: Long,
    val mutagen: Long,
    val research: Long,
    val xp: Long,
    val clears: Long,
    val eggsHatched: Int,
    val expeditionsDone: Int,
    val habitatGold: Long,
    val habitatEssence: Long,
    val habitatMutagen: Long,
    val habitatResearch: Long,
) {
    val isEmpty: Boolean get() = gold + essence + mutagen + research + xp + habitatGold + habitatEssence + habitatMutagen + habitatResearch == 0L && eggsHatched == 0 && expeditionsDone == 0
}

data class LootResult(
    val gold: Long, val essence: Long, val xp: Long,
    val mutagen: Long = 0, val research: Long = 0,
    val egg: Egg? = null, val catalyst: Boolean = false,
)

/**
 * Central game orchestrator. Pure Kotlin (clock + rng injected) so the whole
 * economy and progression can be unit-tested on the JVM.
 */
class GameEngine(
    var state: GameState,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val rng: Random = Random(System.nanoTime()),
) {
    var revision: Long = 0
        private set

    private fun touch() { revision++ }

    // ---------- helpers ----------
    fun squadPower(): Long {
        val mods = Prestige.globalStatMods(state) + Prestige.researchStatMods(state)
        val am = Prestige.ascensionMult(state) * Prestige.stardustMult(state)
        return state.squadSlimes().sumOf {
            Balance.powerOf(it, am, mods, Prestige.masteryOf(state, it.element))
        }
    }

    fun slimePower(s: Slime): Long {
        val mods = Prestige.globalStatMods(state) + Prestige.researchStatMods(state)
        val am = Prestige.ascensionMult(state) * Prestige.stardustMult(state)
        return Balance.powerOf(s, am, mods, Prestige.masteryOf(state, s.element))
    }

    private fun gainGold(amount: Long) {
        val g = (amount * Prestige.goldMult(state)).toLong()
        state.resources.gold += g
        state.bump(StatKey.GOLD_EARNED, g)
    }

    private fun gainEssence(amount: Long) {
        val e = (amount * Prestige.essenceMult(state)).toLong()
        state.resources.essence += e
        state.bump(StatKey.ESSENCE_EARNED, e)
    }

    fun addBreederXp(amount: Long) {
        state.breederXp += amount
        var need = breederXpToNext(state.breederLevel)
        while (state.breederXp >= need) {
            state.breederXp -= need
            state.breederLevel++
            state.resources.talentPoints += 1
            need = breederXpToNext(state.breederLevel)
        }
    }

    private fun breederXpToNext(level: Int): Long = (100.0 * Math.pow(level.toDouble(), 1.5)).toLong()

    fun registerSlime(s: Slime) {
        state.slimes.add(s)
        state.discoverSpecies(s.speciesId)
        state.discover(s.formKey)
        state.bump(StatKey.SLIMES_OWNED, 0) // recomputed
        if (s.prismatic) state.bump(StatKey.PRISMATIC_FOUND)
        when (s.rarity) {
            Rarity.LEGENDARY -> state.bump(StatKey.LEGENDARY_OWNED)
            Rarity.MYTHIC -> state.bump(StatKey.MYTHIC_OWNED)
            Rarity.CELESTIAL -> state.bump(StatKey.CELESTIAL_OWNED)
            else -> {}
        }
        if (s.def.family.hybrid) state.bump(StatKey.HYBRIDS_FOUND)
        if (s.genome.rarityScore() >= 5) state.bump(StatKey.PERFECT_GENOME)
    }

    fun makeSlime(
        speciesId: String, element: Element, secondary: Element?, rarity: Rarity,
        genome: Genome, level: Int = 1, tier: Int = 0, branch: String? = null,
        parents: List<Long> = emptyList(), fusionCount: Int = 0,
    ): Slime {
        val uid = state.nextUid++
        val s = Slime(uid, speciesId, tier, branch, element, secondary, rarity, genome, level, 0, now(), parents = parents, fusionCount = fusionCount)
        registerSlime(s)
        return s
    }

    fun randomEgg(source: String, zoneBias: Int = state.farmZone, rarityFloor: Int = 0, rarityBonus: Double = 0.0): Egg {
        val z = Zones.get(zoneBias)
        val fam = z.families[rng.nextInt(z.families.size)]
        var el = z.elements[rng.nextInt(z.elements.size)]
        if (el == Element.AETHER && !AscensionPerks.has(state, "ap_aether")) el = Element.LUMEN
        val rarity = Rarity.roll(rng, rarityFloor + fam.rarityFloor / 2, rarityBonus)
        val genome = Genome.random(rng, if (rarity.order >= 3) 0.08 else 0.0)
        val id = "${fam.name.lowercase()}.${el.name.lowercase()}"
        val hatchMs = (Balance.hatchMinutes(rarity) * 60_000L * Prestige.hatchMult(state)).toLong()
        val uid = state.nextUid++
        return Egg(uid, id, el, null, rarity, genome, now() + hatchMs, now(), source)
    }

    // ---------- collection actions ----------
    fun hatchEgg(uid: Long, instant: Boolean = false): Slime? {
        val egg = state.eggs.firstOrNull { it.uid == uid } ?: return null
        if (!instant && egg.hatchAt > now()) return null
        state.eggs.removeAll { it.uid == uid }
        val s = makeSlime(egg.speciesId, egg.element, egg.secondary, egg.rarity, egg.genome)
        state.bump(StatKey.EGGS_HATCHED)
        touch()
        return s
    }

    fun hatchCost(egg: Egg): Long {
        val remaining = ((egg.hatchAt - now()) / 60_000L).coerceAtLeast(0)
        return (remaining * 8L * egg.rarity.mult).toLong()
    }

    fun feed(uid: Long): Boolean {
        val s = state.slime(uid) ?: return false
        if (s.level >= Balance.MAX_LEVEL) return false
        var cost = Balance.feedCost(s.level)
        if (Trait.METABOLIC in s.traits) cost = (cost * 0.7).toLong()
        if (state.resources.gold < cost) return false
        state.resources.gold -= cost
        grantXp(s, Balance.feedXp(s.level))
        state.bump(StatKey.FEEDS)
        touch()
        return true
    }

    fun grantXp(s: Slime, amount: Long) {
        var xp = (amount * Prestige.xpMult(state) * (if (Trait.SCHOLAR in s.traits) 1.25 else 1.0)).toLong()
        s.xp += xp
        var need = Balance.xpToNext(s.level)
        while (s.xp >= need && s.level < Balance.MAX_LEVEL) {
            s.xp -= need
            s.level++
            need = Balance.xpToNext(s.level)
        }
        if (s.level >= Balance.MAX_LEVEL) { s.xp = 0; state.bump(StatKey.MAX_LEVEL_SLIME) }
    }

    fun canEvolve(s: Slime): Boolean {
        val needLevel = if (s.tier == 0) Balance.EVOLVE_1_LEVEL else Balance.EVOLVE_2_LEVEL
        if (s.tier >= 2 || s.level < needLevel) return false
        return state.resources.essence >= Balance.evolveEssence(s.tier, s.rarity) &&
            state.resources.gold >= Balance.evolveGold(s.tier, s.rarity)
    }

    fun evolve(uid: Long, branch: String): Boolean {
        val s = state.slime(uid) ?: return false
        if (!canEvolve(s)) return false
        val options = Species.evolveOptions(s.def)
        val target = options.firstOrNull { it.branch == branch } ?: return false
        state.resources.essence -= Balance.evolveEssence(s.tier, s.rarity)
        state.resources.gold -= Balance.evolveGold(s.tier, s.rarity)
        val idx = state.slimes.indexOf(s)
        val evolved = s.copy(tier = target.tier, branch = target.branch, rarity = Rarity.of((s.rarity.order + 1).coerceAtMost(6)))
        state.slimes[idx] = evolved
        state.discover(evolved.formKey)
        state.bump(StatKey.EVOLUTIONS)
        if (evolved.rarity == Rarity.LEGENDARY) state.bump(StatKey.LEGENDARY_OWNED)
        if (evolved.rarity == Rarity.MYTHIC) state.bump(StatKey.MYTHIC_OWNED)
        if (evolved.rarity == Rarity.CELESTIAL) state.bump(StatKey.CELESTIAL_OWNED)
        touch()
        return true
    }

    fun release(uid: Long): Long {
        val s = state.slime(uid) ?: return 0
        if (s.uid in state.squad.filterNotNull()) return 0
        val refund = (40.0 * s.rarity.mult * (1 + s.level / 10.0)).toLong()
        state.slimes.removeAll { it.uid == uid }
        state.habitats.forEach { it.assigned.remove(uid) }
        state.expeditions.forEach { /* keep uids; expedition resolves with missing slimes gracefully */ }
        gainEssence(refund)
        state.bump(StatKey.RELEASES)
        touch()
        return refund
    }

    // ---------- fusion & breeding ----------
    fun fusionPreview(a: Slime, b: Slime): Pair<Family, Element> {
        // deterministic preview of most-likely family (matrix is deterministic except hybrid rules)
        val fam = Families.fusionResult(a.def.family, b.def.family, Random(0))
        return fam to a.element
    }

    fun fuse(uidA: Long, uidB: Long, useMutagen: Boolean = false): Slime? {
        val a = state.slime(uidA) ?: return null
        val b = state.slime(uidB) ?: return null
        if (a.uid == b.uid) return null
        val cost = Balance.fusionGold(a, b)
        if (state.resources.gold < cost) return null
        if (useMutagen && state.resources.mutagen < 2) return null
        state.resources.gold -= cost
        if (useMutagen) state.resources.mutagen -= 2

        val fam = Families.fusionResult(a.def.family, b.def.family, rng)
        val element = a.element
        val secondary = if (b.element != a.element) b.element else null
        val mutChance = Prestige.mutationChance(state) + if (useMutagen) 0.20 else 0.0
        val genome = Genome.fuse(a.genome, b.genome, rng, mutChance)
        val before = genome.rarityScore()
        val rarityBonus = Prestige.fusionRarityBonus(state) + (if (useMutagen) 0.3 else 0.0) + (if (a.def.family == b.def.family) 0.2 else 0.0)
        val rarity = Rarity.roll(rng, maxOf(a.rarity.order, b.rarity.order), rarityBonus)
        val level = maxOf(1, (a.level + b.level) / 4)
        val id = "${fam.name.lowercase()}.${element.name.lowercase()}"

        // remove parents
        state.slimes.removeAll { it.uid == uidA || it.uid == uidB }
        state.habitats.forEach { it.assigned.removeAll(listOf(uidA, uidB)) }
        for (i in state.squad.indices) if (state.squad[i] == uidA || state.squad[i] == uidB) state.squad[i] = null

        val child = makeSlime(id, element, secondary, rarity, genome, level, parents = listOf(uidA, uidB), fusionCount = a.fusionCount + b.fusionCount + 1)
        state.bump(StatKey.FUSIONS)
        if (genome.rarityScore() > before) state.bump(StatKey.MUTATIONS)
        state.codexFusions.add(fusionKey(a.def.family, b.def.family))
        if (fam.hybrid) state.bump(StatKey.HYBRIDS_FOUND)
        touch()
        return child
    }

    fun fusionKey(a: Family, b: Family): String =
        listOf(a.name, b.name).sorted().joinToString("+")

    fun breedCost(a: Slime, b: Slime): Pair<Long, Long> {
        var g = Balance.breedGold(a, b)
        var e = Balance.breedEssence(a, b)
        val m = Prestige.breedCostMult(state)
        if (Trait.FERTILE in a.traits || Trait.FERTILE in b.traits) { g = (g * 0.7).toLong(); e = (e * 0.7).toLong() }
        return (g * m).toLong() to (e * m).toLong()
    }

    fun breed(uidA: Long, uidB: Long): BreedJob? {
        val a = state.slime(uidA) ?: return null
        val b = state.slime(uidB) ?: return null
        if (a.uid == b.uid) return null
        if (state.breedJobs.size >= Prestige.breedQueueCap(state)) return null
        val (g, e) = breedCost(a, b)
        if (state.resources.gold < g || state.resources.essence < e) return null
        state.resources.gold -= g
        state.resources.essence -= e

        val mutChance = Prestige.mutationChance(state)
        val genome = Genome.breed(a.genome, b.genome, rng, mutChance)
        val fam = if (a.def.family != b.def.family && rng.nextDouble() < 0.25)
            Families.fusionResult(a.def.family, b.def.family, rng) else if (rng.nextBoolean()) a.def.family else b.def.family
        val element = if (rng.nextBoolean()) a.element else b.element
        val secondary = if (a.element != b.element) (if (element == a.element) b.element else a.element) else null
        val rarity = Rarity.roll(rng, maxOf(a.rarity.order, b.rarity.order) - 1, 0.1)
        val id = "${fam.name.lowercase()}.${element.name.lowercase()}"
        val hatchMs = (Balance.hatchMinutes(rarity) * 60_000L * Prestige.hatchMult(state)).toLong()
        val gestation = 120_000L
        val eggUid = state.nextUid++
        val egg = Egg(eggUid, id, element, secondary, rarity, genome, now() + gestation + hatchMs, now(), "breed")
        val job = BreedJob(state.nextUid++, uidA, uidB, now() + gestation, egg)
        state.breedJobs.add(job)
        state.bump(StatKey.BREEDS)
        if (genome.rarityScore() > maxOf(a.genome.rarityScore(), b.genome.rarityScore())) state.bump(StatKey.MUTATIONS)
        if ("r4" in state.research && rng.nextDouble() < 0.15) {
            val twin = egg.copy(uid = state.nextUid++, genome = Genome.breed(a.genome, b.genome, rng, mutChance))
            state.breedJobs.add(BreedJob(state.nextUid++, uidA, uidB, now() + gestation, twin))
        }
        touch()
        return job
    }

    fun mutate(uid: Long, locus: Locus): Boolean {
        val s = state.slime(uid) ?: return false
        if (state.resources.mutagen < 3) return false
        state.resources.mutagen -= 3
        val idx = state.slimes.indexOf(s)
        val pair = s.genome.pairs.getValue(locus)
        val newAllele = Genes.randomAllele(locus, rng, 0.25).id
        val newPairs = s.genome.pairs.toMutableMap()
        newPairs[locus] = newAllele to pair.second
        val mutated = s.copy(genome = Genome(newPairs))
        state.slimes[idx] = mutated
        state.bump(StatKey.MUTATIONS)
        if (mutated.prismatic) state.bump(StatKey.PRISMATIC_FOUND)
        touch()
        return true
    }

    // ---------- squad ----------
    fun setSquadSlot(slot: Int, uid: Long?) {
        if (slot !in 0..4) return
        if (uid != null) {
            for (i in state.squad.indices) if (state.squad[i] == uid) state.squad[i] = null
        }
        state.squad[slot] = uid
        touch()
    }

    fun savePreset(name: String) {
        state.presets.removeAll { it.name == name }
        state.presets.add(SquadPreset(name, state.squad.toList()))
        if (state.presets.size > 3) state.presets.removeAt(0)
        touch()
    }

    fun loadPreset(name: String) {
        val p = state.presets.firstOrNull { it.name == name } ?: return
        for (i in 0..4) state.squad[i] = p.slots.getOrElse(i) { null }?.takeIf { uid -> state.slime(uid) != null }
        touch()
    }

    fun smartSquad() {
        val ranked = state.slimes.sortedByDescending { slimePower(it) }
        for (i in 0..4) state.squad[i] = ranked.getOrNull(i)?.uid
        touch()
    }

    fun squadSynergy(): List<String> {
        val squad = state.squadSlimes()
        val out = mutableListOf<String>()
        val byElement = squad.groupBy { it.element }
        byElement.forEach { (el, list) -> if (list.size >= 3) out.add("${el.displayName} Resonance: +12% ${el.displayName} stats") }
        val byFamily = squad.groupBy { it.def.family }
        byFamily.forEach { (f, list) -> if (list.size >= 2) out.add("${f.displayName} Bond: +6% squad HP") }
        if (squad.any { Trait.PACK_LEADER in it.traits }) out.add("Pack Leader: +8% squad ATK")
        if (squad.count { it.row() == 0 } >= 2) out.add("Front Wall: front row takes -10% damage")
        return out
    }

    private fun Slime.row(): Int = state.squad.indexOf(uid).let { if (it in 0..1) 0 else 1 }

    fun synergyStatMods(): StatMods {
        val squad = state.squadSlimes()
        var m = StatMods.ZERO
        val byElement = squad.groupBy { it.element }
        byElement.forEach { (_, list) -> if (list.size >= 3) m += StatMods(hp = 0.04, atk = 0.04, def = 0.04) }
        val byFamily = squad.groupBy { it.def.family }
        byFamily.forEach { (_, list) -> if (list.size >= 2) m += StatMods(hp = 0.06) }
        return m
    }

    // ---------- habitats ----------
    fun assignHabitat(kind: Int, uid: Long) {
        val h = state.habitats[kind]
        val s = state.slime(uid) ?: return
        if (h.assigned.size >= Habitats.capacity(h.level)) return
        state.habitats.forEach { it.assigned.remove(uid) }
        h.assigned.add(uid)
        s.habitat = kind
        touch()
    }

    fun unassignHabitat(kind: Int, uid: Long) {
        state.habitats[kind].assigned.remove(uid)
        state.slime(uid)?.habitat = -1
        touch()
    }

    fun upgradeHabitat(kind: Int): Boolean {
        val h = state.habitats[kind]
        if (h.level >= 10) return false
        val cost = Habitats.upgradeCost(h.level)
        if (state.resources.gold < cost) return false
        state.resources.gold -= cost
        h.level++
        touch()
        return true
    }

    fun habitatRate(kind: Int): Double {
        val h = state.habitats[kind]
        val k = HabitatKind.entries[kind]
        var rate = h.assigned.sumOf { uid ->
            val s = state.slime(uid) ?: return@sumOf 0.0
            Habitats.slimeRate(k, s, h.level, slimePower(s))
        }
        if (AscensionPerks.has(state, "ap_habitat")) rate *= 1.5
        return rate
    }

    fun collectHabitat(kind: Int): Long {
        val h = state.habitats[kind]
        val amount = h.stored.toLong()
        if (amount <= 0) return 0
        h.stored -= amount
        when (HabitatKind.entries[kind]) {
            HabitatKind.POND -> gainGold(amount)
            HabitatKind.GROVE -> gainEssence(amount)
            HabitatKind.GROTTO -> { state.resources.mutagen += amount; }
            HabitatKind.SKYNEST -> { state.resources.research += amount; }
        }
        touch()
        return amount
    }

    // ---------- expeditions ----------
    fun startExpedition(defId: String, uids: List<Long>): Boolean {
        if (state.expeditions.size >= Prestige.expeditionSlots(state)) return false
        val def = Expeditions.get(defId)
        val slimes = uids.mapNotNull { state.slime(it) }
        if (slimes.isEmpty()) return false
        if (slimes.sumOf { slimePower(it) } < def.minPower) return false
        if (slimes.any { s -> state.expeditions.any { s.uid in it.slimeUids } }) return false
        val dur = def.minutes * 60_000L
        state.expeditions.add(Expedition(state.nextUid++, defId, uids, now(), now() + dur))
        touch()
        return true
    }

    fun claimExpedition(uid: Long): LootResult? {
        val exp = state.expeditions.firstOrNull { it.uid == uid } ?: return null
        if (exp.endsAt > now()) return null
        val def = Expeditions.get(exp.defId)
        val slimes = exp.slimeUids.mapNotNull { state.slime(it) }
        val match = Expeditions.matchBonus(slimes.map { it.element }, def.favored)
        val powerBonus = 1.0 + (slimes.sumOf { slimePower(it) } / (def.minPower * 4.0)).coerceAtMost(1.0)
        val m = match * powerBonus
        val gold = (def.gold * m).toLong()
        val essence = (def.essence * m).toLong()
        gainGold(gold)
        gainEssence(essence)
        state.resources.mutagen += (def.mutagen * m).toLong()
        state.resources.research += (def.research * m).toLong()
        var egg: Egg? = null
        if (rng.nextDouble() < def.eggChance * m) {
            egg = randomEgg("expedition", rarityFloor = 1)
            state.eggs.add(egg)
        }
        slimes.forEach { grantXp(it, def.minutes * 2L) }
        state.expeditions.removeAll { it.uid == uid }
        state.bump(StatKey.EXPEDITIONS_DONE)
        touch()
        return LootResult(gold, essence, 0, (def.mutagen * m).toLong(), (def.research * m).toLong(), egg)
    }

    // ---------- research / talents ----------
    fun doResearch(id: String): Boolean {
        if (id in state.research) return false
        val node = Research.get(id)
        if (node.requires != null && node.requires !in state.research) return false
        if (state.resources.research < node.rpCost || state.resources.gold < node.goldCost) return false
        state.resources.research -= node.rpCost
        state.resources.gold -= node.goldCost
        state.research.add(id)
        state.bump(StatKey.RESEARCH_DONE)
        touch()
        return true
    }

    fun learnTalent(id: String): Boolean {
        val node = BreederTalents.get(id)
        val level = state.talents[id] ?: 0
        if (level >= node.maxLevel) return false
        val cost = node.costPerLevel
        if (state.resources.talentPoints < cost) return false
        state.resources.talentPoints -= cost
        state.talents[id] = level + 1
        state.bump(StatKey.TALENTS_LEARNED)
        touch()
        return true
    }

    fun addMasteryXp(el: Element, amount: Long) {
        val cur = state.masteryXp[el] ?: 0L
        var lvl = state.masteryLevel[el] ?: 0
        var xp = cur + amount
        while (lvl < Mastery.MAX && xp >= Mastery.xpToLevel(lvl)) {
            xp -= Mastery.xpToLevel(lvl)
            lvl++
        }
        state.masteryXp[el] = xp
        state.masteryLevel[el] = lvl
    }

    // ---------- achievements ----------
    fun statValue(key: StatKey): Long = when (key) {
        StatKey.SLIMES_OWNED -> state.slimes.size.toLong()
        StatKey.SPECIES_DISCOVERED -> state.codexSpecies.count { !it.contains(".t") }.toLong()
        StatKey.SQUAD_POWER -> squadPower()
        StatKey.ZONES_CLEARED -> (state.highestZoneCleared() + 1).coerceAtLeast(0).toLong()
        StatKey.LEGENDARY_OWNED -> state.slimes.count { it.rarity.order >= 4 }.toLong()
        StatKey.MYTHIC_OWNED -> state.slimes.count { it.rarity.order >= 5 }.toLong()
        StatKey.CELESTIAL_OWNED -> state.slimes.count { it.rarity.order >= 6 }.toLong()
        StatKey.MAX_LEVEL_SLIME -> state.slimes.count { it.level >= Balance.MAX_LEVEL }.toLong()
        StatKey.HYBRIDS_FOUND -> state.slimes.map { it.def.family }.filter { it.hybrid }.distinct().size.toLong()
        StatKey.RESEARCH_DONE -> state.research.size.toLong()
        StatKey.TOWER_FLOOR -> state.towerBest.toLong()
        else -> state.stat(key)
    }

    fun claimableAchievements(): List<AchievementDef> =
        Achievements.all.filter { it.id !in state.achievementsClaimed && statValue(it.key) >= it.threshold }

    fun claimAchievement(id: String): Boolean {
        val def = Achievements.get(id)
        if (id in state.achievementsClaimed || statValue(def.key) < def.threshold) return false
        state.achievementsClaimed.add(id)
        gainGold(def.rewardGold)
        gainEssence(def.rewardEssence)
        state.resources.mutagen += def.rewardMutagen
        state.resources.talentPoints += def.rewardTalent
        if (def.rewardEgg) state.eggs.add(randomEgg("achievement", rarityFloor = 2))
        touch()
        return true
    }

    fun claimAllAchievements(): Int {
        val list = claimableAchievements()
        list.forEach { claimAchievement(it.id) }
        return list.size
    }

    // ---------- battle rewards ----------
    fun applyBattleWin(zone: Int, stage: Int, boss: Boolean, squad: List<Slime>, tower: Boolean = false, dungeon: DungeonKind? = null, tier: Int = 0): LootResult {
        val lucky = squad.any { Trait.LUCKY in it.traits }
        val gem = squad.any { Trait.GEMTOUCH in it.traits }
        val lootM = Prestige.lootMult(state) * (if (lucky) 1.2 else 1.0)
        val gold = (Balance.stageGold(zone, stage, boss) * lootM).toLong()
        val essence = (Balance.stageEssence(zone, stage, boss) * lootM * (if (gem) 1.25 else 1.0)).toLong()
        val xp = Balance.stageXp(zone, stage, boss)
        gainGold(gold)
        gainEssence(essence)
        squad.forEach { grantXp(it, xp); addMasteryXp(it.element, xp / 3 + 2) }
        addBreederXp(xp / 2 + 3)
        state.bump(StatKey.BATTLES_WON)
        if (boss) state.bump(StatKey.BOSSES_SLAIN)

        var mutagen = 0L
        var research = 0L
        var egg: Egg? = null
        var catalyst = false
        if (boss) {
            research += 2 + zone
            if (rng.nextDouble() < 0.5) catalyst = true
            if (rng.nextDouble() < 0.25) { egg = randomEgg("boss", zone, rarityFloor = 1); state.eggs.add(egg) }
        } else {
            if (rng.nextDouble() < 0.04 * lootM) { egg = randomEgg("wild", zone); state.eggs.add(egg) }
            if (rng.nextDouble() < 0.03 * lootM) mutagen += 1
        }
        if (dungeon != null) {
            val reward = Dungeons.reward(dungeon, tier)
            when (dungeon) {
                DungeonKind.ESSENCE_MINE -> gainEssence(reward)
                DungeonKind.GENE_VAULT -> { mutagen += reward; if (rng.nextDouble() < Dungeons.bonusEggChance(tier)) { egg = randomEgg("dungeon", rarityFloor = 2); state.eggs.add(egg) } }
                DungeonKind.GOLD_RUSH -> gainGold(reward)
            }
            research += 3 + tier * 2
            state.bump(StatKey.DUNGEONS_CLEARED)
            state.dungeonClears["${dungeon.name}:$tier"] = (state.dungeonClears["${dungeon.name}:$tier"] ?: 0) + 1
        }
        state.resources.mutagen += mutagen
        state.resources.research += research

        if (!tower && !boss && dungeon == null) {
            val cleared = state.maxStage(zone)
            if (stage > cleared) state.zoneProgress[zone] = stage
        }
        if (boss && dungeon == null && !tower) {
            state.zoneProgress[zone] = Zones.STAGES
            if (state.farmZone == zone && state.farmStage >= Zones.STAGES) {
                state.farmStage = 1
                if (zone < Zones.all.lastIndex) state.farmZone = zone + 1
            }
        }
        touch()
        return LootResult(gold, essence, xp, mutagen, research, egg, catalyst)
    }

    fun recordTowerWin(floor: Int) {
        state.towerFloor = floor
        if (floor > state.towerBest) state.towerBest = floor
        if (floor % 10 == 0) {
            gainGold(Balance.stageGold(0, 10, true) * (1 + floor / 10))
            gainEssence(Balance.stageEssence(0, 10, true) * (1 + floor / 10))
            state.resources.research += 2 + floor / 10
            if (floor % 25 == 0) state.eggs.add(randomEgg("tower", rarityFloor = 2))
        }
        touch()
    }

    // ---------- automation ----------
    fun autoFusePass(): Int {
        if (!state.autoFuse.enabled && !Prestige.automationUnlocked(state)) return 0
        if (!state.autoFuse.enabled) return 0
        val rules = state.autoFuse
        val groups = state.slimes.groupBy { it.speciesId }
        var fused = 0
        for ((_, group) in groups) {
            val candidates = group.filter {
                it.rarity.order < rules.keepRarityAtLeast &&
                    (!rules.keepPrismatic || !it.prismatic) &&
                    (!rules.keepLocked || !it.locked) &&
                    (!rules.keepSquad || it.uid !in state.squad.filterNotNull()) &&
                    it.habitat == -1
            }
            if (candidates.size > rules.maxCopies) {
                val sorted = candidates.sortedBy { slimePower(it) }
                val toFuse = sorted.take(sorted.size - rules.maxCopies)
                var i = 0
                while (i + 1 < toFuse.size) {
                    val child = fuse(toFuse[i].uid, toFuse[i + 1].uid)
                    if (child != null) fused++ else break
                    i += 2
                }
            }
        }
        return fused
    }

    fun autoFeedPass() {
        if (!state.settings.autoFeed && !Prestige.automationUnlocked(state)) return
        if (!state.settings.autoFeed) return
        val squad = state.squadSlimes().filter { it.level < Balance.MAX_LEVEL }.minByOrNull { it.level } ?: return
        feed(squad.uid)
    }

    fun autoEvolvePass() {
        if (!state.settings.autoEvolve && !Prestige.automationUnlocked(state)) return
        if (!state.settings.autoEvolve) return
        val s = state.squadSlimes().firstOrNull { canEvolve(it) } ?: return
        val branch = if ((state.slime(s.uid)?.level ?: 0) % 2 == 0) "a" else "b"
        evolve(s.uid, branch)
    }

    // ---------- economy tick ----------
    fun economyTick(dtMs: Long) {
        val t = now()
        val hours = dtMs / 3_600_000.0
        // habitats
        for (k in HabitatKind.entries.indices) {
            val h = state.habitats[k]
            val cap = habitatRate(k) * Prestige.offlineCapHours(state)
            h.stored = (h.stored + habitatRate(k) * hours).coerceAtMost(cap)
        }
        // breed jobs -> eggs
        val done = state.breedJobs.filter { it.completesAt <= t }
        done.forEach { state.eggs.add(it.egg) }
        state.breedJobs.removeAll { it.completesAt <= t }
        // hatch eggs
        val hatched = state.eggs.filter { it.hatchAt <= t }
        hatched.forEach { hatchEgg(it.uid) }
        // expeditions auto-claim
        val expDone = state.expeditions.filter { it.endsAt <= t }
        expDone.forEach { claimExpedition(it.uid) }
        // daily keys reset
        val day = t / 86_400_000L
        if (day != state.keysResetDay) {
            state.keysResetDay = day
            state.resources.keys = Prestige.dailyKeys(state)
            rollDailyChallenge(day)
        }
        touch()
    }

    fun rollDailyChallenge(day: Long) {
        val r = Random(day * 7919)
        val types = listOf("mono", "noheal", "speed", "fuse", "boss")
        val type = types[r.nextInt(types.size)]
        val target = when (type) {
            "mono" -> 3L
            "noheal" -> 3L
            "speed" -> 2L
            "fuse" -> 3L
            "boss" -> 1L
            else -> 3L
        }
        state.daily = DailyChallengeState(day, type, target, 0, false)
    }

    fun dailyChallengeDesc(): String = when (state.daily.type) {
        "mono" -> "Win ${state.daily.target} battles with a mono-element squad."
        "noheal" -> "Win ${state.daily.target} battles without a healer in the squad."
        "speed" -> "Win ${state.daily.target} battles in under 25 seconds."
        "fuse" -> "Perform ${state.daily.target} fusions today."
        "boss" -> "Slay a zone boss today."
        else -> ""
    }

    fun onBattleWonForDaily(squad: List<Slime>, ticks: Int, boss: Boolean) {
        val d = state.daily
        if (d.claimed) return
        val ok = when (d.type) {
            "mono" -> squad.map { it.element }.distinct().size == 1
            "noheal" -> squad.none { it.def.skillId in listOf("royal_decree", "tidal_mend", "sanctuary") }
            "speed" -> ticks < 250
            "boss" -> boss
            else -> false
        }
        if (ok) {
            d.progress++
            if (d.progress >= d.target) {
                d.claimed = true
                gainGold(1500)
                gainEssence(120)
                state.resources.mutagen += 3
            }
            touch()
        }
    }

    // ---------- offline ----------
    fun computeOffline(): OfflineReport {
        val t = now()
        val elapsed = (t - state.lastSeen).coerceAtLeast(0)
        val capMs = Prestige.offlineCapHours(state) * 3_600_000L
        val effective = elapsed.coerceAtMost(capMs)
        val minutes = effective / 60_000.0
        val hours = effective / 3_600_000.0

        val sp = squadPower()
        val stageP = Balance.stagePower(state.farmZone, state.farmStage, false, Zones.teamSize(state.farmZone, state.farmStage))
        val cpm = Balance.idleClearsPerMinute(sp, stageP)
        val clears = (cpm * minutes).toLong()
        val lootM = Prestige.lootMult(state)
        val gold = (clears * Balance.stageGold(state.farmZone, state.farmStage, false) * lootM).toLong()
        val essence = (clears * Balance.stageEssence(state.farmZone, state.farmStage, false) * lootM).toLong()
        val xp = clears * Balance.stageXp(state.farmZone, state.farmStage, false)

        var hg = 0L; var he = 0L; var hm = 0L; var hr = 0L
        for (k in HabitatKind.entries.indices) {
            val rate = habitatRate(k)
            val amt = (rate * hours).toLong()
            when (HabitatKind.entries[k]) {
                HabitatKind.POND -> hg += amt
                HabitatKind.GROVE -> he += amt
                HabitatKind.GROTTO -> hm += amt
                HabitatKind.SKYNEST -> hr += amt
            }
        }

        val eggsBefore = state.slimes.size
        // apply
        gainGold(gold)
        gainEssence(essence)
        state.squadSlimes().forEach { grantXp(it, xp) }
        state.resources.mutagen += hm
        state.resources.research += hr
        // habitats store + collect inline for offline
        for (k in HabitatKind.entries.indices) {
            state.habitats[k].stored = 0.0
        }
        gainGold(hg); gainEssence(he)
        // eggs & expeditions
        economyTick(effective)
        val eggsHatched = state.slimes.size - eggsBefore
        val expDone = 0 // already auto-claimed in economyTick

        state.lastSeen = t
        state.bump(StatKey.OFFLINE_CLAIMED)
        touch()
        return OfflineReport(effective, Prestige.offlineCapHours(state), gold, essence, 0, 0, xp, clears, eggsHatched.coerceAtLeast(0), expDone, hg, he, hm, hr)
    }

    // ---------- prestige ----------
    fun canRebirth(): Boolean = state.highestZoneCleared() >= 5
    fun rebirthGain(): Long = Balance.stardustGain(state.highestZoneCleared(), state.towerBest, squadPower())

    fun rebirth(): Long {
        if (!canRebirth()) return 0
        val gain = rebirthGain()
        state.resources.stardust += gain
        state.lifetimeStardust += gain
        state.rebirths++
        state.bump(StatKey.REBIRTHS)
        // reset progression, keep collection/codex/research/talents/perks
        state.farmZone = 0
        state.farmStage = 1
        state.zoneProgress.clear()
        state.towerFloor = 0
        state.resources.gold = 500
        state.resources.essence = 0
        state.slimes.forEach { it.level = 1; it.xp = 0 }
        val heirloom = RebirthUpgrades.level(state, "rb_egg")
        repeat(heirloom) { state.eggs.add(randomEgg("heirloom", rarityFloor = 2, rarityBonus = 0.3)) }
        state.lastSeen = now()
        touch()
        return gain
    }

    fun canAscend(): Boolean = state.highestZoneCleared() >= 11 && state.towerBest >= 50
    fun ascendGain(): Long = Balance.aetherGain(state.highestZoneCleared(), state.towerBest, state.rebirths)

    fun ascend(): Long {
        if (!canAscend()) return 0
        val gain = ascendGain()
        state.resources.aether += gain
        state.lifetimeAether += gain
        state.ascensions++
        state.bump(StatKey.ASCENSIONS)
        if (AscensionPerks.has(state, "ap_talent")) state.resources.talentPoints += 5
        // keep celestial slimes + codex + perks + talents + research; reset the rest
        val keep = state.slimes.filter { it.rarity == Rarity.CELESTIAL }
        state.slimes.clear()
        keep.forEach { it.level = 1; it.xp = 0; state.slimes.add(it) }
        state.squad = mutableListOf(null, null, null, null, null)
        keep.take(5).forEachIndexed { i, s -> state.squad[i] = s.uid }
        state.farmZone = if (AscensionPerks.has(state, "ap_start")) 3 else 0
        state.farmStage = 1
        state.zoneProgress.clear()
        state.towerFloor = 0
        state.resources.gold = if (AscensionPerks.has(state, "ap_start")) 5000 else 500
        state.resources.essence = 0
        state.resources.mutagen = 0
        state.eggs.clear()
        state.breedJobs.clear()
        state.expeditions.clear()
        state.habitats.forEach { it.assigned.clear(); it.stored = 0.0 }
        if (state.slimes.isEmpty()) {
            val starter = makeSlime("blob.aether", Element.AETHER, null, Rarity.EPIC, Genome.random(rng, 0.2), level = if (AscensionPerks.has(state, "ap_start")) 15 else 1)
            state.squad[0] = starter.uid
        }
        state.lastSeen = now()
        touch()
        return gain
    }

    fun buyRebirthUpgrade(id: String): Boolean {
        val level = RebirthUpgrades.level(state, id)
        val node = RebirthUpgrades.get(id)
        if (level >= node.maxLevel) return false
        val cost = RebirthUpgrades.cost(id, level)
        if (state.resources.stardust < cost) return false
        state.resources.stardust -= cost
        state.rebirthUpgrades[id] = level + 1
        touch()
        return true
    }

    fun buyAscensionPerk(id: String): Boolean {
        if (id in state.ascensionPerks) return false
        val perk = AscensionPerks.get(id)
        if (state.resources.aether < perk.cost) return false
        state.resources.aether -= perk.cost
        state.ascensionPerks.add(id)
        if (id == "ap_talent") state.resources.talentPoints += 5
        touch()
        return true
    }

    // ---------- new game ----------
    companion object {
        fun newGame(now: Long = System.currentTimeMillis()): GameState {
            val state = GameState(createdAt = now, lastSeen = now)
            val engine = GameEngine(state, { now }, Random(now))
            val starter1 = engine.makeSlime("blob.aqua", Element.AQUA, null, Rarity.UNCOMMON, Genome.random(Random(1)), level = 1)
            val starter2 = engine.makeSlime("shroom.bloom", Element.BLOOM, null, Rarity.COMMON, Genome.random(Random(2)), level = 1)
            val starter3 = engine.makeSlime("fin.aqua", Element.AQUA, null, Rarity.COMMON, Genome.random(Random(3)), level = 1)
            state.squad[0] = starter1.uid
            state.squad[1] = starter2.uid
            state.squad[2] = starter3.uid
            state.resources.gold = 300
            state.resources.essence = 20
            state.eggs.add(engine.randomEgg("starter", rarityFloor = 0))
            state.keysResetDay = now / 86_400_000L
            state.resources.keys = Prestige.dailyKeys(state)
            engine.rollDailyChallenge(now / 86_400_000L)
            state.presets.add(SquadPreset("Starter", state.squad.toList()))
            return state
        }
    }
}