package com.widgify.service.launcher;

import com.widgify.model.AppShortcut;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MacAppLauncher implements AppLauncher {

    private final List<AppShortcut> shortcuts = Arrays.asList(
        new AppShortcut("vscode", "VS Code", "💻"),
        new AppShortcut("chrome", "Chrome", "🌐"),
        new AppShortcut("terminal", "Terminal", "⚡"),
        new AppShortcut("notepad", "TextEdit", "📝"),
        new AppShortcut("explorer", "Finder", "📁"),
        new AppShortcut("calculator", "Calculator", "🧮")
    );

    @Override
    public List<AppShortcut> getSupportedShortcuts() {
        return new ArrayList<>(shortcuts);
    }

    @Override
    public LaunchResult launchApp(String appId) {
        if (appId == null) {
            return new LaunchResult(false, "Invalid application ID.");
        }

        switch (appId.toLowerCase().trim()) {
            case "vscode":
                return launchMacApp("Visual Studio Code", "VS Code");

            case "chrome":
                return launchMacApp("Google Chrome", "Chrome");

            case "terminal":
                return launchMacApp("Terminal", "Terminal");

            case "notepad":
                return launchMacApp("TextEdit", "TextEdit");

            case "explorer":
                return launchMacApp("Finder", "Finder");

            case "calculator":
                return launchMacApp("Calculator", "Calculator");

            default:
                return new LaunchResult(false, "Application ID '" + appId + "' is not recognized.");
        }
    }

    private LaunchResult launchMacApp(String macAppName, String displayName) {
        // Check if app exists in /Applications or /System/Applications
        File appDir = new File("/Applications/" + macAppName + ".app");
        File sysAppDir = new File("/System/Applications/" + macAppName + ".app");
        File sysUtilDir = new File("/System/Applications/Utilities/" + macAppName + ".app");

        boolean exists = appDir.exists() || sysAppDir.exists() || sysUtilDir.exists();

        try {
            new ProcessBuilder("open", "-a", macAppName).start();
            return new LaunchResult(true, displayName + " launched.");
        } catch (Exception e) {
            if (!exists) {
                return new LaunchResult(false, displayName + " is not installed or could not be found.");
            }
            return new LaunchResult(false, "Failed to launch " + displayName + ": " + e.getMessage());
        }
    }
}
