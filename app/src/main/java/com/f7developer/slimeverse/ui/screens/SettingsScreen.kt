package com.f7developer.slimeverse.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.f7developer.slimeverse.SlimeverseApp
import com.f7developer.slimeverse.ui.LocalRepo
import com.f7developer.slimeverse.ui.components.*
import com.f7developer.slimeverse.ui.theme.LocalExtraColors

@Composable
fun SettingsScreen(nav: NavController) {
    val repo = LocalRepo.current
    val rev by repo.rev.collectAsState()
    val state = repo.state
    val context = LocalContext.current
    val app = context.applicationContext as SlimeverseApp
    val activity = context as? Activity
    val price by app.billing.price.collectAsState()
    val removeAds by app.billing.removeAds.collectAsState()
    val available by app.billing.available.collectAsState()
    var confirmReset by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = repo.saveManager.exportTo(uri, state)
            repo.toast(if (ok) "Save exported." else "Export failed.")
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val ok = repo.importSave(uri)
            repo.toast(if (ok) "Save imported." else "Import failed.")
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(nav, "Settings")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GameCard {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LIGHT" to "Light", "DARK" to "Dark", "SYSTEM" to "Auto").forEach { (key, label) ->
                        FilterChip(state.settings.theme == key, { repo.act { state.settings.theme = key } }, { Text(label, fontSize = 13.sp) })
                    }
                }
                SettingToggle("Sound", state.settings.sound) { repo.act { state.settings.sound = it } }
                SettingToggle("Haptics", state.settings.haptics) { repo.act { state.settings.haptics = it } }
                SettingToggle("Damage numbers", state.settings.showDamageNumbers) { repo.act { state.settings.showDamageNumbers = it } }
                SettingToggle("Reduced motion", state.settings.reducedMotion) { repo.act { state.settings.reducedMotion = it } }
            }
            GameCard {
                Text("Remove Ads", style = MaterialTheme.typography.titleMedium)
                if (removeAds) {
                    Text("Ads removed. Thank you for supporting F7 Developer!", color = Color(0xFF43A047), fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                } else {
                    Text("One-time purchase. Removes banners and interstitials forever. Optional rewarded ads remain available.", style = MaterialTheme.typography.bodySmall, color = LocalExtraColors.current.inkSoft)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton("Buy · $price", { activity?.let { app.billing.launchPurchase(it) } }, modifier = Modifier.weight(1f), enabled = available, icon = Icons.Filled.ShoppingCart)
                        SoftButton("Restore", { app.billing.restorePurchases(); repo.toast("Checking purchases…") }, modifier = Modifier.weight(1f))
                    }
                    if (!available) Text("Store unavailable right now. Try Restore later.", fontSize = 13.sp, color = LocalExtraColors.current.inkFaint)
                }
            }
            GameCard {
                Text("Save Data", style = MaterialTheme.typography.titleMedium)
                Text("Your progress autosaves every 30s and on exit. Export a backup file any time.", style = MaterialTheme.typography.bodySmall, color = LocalExtraColors.current.inkSoft)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoftButton("Export", { exportLauncher.launch("slimeverse-save.json") }, icon = Icons.Filled.Upload, modifier = Modifier.weight(1f))
                    SoftButton("Import", { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }, icon = Icons.Filled.Download, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                SoftButton("Reset game", { confirmReset = true }, icon = Icons.Filled.DeleteForever, tint = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth())
            }
            GameCard {
                Text("Privacy", style = MaterialTheme.typography.titleMedium)
                Text("Ads are provided by Google AdMob. A one-time Remove Ads purchase hides banners and interstitials. No account or personal data is required to play.", style = MaterialTheme.typography.bodySmall, color = LocalExtraColors.current.inkSoft)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (confirmReset) ConfirmDialog("Reset everything?", "All progress will be permanently erased. Export a backup first if unsure.", "Reset", {
        repo.resetGame(); confirmReset = false; nav.popBackStack()
    }, { confirmReset = false }, danger = true)
}

@Composable
private fun SettingToggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        Switch(value, onChange)
    }
}

fun rateApp(context: android.content.Context) {
    val pkg = context.packageName.removeSuffix(".debug")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
    } catch (e: Exception) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
    }
}

fun shareApp(context: android.content.Context) {
    val pkg = context.packageName.removeSuffix(".debug")
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "I'm breeding slimes and ascending in Slimeverse: Idle Evolution RPG! Try it: https://play.google.com/store/apps/details?id=$pkg")
    }
    context.startActivity(Intent.createChooser(intent, "Share Slimeverse"))
}