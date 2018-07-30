package com.omarea.shell.units

import com.omarea.shared.Consts
import com.omarea.shell.SuDo
import java.util.*

/**
 * Created by Hello on 2017/11/01.
 */

class FlymeUnit {
    fun staticBlur(): Boolean {
        val commands = object : ArrayList<String>() {
            init {
                add("setprop persist.sys.static_blur_mode true")
                add(Consts.MountSystemRW)
                add("busybox sed 's/^persist.sys.static_blur_mode=.*/persist.sys.static_blur_mode=true/' /system/build.prop > /data/build.prop;")
                add("cp /data/build.prop /system/build.prop\n")
                add("rm /data/build.prop\n")
                add("chmod 0644 /system/build.prop\n")
                add(Consts.Reboot)
            }
        }
        return SuDo.execCmdSync(commands)
    }
}
