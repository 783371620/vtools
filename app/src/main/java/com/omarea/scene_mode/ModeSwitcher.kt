package com.omarea.scene_mode

import com.omarea.Scene
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KeepShellPublic
import com.omarea.shell_utils.PropsUtils
import com.omarea.store.CpuConfigStorage
import com.omarea.vtools.R

/**
 * Created by Hello on 2018/06/03.
 */

open class ModeSwitcher {
    private var inited = false

    companion object {
        const val OUTSIDE_POWER_CFG_PATH = "/data/powercfg.sh"
        const val OUTSIDE_POWER_CFG_BASE = "/data/powercfg-base.sh"

        internal var DEFAULT = "balance"
        internal var POWERSAVE = "powersave"
        internal var PERFORMANCE = "performance"
        internal var FAST = "fast"
        internal var BALANCE = "balance"
        internal var IGONED = "igoned"
        private var INIT = "init"

        internal fun getModName(mode: String): String {
            when (mode) {
                POWERSAVE -> return "省电模式"
                PERFORMANCE -> return "性能模式"
                FAST -> return "极速模式"
                BALANCE -> return "均衡模式"
                IGONED -> return "保持状态"
                "" -> return "全局默认"
                else -> return "未知模式"
            }
        }

        private var currentPowercfg: String = ""
        private var currentPowercfgApp: String = ""
    }

    internal fun getModIcon(mode: String): Int {
        when (mode) {
            POWERSAVE -> return R.drawable.p1
            BALANCE -> return R.drawable.p2
            PERFORMANCE -> return R.drawable.p3
            FAST -> return R.drawable.p4
            else -> return R.drawable.p3
        }
    }

    internal fun getModImage(mode: String): Int {
        return when (mode) {
            POWERSAVE -> R.drawable.shortcut_p0
            BALANCE -> R.drawable.shortcut_p1
            PERFORMANCE -> R.drawable.shortcut_p2
            FAST -> R.drawable.shortcut_p3
            else -> R.drawable.shortcut_p2
        }
    }

    fun getCurrentPowerMode(): String {
        if (!currentPowercfg.isEmpty()) {
            return currentPowercfg
        }
        return PropsUtils.getProp("vtools.powercfg")
    }

    internal fun getCurrentPowermodeApp(): String {
        if (!currentPowercfgApp.isEmpty()) {
            return currentPowercfgApp
        }
        return PropsUtils.getProp("vtools.powercfg_app")
    }

    internal fun setCurrent(powerCfg: String, app: String): ModeSwitcher {
        setCurrentPowercfg(powerCfg)
        setCurrentPowercfgApp(app)
        return this
    }

    internal fun setCurrentPowercfg(powerCfg: String): ModeSwitcher {
        currentPowercfg = powerCfg
        PropsUtils.setPorp("vtools.powercfg", powerCfg)
        return this
    }

    internal fun setCurrentPowercfgApp(app: String): ModeSwitcher {
        currentPowercfgApp = app
        PropsUtils.setPorp("vtools.powercfg_app", app)
        return this
    }

    private fun keepShellExec(cmd: String) {
        KeepShellPublic.doCmdSync(cmd)
    }

    private var configProvider: String = ""

    // init
    internal fun initPowercfg(): ModeSwitcher {
        if (configProvider.isEmpty()) {
            val installer = CpuConfigInstaller()
            if (installer.outsideConfigInstalled()) {
                configProvider = OUTSIDE_POWER_CFG_PATH
            } else {
                configProvider = FileWrite.getPrivateFilePath(Scene.context, "powercfg.sh")
            }
        }

        if (configProvider.isNotEmpty()) {
            keepShellExec("sh $configProvider $INIT")
            setCurrentPowercfg("")
        }

        inited = true
        return this
    }

    // 切换模式
    internal fun executePowercfgMode(mode: String): ModeSwitcher {
        if (!inited) {
            initPowercfg()
        }

        val cpuConfigStorage = CpuConfigStorage(Scene.context)
        if (cpuConfigStorage.exists(mode)) {
            cpuConfigStorage.applyCpuConfig(Scene.context, mode)
        } else if (configProvider.isNotEmpty()) {
            keepShellExec("sh $configProvider $mode")
        } else {
            return this
        }
        setCurrentPowercfg(mode)
        return this
    }

    internal fun executePowercfgMode(mode: String, app: String): ModeSwitcher {
        executePowercfgMode(mode)
        setCurrentPowercfgApp(app)
        return this
    }

    // 是否已经完成指定模式的自定义
    public fun modeReplaced(mode: String): Boolean {
        return CpuConfigStorage(Scene.context).exists(mode)
    }

    // 是否已完成四个模式的配置
    public fun modeConfigCompleted(): Boolean {
        return ((CpuConfigInstaller().insideConfigInstalled() || CpuConfigInstaller().outsideConfigInstalled()) && !anyModeReplaced()) || allModeReplaced()
    }

    // 是否已经完成所有模式的自定义
    public fun allModeReplaced(): Boolean {
        val storage = CpuConfigStorage(Scene.context)

        return storage.exists(POWERSAVE) &&
                storage.exists(BALANCE) &&
                storage.exists(PERFORMANCE) &&
                storage.exists(FAST)
    }

    public fun anyModeReplaced(): Boolean {
        val storage = CpuConfigStorage(Scene.context)

        return storage.exists(POWERSAVE) ||
                storage.exists(BALANCE) ||
                storage.exists(PERFORMANCE) ||
                storage.exists(FAST)
    }
}
