package com.widgify.desktop.ui;

import com.widgify.desktop.widgets.DesktopWidget;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DesktopPreviewWidget extends VBox {

    private int widgetId;
    private final String widgetType;
    private final DesktopWidget desktopWidget;
    private final DesktopPreviewCanvas parentCanvas;

    private double posX;
    private double posY;
    private double widgetWidth;
    private double widgetHeight;

    private boolean isSelected = false;
    private double dragOffsetX;
    private double dragOffsetY;

    private double resizeStartX;
    private double resizeStartY;
    private double resizeStartW;
    private double resizeStartH;

    private final Map<String, Object> configMap = new HashMap<>();

    public DesktopPreviewWidget(int widgetId, String widgetType, DesktopWidget desktopWidget, double posX, double posY, double width, double height, DesktopPreviewCanvas parentCanvas) {
        this.widgetId = widgetId;
        this.widgetType = widgetType;
        this.desktopWidget = desktopWidget;
        this.posX = posX;
        this.posY = posY;
        this.widgetWidth = width;
        this.widgetHeight = height;
        this.parentCanvas = parentCanvas;

        setPadding(new Insets(6));
        setSpacing(4);

        // Header Bar (Title + Drag handle + Close)
        HBox headerBar = new HBox(6);
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setPadding(new Insets(2, 6, 2, 6));
        headerBar.setStyle("-fx-cursor: move;");

        Label titleLabel = new Label(desktopWidget.getWidgetTitle().toUpperCase());
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        titleLabel.setTextFill(Color.web("#888888"));
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Label removeBtn = new Label("×");
        removeBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        removeBtn.setTextFill(Color.web("#666666"));
        removeBtn.setStyle("-fx-cursor: hand;");
        removeBtn.setOnMouseEntered(e -> removeBtn.setTextFill(Color.web("#ff5555")));
        removeBtn.setOnMouseExited(e -> removeBtn.setTextFill(Color.web("#666666")));
        removeBtn.setOnMouseClicked(e -> parentCanvas.removePreviewWidget(this));

        headerBar.getChildren().addAll(titleLabel, removeBtn);

        // Actual Widget Content Node
        Node contentNode = desktopWidget.getWidgetNode();
        VBox.setVgrow(contentNode, Priority.ALWAYS);

        // Resize Handle Footer
        HBox footerBar = new HBox();
        footerBar.setAlignment(Pos.BOTTOM_RIGHT);
        Label resizeHandle = new Label("⌟");
        resizeHandle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        resizeHandle.setTextFill(Color.web("#555555"));
        resizeHandle.setStyle("-fx-cursor: se-resize;");
        footerBar.getChildren().add(resizeHandle);

        getChildren().addAll(headerBar, contentNode, footerBar);

        // Set dimensions & position in canvas
        setLayoutX(posX);
        setLayoutY(posY);
        setPrefWidth(width);
        setPrefHeight(height);
        

        // Per-type minimum dimensions — content must never escape bounds
        double minW = 160, minH = 110;
        switch (widgetType.toLowerCase()) {
            case "clock":    minW = 200; minH = 90;  break;
            case "timer":    minW = 210; minH = 235; break;
            case "system":   minW = 220; minH = 130; break;
            case "weather":  minW = 230; minH = 140; break;
            case "notes":    minW = 180; minH = 140; break;
            case "tasks":    minW = 180; minH = 150; break;
            case "launcher": minW = 300; minH = 110; break;
        }
        setMinWidth(minW);
        setMinHeight(minH);
        widgetWidth = Math.max(widgetWidth, minW);
        widgetHeight = Math.max(widgetHeight, minH);

        setPrefWidth(widgetWidth);
        setPrefHeight(widgetHeight);

        updateStyle();

        // Mouse Drag & Movement Handlers inside Canvas
        setupDragging(headerBar);
        setupDragging(this);

        // Resize Handler
        setupResizing(resizeHandle);

        // Selection & Keyboard Handlers
        setOnMouseClicked(e -> {
            parentCanvas.setSelectedWidget(this);
            e.consume();
        });

        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                parentCanvas.removePreviewWidget(this);
                e.consume();
            }
        });

        // Context Menu Setup
        setupContextMenu();
    }

    private void updateStyle() {
        if (isSelected) {
            // Subtle white border — no neon green
            setStyle("-fx-background-color: #0d0d0d; -fx-border-color: #aaaaaa; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6;");
        } else {
            setStyle("-fx-background-color: #0a0a0a; -fx-border-color: #242424; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");
        }
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
        updateStyle();
    }

    private void setupDragging(Node target) {
        target.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.isPrimaryButtonDown()) {
                dragOffsetX = e.getSceneX() - getLayoutX();
                dragOffsetY = e.getSceneY() - getLayoutY();
                parentCanvas.setSelectedWidget(this);
                e.consume();
            }
        });

        target.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (e.isPrimaryButtonDown()) {
                double newX = e.getSceneX() - dragOffsetX;
                double newY = e.getSceneY() - dragOffsetY;

                // Clamp within canvas bounds
                newX = Math.max(0, Math.min(newX, parentCanvas.getWidth() - getWidth()));
                newY = Math.max(0, Math.min(newY, parentCanvas.getHeight() - getHeight()));

                // Grid snap option
                if (parentCanvas.isGridSnapEnabled()) {
                    newX = Math.round(newX / 20.0) * 20;
                    newY = Math.round(newY / 20.0) * 20;
                }

                setLayoutX(newX);
                setLayoutY(newY);
                this.posX = newX;
                this.posY = newY;
                e.consume();
            }
        });
    }

    private void setupResizing(Node handle) {
        handle.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            resizeStartX = e.getSceneX();
            resizeStartY = e.getSceneY();
            resizeStartW = getWidth();
            resizeStartH = getHeight();
            e.consume();
        });

        handle.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            double deltaX = e.getSceneX() - resizeStartX;
            double deltaY = e.getSceneY() - resizeStartY;

            double newW = Math.max(getMinWidth(), resizeStartW + deltaX);
            double newH = Math.max(getMinHeight(), resizeStartH + deltaY);

            if (parentCanvas.isGridSnapEnabled()) {
                newW = Math.round(newW / 20.0) * 20;
                newH = Math.round(newH / 20.0) * 20;
            }

            setPrefWidth(newW);
            setPrefHeight(newH);
            this.widgetWidth = newW;
            this.widgetHeight = newH;
            e.consume();
        });
    }

    private int assignedDisplay = 0;

    public int getAssignedDisplay() { return assignedDisplay; }
    public void setAssignedDisplay(int display) { this.assignedDisplay = display; }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setStyle("-fx-background-color: #0a0a0a; -fx-border-color: #333333;");

        MenuItem configItem = new MenuItem("Configure Widget");
        MenuItem resetPosItem = new MenuItem("Reset Position");
        MenuItem removeItem = new MenuItem("Remove from Canvas");

        // Move to Display Submenu
        javafx.scene.control.Menu moveDisplayMenu = new javafx.scene.control.Menu("Move to Display");
        List<javafx.stage.Screen> screens = javafx.stage.Screen.getScreens();
        int screenCount = (screens != null && !screens.isEmpty()) ? screens.size() : 1;

        for (int i = 0; i < screenCount; i++) {
            final int displayIdx = i;
            MenuItem displayItem = new MenuItem("Display " + (i + 1));
            displayItem.setOnAction(e -> {
                this.assignedDisplay = displayIdx;
                parentCanvas.showToast("Assigned " + widgetType + " to Display " + (displayIdx + 1));
            });
            moveDisplayMenu.getItems().add(displayItem);
        }

        configItem.setOnAction(e -> WidgetConfigDialog.showConfigDialog(null, desktopWidget, parentCanvas.getApiClient()));
        removeItem.setOnAction(e -> parentCanvas.removePreviewWidget(this));
        resetPosItem.setOnAction(e -> {
            setLayoutX(50);
            setLayoutY(50);
            this.posX = 50;
            this.posY = 50;
        });

        contextMenu.getItems().addAll(configItem, moveDisplayMenu, resetPosItem, removeItem);

        addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, e -> {
            contextMenu.show(this, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

    public int getWidgetId() { return widgetId; }
    public void setWidgetId(int id) { this.widgetId = id; }
    public String getWidgetType() { return widgetType; }
    public DesktopWidget getDesktopWidget() { return desktopWidget; }
    public double getPosX() { return posX; }
    public double getPosY() { return posY; }
    public void setPosX(double x) {
        this.posX = x;
        setLayoutX(x);
    }
    public void setPosY(double y) {
        this.posY = y;
        setLayoutY(y);
    }
    public double getWidgetWidth() { return widgetWidth; }
    public double getWidgetHeight() { return widgetHeight; }
    public Map<String, Object> getConfigMap() { return configMap; }
}
