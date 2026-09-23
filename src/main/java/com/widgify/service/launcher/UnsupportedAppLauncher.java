package com.widgify.service.launcher;

import com.widgify.model.AppShortcut;

import java.util.Collections;
import java.util.List;

public class UnsupportedAppLauncher implements AppLauncher {

    @Override
    public List<AppShortcut> getSupportedShortcuts() {
        return Collections.emptyList();
    }

    @Override
    public LaunchResult launchApp(String appId) {
        return new LaunchResult(false, "Application launching is not supported on this operating system.");
    }
}
