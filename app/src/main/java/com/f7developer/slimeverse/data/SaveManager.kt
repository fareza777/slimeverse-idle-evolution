package com.f7developer.slimeverse.data

import android.content.Context
import com.f7developer.slimeverse.game.GameState
import kotlinx.serialization.json.Json
import java.io.File

/** Reliable save/load: atomic write (tmp + rename) plus a rolling backup. */
class SaveManager(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }
    private val mainFile = File(context.filesDir, "slimeverse_save.json")
    private val backupFile = File(context.filesDir, "slimeverse_save.bak")
    private val tmpFile = File(context.filesDir, "slimeverse_save.tmp")

    fun save(state: GameState): Boolean = try {
        val text = json.encodeToString(GameState.serializer(), state)
        tmpFile.writeText(text)
        if (mainFile.exists()) {
            mainFile.copyTo(backupFile, overwrite = true)
        }
        tmpFile.renameTo(mainFile)
        true
    } catch (e: Exception) {
        false
    }

    fun load(): GameState? {
        for (f in listOf(mainFile, backupFile)) {
            if (!f.exists()) continue
            try {
                return json.decodeFromString(GameState.serializer(), f.readText())
            } catch (e: Exception) {
                // try next
            }
        }
        return null
    }

    fun exportTo(uri: android.net.Uri, state: GameState): Boolean = try {
        context.contentResolver.openOutputStream(uri)?.use {
            it.write(json.encodeToString(GameState.serializer(), state).toByteArray())
        }
        true
    } catch (e: Exception) {
        false
    }

    fun importFrom(uri: android.net.Uri): GameState? = try {
        context.contentResolver.openInputStream(uri)?.use {
            json.decodeFromString(GameState.serializer(), it.readBytes().decodeToString())
        }
    } catch (e: Exception) {
        null
    }

    fun hasSave(): Boolean = mainFile.exists() || backupFile.exists()
}