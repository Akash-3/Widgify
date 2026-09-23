package com.widgify.service.battery;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * WindowsBatteryProvider — queries real battery data on Windows.
 *
 * Strategy (in order of attempt):
 *   1. PowerShell Get-WmiObject Win32_Battery  (primary — works on Win10/11)
 *   2. WMIC path Win32_Battery                 (fallback — deprecated but kept)
 *   3. PowerShell Get-CimInstance Win32_Battery (last resort)
 *
 * If all methods fail (desktop PC, VM, no battery driver) returns
 * BatteryInfo(-1, false, "No Battery").
 */
public class WindowsBatteryProvider implements BatteryProvider {

    @Override
    public BatteryInfo getBatteryInfo() {

        BatteryInfo result = tryPowerShellCim();

        if (result != null && result.isAvailable()) {
            return result;
        }

        result = tryPowerShellWmi();

        if (result != null && result.isAvailable()) {
            return result;
        }

        result = tryWmic();

        if (result != null && result.isAvailable()) {
            return result;
        }

        return new BatteryInfo(-1, false, "No Battery");
    }

    /**
     * PowerShell: (Get-WmiObject -Class Win32_Battery)
     * Outputs: LEVEL=<n> STATUS=<n>
     */
    private BatteryInfo tryPowerShellWmi() {
        String script =
            "$b = Get-WmiObject -Class Win32_Battery -ErrorAction SilentlyContinue;" +
            "if ($b) { Write-Output ('LEVEL=' + $b.EstimatedChargeRemaining + ' STATUS=' + $b.BatteryStatus) }";
        return runPowerShell(script);
    }

    /**
     * PowerShell: Get-CimInstance (Win11 replacement for WMI)
     */
    private BatteryInfo tryPowerShellCim() {
        String script =
           "$b = Get-CimInstance Win32_Battery " +
            "-ErrorAction SilentlyContinue | Select-Object -First 1; " +
            "if ($null -ne $b) { " +
            "Write-Output ('LEVEL=' + $b.EstimatedChargeRemaining + " +
            "' STATUS=' + $b.BatteryStatus) " +
            "}";
        return runPowerShell(script);
    }

    private BatteryInfo runPowerShell(String script) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe", "-NoProfile", "-NonInteractive",
                "-ExecutionPolicy", "Bypass",
                "-Command", script
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(3000, TimeUnit.MILLISECONDS);
            if (!finished) { process.destroyForcibly(); return null; }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("LEVEL=")) {
                        return parseLevelStatusLine(line);
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /** Legacy WMIC /format:list fallback */
    private BatteryInfo tryWmic() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "cmd.exe", "/c",
                "wmic path Win32_Battery get EstimatedChargeRemaining,BatteryStatus /format:list"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(2000, TimeUnit.MILLISECONDS);
            if (!finished) { process.destroyForcibly(); return null; }

            int level = -1, statusCode = -1;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("EstimatedChargeRemaining=")) {
                        String val = line.substring("EstimatedChargeRemaining=".length()).trim();
                        if (!val.isEmpty()) { try { level = Integer.parseInt(val); } catch (Exception ignored) {} }
                    } else if (line.startsWith("BatteryStatus=")) {
                        String val = line.substring("BatteryStatus=".length()).trim();
                        if (!val.isEmpty()) { try { statusCode = Integer.parseInt(val); } catch (Exception ignored) {} }
                    }
                }
            }
            if (level >= 0) return new BatteryInfo(level, true, resolveStatus(statusCode));
        } catch (Exception ignored) {}
        return null;
    }

    private BatteryInfo parseLevelStatusLine(String line) {
        // Format: LEVEL=78 STATUS=2
        try {
            int level = -1, statusCode = -1;
            String[] parts = line.split(" ");
            for (String part : parts) {
                if (part.startsWith("LEVEL=")) {
                    String v = part.substring(6).trim();
                    if (!v.isEmpty() && !v.equals("")) level = Integer.parseInt(v);
                } else if (part.startsWith("STATUS=")) {
                    String v = part.substring(7).trim();
                    if (!v.isEmpty() && !v.equals("")) statusCode = Integer.parseInt(v);
                }
            }
            if (level >= 0 && level <= 100) {
                return new BatteryInfo(level, true, resolveStatus(statusCode));
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String resolveStatus(int statusCode) {
        switch (statusCode) {
            case 2: case 6: case 7: case 8: case 9: return "Charging";
            case 3: return "Fully Charged";
            case 1: return "Discharging";
            default: return "";
        }
    }
}
