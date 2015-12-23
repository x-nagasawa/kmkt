package com.github.kmkt.util;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

/**
 * 
 */
public class MXBeanUtil {
    /**
     * ホスト名を取得する<br>
     * {@link RuntimeMXBean#getName()} が返す PID@hostname 形式に依存
     * @return ホスト名
     */
    public static String getHostnameViaMXBean() {
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        String vmName = bean.getName();
        return vmName.split("@")[1].trim();
    }

    /**
     * PID を取得する<br>
     * {@link RuntimeMXBean#getName()} が返す PID@hostname 形式に依存
     * @return PID
     */
    public static long getPIDViaMXBean() {
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        String vmName = bean.getName();
        long pid = Long.valueOf(vmName.split("@")[0]);
        return pid;
    }

}
