package com.omarea.vboot

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.Spinner
import com.omarea.shell.cpucontrol.CpuFrequencyUtils
import com.omarea.shell.cpucontrol.ThermalControlUtils
import com.omarea.ui.StringAdapter
import kotlinx.android.synthetic.main.fragment_cpu_control.*
import java.util.*


class FragmentCpuControl : Fragment() {
    private var hasBigCore = false
    private var littleFreqs = arrayOf("")
    private var littleGovernor = arrayOf("")
    private var bigFreqs = arrayOf("")
    private var bigGovernor = arrayOf("")
    private var handler = Handler()
    private var coreCount = 0
    private var cores = arrayListOf<CheckBox>()
    private var exynosCpuhotplug = false;
    private var exynosHMP = false;

    private fun initData() {
        hasBigCore = CpuFrequencyUtils.getClusterInfo().size > 1
        littleFreqs = CpuFrequencyUtils.getAvailableFrequencies(0)
        littleGovernor = CpuFrequencyUtils.getAvailableGovernors(0)
        bigFreqs = CpuFrequencyUtils.getAvailableFrequencies(1)
        bigGovernor = CpuFrequencyUtils.getAvailableGovernors(1)
        coreCount = CpuFrequencyUtils.getCoreCount()
        exynosCpuhotplug = CpuFrequencyUtils.exynosCpuhotplugSupport()
        exynosHMP = CpuFrequencyUtils.exynosHMP()

        handler.post {
            try {
                if (!hasBigCore) {
                    big_core_configs.visibility = View.GONE
                }
                cluster_little_min_freq.adapter = StringAdapter(this.context, littleFreqs)
                cluster_little_max_freq.adapter = StringAdapter(this.context, littleFreqs)
                cluster_little_governor.adapter = StringAdapter(this.context, littleGovernor)

                if (hasBigCore) {
                    cluster_big_min_freq.adapter = StringAdapter(this.context, bigFreqs)
                    cluster_big_max_freq.adapter = StringAdapter(this.context, bigFreqs)
                    cluster_big_governor.adapter = StringAdapter(this.context, bigGovernor)
                }

                exynos_cpuhotplug.isEnabled = exynosCpuhotplug
                exynos_hmp_up.isEnabled = exynosHMP
                exynos_hmp_down.isEnabled = exynosHMP
                exynos_hmp_booster.isEnabled = exynosHMP

                cores = arrayListOf<CheckBox>(core_0, core_1, core_2, core_3, core_4, core_5, core_6, core_7)
                if (coreCount > cores.size) coreCount = cores.size;
                for (i in 0..cores.size - 1) {
                    if (i >= coreCount) {
                        cores[i].isEnabled = false
                    }
                }
            } catch (ex: Exception) {

            }
        }
    }

    /*
    * 获得近似值
    */
    private fun getApproximation(arr: Array<String>, value: String): String {
        if (arr.contains(value)) {
            return value
        } else {
            var approximation = if (arr.size > 0) arr[0] else ""
            for (item in arr) {
                if (item <= value) {
                    approximation = item
                } else {
                    break
                }
            }

            return approximation
        }
    }

    @SuppressLint("InflateParams")
    private fun bindEvent() {
        thermal_core_control.setOnCheckedChangeListener({ buttonView, isChecked ->
            ThermalControlUtils.setCoreControlState(isChecked, this.context)
        })
        thermal_vdd.setOnCheckedChangeListener({ buttonView, isChecked ->
            ThermalControlUtils.setVDDRestrictionState(isChecked, this.context)
        })
        thermal_paramters.setOnCheckedChangeListener({ buttonView, isChecked ->
            ThermalControlUtils.setTheramlState(isChecked, this.context)
        })
        cpu_sched_boost.setOnCheckedChangeListener({ buttonView, isChecked ->
            CpuFrequencyUtils.setSechedBoostState(isChecked, this.context)
        })

        val next = Runnable {
        }

        cluster_little_min_freq.onItemSelectedListener = ItemSelected(R.id.cluster_little_min_freq, next)
        cluster_little_max_freq.onItemSelectedListener = ItemSelected(R.id.cluster_little_max_freq, next)
        cluster_little_governor.onItemSelectedListener = ItemSelected(R.id.cluster_little_governor, next)

        if (hasBigCore) {
            cluster_big_min_freq.onItemSelectedListener = ItemSelected(R.id.cluster_big_min_freq, next)
            cluster_big_max_freq.onItemSelectedListener = ItemSelected(R.id.cluster_big_max_freq, next)
            cluster_big_governor.onItemSelectedListener = ItemSelected(R.id.cluster_big_governor, next)
        }

        cpu_inputboost_freq.setOnClickListener({ view ->
            val dialogView = layoutInflater.inflate(R.layout.cpu_inputboost_dialog, null)
            val currentValues = cpu_inputboost_freq.text.toString().split(" ")

            val clusterInfos = CpuFrequencyUtils.getClusterInfo()
            if (clusterInfos.size == 0)
                return@setOnClickListener

            val cluster0 = dialogView.findViewById<Spinner>(R.id.boost_cluster0)
            val cluster1 = dialogView.findViewById<Spinner>(R.id.boost_cluster1)
            cluster0.adapter = StringAdapter(context, littleFreqs)
            for (value in currentValues) {
                for (cpu in clusterInfos.get(0)) {
                    if (value.startsWith(cpu + ":")) {
                        cluster0.setSelection(littleFreqs.indexOf(value.substring(2, value.length)), true)
                    }
                }
            }

            if (clusterInfos.size == 1) {
                cluster1.isEnabled = false
            } else {
                cluster1.adapter = StringAdapter(context, bigFreqs)
                for (value in currentValues) {
                    for (cpu in clusterInfos.get(1)) {
                        if (value.startsWith(cpu + ":")) {
                            cluster1.setSelection(bigFreqs.indexOf(value.substring(2, value.length)), true)
                        }
                    }
                }
            }

            AlertDialog.Builder(context)
                    .setTitle("input_boost_freq")
                    .setView(dialogView)
                    .setNegativeButton("确定", { dialog, which ->
                        val config = StringBuilder()
                        val c0 = cluster0.selectedItem.toString()
                        val c1 = cluster1.selectedItem.toString()
                        for (core in clusterInfos.get(0)) {
                            config.append(core)
                            config.append(":")
                            config.append(c0)
                            config.append(" ")
                        }
                        if (clusterInfos.size > 1) {
                            for (core in clusterInfos.get(1)) {
                                config.append(core)
                                config.append(":")
                                config.append(c1)
                                config.append(" ")
                            }
                        }
                        CpuFrequencyUtils.setInputBoosterFreq(config.toString().trim())
                        updateState()
                    })
                    .create()
                    .show()
        })
        cpu_inputboost_time.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                CpuFrequencyUtils.setInputBoosterTime(v.text.toString())
                val imm = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                // 得到InputMethodManager的实例
                if (imm.isActive()) {
                    // 如果开启
                    imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS)
                }
            }
            true
        }
        for (i in 0..cores.size - 1) {
            val core = i
            cores[core].setOnClickListener {
                CpuFrequencyUtils.setCoreOnlineState(core, (it as CheckBox).isChecked)
            }
        }

        exynos_cpuhotplug.setOnClickListener {
            CpuFrequencyUtils.setExynosHotplug((it as CheckBox).isChecked)
        }
        exynos_hmp_booster.setOnClickListener {
            CpuFrequencyUtils.setExynosBooster((it as CheckBox).isChecked)
        }
        exynos_hmp_up.setOnSeekBarChangeListener(OnSeekBarChangeListener(true))
        exynos_hmp_down.setOnSeekBarChangeListener(OnSeekBarChangeListener(false))
    }

    class OnSeekBarChangeListener(private var up: Boolean) : SeekBar.OnSeekBarChangeListener {
        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            if (seekBar != null) {
                if (up)
                    CpuFrequencyUtils.setExynosHmpUP(seekBar.progress)
                else
                    CpuFrequencyUtils.setExynosHmpDown(seekBar.progress)
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        @SuppressLint("ApplySharedPref")
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        }
    }

    class ItemSelected(private val spinner: Int, next: Runnable) : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            try {
                if (parent == null)
                    return

                val freq = parent.selectedItem.toString()
                when (spinner) {
                    R.id.cluster_little_min_freq -> {
                        if (CpuFrequencyUtils.getCurrentMinFrequency(0) == freq) {
                            return
                        }
                        CpuFrequencyUtils.setMinFrequency(freq, 0, parent.context)
                    }
                    R.id.cluster_little_max_freq -> {
                        if (CpuFrequencyUtils.getCurrentMaxFrequency(0) == freq) {
                            return
                        }
                        CpuFrequencyUtils.setMaxFrequency(freq, 0, parent.context)
                    }
                    R.id.cluster_little_governor -> {
                        if (CpuFrequencyUtils.getCurrentScalingGovernor(0) == freq) {
                            return
                        }
                        CpuFrequencyUtils.setGovernor(freq, 0, parent.context)
                    }
                    R.id.cluster_big_min_freq -> {
                        if (CpuFrequencyUtils.getCurrentMinFrequency(1) == freq) {
                            return
                        }
                        CpuFrequencyUtils.setMinFrequency(freq, 1, parent.context)
                    }
                    R.id.cluster_big_max_freq -> {
                        if (CpuFrequencyUtils.getCurrentMaxFrequency(1) == freq) {
                            return
                        }
                        CpuFrequencyUtils.setMaxFrequency(freq, 1, parent.context)
                    }
                    R.id.cluster_big_governor -> {
                        if (CpuFrequencyUtils.getCurrentScalingGovernor(1) == freq) {
                            return
                        }
                        CpuFrequencyUtils.setGovernor(freq, 1, parent.context)
                    }
                }
            } catch (ex: Exception) {
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    class Status {
        var cluster_little_min_freq = -1;
        var cluster_little_max_freq = -1;
        var cluster_little_governor = -1;

        var cluster_big_min_freq = -1;
        var cluster_big_max_freq = -1;
        var cluster_big_governor = -1;

        var coreControl = ""
        var vdd = ""
        var msmThermal = ""
        var boost = ""
        var boostFreq = ""
        var boostTime = ""
        var coreOnline = arrayListOf<Boolean>()

        var exynosHmpUP = 0;
        var exynosHmpDown = 0;
        var exynosHmpBooster = false;
        var exynosHotplug = false;
    }

    private var status = Status()

    private fun updateState() {
        Thread(Runnable {
            try {
                status.cluster_little_min_freq = littleFreqs.indexOf(getApproximation(littleFreqs, CpuFrequencyUtils.getCurrentMinFrequency(0)))
                status.cluster_little_max_freq = littleFreqs.indexOf(getApproximation(littleFreqs, CpuFrequencyUtils.getCurrentMaxFrequency(0)))
                status.cluster_little_governor = littleGovernor.indexOf(CpuFrequencyUtils.getCurrentScalingGovernor(0))
                status.coreControl = ThermalControlUtils.getCoreControlState()
                status.vdd = ThermalControlUtils.getVDDRestrictionState()
                status.msmThermal = ThermalControlUtils.getTheramlState()
                status.boost = CpuFrequencyUtils.getSechedBoostState()
                status.boostFreq = CpuFrequencyUtils.getInputBoosterFreq()
                status.boostTime = CpuFrequencyUtils.getInputBoosterTime()
                status.exynosHmpUP = CpuFrequencyUtils.getExynosHmpUP()
                status.exynosHmpDown = CpuFrequencyUtils.getExynosHmpDown()
                status.exynosHmpBooster = CpuFrequencyUtils.getExynosBooster()
                status.exynosHotplug = CpuFrequencyUtils.getExynosHotplug()

                if (hasBigCore) {
                    status.cluster_big_min_freq = bigFreqs.indexOf(getApproximation(bigFreqs, CpuFrequencyUtils.getCurrentMinFrequency(1)))
                    status.cluster_big_max_freq = bigFreqs.indexOf(getApproximation(bigFreqs, CpuFrequencyUtils.getCurrentMaxFrequency(1)))
                    status.cluster_big_governor = bigGovernor.indexOf(CpuFrequencyUtils.getCurrentScalingGovernor(1))
                }

                status.coreOnline = arrayListOf()
                for (i in 0..coreCount - 1) {
                    status.coreOnline.add(CpuFrequencyUtils.getCoreOnlineState(i))
                }

                handler.post {
                    updateUI()
                }
            } catch (ex: Exception) {
            }
        }).start()
    }

    private fun updateUI() {
        try {
            cluster_little_min_freq.setSelection(status.cluster_little_min_freq, true)
            cluster_little_max_freq.setSelection(status.cluster_little_max_freq, true)
            cluster_little_governor.setSelection(status.cluster_little_governor, true)

            if (hasBigCore) {
                cluster_big_min_freq.setSelection(status.cluster_big_min_freq, true)
                cluster_big_max_freq.setSelection(status.cluster_big_max_freq, true)
                cluster_big_governor.setSelection(status.cluster_big_governor, true)
            }

            if (status.coreControl.isEmpty()) {
                thermal_core_control.isEnabled = false
            }
            thermal_core_control.isChecked = status.coreControl == "1"

            if (status.vdd.isEmpty()) {
                thermal_vdd.isEnabled = false
            }
            thermal_vdd.isChecked = status.vdd == "1"


            if (status.msmThermal.isEmpty()) {
                thermal_paramters.isEnabled = false
            }
            thermal_paramters.isChecked = status.msmThermal == "Y"

            if (status.boost.isEmpty()) {
                cpu_sched_boost.isEnabled = false
            }
            cpu_sched_boost.isChecked = status.boost == "1"

            exynos_hmp_down.setProgress(status.exynosHmpDown)
            exynos_hmp_down_text.setText(status.exynosHmpDown.toString())
            exynos_hmp_up.setProgress(status.exynosHmpUP)
            exynos_hmp_up_text.setText(status.exynosHmpUP.toString())
            exynos_cpuhotplug.isChecked = status.exynosHotplug
            exynos_hmp_booster.isChecked = status.exynosHmpBooster

            if (status.boostFreq.isEmpty()) {
                cpu_inputboost_freq.isEnabled = false
            }
            cpu_inputboost_freq.setText(status.boostFreq)

            if (status.boostTime.isEmpty()) {
                cpu_inputboost_time.isEnabled = false
            }
            if (!cpu_inputboost_time.isFocused)
                cpu_inputboost_time.setText(status.boostTime)

            for (i in 0..coreCount - 1) {
                cores[i].isChecked = status.coreOnline.get(i)
            }
        } catch (ex: Exception) {
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Thread(Runnable {
            initData()
            updateState()
            handler.postDelayed({
                bindEvent()
            }, 1000)
        }).start()
    }

    private var timer: Timer? = null
    override fun onResume() {
        super.onResume()
        if (timer == null) {
            timer = Timer()
            timer!!.schedule(object : TimerTask() {
                override fun run() {
                    handler.post({
                        updateState()
                    })
                }
            }, 3000, 3000)
        }
    }

    override fun onPause() {
        super.onPause()
        cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    private fun cancel() {
        try {
            if (timer != null) {
                timer!!.cancel()
                timer = null
            }
        } catch (ex: Exception) {

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_cpu_control, container, false)
    }

    companion object {
        fun newInstance(): FragmentCpuControl {
            val fragment = FragmentCpuControl()
            return fragment
        }
    }
}