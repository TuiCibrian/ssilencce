package com.ssilencce.util.oem

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.ssilencce.R
import java.util.Locale

/**
 * Detects the device vendor and exposes the system screens that control background playback.
 * All intents are resolved before being started and fall back to the app details screen, since
 * OEM components vary between versions and regions.
 */
object PowerManagementGuide {
    const val KEY_BATTERY_OPTIMIZATION = "battery_optimization"
    const val KEY_STEPS = "oem_steps"
    const val KEY_PIN_RECENTS = "pin_recents"
    const val KEY_AUTO_START = "auto_start"
    const val KEY_MIUI_SHORTCUTS = "miui_shortcuts"
    const val KEY_APP_SETTINGS = "app_settings"
    const val KEY_REFRESH_STATUS = "refresh_status"

    enum class OemFamily(
        @StringRes val displayNameRes: Int,
        @ArrayRes val stepsRes: Int,
    ) {
        Xiaomi(R.string.oem_xiaomi, R.array.battery_guide_steps_xiaomi),
        Huawei(R.string.oem_huawei, R.array.battery_guide_steps_huawei),
        Samsung(R.string.oem_samsung, R.array.battery_guide_steps_samsung),
        Oppo(R.string.oem_oppo, R.array.battery_guide_steps_oppo),
        Realme(R.string.oem_realme, R.array.battery_guide_steps_realme),
        Vivo(R.string.oem_vivo, R.array.battery_guide_steps_vivo),
        Asus(R.string.oem_asus, R.array.battery_guide_steps_asus),
        Generic(R.string.oem_generic, R.array.battery_guide_steps_generic),
    }

    fun detect(): OemFamily {
        val identity = "${Build.MANUFACTURER} ${Build.BRAND}".lowercase(Locale.ROOT)
        return when {
            identity.containsAny("xiaomi", "redmi", "poco") -> OemFamily.Xiaomi
            identity.containsAny("huawei", "honor") -> OemFamily.Huawei
            identity.containsAny("samsung") -> OemFamily.Samsung
            identity.containsAny("realme") -> OemFamily.Realme
            identity.containsAny("oppo", "oneplus") -> OemFamily.Oppo
            identity.containsAny("vivo", "iqoo") -> OemFamily.Vivo
            identity.containsAny("asus") -> OemFamily.Asus
            else -> OemFamily.Generic
        }
    }

    fun steps(
        context: Context,
        family: OemFamily = detect(),
    ): List<String> = context.resources.getStringArray(family.stepsRes).toList()

    fun isIgnoringBatteryOptimizations(context: Context): Boolean =
        context.getSystemService<PowerManager>()?.isIgnoringBatteryOptimizations(context.packageName)
            ?: true

    /**
     * Opens the battery-optimization screen. Per platform rules, the direct exemption dialog is
     * used only when the app actually declares REQUEST_IGNORE_BATTERY_OPTIMIZATIONS.
     */
    fun openBatteryOptimizationSettings(context: Context): Boolean {
        val canRequestExemption =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        val intent =
            if (canRequestExemption) {
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${context.packageName}"),
                )
            } else {
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            }
        return startSafely(context, intent) || openAppDetails(context)
    }

    fun hasAutoStartSettings(family: OemFamily = detect()): Boolean = autoStartComponents(family).isNotEmpty()

    fun openAutoStartSettings(
        context: Context,
        family: OemFamily = detect(),
    ): Boolean {
        for (component in autoStartComponents(family)) {
            val intent =
                Intent()
                    .setComponent(component)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (startSafely(context, intent)) return true
        }
        return openAppDetails(context)
    }

    fun openShortcutPermissionSettings(context: Context): Boolean {
        if (detect() != OemFamily.Xiaomi) return openAppDetails(context)
        val candidates =
            listOf(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity",
                ),
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.AppPermissionsEditorActivity",
                ),
            )
        for (component in candidates) {
            val intent =
                Intent()
                    .setComponent(component)
                    .putExtra("extra_pkgname", context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (startSafely(context, intent)) return true
        }
        return openAppDetails(context)
    }

    fun openAppDetails(context: Context): Boolean =
        startSafely(
            context,
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ) ||
            startSafely(
                context,
                Intent(Settings.ACTION_APPLICATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )

    private fun startSafely(
        context: Context,
        intent: Intent,
    ): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) == null) return false
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    private fun autoStartComponents(family: OemFamily): List<ComponentName> =
        when (family) {
            OemFamily.Xiaomi -> {
                listOf(
                    ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity",
                    ),
                )
            }

            OemFamily.Huawei -> {
                listOf(
                    ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
                    ),
                    ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity",
                    ),
                )
            }

            OemFamily.Samsung -> {
                listOf(
                    ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.ui.battery.BatteryActivity",
                    ),
                    ComponentName(
                        "com.samsung.android.sm",
                        "com.samsung.android.sm.ui.battery.BatteryActivity",
                    ),
                )
            }

            OemFamily.Oppo,
            OemFamily.Realme,
            -> {
                listOf(
                    ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity",
                    ),
                    ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.startupapp.StartupAppListActivity",
                    ),
                    ComponentName(
                        "com.oplus.safecenter",
                        "com.oplus.safecenter.permission.startup.StartupAppListActivity",
                    ),
                    ComponentName(
                        "com.oplus.safecenter",
                        "com.oplus.safecenter.startupapp.StartupAppListActivity",
                    ),
                )
            }

            OemFamily.Vivo -> {
                listOf(
                    ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
                    ),
                    ComponentName(
                        "com.iqoo.secure",
                        "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager",
                    ),
                )
            }

            OemFamily.Asus -> {
                listOf(
                    ComponentName(
                        "com.asus.mobilemanager",
                        "com.asus.mobilemanager.autostart.AutoStartActivity",
                    ),
                    ComponentName(
                        "com.asus.mobilemanager",
                        "com.asus.mobilemanager.MainActivity",
                    ),
                )
            }

            OemFamily.Generic -> {
                emptyList()
            }
        }

    private fun String.containsAny(vararg values: String): Boolean = values.any { contains(it) }
}
