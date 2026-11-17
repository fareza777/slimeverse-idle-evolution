package com.f7developer.slimeverse.game

import kotlinx.serialization.Serializable

@Serializable
data class Resources(
    var gold: Long = 0,
    var essence: Long = 0,
    var mutagen: Long = 0,
    var research: Long = 0,
    var stardust: Long = 0,
    var aether: Long = 0,
    var talentPoints: Int = 0,
    var keys: Int = 3,
)

@Serializable
data class Egg(
    val uid: Long,
    val speciesId: String,
    val element: Element,
    val secondary: Element? = null,
    val rarity: Rarity,
    val genome: Genome,
    val hatchAt: Long,
    val startedAt: Long,
    val source: String = "wild",
)

@Serializable
data class BreedJob(
    val uid: Long,
    val motherUid: Long,
    val fatherUid: Long,
    val completesAt: Long,
    val egg: Egg,
)

@Serializable
data class Expedition(
    val uid: Long,
    val defId: String,
    val slimeUids: List<Long>,
    val startedAt: Long,
    val endsAt: Long,
)

@Serializable
data class HabitatState(
    var level: Int = 1,
    var assigned: MutableList<Long> = mutableListOf(),
    var stored: Double = 0.0,
    var lastTick: Long = 0,
)

@Serializable
data class SquadPreset(val name: String, val slots: List<Long?>)

@Serializable
data class AutoFuseRules(
    var enabled: Boolean = false,
    var maxCopies: Int = 3,
    var keepRarityAtLeast: Int = 3,
    var keepPrismatic: Boolean = true,
    var keepLocked: Boolean = true,
    var keepSquad: Boolean = true,
)

@Serializable
data class Settings(
    var theme: String = "LIGHT",
    var sound: Boolean = true,
    var haptics: Boolean = true,
    var battleSpeed: Int = 1,
    var autoProgress: Boolean = false,
    var autoFeed: Boolean = false,
    var autoEvolve: Boolean = false,
    var reducedMotion: Boolean = false,
    var showDamageNumbers: Boolean = true,
)

@Serializable
data class DailyChallengeState(
    var daySeed: Long = 0,
    var type: String = "mono",
    var target: Long = 3,
    var progress: Long = 0,
    var claimed: Boolean = false,
)

@Serializable
data class GameState(
    var version: Int = 3,
    var createdAt: Long = 0,
    var lastSeen: Long = 0,
    var resources: Resources = Resources(),
    var slimes: MutableList<Slime> = mutableListOf(),
    var nextUid: Long = 1,
    var squad: MutableList<Long?> = mutableListOf(null, null, null, null, null),
    var presets: MutableList<SquadPreset> = mutableListOf(),
    var eggs: MutableList<Egg> = mutableListOf(),
    var breedJobs: MutableList<BreedJob> = mutableListOf(),
    var expeditions: MutableList<Expedition> = mutableListOf(),
    var habitats: MutableList<HabitatState> = mutableListOf(HabitatState(), HabitatState(), HabitatState(), HabitatState()),
    var farmZone: Int = 0,
    var farmStage: Int = 1,
    var zoneProgress: MutableMap<Int, Int> = mutableMapOf(),
    var towerFloor: Int = 0,
    var towerBest: Int = 0,
    var keysResetDay: Long = 0,
    var dungeonClears: MutableMap<String, Int> = mutableMapOf(),
    var research: MutableSet<String> = mutableSetOf(),
    var talents: MutableMap<String, Int> = mutableMapOf(),
    var masteryXp: MutableMap<Element, Long> = mutableMapOf(),
    var masteryLevel: MutableMap<Element, Int> = mutableMapOf(),
    var achievementsClaimed: MutableSet<String> = mutableSetOf(),
    var stats: MutableMap<StatKey, Long> = mutableMapOf(),
    var codexSpecies: MutableSet<String> = mutableSetOf(),
    var codexFusions: MutableSet<String> = mutableSetOf(),
    var rebirths: Int = 0,
    var ascensions: Int = 0,
    var lifetimeStardust: Long = 0,
    var lifetimeAether: Long = 0,
    var rebirthUpgrades: MutableMap<String, Int> = mutableMapOf(),
    var ascensionPerks: MutableSet<String> = mutableSetOf(),
    var autoFuse: AutoFuseRules = AutoFuseRules(),
    var settings: Settings = Settings(),
    var onboardingStep: Int = 0,
    var onboardingDone: Boolean = false,
    var removeAds: Boolean = false,
    var daily: DailyChallengeState = DailyChallengeState(),
    var lastInterstitial: Long = 0,
    var breederXp: Long = 0,
    var breederLevel: Int = 1,
    var totalPlaytimeMs: Long = 0,
) {
    fun slime(uid: Long?): Slime? = if (uid == null) null else slimes.firstOrNull { it.uid == uid }
    fun squadSlimes(): List<Slime> = squad.mapNotNull { slime(it) }
    fun stat(key: StatKey): Long = stats[key] ?: 0L
    fun bump(key: StatKey, amount: Long = 1) { stats[key] = stat(key) + amount }
    fun maxStage(zone: Int): Int = zoneProgress[zone] ?: 0
    fun highestZoneCleared(): Int = zoneProgress.keys.maxOrNull() ?: -1

    fun discover(formKey: String) { codexSpecies.add(formKey) }
    fun discoverSpecies(speciesId: String) { codexSpecies.add(speciesId) }

    companion object {
        const val SAVE_VERSION = 3
    }
}