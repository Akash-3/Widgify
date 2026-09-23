package com.widgify.service.battery;

public class NullBatteryProvider implements BatteryProvider {
    @Override
    public BatteryInfo getBatteryInfo() {
        return new BatteryInfo(-1, false, "No Battery");
    }
}
