package com.f7developer.slimeverse.data

import android.content.Context
import com.f7developer.slimeverse.game.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class BattleMode { STAGE, TOWER, DUNGEON }

data class ActiveBattle(
    val battle: Battle,
    val mode: BattleMode,
    val zone: Int,
    val stage: Int,
    val dungeon: DungeonKind?,
    val tier: Int,
    val squadUids: List<Long>,
    val seed: Long,
)

data class BattleOutcome(
    val win: Boolean,
    val mode: BattleMode,
    val loot: LootResult?,
    val zone: Int,
    val stage: Int,
    val ticks: Int,
)

class GameRepository(private val context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val saveManager = SaveManager(context)

    lateinit var engine: GameEngine
        private set

    private val _rev = MutableStateFlow(0L)
    val rev: StateFlow<Long> = _rev.asStateFlow()

    private val _offline = MutableStateFlow<OfflineReport?>(null)
    val offline: StateFlow<OfflineReport?> = _offline.asStateFlow()

    private val _activeBattle = MutableStateFlow<ActiveBattle?>(null)
    val activeBattle: StateFlow<ActiveBattle?> = _activeBattle.asStateFlow()

    private val _battleEvents = MutableSharedFlow<List<BattleEvent>>(extraBufferCapacity = 64)
    val battleEvents: SharedFlow<List<BattleEvent>> = _battleEvents

    private val _outcome = MutableStateFlow<BattleOutcome?>(null)
    val outcome: StateFlow<BattleOutcome?> = _outcome.asStateFlow()

    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toast: SharedFlow<String> = _toast

    private var battleJob: Job? = null
    var initialized = false
        private set

    val state: GameState get() = engine.state

    fun init() {
        if (initialized) return
        initialized = true
        val loaded = saveManager.load()
        engine = if (loaded != null) {
            GameEngine(migrate(loaded))
        } else {
            GameEngine(GameEngine.newGame())
        }
        val now = System.currentTimeMillis()
        if (loaded != null && now - engine.state.lastSeen > 120_000L) {
            val report = engine.computeOffline()
            if (!report.isEmpty) _offline.value = report
        }
        engine.state.lastSeen = now
        bump()
        scope.launch {
            while (isActive) {
                delay(1000)
                engine.economyTick(1000)
                engine.autoFeedPass()
                engine.autoEvolvePass()
                if (engine.state.autoFuse.enabled) engine.autoFusePass()
                bump()
            }
        }
        scope.launch {
            while (isActive) {
                delay(30_000)
                save()
            }
        }
    }

    private fun migrate(s: GameState): GameState {
        // forward-compatible defaults; version bumps handled here
        if (s.habitats.size < 4) repeat(4 - s.habitats.size) { s.habitats.add(HabitatState()) }
        while (s.squad.size < 5) s.squad.add(null)
        s.version = GameState.SAVE_VERSION
        return s
    }

    fun bump() { _rev.value = engine.revision }

    fun toast(msg: String) { _toast.tryEmit(msg) }

    fun save() {
        engine.state.lastSeen = System.currentTimeMillis()
        saveManager.save(engine.state)
    }

    fun dismissOffline() { _offline.value = null }

    fun clearOutcome() { _outcome.value = null }

    fun act(block: () -> Unit) {
        block()
        bump()
    }

    // ---------- battles ----------
    fun startStageBattle(zone: Int, stage: Int) {
        val squad = state.squad.mapIndexedNotNull { i, uid -> state.slime(uid)?.let { it to i } }
        if (squad.isEmpty()) { toast("Add slimes to your squad first."); return }
        val seed = System.nanoTime()
        val battle = BattleFactory.stageBattle(
            squad, zone, stage, seed,
            Prestige.ascensionMult(state) * Prestige.stardustMult(state),
            Prestige.globalStatMods(state) + Prestige.researchStatMods(state) + engine.synergyStatMods(),
            Element.entries.associateWith { Prestige.masteryOf(state, it) },
        )
        launchBattle(ActiveBattle(battle, BattleMode.STAGE, zone, stage, null, 0, squad.map { it.first.uid }, seed))
    }

    fun startTowerBattle() {
        val squad = state.squad.mapIndexedNotNull { i, uid -> state.slime(uid)?.let { it to i } }
        if (squad.isEmpty()) { toast("Add slimes to your squad first."); return }
        val floor = state.towerFloor + 1
        val zone = ((floor - 1) / 10).coerceIn(0, 11)
        val stage = ((floor - 1) % 10) + 1
        val seed = System.nanoTime()
        val battle = BattleFactory.stageBattle(
            squad, zone, stage, seed,
            Prestige.ascensionMult(state) * Prestige.stardustMult(state),
            Prestige.globalStatMods(state) + Prestige.researchStatMods(state) + engine.synergyStatMods(),
            Element.entries.associateWith { Prestige.masteryOf(state, it) },
        )
        launchBattle(ActiveBattle(battle, BattleMode.TOWER, zone, stage, null, floor, squad.map { it.first.uid }, seed))
    }

    fun startDungeonBattle(kind: DungeonKind, tier: Int) {
        if (state.resources.keys <= 0) { toast("No dungeon keys left today."); return }
        if (!Dungeons.unlocked(tier, state.highestZoneCleared())) { toast("Reach a higher zone to unlock this tier."); return }
        val squad = state.squad.mapIndexedNotNull { i, uid -> state.slime(uid)?.let { it to i } }
        if (squad.isEmpty()) { toast("Add slimes to your squad first."); return }
        state.resources.keys--
        val zone = Dungeons.tierZone(tier)
        val seed = System.nanoTime()
        val battle = BattleFactory.stageBattle(
            squad, zone, Zones.STAGES, seed,
            Prestige.ascensionMult(state) * Prestige.stardustMult(state),
            Prestige.globalStatMods(state) + Prestige.researchStatMods(state) + engine.synergyStatMods(),
            Element.entries.associateWith { Prestige.masteryOf(state, it) },
        )
        launchBattle(ActiveBattle(battle, BattleMode.DUNGEON, zone, Zones.STAGES, kind, tier, squad.map { it.first.uid }, seed))
        bump()
    }

    private fun launchBattle(ab: ActiveBattle) {
        battleJob?.cancel()
        _outcome.value = null
        _activeBattle.value = ab
        battleJob = scope.launch {
            val speed = state.settings.battleSpeed.coerceIn(1, Prestige.battleSpeedMax(state))
            while (isActive && ab.battle.result == BattleResult.ONGOING) {
                val events = ab.battle.tick()
                if (events.isNotEmpty()) _battleEvents.tryEmit(events)
                delay(100L / speed)
            }
            val win = ab.battle.result == BattleResult.WIN
            val squad = ab.squadUids.mapNotNull { state.slime(it) }
            var loot: LootResult? = null
            if (win) {
                loot = when (ab.mode) {
                    BattleMode.STAGE -> {
                        val boss = Zones.isBoss(ab.stage)
                        val l = engine.applyBattleWin(ab.zone, ab.stage, boss, squad)
                        engine.onBattleWonForDaily(squad, ab.battle.tick, boss)
                        if (state.settings.autoProgress && !boss && ab.stage < Zones.STAGES) {
                            state.farmZone = ab.zone; state.farmStage = ab.stage + 1
                        } else if (boss) {
                            state.farmZone = ab.zone; state.farmStage = Zones.STAGES
                        }
                        l
                    }
                    BattleMode.TOWER -> {
                        engine.recordTowerWin(ab.tier)
                        engine.applyBattleWin(ab.zone, ab.stage, false, squad, tower = true)
                    }
                    BattleMode.DUNGEON -> engine.applyBattleWin(ab.zone, ab.stage, true, squad, dungeon = ab.dungeon, tier = ab.tier)
                }
            }
            _outcome.value = BattleOutcome(win, ab.mode, loot, ab.zone, ab.stage, ab.battle.tick)
            bump()
            save()
        }
    }

    fun stopBattle() {
        battleJob?.cancel()
        battleJob = null
        _activeBattle.value = null
    }

    fun importSave(uri: android.net.Uri): Boolean {
        val s = saveManager.importFrom(uri) ?: return false
        engine = GameEngine(migrate(s))
        engine.state.lastSeen = System.currentTimeMillis()
        bump()
        save()
        return true
    }

    fun resetGame() {
        engine = GameEngine(GameEngine.newGame())
        bump()
        save()
    }
}