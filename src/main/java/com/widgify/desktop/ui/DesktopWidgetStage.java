package com.widgify.desktop.ui;

import com.widgify.desktop.widgets.DesktopWidget;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;

/**
 * DesktopWidgetStage — a transparent, undecorated JavaFX Stage deployed on the real desktop.
 *
 * Windows desktop behavior (Rainmeter-style):
 *   - Never steals focus (WS_EX_NOACTIVATE)
 *   - Hidden from Alt+Tab and taskbar (WS_EX_TOOLWINDOW)
 *   - Placed behind all normal application windows (HWND_BOTTOM)
 *   - Normal windows (Chrome, VS Code, etc.) always render on top
 *   - setAlwaysOnTop is intentionally NEVER called
 *
 * The native window properties are applied by WindowsDesktopHelper after show().
 */
public class DesktopWidgetStage extends Stage {

    private final int widgetId;
    private final DesktopWidget widget;
    private final WidgetWindowManager windowManager;

    // Window title is stable and unique per stage — used to find HWND for native styling
    private final String nativeTitle;

    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    private double resizeStartX = 0;
    private double resizeStartY = 0;
    private double resizeStartW = 0;
    private double resizeStartH = 0;

    private final VBox rootSurface;

    public DesktopWidgetStage(int widgetId, DesktopWidget widget, double initialX, double initialY,
                               double initialWidth, double initialHeight, WidgetWindowManager windowManager) {
        this.widgetId = widgetId;
        this.widget = widget;
        this.windowManager = windowManager;

        // Unique title used for HWND lookup by WindowsDesktopHelper
        this.nativeTitle = "Widgify-Widget-" + widgetId + "-" + widget.getWidgetType();

        initStyle(StageStyle.TRANSPARENT);
        setTitle(nativeTitle);

        // setAlwaysOnTop intentionally NOT called — widgets must be behind normal windows.
        // setAlwaysOnTop(false); is also NOT needed — it is false by default.

        // Root container — Nothing OS styled dark surface
        rootSurface = new VBox();
        rootSurface.setStyle("-fx-background-color: #0a0a0a; -fx-border-color: #222222; "
                + "-fx-border-width: 1; -fx-background-radius: 8; -fx-border-radius: 8;");
        rootSurface.setPadding(new Insets(8));

        // Header Bar
        HBox headerBar = new HBox(8);
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setPadding(new Insets(4, 8, 4, 8));
        headerBar.setStyle("-fx-cursor: move;");

        Label titleLabel = new Label(widget.getWidgetTitle().toUpperCase());
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        titleLabel.setTextFill(Color.web("#666666"));
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Label closeBtn = new Label("×");
        closeBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        closeBtn.setTextFill(Color.web("#666666"));
        closeBtn.setStyle("-fx-cursor: hand; -fx-padding: 0 4 0 4;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setTextFill(Color.web("#ff5555")));
        closeBtn.setOnMouseExited(e -> closeBtn.setTextFill(Color.web("#666666")));
        closeBtn.setOnMouseClicked(e -> windowManager.closeWidget(widgetId));

        headerBar.getChildren().addAll(titleLabel, closeBtn);

        // Content Area
        Node contentNode = widget.getWidgetNode();
        VBox.setVgrow(contentNode, Priority.ALWAYS);

        // Resize Handle
        HBox footerBar = new HBox();
        footerBar.setAlignment(Pos.BOTTOM_RIGHT);
        Label resizeHandle = new Label("⌟");
        resizeHandle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        resizeHandle.setTextFill(Color.web("#444444"));
        resizeHandle.setStyle("-fx-cursor: se-resize; -fx-padding: 0 2 0 0;");
        footerBar.getChildren().add(resizeHandle);

        rootSurface.getChildren().addAll(headerBar, contentNode, footerBar);

        Scene scene = new Scene(rootSurface);
        scene.setFill(Color.TRANSPARENT);
        setScene(scene);

        setupContextMenu(rootSurface);

        scene.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == javafx.scene.input.KeyCode.H) {
                windowManager.closeWidget(widgetId);
                e.consume();
            } else if (e.isControlDown() && e.getCode() == javafx.scene.input.KeyCode.R) {
                resetPosition();
                e.consume();
            }
        });

        setupDragging(headerBar);
        setupDragging(rootSurface);
        setupResizing(resizeHandle);

        // Per-type minimum heights — enforced BEFORE setWidth/setHeight
        // Stage chrome overhead = rootSurface padding (16px) + headerBar (~26px) + footerBar (~18px) = ~60px
        // Content heights: timer ~170px → total 230px; others 120-150px
        String wType = widget.getWidgetType().toLowerCase();
        double typeMinH;
        switch (wType) {
            case "timer":    typeMinH = 235; break;
            case "system":   typeMinH = 170; break;
            case "weather":  typeMinH = 180; break;
            case "notes":    typeMinH = 170; break;
            case "tasks":    typeMinH = 170; break;
            case "launcher": typeMinH = 130; break;
            default:         typeMinH = 130; break; // clock, etc.
        }

        double w = Math.max(180, initialWidth);
        double h = Math.max(typeMinH, initialHeight);
        setWidth(w);
        setHeight(h);
        setMinWidth(160);
        setMinHeight(typeMinH);

        positionSafely(initialX, initialY, w, h);
    }

    /**
     * Applies Windows native desktop-widget properties after the stage is visible.
     * Must be called AFTER show() by WidgetWindowManager.
     * WS_EX_NOACTIVATE + WS_EX_TOOLWINDOW + HWND_BOTTOM are applied on a background thread.
     */
    public void showAndApplyNativeStyle() {
        show();
        final String title = nativeTitle;
        Thread nativeThread = new Thread(() -> {
            try { Thread.sleep(180); } catch (InterruptedException ignored) {}
            WindowsDesktopHelper.applyDesktopWidgetStyle(title);
        }, "WidgifyNativeStyle-" + widgetId);
        nativeThread.setDaemon(true);
        nativeThread.start();
    }

    private void setupDragging(Node dragTarget) {
        dragTarget.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.isPrimaryButtonDown()) {
                dragOffsetX = e.getScreenX() - getX();
                dragOffsetY = e.getScreenY() - getY();
            }
        });

        dragTarget.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (e.isPrimaryButtonDown()) {
                double newX = e.getScreenX() - dragOffsetX;
                double newY = e.getScreenY() - dragOffsetY;

                Rectangle2D screenBounds = getScreenBoundsFor(newX, newY);
                double snapThreshold = 10.0;

                if (Math.abs(newX - screenBounds.getMinX()) < snapThreshold) {
                    newX = screenBounds.getMinX();
                } else if (Math.abs((newX + getWidth()) - screenBounds.getMaxX()) < snapThreshold) {
                    newX = screenBounds.getMaxX() - getWidth();
                }
                if (Math.abs(newY - screenBounds.getMinY()) < snapThreshold) {
                    newY = screenBounds.getMinY();
                } else if (Math.abs((newY + getHeight()) - screenBounds.getMaxY()) < snapThreshold) {
                    newY = screenBounds.getMaxY() - getHeight();
                }

                setX(newX);
                setY(newY);
                windowManager.onWidgetMoved(widgetId, (int) newX, (int) newY);

                // Periodically re-assert HWND_BOTTOM after drag (Windows may reorder)
                final String title = nativeTitle;
                new Thread(() -> WindowsDesktopHelper.reapplyZOrder(title),
                        "WidgifyZOrder-" + widgetId).start();
            }
        });
    }

    private Rectangle2D getScreenBoundsFor(double x, double y) {
        for (Screen s : Screen.getScreens()) {
            Rectangle2D b = s.getVisualBounds();
            if (x >= b.getMinX() - 100 && x <= b.getMaxX() && y >= b.getMinY() - 100 && y <= b.getMaxY()) {
                return b;
            }
        }
        return Screen.getPrimary().getVisualBounds();
    }

    private void setupResizing(Node handle) {
        handle.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            resizeStartX = e.getScreenX();
            resizeStartY = e.getScreenY();
            resizeStartW = getWidth();
            resizeStartH = getHeight();
            e.consume();
        });

        handle.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            double deltaX = e.getSceneX() != 0 ? (e.getScreenX() - resizeStartX) : 0;
            double deltaY = e.getSceneY() != 0 ? (e.getScreenY() - resizeStartY) : 0;
            double newW = Math.max(getMinWidth(), resizeStartW + deltaX);
            double newH = Math.max(getMinHeight(), resizeStartH + deltaY);
            setWidth(newW);
            setHeight(newH);
            windowManager.onWidgetResized(widgetId, (int) newW, (int) newH);
            e.consume();
        });
    }

    private void positionSafely(double x, double y, double width, double height) {
        boolean visibleOnScreen = false;
        List<Screen> screens = Screen.getScreens();

        for (Screen s : screens) {
            Rectangle2D bounds = s.getVisualBounds();
            // Accept if at least 50px of the widget is visible on this screen
            if (x + 50 >= bounds.getMinX() && x <= bounds.getMaxX() - 50 &&
                y + 50 >= bounds.getMinY() && y <= bounds.getMaxY() - 50) {
                visibleOnScreen = true;
                break;
            }
        }

        if (visibleOnScreen) {
            setX(x);
            setY(y);
        } else {
            // Safe fallback on primary screen
            Rectangle2D pb = Screen.getPrimary().getVisualBounds();
            setX(pb.getMinX() + 60 + (widgetId * 30 % 300));
            setY(pb.getMinY() + 60 + (widgetId * 30 % 200));
        }
    }

    private void setupContextMenu(Node targetNode) {
        javafx.scene.control.ContextMenu cm = new javafx.scene.control.ContextMenu();
        cm.setStyle("-fx-background-color: #0a0a0a; -fx-border-color: #333333;");

        javafx.scene.control.MenuItem hideItem = new javafx.scene.control.MenuItem("Hide Widget");
        javafx.scene.control.MenuItem configItem = new javafx.scene.control.MenuItem("Configure Widget");
        javafx.scene.control.MenuItem resetPosItem = new javafx.scene.control.MenuItem("Reset Position");

        hideItem.setOnAction(e -> windowManager.closeWidget(widgetId));
        configItem.setOnAction(e -> WidgetConfigDialog.showConfigDialog(this, widget, windowManager.getApiClient()));
        resetPosItem.setOnAction(e -> resetPosition());

        cm.getItems().addAll(hideItem, configItem, resetPosItem);

        targetNode.addEventHandler(javafx.scene.input.ContextMenuEvent.CONTEXT_MENU_REQUESTED, e -> {
            cm.show(targetNode, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

    public void resetPosition() {
        Rectangle2D pb = Screen.getPrimary().getVisualBounds();
        double defaultX = pb.getMinX() + 60 + ((widgetId * 50) % 400);
        double defaultY = pb.getMinY() + 60 + ((widgetId * 40) % 300);
        setX(defaultX);
        setY(defaultY);
        windowManager.onWidgetMoved(widgetId, (int) defaultX, (int) defaultY);
    }

    public int getWidgetId()       { return widgetId; }
    public DesktopWidget getWidget() { return widget; }
    public String getNativeTitle()  { return nativeTitle; }

    public void closeStage() {
        widget.onClose();
        close();
    }
}
