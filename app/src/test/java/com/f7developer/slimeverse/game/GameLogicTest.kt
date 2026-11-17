package com.f7developer.slimeverse.game

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GeneticsTest {
    @Test
    fun randomGenomeHasAllLoci() {
        val g = Genome.random(Random(1))
        assertEquals(Locus.entries.size, g.pairs.size)
        for (l in Locus.entries) {
            val (a, b) = g.pairs.getValue(l)
            assertEquals(l, Genes.allele(a).locus)
            assertEquals(l, Genes.allele(b).locus)
        }
    }

    @Test
    fun dominanceExpressesHigherRank() {
        val g = Genome(mapOf(Locus.SIZE to ("size.tiny" to "size.giant")))
        assertEquals("size.giant", g.expressed(Locus.SIZE).id)
        val g2 = Genome(mapOf(Locus.SIZE to ("size.giant" to "size.tiny")))
        assertEquals("size.giant", g2.expressed(Locus.SIZE).id)
    }

    @Test
    fun breedInheritsOneAllelePerParent() {
        val mother = Genome.random(Random(10))
        val father = Genome.random(Random(20))
        repeat(200) {
            val child = Genome.breed(mother, father, Random(it.toLong()), 0.0)
            for (l in Locus.entries) {
                val (ca, cb) = child.pairs.getValue(l)
                val mp = mother.pairs.getValue(l)
                val fp = father.pairs.getValue(l)
                assertTrue("maternal allele must come from mother", ca == mp.first || ca == mp.second)
                assertTrue("paternal allele must come from father", cb == fp.first || cb == fp.second)
            }
        }
    }

    @Test
    fun mutationIntroducesNewAlleles() {
        val mother = Genome(Locus.entries.associateWith { l -> Genes.pool(l).first().id to Genes.pool(l).first().id })
        val father = mother
        var changed = 0
        repeat(300) {
            val child = Genome.breed(mother, father, Random(it.toLong()), 0.5)
            for (l in Locus.entries) {
                val (a, b) = child.pairs.getValue(l)
                if (a != Genes.pool(l).first().id || b != Genes.pool(l).first().id) changed++
            }
        }
        assertTrue("high mutation rate must change alleles", changed > 100)
        // negative control: zero mutation never changes
        var changedZero = 0
        repeat(100) {
            val child = Genome.breed(mother, father, Random(it.toLong()), 0.0)
            for (l in Locus.entries) {
                val (a, b) = child.pairs.getValue(l)
                if (a != Genes.pool(l).first().id || b != Genes.pool(l).first().id) changedZero++
            }
        }
        assertEquals("zero mutation must not change alleles", 0, changedZero)
    }

    @Test
    fun prismaticTraitExpressed() {
        val base = Genome.random(Random(1))
        val g = Genome(base.pairs + (Locus.AURA to ("aura.prismatic" to "aura.none")))
        assertTrue(g.isPrismatic())
        assertTrue(Trait.PRISMATIC in g.traits())
    }
}

class FusionTest {
    private fun engine() = GameEngine(GameEngine.newGame(1000L), { 1000L }, Random(42))

    @Test
    fun sameFamilyFusionKeepsFamily() {
        assertEquals(Family.BLOB, Families.fusionResult(Family.BLOB, Family.BLOB, Random(0)))
        assertEquals(Family.CHIMERA, Families.fusionResult(Family.CHIMERA, Family.CHIMERA, Random(0)))
    }

    @Test
    fun mightPlusMightYieldsChimera() {
        assertEquals(Family.CHIMERA, Families.fusionResult(Family.FANG, Family.MANTIS, Random(0)))
        assertEquals(Family.TITAN, Families.fusionResult(Family.SHARD, Family.GOLEM, Random(0)))
        assertEquals(Family.SERAPH, Families.fusionResult(Family.ROYAL, Family.RUNE, Random(0)))
    }

    @Test
    fun fusionConsumesParentsAndRegistersChild() {
        val e = engine()
        e.state.resources.gold = 100000
        val a = e.state.slimes[0]
        val b = e.state.slimes[1]
        val before = e.state.slimes.size
        val child = e.fuse(a.uid, b.uid)
        assertNotNull(child)
        assertNull(e.state.slime(a.uid))
        assertNull(e.state.slime(b.uid))
        assertEquals(before - 1, e.state.slimes.size)
        assertTrue(child!!.uid in e.state.slimes.map { it.uid })
        assertEquals(1L, e.state.stat(StatKey.FUSIONS))
    }

    @Test
    fun fusionFailsWithoutGold() {
        val e = engine()
        e.state.resources.gold = 0
        val a = e.state.slimes[0]; val b = e.state.slimes[1]
        assertNull(e.fuse(a.uid, b.uid))
        assertEquals(3, e.state.slimes.size)
    }
}

class BattleTest {
    @Test
    fun deterministicWithSameSeed() {
        val state = GameEngine.newGame(1L)
        val squad = state.squad.mapIndexedNotNull { i, uid -> state.slime(uid)?.let { it to i } }
        val b1 = BattleFactory.stageBattle(squad, 0, 1, 999L)
        val b2 = BattleFactory.stageBattle(squad, 0, 1, 999L)
        val r1 = b1.resolve(); val r2 = b2.resolve()
        assertEquals(r1, r2)
        assertEquals(b1.tick, b2.tick)
    }

    @Test
    fun starterSquadBeatsFirstStage() {
        val state = GameEngine.newGame(1L)
        val squad = state.squad.mapIndexedNotNull { i, uid -> state.slime(uid)?.let { it to i } }
        val battle = BattleFactory.stageBattle(squad, 0, 1, 7L)
        assertEquals(BattleResult.WIN, battle.resolve())
    }

    @Test
    fun battleTerminates() {
        val state = GameEngine.newGame(1L)
        val squad = state.squad.mapIndexedNotNull { i, uid -> state.slime(uid)?.let { it to i } }
        val battle = BattleFactory.stageBattle(squad, 11, 10, 7L)
        battle.resolve()
        assertNotEquals(BattleResult.ONGOING, battle.result)
        assertTrue(battle.tick <= 901)
    }

    @Test
    fun elementMultiplierChart() {
        assertEquals(1.5, Elements.multiplier(Element.AQUA, Element.EMBER), 0.001)
        assertEquals(0.75, Elements.multiplier(Element.EMBER, Element.AQUA), 0.001)
        assertEquals(1.0, Elements.multiplier(Element.AQUA, Element.AQUA), 0.001)
        assertEquals(1.25, Elements.multiplier(Element.AETHER, Element.UMBRA), 0.001)
    }
}

class EconomyTest {
    private fun engine() = GameEngine(GameEngine.newGame(1000L), { 1000L }, Random(42))

    @Test
    fun feedGrantsXpAndCostsGold() {
        val e = engine()
        e.state.resources.gold = 100000
        val s = e.state.slimes[0]
        val goldBefore = e.state.resources.gold
        val ok = e.feed(s.uid)
        assertTrue(ok)
        assertTrue(e.state.resources.gold < goldBefore)
        assertTrue(s.xp > 0 || s.level > 1)
    }

    @Test
    fun evolveRequiresLevel() {
        val e = engine()
        e.state.resources.gold = 1000000
        e.state.resources.essence = 1000000
        val s = e.state.slimes[0]
        assertFalse(e.canEvolve(s)) // level 1
        s.level = 20
        assertTrue(e.canEvolve(s))
        assertTrue(e.evolve(s.uid, "a"))
        assertEquals(1, e.state.slime(s.uid)!!.tier)
    }

    @Test
    fun levelGrowthMonotonic() {
        var prev = 0.0
        for (l in 1..100) {
            val g = Balance.levelGrowth(l)
            assertTrue(g > prev)
            prev = g
        }
    }

    @Test
    fun mitigationBounded() {
        assertTrue(Balance.mitigation(0.0) == 1.0)
        assertTrue(Balance.mitigation(1000.0) < 0.2)
        assertTrue(Balance.mitigation(1000.0) > 0.0)
    }

    @Test
    fun offlineGrantsRewardsAndRespectsCap() {
        var clock = 1000L
        val state = GameEngine.newGame(clock)
        val e = GameEngine(state, { clock }, Random(1))
        // strong squad so idle clears happen
        state.slimes.forEach { it.level = 50 }
        clock += 3_600_000L * 3 // 3 hours
        val report = e.computeOffline()
        assertTrue(report.gold > 0)
        assertTrue(report.clears > 0)
        assertEquals(3_600_000L * 3, report.elapsedMs)
        // cap test: 100 hours offline capped to <= 48h
        clock += 3_600_000L * 100
        val report2 = e.computeOffline()
        assertTrue(report2.elapsedMs <= 48L * 3_600_000L)
    }

    @Test
    fun achievementClaimRequiresThreshold() {
        val e = engine()
        assertFalse(e.claimAchievement("own80")) // only 3 slimes
        assertTrue(e.claimAchievement("own5") || e.state.slimes.size < 5)
    }
}

class SaveTest {
    @Test
    fun serializationRoundTrip() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val state = GameEngine.newGame(5000L)
        state.resources.gold = 123456
        state.talents["t_gold"] = 3
        state.masteryLevel[Element.AQUA] = 2
        val text = json.encodeToString(GameState.serializer(), state)
        val restored = json.decodeFromString(GameState.serializer(), text)
        assertEquals(123456L, restored.resources.gold)
        assertEquals(3, restored.talents["t_gold"])
        assertEquals(2, restored.masteryLevel[Element.AQUA])
        assertEquals(state.slimes.size, restored.slimes.size)
        assertEquals(state.slimes[0].genome.pairs, restored.slimes[0].genome.pairs)
        assertEquals(state.squad, restored.squad)
    }

    @Test
    fun genomeSurvivesSerialization() {
        val json = Json { encodeDefaults = true }
        val g = Genome.random(Random(7))
        val text = json.encodeToString(Genome.serializer(), g)
        val back = json.decodeFromString(Genome.serializer(), text)
        assertEquals(g.pairs, back.pairs)
        assertEquals(g.phenotype().mapValues { it.value.id }, back.phenotype().mapValues { it.value.id })
    }
}

class ContentTest {
    @Test
    fun speciesCounts() {
        assertEquals(160, Species.baseSpeciesCount)
        assertEquals(800, Species.totalForms)
        assertTrue(Species.exists("blob.aqua"))
        assertTrue(Species.exists("phantom.aether"))
    }

    @Test
    fun evolutionOptionsBranch() {
        val base = Species.base("fang.ember")
        val opts = Species.evolveOptions(base)
        assertEquals(2, opts.size)
        assertEquals(setOf("a", "b"), opts.map { it.branch }.toSet())
        val t1 = opts.first { it.branch == "a" }
        val t2opts = Species.evolveOptions(t1)
        assertEquals(1, t2opts.size)
        assertEquals("a", t2opts[0].branch)
    }

    @Test
    fun fusionPairsCoverAllBase() {
        assertEquals(78, Families.fusionPairs.size) // 12*13/2
    }

    @Test
    fun researchPrereqsValid() {
        for (n in Research.all) {
            if (n.requires != null) assertTrue(Research.all.any { it.id == n.requires })
        }
        assertEquals(24, Research.all.size)
    }

    @Test
    fun zoneEnemyTeamsValid() {
        for (z in 0..11) {
            val enemies = Zones.enemies(z, 10)
            assertTrue(enemies.isNotEmpty())
            assertTrue(enemies[0].boss)
            for (e in enemies) assertTrue(Species.exists(e.speciesId))
        }
    }
}