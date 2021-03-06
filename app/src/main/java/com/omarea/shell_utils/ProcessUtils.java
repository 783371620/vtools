package com.omarea.shell_utils;

import android.content.Context;
import android.util.Log;

import com.omarea.common.shared.FileWrite;
import com.omarea.common.shell.KeepShellPublic;
import com.omarea.common.shell.KernelProrp;
import com.omarea.model.ProcessInfo;
import com.omarea.vtools.R;

import java.io.File;
import java.util.ArrayList;

public class ProcessUtils {

    /*
    VSS- Virtual Set Size 虚拟耗用内存（包含共享库占用的内存）
    RSS- Resident Set Size 实际使用物理内存（包含共享库占用的内存）
    PSS- Proportional Set Size 实际使用的物理内存（比例分配共享库占用的内存）
    USS- Unique Set Size 进程独自占用的物理内存（不包含共享库占用的内存）
    一般来说内存占用大小有如下规律：VSS >= RSS >= PSS >= USS
    ————————————————
    版权声明：本文为CSDN博主「火山石」的原创文章，遵循 CC 4.0 BY-SA 版权协议，转载请附上原文出处链接及本声明。
    原文链接：https://blog.csdn.net/zhangcanyan/java/article/details/84556808
    */

    // pageSize 获取 : getconf PAGESIZE

    private static String PS_COMMAND = null;

    // 兼容性检查
    public boolean supported(Context context) {
        if (PS_COMMAND == null) {
            PS_COMMAND = "";
            String installPath = context.getString(R.string.toolkit_install_path);
            String toyboxInstallPath = installPath + "/toybox-outside";
            String outsideToybox = FileWrite.INSTANCE.getPrivateFilePath(context, toyboxInstallPath);

            if (!new File(outsideToybox).exists()) {
                FileWrite.INSTANCE.writePrivateFile(context.getAssets(), "toolkit/toybox-outside", toyboxInstallPath, context);
            }

            String insideCmd = "ps -e -o %CPU,RSS,NAME,PID,USER,COMMAND,CMDLINE";
            String outsideCmd = outsideToybox + " " + insideCmd;
            Log.d(">>>>", outsideCmd);

            for (String cmd : new String[]{ insideCmd, outsideCmd }) {
                String[] rows = KeepShellPublic.INSTANCE.doCmdSync(cmd + " 2>&1").split("\n");
                String result = rows[0];
                if (rows.length > 10 && !(result.contains("bad -o") || result.contains("Unknown option") || result.contains("bad"))) {
                    PS_COMMAND = cmd;
                    break;
                }
            }
        }

        return !PS_COMMAND.isEmpty();
    }

    // 解析单行数据
    private ProcessInfo readRow(String row) {
        String[] columns = row.split(" +");
        if (columns.length >= 6) {
            try {
                ProcessInfo processInfo = new ProcessInfo();
                processInfo.cpu = Float.parseFloat(columns[0]);
                processInfo.rss = Integer.parseInt(columns[1]);
                processInfo.name = columns[2];
                processInfo.pid = Integer.parseInt(columns[3]);
                processInfo.user = columns[4];
                processInfo.command = columns[5];
                processInfo.cmdline = row.substring(row.indexOf(processInfo.command) + processInfo.command.length()).trim();
                return processInfo;
            } catch (Exception ex) {
                Log.e("Scene-Process", "" + ex.getMessage());
            }
        } else {
            Log.e("Scene-Process", "" + row);
        }
        return null;
    }

    // 获取所有进程
    public ArrayList<ProcessInfo> getAllProcess() {
        ArrayList<ProcessInfo> processInfoList = new ArrayList<>();
        boolean isFristRow = true;
        for (String row : KeepShellPublic.INSTANCE.doCmdSync(PS_COMMAND).split("\n")) {
            if (isFristRow) {
                isFristRow = false;
                continue;
            }

            ProcessInfo processInfo = readRow(row.trim());
            if (processInfo != null) {
                processInfoList.add(processInfo);
            }
        }
        return processInfoList;
    }

    // 获取进程详情
    public ProcessInfo getProcessDetail(int pid) {
        // Log.d("Scene-Process", PS_COMMAND + " --pid " + pid);

        String[] rows = KeepShellPublic.INSTANCE.doCmdSync(PS_COMMAND + " --pid " + pid).split("\n");
        if (rows.length > 1) {
            ProcessInfo row = readRow(rows[1].trim());
            if (row != null) {
                row.cpuSet = KernelProrp.INSTANCE.getProp("/proc/" + pid + "/cpuset");
            }
            return row;
        }
        return null;
    }

    // 强制结束进程
    public void killProcess(int pid) {
        KeepShellPublic.INSTANCE.doCmdSync("kill -9 " + pid);
    }

    private boolean isAndroidProcess(ProcessInfo processInfo) {
        return (processInfo.command.contains("app_process") && processInfo.name.matches(".*\\..*"));
    }


    // 强制结束进程
    public void killProcess(ProcessInfo processInfo) {
        if (isAndroidProcess(processInfo)) {
            String packageName = processInfo.name.contains(":") ? processInfo.name.substring(0, processInfo.name.indexOf(":")) : processInfo.name;
            KeepShellPublic.INSTANCE.doCmdSync(String.format("killall -9 %s;am force-stop %s", packageName, packageName));
        } else {
            killProcess(processInfo.pid);
        }
    }
}
