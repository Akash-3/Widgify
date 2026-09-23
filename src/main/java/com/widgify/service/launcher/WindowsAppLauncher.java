package com.widgify.service.launcher;

import com.widgify.model.AppShortcut;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WindowsAppLauncher implements AppLauncher {

    private final List<AppShortcut> shortcuts = Arrays.asList(
        new AppShortcut("vscode", "VS Code", "💻"),
        new AppShortcut("chrome", "Chrome", "🌐"),
        new AppShortcut("terminal", "Terminal", "⚡"),
        new AppShortcut("notepad", "Notepad", "📝"),
        new AppShortcut("explorer", "Explorer", "📁"),
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
                return launchVSCode();

            case "chrome":
                return launchChrome();

            case "terminal":
                return launchTerminal();

            case "notepad":
                return launchCommand("notepad.exe", "Notepad");

            case "explorer":
                return launchCommand("explorer.exe", "File Explorer");

            case "calculator":
                return launchCommand("calc.exe", "Calculator");

            default:
                return new LaunchResult(false, "Application ID '" + appId + "' is not recognized.");
        }
    }

    private LaunchResult launchVSCode() {
        String localAppData = System.getenv("LOCALAPPDATA");
        String programFiles = System.getenv("ProgramFiles");
        String programFilesX86 = System.getenv("ProgramFiles(x86)");

        List<String> candidates = new ArrayList<>();
        if (localAppData != null) candidates.add(localAppData + "\\Programs\\Microsoft VS Code\\Code.exe");
        if (programFiles != null) candidates.add(programFiles + "\\Microsoft VS Code\\Code.exe");
        if (programFilesX86 != null) candidates.add(programFilesX86 + "\\Microsoft VS Code\\Code.exe");

        for (String path : candidates) {
            if (new File(path).exists()) {
                return executePath(path, "VS Code");
            }
        }

        // Try 'code' via PATH
        if (isExecutableInPath("code") || isExecutableInPath("code.cmd")) {
            return executeCommand("cmd.exe", "/c", "start", "", "code");
        }

        return new LaunchResult(false, "VS Code is not installed or could not be found.");
    }

    private LaunchResult launchChrome() {
        String programFiles = System.getenv("ProgramFiles");
        String programFilesX86 = System.getenv("ProgramFiles(x86)");
        String localAppData = System.getenv("LOCALAPPDATA");

        List<String> candidates = new ArrayList<>();
        if (programFiles != null) candidates.add(programFiles + "\\Google\\Chrome\\Application\\chrome.exe");
        if (programFilesX86 != null) candidates.add(programFilesX86 + "\\Google\\Chrome\\Application\\chrome.exe");
        if (localAppData != null) candidates.add(localAppData + "\\Google\\Chrome\\Application\\chrome.exe");

        for (String path : candidates) {
            if (new File(path).exists()) {
                return executePath(path, "Chrome");
            }
        }

        // Try 'chrome' via PATH
        if (isExecutableInPath("chrome")) {
            return executeCommand("cmd.exe", "/c", "start", "", "chrome");
        }

        return new LaunchResult(false, "Google Chrome is not installed or could not be found.");
    }

    private LaunchResult launchTerminal() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && new File(localAppData + "\\Microsoft\\WindowsApps\\wt.exe").exists()) {
            return executeCommand("cmd.exe", "/c", "start", "", "wt");
        }
        if (isExecutableInPath("wt")) {
            return executeCommand("cmd.exe", "/c", "start", "", "wt");
        }
        return executeCommand("cmd.exe", "/c", "start", "", "cmd.exe");
    }

    private LaunchResult launchCommand(String command, String appName) {
        return executeCommand("cmd.exe", "/c", "start", "", command);
    }

    private LaunchResult executePath(String path, String appName) {
        try {
            new ProcessBuilder(path).start();
            return new LaunchResult(true, appName + " launched.");
        } catch (Exception e) {
            return new LaunchResult(false, "Failed to launch " + appName + ": " + e.getMessage());
        }
    }

    private LaunchResult executeCommand(String... cmdArray) {
        try {
            new ProcessBuilder(cmdArray).start();
            return new LaunchResult(true, "Application launched.");
        } catch (Exception e) {
            return new LaunchResult(false, "Failed to launch application: " + e.getMessage());
        }
    }

    private boolean isExecutableInPath(String exec) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) return false;
        String[] paths = pathEnv.split(File.pathSeparator);
        for (String path : paths) {
            File file = new File(path, exec);
            if (file.exists() && !file.isDirectory()) {
                return true;
            }
            File fileExe = new File(path, exec + ".exe");
            if (fileExe.exists() && !fileExe.isDirectory()) {
                return true;
            }
            File fileCmd = new File(path, exec + ".cmd");
            if (fileCmd.exists() && !fileCmd.isDirectory()) {
                return true;
            }
        }
        return false;
    }
}
