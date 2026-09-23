package com.widgify.desktop.widgets;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * ClockDesktopWidget provides a Rainmeter / Nothing OS inspired minimalist clock.
 * Time is the dominant visual element with secondary day and date typography.
 */
public class ClockDesktopWidget implements DesktopWidget {

    private final HBox container;
    private final Label timeLabel;
    private final Label dayLabel;
    private final Label dateLabel;
    private Timeline timeline;

    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("EEE");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private boolean is24HourFormat = true;

    public ClockDesktopWidget() {
        container = new HBox(16);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(16, 20, 16, 20));
        container.setStyle("-fx-background-color: transparent;");

        timeLabel = new Label("12:48");
        timeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 42));
        timeLabel.setTextFill(Color.web("#ffffff"));

        dayLabel = new Label("WED");
        dayLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        dayLabel.setTextFill(Color.web("#888888"));

        dateLabel = new Label("23 SEP 2026");
        dateLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
        dateLabel.setTextFill(Color.web("#666666"));

        VBox dateBox = new VBox(2, dayLabel, dateLabel);
        dateBox.setAlignment(Pos.CENTER_LEFT);

        container.getChildren().addAll(timeLabel, dateBox);
    }

    @Override
    public String getWidgetType() {
        return "clock";
    }

    @Override
    public String getWidgetTitle() {
        return "Clock";
    }

    @Override
    public Node getWidgetNode() {
        return container;
    }

    @Override
    public void onInitialize(Map<String, Object> config) {
        applyConfig(config);
        updateTime();
        if (timeline == null) {
            timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateTime()));
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.play();
        }
    }

    public void applyConfig(Map<String, Object> config) {
        if (config != null) {
            Object fmt = config.get("format");
            if (fmt != null && ("12h".equalsIgnoreCase(fmt.toString()) || "12".equals(fmt.toString()))) {
                is24HourFormat = false;
                timeFormatter = DateTimeFormatter.ofPattern("hh:mm");
            } else {
                is24HourFormat = true;
                timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            }
        }
        updateTime();
    }

    public boolean is24HourFormat() {
        return is24HourFormat;
    }

    private void updateTime() {
        LocalDateTime now = LocalDateTime.now();
        timeLabel.setText(now.format(timeFormatter));
        dayLabel.setText(now.format(DAY_FORMATTER).toUpperCase());
        dateLabel.setText(now.format(DATE_FORMATTER).toUpperCase());
    }

    @Override
    public void onClose() {
        if (timeline != null) {
            timeline.stop();
        }
    }
}
