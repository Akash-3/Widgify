package com.widgify.service.battery;

public class BatteryInfo {
    private final int level;
    private final boolean available;
    private final String status;

    public BatteryInfo(int level, boolean available, String status) {
        this.level = level;
        this.available = available;
        this.status = status != null ? status : "Unknown";
    }

    public int getLevel() {
        return level;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getStatus() {
        return status;
    }
}
