package com.widgify.desktop.ui;

import com.widgify.desktop.net.ServerApiClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WidgetManagerStage extends Stage {

    private final ServerApiClient apiClient;
    private final WidgetWindowManager windowManager;
    private final Runnable logoutHandler;

    private final VBox galleryContainer;
    private final Label userEmailLabel;
    private final ComboBox<String> presetCombo;

    private static final String[] SUPPORTED_TYPES = {
            "clock", "timer", "system", "weather", "notes", "tasks", "server_health", "launcher"
    };

    private static final Map<String, String> DISPLAY_NAMES = new HashMap<>();
    static {
        DISPLAY_NAMES.put("clock", "Digital Clock");
        DISPLAY_NAMES.put("timer", "Study Countdown Timer");
        DISPLAY_NAMES.put("system", "Local System Monitor");
        DISPLAY_NAMES.put("weather", "Weather Forecast");
        DISPLAY_NAMES.put("notes", "Sticky Notes");
        DISPLAY_NAMES.put("tasks", "Tasks & To-Do");
        DISPLAY_NAMES.put("server_health", "Tomcat Server Health");
        DISPLAY_NAMES.put("launcher", "Quick Web Launcher");
    }

    public WidgetManagerStage(ServerApiClient apiClient, WidgetWindowManager windowManager, Runnable logoutHandler) {
        this.apiClient = apiClient;
        this.windowManager = windowManager;
        this.logoutHandler = logoutHandler;

        setTitle("Widgify — Desktop Widget Manager");

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #0a0a0a; -fx-border-color: #222222; -fx-border-width: 1;");

        // 1. Top Bar: Branding & User Profile
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 10, 0));
        topBar.setStyle("-fx-border-color: transparent transparent #222222 transparent; -fx-border-width: 1;");

        Label brandLabel = new Label("WIDGIFY");
        brandLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        brandLabel.setTextFill(Color.web("#ffffff"));

        Label subBrand = new Label("MANAGER");
        subBrand.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        subBrand.setTextFill(Color.web("#888888"));

        HBox brandBox = new HBox(6, brandLabel, subBrand);
        brandBox.setAlignment(Pos.BASELINE_LEFT);
        HBox.setHgrow(brandBox, Priority.ALWAYS);

        String email = apiClient != null && apiClient.getCurrentAuthenticatedEmail() != null
                ? apiClient.getCurrentAuthenticatedEmail() : "user@widgify.desktop";
        userEmailLabel = new Label(email);
        userEmailLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
        userEmailLabel.setTextFill(Color.web("#aaaaaa"));

        Button refreshBtn = new Button("REFRESH");
        refreshBtn.setStyle("-fx-background-color: #1a1a1a; -fx-text-fill: #ffffff; -fx-font-size: 10px; -fx-padding: 4 10; -fx-border-color: #333333; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> refreshWidgets());

        Button logoutBtn = new Button("LOGOUT");
        logoutBtn.setStyle("-fx-background-color: #ff5555; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 10; -fx-cursor: hand;");
        logoutBtn.setOnAction(e -> {
            if (logoutHandler != null) {
                logoutHandler.run();
            }
            close();
        });

        topBar.getChildren().addAll(brandBox, userEmailLabel, refreshBtn, logoutBtn);

        // 2. Preset Layout & Master Controls Section
        HBox masterControlsBox = new HBox(10);
        masterControlsBox.setAlignment(Pos.CENTER_LEFT);

        Label presetLabel = new Label("PRESETS:");
        presetLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        presetLabel.setTextFill(Color.web("#888888"));

        presetCombo = new ComboBox<>();
        presetCombo.getItems().addAll("Select Preset...", "Minimalist", "Productivity", "Developer", "All Active");
        presetCombo.setValue("Select Preset...");
        presetCombo.setStyle("-fx-background-color: #141414; -fx-text-fill: #ffffff; -fx-border-color: #333333; -fx-font-size: 10px;");

        Button applyPresetBtn = new Button("APPLY");
        applyPresetBtn.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 10; -fx-cursor: hand;");
        applyPresetBtn.setOnAction(e -> {
            String selected = presetCombo.getValue();
            if (selected != null && !selected.startsWith("Select")) {
                windowManager.applyLayoutPreset(selected);
                refreshWidgets();
            }
        });

        Button showAllBtn = new Button("SHOW ALL WIDGETS");
        showAllBtn.setStyle("-fx-background-color: #1a1a1a; -fx-text-fill: #00ff88; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 10; -fx-border-color: #333333; -fx-cursor: hand;");
        showAllBtn.setOnAction(e -> {
            for (String type : SUPPORTED_TYPES) {
                windowManager.enableWidget(type);
            }
            refreshWidgets();
        });

        Button hideAllBtn = new Button("HIDE ALL WIDGETS");
        hideAllBtn.setStyle("-fx-background-color: #1a1a1a; -fx-text-fill: #ff5555; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 10; -fx-border-color: #333333; -fx-cursor: hand;");
        hideAllBtn.setOnAction(e -> {
            for (String type : SUPPORTED_TYPES) {
                windowManager.disableWidget(type);
            }
            refreshWidgets();
        });

        masterControlsBox.getChildren().addAll(presetLabel, presetCombo, applyPresetBtn, showAllBtn, hideAllBtn);

        // 3. Network Health Indicator Line
        HBox healthBox = new HBox(8);
        healthBox.setAlignment(Pos.CENTER_LEFT);
        Label netTitle = new Label("SERVER CONNECTION:");
        netTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        netTitle.setTextFill(Color.web("#888888"));

        networkStatusLabel = new Label("ONLINE");
        networkStatusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        networkStatusLabel.setTextFill(Color.web("#00ff88"));

        healthBox.getChildren().addAll(netTitle, networkStatusLabel);

        // 4. Widget Gallery Grid
        Label galleryTitle = new Label("ALL SUPPORTED DESKTOP WIDGETS (8)");
        galleryTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        galleryTitle.setTextFill(Color.web("#666666"));

        galleryContainer = new VBox(8);
        galleryContainer.setPadding(new Insets(4, 0, 4, 0));

        ScrollPane scrollPane = new ScrollPane(galleryContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #0a0a0a; -fx-background-color: transparent; -fx-border-color: #1a1a1a;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        root.getChildren().addAll(topBar, masterControlsBox, healthBox, galleryTitle, scrollPane);

        Scene scene = new Scene(root, 620, 500);
        scene.setFill(Color.web("#0a0a0a"));
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                close();
            }
        });
        setScene(scene);

        setupHealthCheckTimer();
        refreshWidgets();
    }

    private final Label networkStatusLabel;
    private javafx.animation.Timeline healthTimeline;
    private int consecutiveFailures = 0;

    private void setupHealthCheckTimer() {
        healthTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(15), e -> pingServerHealth())
        );
        healthTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        healthTimeline.play();
    }

    private void pingServerHealth() {
        if (apiClient == null) return;
        apiClient.getServerHealth().thenAcceptAsync(res -> {
            Platform.runLater(() -> {
                boolean ok = Boolean.TRUE.equals(res.get("success"));
                if (ok) {
                    consecutiveFailures = 0;
                    networkStatusLabel.setText("ONLINE");
                    networkStatusLabel.setTextFill(Color.web("#00ff88"));
                } else {
                    consecutiveFailures++;
                    if (consecutiveFailures == 1) {
                        networkStatusLabel.setText("RECONNECTING...");
                        networkStatusLabel.setTextFill(Color.web("#ffcc00"));
                    } else {
                        networkStatusLabel.setText("OFFLINE");
                        networkStatusLabel.setTextFill(Color.web("#ff5555"));
                    }
                }
            });
        });
    }

    public void refreshWidgets() {
        galleryContainer.getChildren().clear();

        for (String type : SUPPORTED_TYPES) {
            boolean isActive = windowManager.isWidgetActive(type);
            String title = DISPLAY_NAMES.getOrDefault(type, type.toUpperCase());

            HBox card = new HBox(12);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(10, 14, 10, 14));
            card.setStyle("-fx-background-color: #121212; -fx-border-color: #222222; -fx-border-radius: 6; -fx-background-radius: 6;");

            VBox info = new VBox(2);
            Label nameLabel = new Label(title);
            nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            nameLabel.setTextFill(Color.web("#ffffff"));

            Label typeLabel = new Label("Type: " + type);
            typeLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
            typeLabel.setTextFill(Color.web("#666666"));

            info.getChildren().addAll(nameLabel, typeLabel);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label statusIndicator = new Label(isActive ? "ACTIVE ON DESKTOP" : "HIDDEN / DISABLED");
            statusIndicator.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
            statusIndicator.setTextFill(Color.web(isActive ? "#00ff88" : "#666666"));

            Button toggleBtn = new Button(isActive ? "HIDE" : "ENABLE");
            if (isActive) {
                toggleBtn.setStyle("-fx-background-color: #222222; -fx-text-fill: #ff5555; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 12; -fx-border-color: #333333; -fx-cursor: hand;");
                toggleBtn.setOnAction(e -> {
                    windowManager.disableWidget(type);
                    refreshWidgets();
                });
            } else {
                toggleBtn.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 12; -fx-cursor: hand;");
                toggleBtn.setOnAction(e -> {
                    windowManager.enableWidget(type);
                    refreshWidgets();
                });
            }

            card.getChildren().addAll(info, statusIndicator, toggleBtn);
            galleryContainer.getChildren().add(card);
        }
    }
}
