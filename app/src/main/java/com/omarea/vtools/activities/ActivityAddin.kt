package com.omarea.vtools.activities

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.SimpleAdapter
import com.omarea.common.ui.DialogHelper
import com.omarea.store.SpfConfig
import com.omarea.vtools.R
import com.omarea.vtools.addin.DexCompileAddin
import com.omarea.vtools.addin.FullScreenAddin
import com.omarea.vtools.dialogs.DialogAddinModifyDPI
import com.omarea.vtools.dialogs.DialogAddinModifydevice
import com.omarea.vtools.dialogs.DialogAddinWIFI
import com.omarea.vtools.dialogs.DialogCustomMAC
import kotlinx.android.synthetic.main.activity_addin.*
import java.util.*


class ActivityAddin : ActivityBase() {
    private fun createItem(title: String, desc: String, runnable: Runnable?, wran: Boolean = true): HashMap<String, Any> {
        val item = HashMap<String, Any>()
        item.put("Title", title)
        item.put("Desc", desc)
        item.put("Wran", wran)
        if (runnable != null)
            item.put("Action", runnable)
        return item
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addin)

        setBackArrow()

        initAddin(this.addin_system_listview)
    }

    private fun initAddin(view: View) {
        val activity = this
        val context = this
        val listItem = ArrayList<HashMap<String, Any>>().apply {
            add(createItem(getString(R.string.addin_fullscreen_on), getString(R.string.addin_fullscreen_on_desc), Runnable { FullScreenAddin(activity).fullScreen() }, false))

            add(createItem(getString(R.string.addin_wifi), getString(R.string.addin_wifi_desc), Runnable { DialogAddinWIFI(context).show() }, false))

            add(createItem(getString(R.string.addin_dpi), getString(R.string.addin_dpi_desc), Runnable { DialogAddinModifyDPI(context).modifyDPI(activity.windowManager.defaultDisplay, context) }, false))

            add(createItem(getString(R.string.addin_deviceinfo), getString(
                    R.string.addin_deviceinfo_desc),
                    Runnable {
                        DialogAddinModifydevice(context).modifyDeviceInfo()
                    },
                    false))
            add(createItem(getString(R.string.addin_mac),
                    getString(R.string.addin_mac_desc),
                    Runnable {
                        DialogCustomMAC(context).modifyMAC(SpfConfig.GLOBAL_SPF_MAC_AUTOCHANGE_MODE_1)
                    },
                    false))
            add(createItem(getString(R.string.addin_mac_2),
                    getString(R.string.addin_mac_desc_2),
                    Runnable {
                        DialogCustomMAC(context).modifyMAC(SpfConfig.GLOBAL_SPF_MAC_AUTOCHANGE_MODE_2)
                    },
                    false))

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                add(createItem(getString(R.string.addin_force_dex_compile), getString(R.string.addin_force_dex_compile_desc), Runnable { DexCompileAddin(context).run() }, false))
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(createItem(getString(R.string.addin_pm_dexopt), getString(R.string.addin_pm_dexopt_desc), Runnable { DexCompileAddin(context).modifyConfig() }, false))
            }
        }

        val mSimpleAdapter = SimpleAdapter(
                view.context, listItem,
                R.layout.list_item_action,
                arrayOf("Title", "Desc"),
                intArrayOf(R.id.Title, R.id.Desc)
        )
        addin_system_listview.adapter = mSimpleAdapter
        addin_system_listview.onItemClickListener = onActionClick
    }

    private var onActionClick = AdapterView.OnItemClickListener { parent, _, position, _ ->
        val item = parent.adapter.getItem(position) as HashMap<*, *>
        if (item.get("Wran") == false) {
            (item["Action"] as Runnable).run()
        } else {
            DialogHelper.animDialog(AlertDialog.Builder(this)
                    .setTitle(item["Title"].toString())
                    .setNegativeButton(R.string.btn_cancel, null)
                    .setPositiveButton(R.string.addin_continue) { _, _ ->
                        (item["Action"] as Runnable).run()
                    }
                    .setMessage(item["Desc"].toString()))
        }
    }

    override fun onResume() {
        super.onResume()
        title = getString(R.string.menu_sundry)
    }
}
