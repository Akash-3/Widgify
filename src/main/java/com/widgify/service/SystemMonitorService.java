package com.widgify.service;

import com.widgify.model.SystemHealth;
import com.widgify.service.battery.BatteryInfo;
import com.widgify.service.battery.BatteryProvider;
import com.widgify.service.battery.MacBatteryProvider;
import com.widgify.service.battery.NullBatteryProvider;
import com.widgify.service.battery.WindowsBatteryProvider;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public class SystemMonitorService {

    private final BatteryProvider batteryProvider;

    public SystemMonitorService() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            this.batteryProvider = new WindowsBatteryProvider();
        } else if (os.contains("mac")) {
            this.batteryProvider = new MacBatteryProvider();
        } else {
            this.batteryProvider = new NullBatteryProvider();
        }
    }

    public SystemHealth getSystemHealth() {
        double cpuUsage = 0.0;
        double memoryUsage = 0.0;
        String memoryUsedStr = "N/A";
        String memoryTotalStr = "N/A";

        try {
            OperatingSystemMXBean mxBean = ManagementFactory.getOperatingSystemMXBean();
            if (mxBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunBean = (com.sun.management.OperatingSystemMXBean) mxBean;

                // 1. CPU Usage
                double systemCpu = sunBean.getCpuLoad();
                if (systemCpu < 0) {
                    systemCpu = sunBean.getSystemCpuLoad();
                }
                if (systemCpu >= 0) {
                    cpuUsage = Math.round(systemCpu * 1000.0) / 10.0;
                }

                // 2. Physical Memory
                long totalMem = sunBean.getTotalMemorySize();
                long freeMem = sunBean.getFreeMemorySize();
                long usedMem = totalMem - freeMem;

                if (totalMem > 0) {
                    memoryUsage = Math.round(((double) usedMem / totalMem) * 1000.0) / 10.0;
                    memoryUsedStr = formatBytes(usedMem);
                    memoryTotalStr = formatBytes(totalMem);
                }
            }
        } catch (Exception ignored) {
        }

        // 3. Battery Telemetry
        BatteryInfo battInfo = batteryProvider.getBatteryInfo();

        return new SystemHealth(
            cpuUsage,
            memoryUsage,
            memoryUsedStr,
            memoryTotalStr,
            battInfo.getLevel(),
            battInfo.isAvailable(),
            battInfo.getStatus()
        );
    }

    private String formatBytes(long bytes) {
        double gb = bytes / (1024.0 * 1024.0 * 1024.0);
        if (gb >= 1.0) {
            return String.format("%.1f GB", gb);
        }
        double mb = bytes / (1024.0 * 1024.0);
        return String.format("%.0f MB", mb);
    }
}
