package com.widgify.model;

public class SystemHealth {
    private double cpuUsage;
    private double memoryUsage;
    private String memoryUsed;
    private String memoryTotal;
    private int batteryLevel;
    private boolean batteryAvailable;
    private String batteryStatus;

    public SystemHealth() {
    }

    public SystemHealth(double cpuUsage, double memoryUsage, String memoryUsed, String memoryTotal, 
                        int batteryLevel, boolean batteryAvailable, String batteryStatus) {
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.memoryUsed = memoryUsed;
        this.memoryTotal = memoryTotal;
        this.batteryLevel = batteryLevel;
        this.batteryAvailable = batteryAvailable;
        this.batteryStatus = batteryStatus;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public double getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(double memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public String getMemoryUsed() {
        return memoryUsed;
    }

    public void setMemoryUsed(String memoryUsed) {
        this.memoryUsed = memoryUsed;
    }

    public String getMemoryTotal() {
        return memoryTotal;
    }

    public void setMemoryTotal(String memoryTotal) {
        this.memoryTotal = memoryTotal;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public boolean isBatteryAvailable() {
        return batteryAvailable;
    }

    public void setBatteryAvailable(boolean batteryAvailable) {
        this.batteryAvailable = batteryAvailable;
    }

    public String getBatteryStatus() {
        return batteryStatus;
    }

    public void setBatteryStatus(String batteryStatus) {
        this.batteryStatus = batteryStatus;
    }
}
