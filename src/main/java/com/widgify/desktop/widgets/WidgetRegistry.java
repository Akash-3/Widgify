package com.widgify.desktop.widgets;

import com.widgify.desktop.net.ServerApiClient;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class WidgetRegistry {

    private static final Map<String, Function<ServerApiClient, DesktopWidget>> REGISTRY = new HashMap<>();

    static {
        register("clock", api -> new ClockDesktopWidget());
        register("timer", api -> new TimerDesktopWidget());
        register("system", api -> new SystemDesktopWidget());
        register("weather", api -> new WeatherDesktopWidget(api));
        register("notes", api -> new NotesDesktopWidget(api));
        register("tasks", api -> new TasksDesktopWidget(api));
        register("launcher", api -> new QuickLauncherDesktopWidget());
    }

    public static void register(String type, Function<ServerApiClient, DesktopWidget> factory) {
        if (type != null && factory != null) {
            REGISTRY.put(type.trim().toLowerCase(), factory);
        }
    }

    public static DesktopWidget createWidget(String type, ServerApiClient apiClient) {
        if (type == null) {
            return new GenericPlaceholderDesktopWidget("unknown");
        }
        String key = type.trim().toLowerCase();
        Function<ServerApiClient, DesktopWidget> factory = REGISTRY.get(key);
        if (factory != null) {
            return factory.apply(apiClient);
        }
        return new GenericPlaceholderDesktopWidget(key);
    }

    public static boolean isSupported(String type) {
        return type != null && REGISTRY.containsKey(type.trim().toLowerCase());
    }
}
