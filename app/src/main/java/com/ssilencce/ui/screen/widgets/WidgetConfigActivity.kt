package com.ssilencce.ui.screen.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.ssilencce.R
import com.ssilencce.core.appwidgets.CardWidget
import com.ssilencce.core.appwidgets.CardWidgetReceiver
import com.ssilencce.core.appwidgets.FullWidget
import com.ssilencce.core.appwidgets.FullWidgetReceiver
import com.ssilencce.core.appwidgets.SsilencceGlanceWidget
import com.ssilencce.core.appwidgets.SsilencceGlanceWidgetReceiver
import com.ssilencce.core.appwidgets.WallpaperThemeProvider
import com.ssilencce.core.appwidgets.state.WidgetSettings
import com.ssilencce.core.appwidgets.state.WidgetSettingsStore
import com.ssilencce.core.appwidgets.state.WidgetThemeMode
import com.ssilencce.ui.theme.SsilencceTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class WidgetConfigActivity : ComponentActivity() {
    private var saving by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId =
            intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        setContent {
            SsilencceTheme {
                WidgetConfigScreen(
                    appWidgetId = appWidgetId,
                    saving = saving,
                    onCancel = {
                        if (!saving) {
                            setResult(RESULT_CANCELED)
                            finish()
                        }
                    },
                    onSave = { settings ->
                        if (!saving) {
                            saving = true
                            lifecycleScope.launch {
                                val updated =
                                    try {
                                        val wallpaperColor =
                                            if (settings.themeMode == WidgetThemeMode.Wallpaper) {
                                                WallpaperThemeProvider.deriveColor(this@WidgetConfigActivity)
                                            } else {
                                                null
                                            }
                                        WidgetSettingsStore.save(
                                            this@WidgetConfigActivity,
                                            appWidgetId,
                                            settings.copy(wallpaperColor = wallpaperColor),
                                        )
                                        updateWidget(appWidgetId)
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        false
                                    }
                                setResult(
                                    if (updated) RESULT_OK else RESULT_CANCELED,
                                    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                                )
                                saving = false
                                finish()
                            }
                        }
                    },
                )
            }
        }
    }

    private suspend fun updateWidget(appWidgetId: Int): Boolean {
        val provider =
            AppWidgetManager
                .getInstance(this)
                .getAppWidgetInfo(appWidgetId)
                ?.provider
                ?: return false
        val widget =
            when (provider) {
                ComponentName(this, SsilencceGlanceWidgetReceiver::class.java) -> SsilencceGlanceWidget()
                ComponentName(this, CardWidgetReceiver::class.java) -> CardWidget()
                ComponentName(this, FullWidgetReceiver::class.java) -> FullWidget()
                else -> return false
            }
        return try {
            val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)
            widget.update(this, glanceId)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetConfigScreen(
    appWidgetId: Int,
    saving: Boolean,
    onCancel: () -> Unit,
    onSave: (WidgetSettings) -> Unit,
) {
    var themeModeName by rememberSaveable { mutableStateOf(WidgetThemeMode.System.name) }
    var transparency by rememberSaveable { mutableStateOf(0f) }
    var loaded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(appWidgetId) {
        val settings = WidgetSettingsStore.load(context, appWidgetId)
        themeModeName = settings.themeMode.name
        transparency = settings.transparencyPercent.toFloat()
        loaded = true
    }

    if (!loaded) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val themeMode = WidgetThemeMode.entries.firstOrNull { it.name == themeModeName } ?: WidgetThemeMode.System
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.widget_config_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.general_theme_title),
            style = MaterialTheme.typography.titleMedium,
        )
        WidgetThemeMode.entries.forEach { mode ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = themeMode == mode,
                            enabled = !saving,
                            role = Role.RadioButton,
                            onClick = { themeModeName = mode.name },
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = themeMode == mode,
                    onClick = null,
                    enabled = !saving,
                )
                Text(
                    text =
                        when (mode) {
                            WidgetThemeMode.System -> stringResource(R.string.system_default)
                            WidgetThemeMode.AlbumArt -> stringResource(R.string.widget_theme_album_art)
                            WidgetThemeMode.Wallpaper -> stringResource(R.string.widget_theme_wallpaper)
                        },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        if (themeMode == WidgetThemeMode.Wallpaper) {
            Text(
                text = stringResource(R.string.widget_theme_wallpaper_summary),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = stringResource(R.string.widget_transparency_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Slider(
            value = transparency,
            onValueChange = { transparency = it },
            valueRange = 0f..100f,
            steps = 9,
            enabled = !saving,
        )
        Text(text = "${transparency.roundToInt()}%")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onCancel, enabled = !saving) {
                Text(stringResource(R.string.action_cancel))
            }
            Button(
                onClick = {
                    onSave(
                        WidgetSettings(
                            themeMode = themeMode,
                            transparencyPercent = transparency.roundToInt(),
                        ),
                    )
                },
                enabled = !saving,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}
