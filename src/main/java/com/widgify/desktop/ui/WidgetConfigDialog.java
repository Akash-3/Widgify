package com.widgify.desktop.ui;

import com.widgify.desktop.net.ServerApiClient;
import com.widgify.desktop.widgets.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class WidgetConfigDialog extends Stage {

    public static void showConfigDialog(Stage parentStage, DesktopWidget widget, ServerApiClient apiClient) {
        if (widget == null) return;

        WidgetConfigDialog dialog = new WidgetConfigDialog(parentStage, widget, apiClient);
        dialog.show();
    }

    private WidgetConfigDialog(Stage parentStage, DesktopWidget widget, ServerApiClient apiClient) {
        initModality(Modality.APPLICATION_MODAL);
        if (parentStage != null) {
            initOwner(parentStage);
        }
        initStyle(StageStyle.UTILITY);
        setTitle("Configure " + widget.getWidgetTitle());

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #0a0a0a; -fx-border-color: #333333; -fx-border-width: 1;");

        Label header = new Label("CONFIGURE " + widget.getWidgetTitle().toUpperCase());
        header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        header.setTextFill(Color.web("#ffffff"));

        VBox contentBox = new VBox(10);
        contentBox.setAlignment(Pos.CENTER_LEFT);

        Button saveBtn = new Button("SAVE CONFIGURATION");
        saveBtn.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 14; -fx-cursor: hand;");

        Button cancelBtn = new Button("CANCEL");
        cancelBtn.setStyle("-fx-background-color: #222222; -fx-text-fill: #aaaaaa; -fx-font-size: 11px; -fx-padding: 6 14; -fx-border-color: #333333; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> close());

        Label statusLabel = new Label("");
        statusLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
        statusLabel.setTextFill(Color.web("#00ff88"));

        String type = widget.getWidgetType().toLowerCase();

        if ("clock".equals(type) && widget instanceof ClockDesktopWidget) {
            ClockDesktopWidget clockWidget = (ClockDesktopWidget) widget;
            Label formatLabel = new Label("Time Display Format:");
            formatLabel.setTextFill(Color.web("#cccccc"));

            ToggleGroup formatGroup = new ToggleGroup();
            RadioButton mode24 = new RadioButton("24-Hour Format (e.g. 14:30:00)");
            mode24.setToggleGroup(formatGroup);
            mode24.setTextFill(Color.web("#ffffff"));
            mode24.setSelected(clockWidget.is24HourFormat());

            RadioButton mode12 = new RadioButton("12-Hour Format (e.g. 02:30:00 PM)");
            mode12.setToggleGroup(formatGroup);
            mode12.setTextFill(Color.web("#ffffff"));
            mode12.setSelected(!clockWidget.is24HourFormat());

            contentBox.getChildren().addAll(formatLabel, mode24, mode12);

            saveBtn.setOnAction(e -> {
                String selectedFmt = mode12.isSelected() ? "12h" : "24h";
                String jsonConfig = "{\"format\":\"" + selectedFmt + "\"}";
                
                // Apply locally
                java.util.Map<String, Object> cfgMap = new java.util.HashMap<>();
                cfgMap.put("format", selectedFmt);
                clockWidget.applyConfig(cfgMap);

                // Persist to server
                apiClient.updateWidgetConfig(getWidgetStageId(parentStage), jsonConfig).thenAccept(ok -> {
                    javafx.application.Platform.runLater(this::close);
                });
            });

        } else if ("timer".equals(type) && widget instanceof TimerDesktopWidget) {
            TimerDesktopWidget timerWidget = (TimerDesktopWidget) widget;
            Label presetLabel = new Label("Countdown Timer Duration:");
            presetLabel.setTextFill(Color.web("#cccccc"));

            ToggleGroup presetGroup = new ToggleGroup();
            RadioButton pStopwatch = new RadioButton("Stopwatch Mode (Count Up)");
            pStopwatch.setToggleGroup(presetGroup);
            pStopwatch.setTextFill(Color.web("#ffffff"));

            RadioButton p5 = new RadioButton("5 Minutes Preset");
            p5.setToggleGroup(presetGroup);
            p5.setTextFill(Color.web("#ffffff"));

            RadioButton p15 = new RadioButton("15 Minutes Preset");
            p15.setToggleGroup(presetGroup);
            p15.setTextFill(Color.web("#ffffff"));

            RadioButton p25 = new RadioButton("25 Minutes (Pomodoro Preset)");
            p25.setToggleGroup(presetGroup);
            p25.setTextFill(Color.web("#ffffff"));

            int currentPreset = timerWidget.getPresetMinutes();
            if (currentPreset == 5) p5.setSelected(true);
            else if (currentPreset == 15) p15.setSelected(true);
            else if (currentPreset == 25) p25.setSelected(true);
            else pStopwatch.setSelected(true);

            contentBox.getChildren().addAll(presetLabel, pStopwatch, p5, p15, p25);

            saveBtn.setOnAction(e -> {
                int selectedMins = 0;
                if (p5.isSelected()) selectedMins = 5;
                else if (p15.isSelected()) selectedMins = 15;
                else if (p25.isSelected()) selectedMins = 25;

                String jsonConfig = "{\"preset\":" + selectedMins + "}";
                timerWidget.setPresetMinutes(selectedMins);

                apiClient.updateWidgetConfig(getWidgetStageId(parentStage), jsonConfig).thenAccept(ok -> {
                    javafx.application.Platform.runLater(this::close);
                });
            });

        } else if ("weather".equals(type) && widget instanceof WeatherDesktopWidget) {
            WeatherDesktopWidget weatherWidget = (WeatherDesktopWidget) widget;
            Label locLabel = new Label("Default Weather Location / City:");
            locLabel.setTextFill(Color.web("#cccccc"));

            TextField cityField = new TextField("Bhubaneswar");
            cityField.setStyle("-fx-background-color: #141414; -fx-text-fill: #ffffff; -fx-border-color: #333333; -fx-padding: 6;");

            contentBox.getChildren().addAll(locLabel, cityField);

            saveBtn.setOnAction(e -> {
                String newCity = cityField.getText().trim();
                if (!newCity.isEmpty()) {
                    String jsonConfig = "{\"location\":\"" + newCity + "\"}";
                    java.util.Map<String, Object> cfg = new java.util.HashMap<>();
                    cfg.put("location", newCity);
                    weatherWidget.onInitialize(cfg);

                    apiClient.updateWidgetConfig(getWidgetStageId(parentStage), jsonConfig).thenAccept(ok -> {
                        javafx.application.Platform.runLater(this::close);
                    });
                }
            });

        } else {
            Label noConfigLabel = new Label("No configuration required for this widget.");
            noConfigLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
            noConfigLabel.setTextFill(Color.web("#888888"));
            contentBox.getChildren().add(noConfigLabel);

            saveBtn.setText("OK");
            saveBtn.setOnAction(e -> close());
            cancelBtn.setVisible(false);
        }

        HBox btnBox = new HBox(8);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.getChildren().addAll(cancelBtn, saveBtn);

        root.getChildren().addAll(header, contentBox, statusLabel, btnBox);

        Scene scene = new Scene(root, 340, 240);
        scene.setFill(Color.web("#0a0a0a"));
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                close();
            }
        });
        setScene(scene);
    }

    private int getWidgetStageId(Stage stage) {
        if (stage instanceof DesktopWidgetStage) {
            return ((DesktopWidgetStage) stage).getWidgetId();
        }
        return 0;
    }
}
