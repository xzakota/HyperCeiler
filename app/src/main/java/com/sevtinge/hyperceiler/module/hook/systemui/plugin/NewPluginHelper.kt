package com.sevtinge.hyperceiler.module.hook.systemui.plugin

import android.content.*
import android.text.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.hook.systemui.*
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.*
import com.sevtinge.hyperceiler.utils.api.*
import java.lang.ref.*

// https://github.com/buffcow/Hyper5GSwitch/blob/master/app/src/main/kotlin/cn/buffcow/hyper5g/hooker/PluginLoader.kt
object NewPluginHelper : BaseHook() {
    override fun init() {
        loadClass("com.android.systemui.shared.plugins.PluginInstance\$PluginFactory")
            .methodFinder().filterByName("createPluginContext")
            .first().createAfterHook {
               runCatching {
                   val wrapper = it.result as ContextWrapper
                   onPluginLoaded(PluginFactory(it.thisObject).also { it.pluginCtxRef = WeakReference(wrapper) })
               }.onFailure {
                   logE(TAG, lpparam.packageName, "Failed to create plugin context.")
                   return@createAfterHook
               }
            }
    }

    private fun onPluginLoaded(factory: PluginFactory) {
        when (factory.mComponentName) {
            factory.componentNames("miui.systemui.volume.VolumeDialogPlugin") -> {
                val classLoader: ClassLoader = factory.pluginCtxRef.get()!!.classLoader
                logD(TAG, lpparam.packageName, "Plugin for sysui volume loaded.")

                if (mPrefsMap.getBoolean("system_ui_plugin_enable_volume_blur"))
                    try {EnableVolumeBlur.initEnableVolumeBlur(classLoader)} catch (_: Exception) {}
                if (mPrefsMap.getBoolean("system_cc_volume_showpct_title"))
                    try {ShowVolumePct.init(classLoader)} catch (_: Exception) {} // 声音百分比
                if (mPrefsMap.getBoolean("system_framework_volume_separate_control") &&
                    mPrefsMap.getBoolean("system_framework_volume_separate_slider"))
                    try {NotificationVolumeSeparateSlider.initHideDeviceControlEntry(classLoader)} catch (_: Exception) {}
            }

            factory.componentNames("miui.systemui.miplay.MiPlayPluginImpl") -> {
                val classLoader: ClassLoader = factory.pluginCtxRef.get()!!.classLoader
                logD(TAG, lpparam.packageName, "Plugin for sysui mipay loaded.")

                if (mPrefsMap.getStringAsInt("system_ui_control_center_mi_play_entry", 0) != 0)
                    try {HideMiPlayEntry.initHideMiPlayEntry(classLoader)} catch (_: Exception) {}
            }

            factory.componentNames("miui.systemui.quicksettings.LocalMiuiQSTilePlugin") -> {
                val classLoader: ClassLoader = factory.pluginCtxRef.get()!!.classLoader
                logD(TAG, lpparam.packageName, "Plugin for sysui mipay loaded.")

                if (mPrefsMap.getBoolean("system_ui_control_center_rounded_rect"))
                    try {CCGridForHyperOS.initCCGridForHyperOS(classLoader)} catch (_: Exception) {} // 控制中心磁贴圆角
            }

            factory.componentNames("miui.systemui.controlcenter.MiuiControlCenter") -> {
                val classLoader: ClassLoader = factory.pluginCtxRef.get()!!.classLoader
                val mCardStyleTiles = getTileList()
                logD(TAG, lpparam.packageName, "Plugin for sysui control center loaded.")

                if (mPrefsMap.getBoolean("system_ui_control_center_qs_open_color") ||
                    mPrefsMap.getBoolean("system_ui_control_center_qs_big_open_color")
               ) {
                    try {QSColor.pluginHook(classLoader)} catch (_: Exception) {}
                }
                if (mPrefsMap.getBoolean("systemui_plugin_card_tiles_enabled") &&
                    mPrefsMap.getString("systemui_plugin_card_tiles", "").isNotEmpty()
                ) {
                    try {CustomCardTiles.initCustomCardTiles(classLoader, mCardStyleTiles)} catch (_: Exception) {}
                }
                if (mPrefsMap.getBoolean("system_ui_control_center_hide_edit_botton"))
                    try {HideEditButton.initHideEditButton(classLoader)} catch (_: Exception) {}
                if (mPrefsMap.getBoolean("system_ui_control_center_rounded_rect"))
                    try {CCGridForHyperOS.initCCGridForHyperOS(classLoader)} catch (_: Exception) {} // 控制中心磁贴圆角
            }

            else -> {
                val classLoader: ClassLoader = factory.pluginCtxRef.get()!!.classLoader

                if (mPrefsMap.getStringAsInt("system_ui_control_center_mi_smart_hub_entry", 0) != 0)
                    try {HideMiSmartHubEntry.initHideMiSmartHubEntry(classLoader)} catch (_: Exception) {}
                if (mPrefsMap.getStringAsInt("system_ui_control_center_device_ctrl_entry", 0) != 0)
                    try {HideDeviceControlEntry.initHideDeviceControlEntry(classLoader)} catch (_: Exception) {}
                if (mPrefsMap.getStringAsInt("system_ui_control_center_cc_bluetooth_tile_style", 1) > 1)
                    try {BluetoothTileStyle.initHideDeviceControlEntry(classLoader)} catch (_: Exception) {}
                if (mPrefsMap.getStringAsInt("system_ui_control_center_hide_operator", 0) == 3)
                    try {ShowDeviceName.initShowDeviceName(classLoader)} catch (_: Exception) {}
                if (mPrefsMap.getBoolean("system_ui_control_center_disable_device_managed"))
                    try {DisableDeviceManaged.initDisableDeviceManaged(classLoader)} catch (_: Exception) {}

                // logD(TAG, lpparam.packageName, "Plugin is ${factory.mComponentName}")
                // 仅备份当前可用注入 ClassLoader
                // miui.systemui.volume.VolumeDialogPlugin
                // miui.systemui.miplay.MiPlayPluginImpl
                // miui.systemui.quicksettings.LocalMiuiQSTilePlugin
                // miui.systemui.controlcenter.MiuiControlCenter
                // ↓
                // miui.systemui.notification.NotificationStatPluginImpl
                // miui.systemui.globalactions.GlobalActionsPlugin
                // miui.systemui.notification.FocusNotificationPluginImpl
                // miui.systemui.notification.unimportant.UnimportantSdkPluginImpl
            }
        }
    }

    private fun getTileList(): List<String> {
        val cardTiles =
            mPrefsMap.getString("systemui_plugin_card_tiles", "").replace("List_", "")

        return if (TextUtils.isEmpty(cardTiles.replace("List_", ""))) ArrayList()
        else listOf(*cardTiles.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
    }
}