package com.widgify.service.battery;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MacBatteryProvider implements BatteryProvider {

    private static final Pattern BATT_PATTERN = Pattern.compile("(\\d+)%");

    @Override
    public BatteryInfo getBatteryInfo() {
        try {
            ProcessBuilder pb = new ProcessBuilder("pmset", "-g", "batt");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(1500, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new BatteryInfo(-1, false, "No Battery");
            }

            int level = -1;
            String status = "Discharging";

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(";")) {
                        Matcher m = BATT_PATTERN.matcher(line);
                        if (m.find()) {
                            level = Integer.parseInt(m.group(1));
                        }
                        if (line.toLowerCase().contains("charging")) {
                            status = "Charging";
                        } else if (line.toLowerCase().contains("charged")) {
                            status = "Fully Charged";
                        }
                    }
                }
            }

            if (level >= 0) {
                return new BatteryInfo(level, true, status);
            }
        } catch (Exception ignored) {
        }

        return new BatteryInfo(-1, false, "No Battery");
    }
}
