package com.widgify.service;

import com.widgify.model.AppShortcut;

import java.util.ArrayList;
import java.util.List;

/**
 * Service providing predefined Web Launcher shortcuts for the web application.
 * Native desktop process execution is removed per web security architecture requirements.
 */
public class AppLauncherService {

    private final List<AppShortcut> shortcuts;

    public AppLauncherService() {
        this.shortcuts = new ArrayList<>();
        shortcuts.add(new AppShortcut("vscode", "VS Code Web", "💻", "https://vscode.dev/", "Online code editor"));
        shortcuts.add(new AppShortcut("github", "GitHub", "🐙", "https://github.com/", "Code repository host"));
        shortcuts.add(new AppShortcut("google", "Google Search", "🔍", "https://www.google.com/", "Web search engine"));
        shortcuts.add(new AppShortcut("mdn", "MDN Web Docs", "📚", "https://developer.mozilla.org/", "Web development documentation"));
        shortcuts.add(new AppShortcut("stackoverflow", "Stack Overflow", "💬", "https://stackoverflow.com/", "Developer Q&A platform"));
        shortcuts.add(new AppShortcut("chatgpt", "ChatGPT", "🤖", "https://chatgpt.com/", "AI assistant"));
    }

    public List<AppShortcut> getSupportedShortcuts() {
        return new ArrayList<>(shortcuts);
    }

    public AppShortcut getShortcutById(String id) {
        if (id == null) return null;
        for (AppShortcut s : shortcuts) {
            if (s.getId().equalsIgnoreCase(id.trim())) {
                return s;
            }
        }
        return null;
    }
}
