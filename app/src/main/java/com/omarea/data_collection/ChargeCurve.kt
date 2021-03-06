package com.omarea.data_collection

import android.content.Context
import android.os.BatteryManager
import com.omarea.store.ChargeSpeedStore
import com.omarea.store.SpfConfig
import java.util.*

class ChargeCurve(private val context: Context) : EventReceiver {
    private val storage = ChargeSpeedStore(context)
    private var timer: Timer? = null
    private var batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private var globalSPF = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

    override fun eventFilter(eventType: EventType): Boolean {
        when (eventType) {
            EventType.POWER_CONNECTED,
            EventType.POWER_DISCONNECTED,
            EventType.BATTERY_CHANGED -> {
                return true
            }
            else -> return false
        }
    }

    override fun onReceive(eventType: EventType) {
        when (eventType) {
            EventType.POWER_CONNECTED -> {
                val last = storage.lastCapacity();
                if (GlobalStatus.batteryCapacity != -1 && GlobalStatus.batteryCapacity != last) {
                    storage.clearAll()
                }
            }
            EventType.POWER_DISCONNECTED -> {
                cancelUpdate()
            }
            EventType.BATTERY_CHANGED -> {
                if (timer == null && GlobalStatus.batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
                    // storage.handleConflics(GlobalStatus.batteryCapacity)

                    startUpdate()
                }
            }
            else -> {
            }
        }
    }

    private fun startUpdate() {
        if (timer == null) {
            timer = Timer().apply {
                schedule(object : TimerTask() {
                    override fun run() {
                        saveLog()
                    }
                }, 15000, 1000)
            }
        }
    }

    private fun saveLog() {
        if (GlobalStatus.batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
            // 电流
            GlobalStatus.batteryCurrentNow = (
                    batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) /
                            globalSPF.getInt(SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT, SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT_DEFAULT)
                    )

            if (Math.abs(GlobalStatus.batteryCurrentNow) > 100) {
                storage.addHistory(GlobalStatus.batteryCurrentNow, GlobalStatus.batteryCapacity, GlobalStatus.batteryTemperature)
            }
        } else {
            cancelUpdate()
        }
    }

    private fun cancelUpdate() {
        timer?.run {
            cancel()
            timer = null
        }
    }
}