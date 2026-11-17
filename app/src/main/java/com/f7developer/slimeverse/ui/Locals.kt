package com.f7developer.slimeverse.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.f7developer.slimeverse.data.GameRepository

val LocalRepo = staticCompositionLocalOf<GameRepository> { error("repo not provided") }