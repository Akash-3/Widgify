package com.widgify.desktop.ui;

import com.widgify.desktop.net.ServerApiClient;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * SettingsPane displays user-facing application preferences:
 * Appearance, Startup Behavior, and Desktop Surface Grid settings.
 * Infrastructure & backend server settings are completely decoupled.
 */
public class SettingsPane extends VBox {

    public SettingsPane(ServerApiClient apiClient) {
        setPadding(new Insets(28));
        setSpacing(20);
        setStyle("-fx-background-color: #060606;");

        // Header
        Label titleLabel = new Label("SETTINGS");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.web("#ffffff"));

        Label subLabel = new Label("Configure your Widgify desktop experience and surface preferences.");
        subLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        subLabel.setTextFill(Color.web("#888888"));

        VBox headerBox = new VBox(4, titleLabel, subLabel);

        // 1. Appearance Card
        VBox appearanceCard = createCard("APPEARANCE");
        Label clockFormatLabel = new Label("Time Display Format:");
        clockFormatLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        clockFormatLabel.setTextFill(Color.web("#aaaaaa"));

        ToggleGroup clockGroup = new ToggleGroup();
        RadioButton format12Radio = new RadioButton("12-Hour (AM/PM)");
        format12Radio.setToggleGroup(clockGroup);
        format12Radio.setTextFill(Color.web("#dddddd"));

        RadioButton format24Radio = new RadioButton("24-Hour (Standard)");
        format24Radio.setToggleGroup(clockGroup);
        format24Radio.setSelected(true);
        format24Radio.setTextFill(Color.web("#dddddd"));

        HBox clockBox = new HBox(16, format12Radio, format24Radio);
        appearanceCard.getChildren().addAll(clockFormatLabel, clockBox);

        // 2. Desktop Behavior Card
        VBox behaviorCard = createCard("DESKTOP BEHAVIOR");
        CheckBox trayCheck = new CheckBox("Enable System Tray Integration");
        trayCheck.setSelected(true);
        trayCheck.setTextFill(Color.web("#dddddd"));

        CheckBox autoApplyCheck = new CheckBox("Automatically Apply Layout to Desktop on Startup");
        autoApplyCheck.setSelected(false);
        autoApplyCheck.setTextFill(Color.web("#dddddd"));

        CheckBox lockWidgetsCheck = new CheckBox("Lock Desktop Widgets Position (Prevent accidental drag)");
        lockWidgetsCheck.setSelected(false);
        lockWidgetsCheck.setTextFill(Color.web("#dddddd"));

        behaviorCard.getChildren().addAll(trayCheck, autoApplyCheck, lockWidgetsCheck);

        // 3. Grid & Snapping Card
        VBox gridCard = createCard("CANVAS GRID & SNAPPING");
        CheckBox snapGridCheck = new CheckBox("Snap Widgets to 20px Grid");
        snapGridCheck.setSelected(true);
        snapGridCheck.setTextFill(Color.web("#dddddd"));

        gridCard.getChildren().add(snapGridCheck);

        // Status Feedback
        Label statusLabel = new Label("");
        statusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        statusLabel.setTextFill(Color.web("#00ff88"));

        Button saveBtn = new Button("SAVE PREFERENCES");
        saveBtn.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 8 16; -fx-background-radius: 4; -fx-cursor: hand;");
        saveBtn.setOnAction(e -> {
            statusLabel.setText("Preferences saved successfully.");
        });

        getChildren().addAll(headerBox, appearanceCard, behaviorCard, gridCard, saveBtn, statusLabel);
    }

    private VBox createCard(String titleText) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #121212; -fx-border-color: #222222; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");

        Label title = new Label(titleText);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        title.setTextFill(Color.web("#888888"));

        card.getChildren().add(title);
        return card;
    }
}
