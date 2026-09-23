package com.widgify.service.launcher;

import com.widgify.model.AppShortcut;
import java.util.List;

public interface AppLauncher {
    LaunchResult launchApp(String appId);
    List<AppShortcut> getSupportedShortcuts();
}
