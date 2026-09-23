package com.widgify.desktop.ui;

import com.widgify.desktop.net.ServerApiClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;
import java.util.Map;

/**
 * WidgifyMainWindow is the SINGLE primary application window for Widgify.
 * Uses StageStyle.UNDECORATED + custom Widgify application chrome (draggable
 * title bar with minimize/maximize/close and profile area).
 * Features:
 * - SidebarPane (left) with branding, navigation and Widget Library
 * - Interactive DesktopPreviewCanvas in the center
 * - Navigation switching between Canvas, Presets, Settings, and About views
 */
public class WidgifyMainWindow extends Stage {

    private final ServerApiClient apiClient;
    private final WidgetWindowManager windowManager;

    private final SidebarPane sidebarPane;
    private final StackPane contentArea;

    private final DesktopPreviewCanvas canvasPane;
    private final LayoutsPane layoutsPane;
    private final SettingsPane settingsPane;
    private final AboutPane aboutPane;

    // For dragging the undecorated window
    private double dragX;
    private double dragY;
    private boolean isMaximized = false;
    private double savedX, savedY, savedW, savedH;

    public WidgifyMainWindow(ServerApiClient apiClient, WidgetWindowManager windowManager, Runnable logoutCallback) {
        this.apiClient = apiClient;
        this.windowManager = windowManager;

        // Remove native OS title bar
        initStyle(StageStyle.UNDECORATED);
        setTitle("WIDGIFY — Desktop Widget Engine & Designer");

        // ── Center Content Views ──────────────────────────────────
        this.canvasPane = new DesktopPreviewCanvas(apiClient, windowManager);
        this.layoutsPane = new LayoutsPane((presetName) -> {
            apiClient.getWidgets().thenAcceptAsync(res -> {
                if (Boolean.TRUE.equals(res.get("success"))) {
                    List<Map<String, Object>> widgets = (List<Map<String, Object>>) res.get("widgets");
                    Platform.runLater(() -> canvasPane.loadWidgetsFromBackend(widgets));
                }
            });
        });
        this.settingsPane = new SettingsPane(apiClient);
        this.aboutPane = new AboutPane();

        this.contentArea = new StackPane();
        this.contentArea.getChildren().addAll(canvasPane, layoutsPane, settingsPane, aboutPane);

        showView("canvas");

        // ── Navigation Sidebar ────────────────────────────────────
        this.sidebarPane = new SidebarPane(apiClient, this::showView, logoutCallback);

        // ── Custom Chrome Title Bar ───────────────────────────────
        HBox titleBar = buildCustomTitleBar(logoutCallback);

        // ── Root Layout ───────────────────────────────────────────
        BorderPane content = new BorderPane();
        content.setLeft(sidebarPane);
        content.setCenter(contentArea);

        VBox root = new VBox(0, titleBar, content);
        root.setStyle("-fx-background-color: #060606;");
        VBox.setVgrow(content, Priority.ALWAYS);

        // Thin outer border for the entire window
        root.setStyle("-fx-background-color: #060606; -fx-border-color: #1e1e1e; -fx-border-width: 1;");

        Scene scene = new Scene(root, 1150, 740);
        scene.setFill(Color.web("#060606"));
        setScene(scene);
        setMinWidth(960);
        setMinHeight(660);

        // Drag-to-move on the custom title bar
        titleBar.setOnMousePressed(e -> {
            if (!isMaximized) {
                dragX = e.getScreenX() - getX();
                dragY = e.getScreenY() - getY();
            }
        });
        titleBar.setOnMouseDragged(e -> {
            if (!isMaximized) {
                setX(e.getScreenX() - dragX);
                setY(e.getScreenY() - dragY);
            }
        });

        setOnCloseRequest(e -> { /* Let close button handle it */ });

        refreshCanvasFromBackend();
    }

    /**
     * Builds the custom Widgify application chrome title bar.
     * Contains: WIDGIFY branding | page title | spacer | profile pill | window controls
     */
    private HBox buildCustomTitleBar(Runnable logoutCallback) {
        HBox bar = new HBox(0);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPrefHeight(40);
        bar.setMinHeight(40);
        bar.setMaxHeight(40);
        bar.setStyle("-fx-background-color: #0a0a0a; -fx-border-color: #1a1a1a; -fx-border-width: 0 0 1 0;");
        bar.setCursor(Cursor.MOVE);

        // Brand label (always visible, left-aligned)
        Label brandLabel = new Label("  W I D G I F Y");
        brandLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        brandLabel.setTextFill(Color.web("#ffffff"));
        brandLabel.setPadding(new Insets(0, 18, 0, 14));

        // Vertical separator
        Label sep = new Label("|");
        sep.setTextFill(Color.web("#333333"));
        sep.setFont(Font.font("Segoe UI", 11));
        sep.setPadding(new Insets(0, 10, 0, 0));

        // Subtitle / page context
        Label subLabel = new Label("Desktop Widget Designer");
        subLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
        subLabel.setTextFill(Color.web("#666666"));

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Profile pill
        HBox profilePill = buildProfilePill(logoutCallback);

        // Window control buttons
        Button minimizeBtn = buildChromeBtn("—", "#333333", "#ffffff");
        minimizeBtn.setCursor(Cursor.HAND);
        minimizeBtn.setOnAction(e -> setIconified(true));

        Button maximizeBtn = buildChromeBtn("⬜", "#333333", "#ffffff");
        maximizeBtn.setCursor(Cursor.HAND);
        maximizeBtn.setOnAction(e -> toggleMaximize());

        Button closeBtn = buildChromeBtn("✕", "#cc4444", "#ffffff");
        closeBtn.setCursor(Cursor.HAND);
        closeBtn.setOnAction(e -> {
            close();
        });

        bar.getChildren().addAll(brandLabel, sep, subLabel, spacer, profilePill, minimizeBtn, maximizeBtn, closeBtn);
        return bar;
    }

    private Button buildChromeBtn(String text, String hoverBg, String fg) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
        btn.setTextFill(Color.web(fg));
        btn.setPrefWidth(38);
        btn.setPrefHeight(40);
        btn.setMinHeight(40);
        btn.setMaxHeight(40);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + fg + "; -fx-cursor: hand; -fx-padding: 0;");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: " + hoverBg + "; -fx-text-fill: " + fg + "; -fx-cursor: hand; -fx-padding: 0;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: " + fg + "; -fx-cursor: hand; -fx-padding: 0;"));
        return btn;
    }

    private HBox buildProfilePill(Runnable logoutCallback) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(4, 12, 4, 10));
        box.setStyle("-fx-background-color: #121212; -fx-border-color: #222222; -fx-border-radius: 14; -fx-background-radius: 14;");
        box.setCursor(Cursor.HAND);

        Circle avatar = new Circle(9, Color.web("#2a2a2a"));

        String email = (apiClient != null && apiClient.getCurrentAuthenticatedEmail() != null)
                ? apiClient.getCurrentAuthenticatedEmail() : "user@widgify.desktop";

        Label emailLbl = new Label(email);
        emailLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        emailLbl.setTextFill(Color.web("#ffffff"));

        // Subtle status dot
        Circle statusDot = new Circle(3, Color.web("#4a9a6a"));

        box.getChildren().addAll(avatar, emailLbl, statusDot);

        // Click on profile pill to show tooltip or just focus
        HBox.setMargin(box, new Insets(0, 8, 0, 0));
        return box;
    }

    private void toggleMaximize() {
        if (isMaximized) {
            setX(savedX);
            setY(savedY);
            setWidth(savedW);
            setHeight(savedH);
            isMaximized = false;
        } else {
            savedX = getX();
            savedY = getY();
            savedW = getWidth();
            savedH = getHeight();
            javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
            javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
            setX(bounds.getMinX());
            setY(bounds.getMinY());
            setWidth(bounds.getWidth());
            setHeight(bounds.getHeight());
            isMaximized = true;
        }
    }

    // ── View Navigation ────────────────────────────────────────────

    public void showView(String viewKey) {
        canvasPane.setVisible(false);
        layoutsPane.setVisible(false);
        settingsPane.setVisible(false);
        aboutPane.setVisible(false);

        switch (viewKey) {
            case "presets":
                layoutsPane.setVisible(true);
                layoutsPane.toFront();
                break;
            case "settings":
                settingsPane.setVisible(true);
                settingsPane.toFront();
                break;
            case "about":
                aboutPane.setVisible(true);
                aboutPane.toFront();
                break;
            case "canvas":
            default:
                canvasPane.setVisible(true);
                canvasPane.toFront();
                break;
        }
    }

    public void refreshCanvasFromBackend() {
        if (apiClient == null) return;
        apiClient.getWidgets().thenAcceptAsync(result -> {
            if (Boolean.TRUE.equals(result.get("success"))) {
                List<Map<String, Object>> widgets = (List<Map<String, Object>>) result.get("widgets");
                Platform.runLater(() -> canvasPane.loadWidgetsFromBackend(widgets));
            }
        });
    }

    public DesktopPreviewCanvas getCanvasPane() { return canvasPane; }
    public SidebarPane getSidebarPane() { return sidebarPane; }
}
