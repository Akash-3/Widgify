package com.widgify.desktop.ui;

import com.widgify.desktop.net.ServerApiClient;
import com.widgify.desktop.widgets.DesktopWidget;
import com.widgify.desktop.widgets.WidgetRegistry;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DesktopPreviewCanvas — central designer workspace with Display selector pills,
 * grid snapping, drag-and-drop, and a bottom action bar.
 *
 * Bottom action bar buttons:
 *   [ Snap to Grid ]  [ Clear Canvas ]  [ Reset Layout ]  [ Save Layout ]  [ Apply to Desktop ]
 *
 * Clear Canvas  = removes all nodes from the canvas preview (does NOT touch DB, no desktop stages).
 * Reset Layout  = calls /widgets/reset to restore 7 canonical defaults in DB, reloads preview.
 * Save Layout   = persists current canvas positions to backend.
 * Apply to Desktop = saves then launches transparent DesktopWidgetStage overlays.
 */
public class DesktopPreviewCanvas extends VBox {

    private final ServerApiClient apiClient;
    private final WidgetWindowManager windowManager;
    private final Pane canvasPane;
    private final List<DesktopPreviewWidget> previewWidgets = new ArrayList<>();
    private DesktopPreviewWidget selectedWidget;

    private int selectedDisplayIndex = 0;
    private boolean gridSnapEnabled = true;

    private final HBox monitorPillBox;
    private final Button saveBtn;
    private final Button applyBtn;
    private final Label toastLabel;
    private final ToggleButton gridSnapToggle;

    public DesktopPreviewCanvas(ServerApiClient apiClient, WidgetWindowManager windowManager) {
        this.apiClient = apiClient;
        this.windowManager = windowManager;

        setSpacing(12);
        setPadding(new Insets(16, 20, 16, 20));
        setStyle("-fx-background-color: #060606;");

        // ── 1. Top Header Chrome ──────────────────────────────────
        HBox topChrome = new HBox(16);
        topChrome.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label canvasTitle = new Label("Desktop Preview Canvas");
        canvasTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        canvasTitle.setTextFill(Color.web("#ffffff"));

        Label canvasSub = new Label("Drag widgets from the library • Move and resize to arrange • Apply to go live");
        canvasSub.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
        canvasSub.setTextFill(Color.web("#666666"));

        titleBox.getChildren().addAll(canvasTitle, canvasSub);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        // Display selector pills
        monitorPillBox = new HBox(8);
        monitorPillBox.setAlignment(Pos.CENTER);
        refreshDisplayPills();

        topChrome.getChildren().addAll(titleBox, monitorPillBox);

        // ── 2. Canvas Surface ─────────────────────────────────────
        canvasPane = new Pane();
        LinearGradient bgGradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0a0b0f")),
                new Stop(1, Color.web("#111420"))
        );
        canvasPane.setBackground(new Background(new BackgroundFill(bgGradient, new CornerRadii(8), Insets.EMPTY)));
        canvasPane.setStyle("-fx-border-color: #1e2230; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
        VBox.setVgrow(canvasPane, Priority.ALWAYS);
        setupCanvasDragAndDrop();

        // ── 3. Bottom Action Bar ──────────────────────────────────
        HBox bottomBar = new HBox(10);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(9, 14, 9, 14));
        bottomBar.setStyle("-fx-background-color: #0a0a0a; -fx-border-color: #1a1a1a; "
                + "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");

        // Snap-to-Grid toggle
        gridSnapToggle = new ToggleButton("Snap to Grid");
        gridSnapToggle.setSelected(true);
        gridSnapToggle.setStyle(snapToggleStyle(true));
        gridSnapToggle.setOnAction(e -> {
            this.gridSnapEnabled = gridSnapToggle.isSelected();
            gridSnapToggle.setStyle(snapToggleStyle(gridSnapToggle.isSelected()));
        });

        HBox snapBox = new HBox(6, new Label("⊞"), gridSnapToggle);
        snapBox.setAlignment(Pos.CENTER_LEFT);
        ((Label) snapBox.getChildren().get(0)).setTextFill(Color.web("#555555"));
        ((Label) snapBox.getChildren().get(0)).setFont(Font.font("Segoe UI", 11));
        HBox.setHgrow(snapBox, Priority.ALWAYS);

        // [ Clear Canvas ] — removes preview nodes; does NOT touch DB or desktop
        Button clearBtn = new Button("Clear Canvas");
        clearBtn.setStyle(secondaryBtnStyle());
        clearBtn.setOnAction(e -> clearCanvas());

        // [ Reset Layout ] — calls backend /widgets/reset, reloads preview
        Button resetBtn = new Button("⟳ Reset Layout");
        resetBtn.setStyle(secondaryBtnStyle());
        resetBtn.setOnAction(e -> resetToDefaults());

        // [ Save Layout ] — persists canvas positions to backend
        saveBtn = new Button("💾 Save Layout");
        saveBtn.setStyle(secondaryBtnStyle());
        saveBtn.setOnAction(e -> saveLayoutToBackend());

        // [ Apply to Desktop ] — primary action
        applyBtn = new Button("Apply to Desktop");
        applyBtn.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #000000; "
                + "-fx-font-weight: bold; -fx-font-size: 11px; "
                + "-fx-padding: 8 20; -fx-background-radius: 4; -fx-cursor: hand;");
        applyBtn.setOnAction(e -> applyLayoutToDesktop());

        // Toast label (shows only when active)
        toastLabel = new Label("");
        toastLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        toastLabel.setTextFill(Color.web("#aaaaaa"));
        toastLabel.setStyle("-fx-background-color: #141414; -fx-padding: 4 10; "
                + "-fx-border-color: #282828; -fx-border-radius: 4; -fx-background-radius: 4;");
        toastLabel.setVisible(false);

        // Subtle online indicator (far right) — dot only, no text
        Circle statusDot = new Circle(3, Color.web("#4a9a6a"));
        Label statusHint = new Label("All systems running smoothly");
        statusHint.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 9));
        statusHint.setTextFill(Color.web("#444444"));
        HBox statusBox = new HBox(5, statusDot, statusHint);
        statusBox.setAlignment(Pos.CENTER_RIGHT);

        bottomBar.getChildren().addAll(snapBox, toastLabel, clearBtn, resetBtn, saveBtn, applyBtn, statusBox);

        getChildren().addAll(topChrome, canvasPane, bottomBar);
    }

    // ── Style helpers ──────────────────────────────────────────────

    private String secondaryBtnStyle() {
        return "-fx-background-color: #141414; -fx-text-fill: #cccccc; "
                + "-fx-font-weight: bold; -fx-font-size: 10px; "
                + "-fx-padding: 8 14; -fx-border-color: #2a2a2a; "
                + "-fx-border-radius: 4; -fx-cursor: hand;";
    }

    private String snapToggleStyle(boolean selected) {
        if (selected) {
            return "-fx-background-color: #1e1e1e; -fx-text-fill: #ffffff; "
                    + "-fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 6 12; "
                    + "-fx-border-color: #444444; -fx-border-radius: 4; -fx-cursor: hand;";
        } else {
            return "-fx-background-color: #0e0e0e; -fx-text-fill: #666666; "
                    + "-fx-font-size: 10px; -fx-padding: 6 12; "
                    + "-fx-border-color: #1e1e1e; -fx-border-radius: 4; -fx-cursor: hand;";
        }
    }

    // ── Display pills ──────────────────────────────────────────────

    private void refreshDisplayPills() {
        monitorPillBox.getChildren().clear();
        List<Screen> screens = Screen.getScreens();
        int count = (screens != null && !screens.isEmpty()) ? screens.size() : 1;

        for (int i = 0; i < count; i++) {
            final int index = i;
            Screen screen = (screens != null && i < screens.size()) ? screens.get(i) : Screen.getPrimary();
            int w = (int) screen.getVisualBounds().getWidth();
            int h = (int) screen.getVisualBounds().getHeight();

            Button pill = new Button(String.format("💻 Display %d   %d × %d", i + 1, w, h));
            pill.setCursor(javafx.scene.Cursor.HAND);
            if (i == selectedDisplayIndex) {
                pill.setStyle("-fx-background-color: #1a1e28; -fx-text-fill: #ffffff; "
                        + "-fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 6 12; "
                        + "-fx-border-color: #ffffff; -fx-border-radius: 6; -fx-background-radius: 6;");
            } else {
                pill.setStyle("-fx-background-color: #101216; -fx-text-fill: #777777; "
                        + "-fx-font-size: 10px; -fx-padding: 6 12; "
                        + "-fx-border-color: #222222; -fx-border-radius: 6; -fx-background-radius: 6;");
            }

            pill.setOnAction(e -> {
                this.selectedDisplayIndex = index;
                refreshDisplayPills();
                showToast("Switched to Display " + (index + 1));
            });

            monitorPillBox.getChildren().add(pill);
        }
    }

    // ── Canvas DnD ────────────────────────────────────────────────

    private void setupCanvasDragAndDrop() {
        canvasPane.setOnDragOver(event -> {
            if (event.getGestureSource() != canvasPane && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        canvasPane.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                String widgetType = db.getString();
                double dropX = event.getX();
                double dropY = event.getY();
                if (gridSnapEnabled) {
                    dropX = Math.round(dropX / 20.0) * 20;
                    dropY = Math.round(dropY / 20.0) * 20;
                }
                addPreviewWidgetFromLibrary(widgetType, dropX, dropY);
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        canvasPane.setOnMouseClicked(e -> {
            if (e.getTarget() == canvasPane) setSelectedWidget(null);
        });
    }

    // ── Public operations ──────────────────────────────────────────

    public void addPreviewWidgetFromLibrary(String widgetType, double x, double y) {
        if ("server_health".equalsIgnoreCase(widgetType) || "server".equalsIgnoreCase(widgetType)) return;

        DesktopWidget widget = WidgetRegistry.createWidget(widgetType, apiClient);
        widget.onInitialize(new HashMap<>());

        double defaultW = 240, defaultH = 150;
        if ("launcher".equalsIgnoreCase(widgetType))  { defaultW = 490; defaultH = 130; }
        else if ("notes".equalsIgnoreCase(widgetType)) { defaultW = 240; defaultH = 180; }
        else if ("tasks".equalsIgnoreCase(widgetType)) { defaultW = 200; defaultH = 180; }
        else if ("weather".equalsIgnoreCase(widgetType)) { defaultW = 280; defaultH = 175; }
        else if ("timer".equalsIgnoreCase(widgetType))   { defaultW = 210; defaultH = 205; }

        // Clamp so nothing starts outside canvas
        double cx = Math.max(0, Math.min(x, Math.max(0, canvasPane.getWidth() - defaultW)));
        double cy = Math.max(0, Math.min(y, Math.max(0, canvasPane.getHeight() - defaultH)));

        DesktopPreviewWidget preview = new DesktopPreviewWidget(0, widgetType, widget, cx, cy, defaultW, defaultH, this);
        previewWidgets.add(preview);
        canvasPane.getChildren().add(preview);
        setSelectedWidget(preview);
        showToast("Added " + widgetType + " widget");
    }

    public void loadWidgetsFromBackend(List<Map<String, Object>> widgetList) {
        canvasPane.getChildren().clear();
        previewWidgets.clear();

        if (widgetList == null || widgetList.isEmpty()) return;

        for (Map<String, Object> data : widgetList) {
            boolean enabled = parseEnabled(data);
            if (!enabled) continue;

            String type = String.valueOf(data.getOrDefault("widgetType", data.getOrDefault("widget_type", "clock")));
            if ("server_health".equalsIgnoreCase(type) || "server".equalsIgnoreCase(type)) continue;

            int id = parseNumber(data.get("id"));

            // Use normalised coordinates if available, fall back to absolute
            double normX = parseDouble(data.getOrDefault("normX", -1));
            double normY = parseDouble(data.getOrDefault("normY", -1));
            double normW = parseDouble(data.getOrDefault("normW", -1));
            double normH = parseDouble(data.getOrDefault("normH", -1));

            double posX, posY, width, height;
            double cw = canvasPane.getWidth()  > 0 ? canvasPane.getWidth()  : 900;
            double ch = canvasPane.getHeight() > 0 ? canvasPane.getHeight() : 580;

            if (normX >= 0 && normW > 0) {
                posX   = normX * cw;
                posY   = normY * ch;
                width  = normW * cw;
                height = normH * ch;
            } else {
                posX   = parseDouble(data.getOrDefault("positionX", data.getOrDefault("position_x", 50)));
                posY   = parseDouble(data.getOrDefault("positionY", data.getOrDefault("position_y", 50)));
                width  = parseDouble(data.getOrDefault("width", 240));
                height = parseDouble(data.getOrDefault("height", 150));
            }

            // Clamp so nothing is placed outside the canvas
            posX  = Math.max(0, Math.min(posX,  Math.max(0, cw - width)));
            posY  = Math.max(0, Math.min(posY,  Math.max(0, ch - height)));

            DesktopWidget widgetNode = WidgetRegistry.createWidget(type, apiClient);
            widgetNode.onInitialize(new HashMap<>());

            DesktopPreviewWidget preview = new DesktopPreviewWidget(id, type, widgetNode, posX, posY, width, height, this);
            previewWidgets.add(preview);
            canvasPane.getChildren().add(preview);
        }
    }

    /**
     * Clear Canvas — removes all preview nodes from the canvas.
     * Does NOT modify the database and does NOT affect live desktop stages.
     */
    public void clearCanvas() {
        canvasPane.getChildren().clear();
        previewWidgets.clear();
        setSelectedWidget(null);
        showToast("Canvas cleared");
    }

    public void saveLayoutToBackend() {
        saveBtn.setDisable(true);
        for (DesktopPreviewWidget pw : previewWidgets) {
            int id = pw.getWidgetId();
            int x = (int) pw.getPosX();
            int y = (int) pw.getPosY();
            int w = (int) pw.getWidgetWidth();
            int h = (int) pw.getWidgetHeight();

            if (id > 0) {
                apiClient.updateWidgetPosition(id, x, y);
                apiClient.updateWidgetSize(id, w, h);
                apiClient.updateWidgetEnabled(id, true);
            } else {
                apiClient.createWidget(pw.getWidgetType(), pw.getWidgetType().toUpperCase(), x, y, w, h, "{}").thenAccept(res -> {
                    if (Boolean.TRUE.equals(res.get("success")) && res.get("widget") instanceof Map) {
                        Map<String, Object> wObj = (Map<String, Object>) res.get("widget");
                        pw.setWidgetId(((Number) wObj.get("id")).intValue());
                    }
                });
            }
        }
        Platform.runLater(() -> {
            saveBtn.setDisable(false);
            showToast("Layout saved");
        });
    }

    public void applyLayoutToDesktop() {
        if (previewWidgets.isEmpty()) {
            // Empty canvas → close all active desktop widgets
            windowManager.closeAllWidgets();
            showToast("Desktop cleared — no widgets applied");
            return;
        }

        // Save current canvas positions to backend first (async)
        saveLayoutToBackend();

        // ── THE CANVAS IS THE SOURCE OF TRUTH ─────────────────────
        // Pass ONLY the current canvas widgets to the window manager.
        // Do NOT call apiClient.getWidgets() — that would deploy ALL backend widgets.
        // Compute real screen coordinates using normalization inside deployFromCanvas.

        double cw = canvasPane.getWidth();
        double ch = canvasPane.getHeight();

        // Get the selected display's Screen object
        javafx.stage.Screen targetScreen;
        try {
            java.util.List<javafx.stage.Screen> screens = javafx.stage.Screen.getScreens();
            targetScreen = (selectedDisplayIndex < screens.size())
                    ? screens.get(selectedDisplayIndex)
                    : javafx.stage.Screen.getPrimary();
        } catch (Exception e) {
            targetScreen = javafx.stage.Screen.getPrimary();
        }

        // Copy the list to avoid ConcurrentModificationException
        java.util.List<DesktopPreviewWidget> snapshot = new java.util.ArrayList<>(previewWidgets);

        windowManager.deployFromCanvas(snapshot, cw, ch, targetScreen);
        showToast("Applied " + snapshot.size() + " widget" + (snapshot.size() == 1 ? "" : "s") + " to desktop");
    }

    public void resetToDefaults() {
        apiClient.resetWidgets().thenAcceptAsync(res -> {
            Platform.runLater(() -> {
                if (Boolean.TRUE.equals(res.get("success"))) {
                    List<Map<String, Object>> widgets = (List<Map<String, Object>>) res.get("widgets");
                    loadWidgetsFromBackend(widgets);
                    showToast("Layout reset to defaults");
                }
            });
        });
    }

    public void removePreviewWidget(DesktopPreviewWidget widget) {
        if (widget == null) return;
        previewWidgets.remove(widget);
        canvasPane.getChildren().remove(widget);
        if (widget.getWidgetId() > 0) {
            apiClient.updateWidgetEnabled(widget.getWidgetId(), false);
        }
        if (selectedWidget == widget) setSelectedWidget(null);
        showToast("Widget removed from canvas");
    }

    public void setSelectedWidget(DesktopPreviewWidget widget) {
        if (selectedWidget != null) selectedWidget.setSelected(false);
        selectedWidget = widget;
        if (selectedWidget != null) selectedWidget.setSelected(true);
    }

    public void showToast(String message) {
        toastLabel.setText("✓  " + message);
        toastLabel.setVisible(true);
        toastLabel.setOpacity(1.0);

        FadeTransition ft = new FadeTransition(Duration.seconds(2), toastLabel);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setDelay(Duration.seconds(1.8));
        ft.setOnFinished(e -> toastLabel.setVisible(false));
        ft.play();
    }

    // ── Getters ───────────────────────────────────────────────────

    public ServerApiClient getApiClient()                    { return apiClient; }
    public boolean isGridSnapEnabled()                       { return gridSnapEnabled; }
    public List<DesktopPreviewWidget> getPreviewWidgets()   { return previewWidgets; }
    public int getSelectedDisplayIndex()                     { return selectedDisplayIndex; }

    // ── Parsing helpers ───────────────────────────────────────────

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
