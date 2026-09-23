package com.widgify.desktop.ui;

import com.widgify.desktop.net.ServerApiClient;
import com.widgify.desktop.widgets.DesktopWidget;
import com.widgify.desktop.widgets.WidgetRegistry;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * WidgetWindowManager manages active DesktopWidgetStage instances.
 *
 * PRIMARY DEPLOYMENT PATH — deployFromCanvas():
 *   Called by DesktopPreviewCanvas.applyLayoutToDesktop().
 *   Takes the CURRENT canvas widgets + selected screen.
 *   Closes all existing stages first, then deploys exactly the canvas widgets
 *   using proper normalized coordinate mapping to real monitor bounds.
 *   The canvas is the source of truth — no backend re-query happens here.
 *
 * SECONDARY PATH — loadWidgets():
 *   Called on initial session load from backend data (not from Apply).
 *   Kept for WidgetManagerStage and startup scenarios.
 *
 * Coordinate mapping (canvas → real screen):
 *   normX = canvasX / canvasWidth
 *   realX = screen.minX + normX * screen.width
 *   (same for Y, W, H)
 */
public class WidgetWindowManager {

    private final ServerApiClient apiClient;
    private final Map<Integer, DesktopWidgetStage> activeStages;

    private final ScheduledExecutorService debounceScheduler;
    private final Map<Integer, ScheduledFuture<?>> pendingMoveTasks;
    private final Map<Integer, ScheduledFuture<?>> pendingResizeTasks;

    // Backend widget data cache (for enable/disable/re-enable flows)
    private final Map<String, Map<String, Object>> allLoadedWidgetData;

    public WidgetWindowManager(ServerApiClient apiClient) {
        this.apiClient = apiClient;
        this.activeStages = new ConcurrentHashMap<>();
        this.allLoadedWidgetData = new ConcurrentHashMap<>();
        this.debounceScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "WidgifyDebounce");
            t.setDaemon(true);
            return t;
        });
        this.pendingMoveTasks = new ConcurrentHashMap<>();
        this.pendingResizeTasks = new ConcurrentHashMap<>();
    }

    public ServerApiClient getApiClient() { return apiClient; }

    // ══════════════════════════════════════════════════════════════════════════
    //  PRIMARY APPLY PATH — Canvas is the source of truth
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Deploys EXACTLY the widgets currently on the canvas preview to the real desktop.
     *
     * Steps:
     * 1. Close all existing active DesktopWidgetStage instances.
     * 2. For each canvas preview widget, compute real screen coordinates via
     *    normalized mapping (canvasX/canvasW → screen.minX + norm * screen.width).
     * 3. Create and show a DesktopWidgetStage for each one.
     *
     * This method does NOT query the backend. The canvas list is the source of truth.
     * Must be called on the JavaFX Application Thread.
     *
     * @param canvasWidgets list of preview widgets currently on the canvas
     * @param canvasWidth   current canvas pane pixel width
     * @param canvasHeight  current canvas pane pixel height
     * @param targetScreen  the Screen the user selected in the Display selector
     */
    public void deployFromCanvas(List<DesktopPreviewWidget> canvasWidgets,
                                 double canvasWidth, double canvasHeight,
                                 Screen targetScreen) {
        // Step 1: Close all existing deployed stages
        closeAllWidgets();

        if (canvasWidgets == null || canvasWidgets.isEmpty()) {
            System.out.println("[WidgetWindowManager] deployFromCanvas: empty canvas — all desktop widgets closed.");
            return;
        }

        Rectangle2D screenBounds = (targetScreen != null)
                ? targetScreen.getVisualBounds()
                : Screen.getPrimary().getVisualBounds();

        double screenX = screenBounds.getMinX();
        double screenY = screenBounds.getMinY();
        double screenW = screenBounds.getWidth();
        double screenH = screenBounds.getHeight();

        // Defensive: if canvas dimensions not measured yet, use logical fallbacks
        double cw = (canvasWidth  > 50) ? canvasWidth  : screenW;
        double ch = (canvasHeight > 50) ? canvasHeight : screenH;

        System.out.printf("[WidgetWindowManager] deployFromCanvas: %d widgets → Screen[%.0f,%.0f %.0fx%.0f] canvas[%.0fx%.0f]%n",
                canvasWidgets.size(), screenX, screenY, screenW, screenH, cw, ch);

        for (DesktopPreviewWidget pw : canvasWidgets) {
            // Normalized position (0.0 – 1.0)
            double normX = pw.getPosX() / cw;
            double normY = pw.getPosY() / ch;
            double normW = pw.getWidgetWidth()  / cw;
            double normH = pw.getWidgetHeight() / ch;

            // Real screen coordinates
            double realX = screenX + normX * screenW;
            double realY = screenY + normY * screenH;
            double realW = normW * screenW;
            double realH = normH * screenH;

            // Enforce sensible minimum sizes per type
            double minW = 200, minH = 130;
            switch (pw.getWidgetType().toLowerCase()) {
                case "timer":    minW = 210; minH = 235; break;
                case "system":   minW = 220; minH = 170; break;
                case "weather":  minW = 240; minH = 180; break;
                case "notes":    minW = 200; minH = 170; break;
                case "tasks":    minW = 200; minH = 170; break;
                case "launcher": minW = 300; minH = 130; break;
                case "clock":    minW = 200; minH = 130; break;
            }
            realW = Math.max(minW, realW);
            realH = Math.max(minH, realH);


            System.out.printf("   %s: canvas(%.0f,%.0f %.0fx%.0f) → screen(%.0f,%.0f %.0fx%.0f)%n",
                    pw.getWidgetType(), pw.getPosX(), pw.getPosY(),
                    pw.getWidgetWidth(), pw.getWidgetHeight(),
                    realX, realY, realW, realH);

            int widgetId = pw.getWidgetId();
            // For canvas-only (unsaved) widgets, use a synthetic negative ID based on position
            if (widgetId <= 0) {
                widgetId = -(int)(Math.abs(pw.getPosX() * 31 + pw.getPosY() * 17) % 100000 + 1);
            }

            String widgetType = pw.getWidgetType();
            DesktopWidget widgetNode = WidgetRegistry.createWidget(widgetType, apiClient);
            widgetNode.onInitialize(new HashMap<>());

            final int finalId = widgetId;
            final double fx = realX, fy = realY, fw = realW, fh = realH;
            final DesktopWidget finalWidget = widgetNode;

            // Must create stages on FX thread
            DesktopWidgetStage stage = new DesktopWidgetStage(finalId, finalWidget, fx, fy, fw, fh, this);
            activeStages.put(finalId, stage);
            stage.showAndApplyNativeStyle();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SECONDARY PATH — Load from backend data (startup / WidgetManager)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Deploys widgets from backend data list.
     * Used for initial session load, NOT for Apply-from-canvas.
     * Uses stored absolute coordinates directly.
     */
    @SuppressWarnings("unchecked")
    public void loadWidgets(List<Map<String, Object>> widgetList) {
        closeAllWidgets();
        allLoadedWidgetData.clear();

        if (widgetList == null || widgetList.isEmpty()) {
            System.out.println("[WidgetWindowManager] loadWidgets: empty list");
            return;
        }

        System.out.println("[WidgetWindowManager] loadWidgets: " + widgetList.size() + " widgets");
        for (Map<String, Object> data : widgetList) {
            String type = String.valueOf(data.getOrDefault("widgetType",
                    data.getOrDefault("widget_type", ""))).toLowerCase();
            if (!type.isEmpty()) allLoadedWidgetData.put(type, data);

            boolean enabled = parseEnabled(data);
            System.out.println("   id=" + data.get("id") + " enabled=" + enabled + " type=" + type);
            if (enabled) createAndShowWidgetStage(data);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Enable / Disable (used by WidgetManagerStage)
    // ══════════════════════════════════════════════════════════════════════════

    public boolean isWidgetActive(String widgetType) {
        if (widgetType == null) return false;
        String key = widgetType.trim().toLowerCase();
        return activeStages.values().stream()
                .anyMatch(s -> key.equalsIgnoreCase(s.getWidget().getWidgetType()));
    }

    public void enableWidget(String widgetType) {
        if (widgetType == null) return;
        String key = widgetType.trim().toLowerCase();
        Map<String, Object> data = allLoadedWidgetData.get(key);
        if (data != null) {
            int id = parseNumber(data.get("id"));
            data.put("enabled", true);
            if (!activeStages.containsKey(id)) createAndShowWidgetStage(data);
            if (id > 0) apiClient.updateWidgetEnabled(id, true);
        } else {
            int defaultX = 100 + (allLoadedWidgetData.size() * 60 % 400);
            int defaultY = 100 + (allLoadedWidgetData.size() * 50 % 300);
            apiClient.createWidget(key, key.toUpperCase(), defaultX, defaultY, 220, 140, "{}").thenAccept(res -> {
                if (Boolean.TRUE.equals(res.get("success")) && res.get("widget") instanceof Map) {
                    Map<String, Object> newData = (Map<String, Object>) res.get("widget");
                    allLoadedWidgetData.put(key, newData);
                    Platform.runLater(() -> createAndShowWidgetStage(newData));
                }
            });
        }
    }

    public void disableWidget(String widgetType) {
        if (widgetType == null) return;
        String key = widgetType.trim().toLowerCase();
        DesktopWidgetStage targetStage = null;
        for (DesktopWidgetStage s : activeStages.values()) {
            if (key.equalsIgnoreCase(s.getWidget().getWidgetType())) { targetStage = s; break; }
        }
        if (targetStage != null) {
            int id = targetStage.getWidgetId();
            activeStages.remove(id);
            targetStage.closeStage();
            Map<String, Object> d = allLoadedWidgetData.get(key);
            if (d != null) d.put("enabled", false);
            apiClient.updateWidgetEnabled(id, false);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Stage creation (secondary path / WidgetManagerStage)
    // ══════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    public DesktopWidgetStage createAndShowWidgetStage(Map<String, Object> data) {
        int id = parseNumber(data.get("id"));
        if (id <= 0) return null;

        String widgetType = String.valueOf(data.getOrDefault("widgetType",
                data.getOrDefault("widget_type", "clock")));
        double posX = parseDouble(data.getOrDefault("positionX", data.getOrDefault("position_x", 100)));
        double posY = parseDouble(data.getOrDefault("positionY", data.getOrDefault("position_y", 100)));
        double width  = parseDouble(data.getOrDefault("width", 220));
        double height = parseDouble(data.getOrDefault("height", 140));

        // Tiny grid-unit conversion safety
        if (width <= 10)  width  *= 110;
        if (height <= 10) height *= 80;

        Map<String, Object> config = new HashMap<>();
        Object configObj = data.get("config");
        if (configObj instanceof Map) config.putAll((Map<String, Object>) configObj);

        DesktopWidget widgetNode = WidgetRegistry.createWidget(widgetType, apiClient);
        widgetNode.onInitialize(config);

        DesktopWidgetStage stage = new DesktopWidgetStage(id, widgetNode, posX, posY, width, height, this);
        activeStages.put(id, stage);
        stage.showAndApplyNativeStyle();
        return stage;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Position / size debounce callbacks
    // ══════════════════════════════════════════════════════════════════════════

    /** Called by DesktopWidgetStage during drag — debounces HTTP update by 300ms. */
    public void onWidgetMoved(int widgetId, int posX, int posY) {
        ScheduledFuture<?> existing = pendingMoveTasks.get(widgetId);
        if (existing != null && !existing.isDone()) existing.cancel(false);
        ScheduledFuture<?> task = debounceScheduler.schedule(
                () -> apiClient.updateWidgetPosition(widgetId, posX, posY), 300, TimeUnit.MILLISECONDS);
        pendingMoveTasks.put(widgetId, task);
    }

    /** Called by DesktopWidgetStage during resize — debounces HTTP update by 300ms. */
    public void onWidgetResized(int widgetId, int width, int height) {
        ScheduledFuture<?> existing = pendingResizeTasks.get(widgetId);
        if (existing != null && !existing.isDone()) existing.cancel(false);
        ScheduledFuture<?> task = debounceScheduler.schedule(
                () -> apiClient.updateWidgetSize(widgetId, width, height), 300, TimeUnit.MILLISECONDS);
        pendingResizeTasks.put(widgetId, task);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ══════════════════════════════════════════════════════════════════════════

    public void closeWidget(int widgetId) {
        DesktopWidgetStage stage = activeStages.remove(widgetId);
        if (stage != null) {
            stage.closeStage();
            if (widgetId > 0) apiClient.updateWidgetEnabled(widgetId, false);
        }
    }

    public void closeAllWidgets() {
        for (DesktopWidgetStage stage : activeStages.values()) {
            stage.closeStage();
        }
        activeStages.clear();
    }

    public Map<Integer, DesktopWidgetStage> getActiveStages() { return activeStages; }

    public void shutdown() {
        closeAllWidgets();
        debounceScheduler.shutdownNow();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Layout preset support (used by WidgetManagerStage)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Applies a named layout preset by positioning and enabling a fixed set of widgets.
     * Used exclusively by WidgetManagerStage — NOT involved in canvas Apply pipeline.
     */
    public void applyLayoutPreset(String presetName) {
        if (presetName == null) return;
        String p = presetName.trim().toLowerCase();
        Map<String, int[]> presetLayout = new java.util.HashMap<>();

        if ("minimalist".equals(p)) {
            presetLayout.put("clock",   new int[]{100, 100, 220, 140});
            presetLayout.put("weather", new int[]{350, 100, 260, 160});
        } else if ("productivity".equals(p)) {
            presetLayout.put("clock",   new int[]{50,  50,  220, 140});
            presetLayout.put("tasks",   new int[]{300, 50,  320, 240});
            presetLayout.put("notes",   new int[]{640, 50,  300, 240});
        } else if ("developer".equals(p)) {
            presetLayout.put("clock",   new int[]{50,  50,  220, 140});
            presetLayout.put("system",  new int[]{300, 50,  280, 160});
            presetLayout.put("launcher",new int[]{50,  260, 550, 140});
        } else if ("all active".equals(p) || "all_active".equals(p) || "allactive".equals(p)) {
            presetLayout.put("clock",   new int[]{50,  50,  220, 140});
            presetLayout.put("timer",   new int[]{290, 50,  220, 140});
            presetLayout.put("system",  new int[]{530, 50,  260, 160});
            presetLayout.put("weather", new int[]{810, 50,  260, 160});
            presetLayout.put("notes",   new int[]{50,  240, 300, 220});
            presetLayout.put("tasks",   new int[]{370, 240, 320, 220});
            presetLayout.put("launcher",new int[]{50,  480, 550, 130});
        } else {
            return;
        }

        String[] allTypes = {"clock", "timer", "system", "weather", "notes", "tasks", "launcher"};
        for (String type : allTypes) {
            if (presetLayout.containsKey(type)) {
                int[] pos = presetLayout.get(type);
                Map<String, Object> data = allLoadedWidgetData.get(type);
                if (data == null) {
                    apiClient.createWidget(type, type.toUpperCase(), pos[0], pos[1], pos[2], pos[3], "{}").thenAccept(res -> {
                        if (Boolean.TRUE.equals(res.get("success")) && res.get("widget") instanceof Map) {
                            Map<String, Object> nd = (Map<String, Object>) res.get("widget");
                            allLoadedWidgetData.put(type, nd);
                            Platform.runLater(() -> createAndShowWidgetStage(nd));
                        }
                    });
                } else {
                    int id = parseNumber(data.get("id"));
                    data.put("position_x", pos[0]); data.put("position_y", pos[1]);
                    data.put("width", pos[2]);       data.put("height", pos[3]);
                    data.put("enabled", true);
                    if (activeStages.containsKey(id)) {
                        DesktopWidgetStage s = activeStages.get(id);
                        s.setX(pos[0]); s.setY(pos[1]); s.setWidth(pos[2]); s.setHeight(pos[3]);
                    } else {
                        createAndShowWidgetStage(data);
                    }
                    apiClient.updateWidgetPosition(id, pos[0], pos[1]);
                    apiClient.updateWidgetSize(id, pos[2], pos[3]);
                    apiClient.updateWidgetEnabled(id, true);
                }
            } else {
                disableWidget(type);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private boolean parseEnabled(Map<String, Object> data) {
        Object val = data.get("enabled");
        if (val == null) val = data.get("is_enabled");
        if (val == null) val = data.get("isEnabled");
        if (val == null) return true;
        if (val instanceof Boolean) return (Boolean) val;
        return Boolean.parseBoolean(val.toString());
    }

    private int parseNumber(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj != null) { try { return Integer.parseInt(obj.toString()); } catch (Exception ignored) {} }
        return 0;
    }

    private double parseDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        if (obj != null) { try { return Double.parseDouble(obj.toString()); } catch (Exception ignored) {} }
        return 0.0;
    }
}
