package com.widgify.desktop.widgets;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.Map;

/**
 * TimerDesktopWidget
 *
 * Compact focus timer designed to remain fully contained inside
 * the Widgify widget bounds at all supported sizes.
 */
public class TimerDesktopWidget implements DesktopWidget {

    private static final double RING_RADIUS = 34;
    private static final double RING_SIZE = 76;

    private final VBox container;
    private final Label displayLabel;
    private final Button startPauseBtn;
    private final Button resetBtn;
    private final Arc progressArc;

    private int targetPresetMinutes = 25;
    private int secondsRemaining = 25 * 60;
    private int secondsElapsed = 0;

    private boolean isRunning = false;
    private boolean isCountdownMode = true;

    private Timeline timeline;

    public TimerDesktopWidget() {

        // ============================================================
        // ROOT
        // ============================================================

        container = new VBox(4);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(7, 8, 7, 8));

        container.setMinSize(180, 155);
        container.setPrefSize(200, 170);

        container.setStyle(
                "-fx-background-color: transparent;"
        );

        // ============================================================
        // HEADER
        // ============================================================

        Label headerLabel = new Label("FOCUS TIMER");

        headerLabel.setFont(
                Font.font("Segoe UI", FontWeight.BOLD, 9)
        );

        headerLabel.setTextFill(
                Color.web("#888888")
        );

        headerLabel.setMaxWidth(Double.MAX_VALUE);
        headerLabel.setAlignment(Pos.CENTER);

        // ============================================================
        // TIMER RING
        // ============================================================

        StackPane ringStack = new StackPane();

        ringStack.setMinSize(RING_SIZE, RING_SIZE);
        ringStack.setPrefSize(RING_SIZE, RING_SIZE);
        ringStack.setMaxSize(RING_SIZE, RING_SIZE);

        Circle backgroundCircle = new Circle(
                RING_RADIUS
        );

        backgroundCircle.setFill(
                Color.TRANSPARENT
        );

        backgroundCircle.setStroke(
                Color.web("#222222")
        );

        backgroundCircle.setStrokeWidth(3);

        progressArc = new Arc(
                0,
                0,
                RING_RADIUS,
                RING_RADIUS,
                90,
                360
        );

        progressArc.setType(
                ArcType.OPEN
        );

        progressArc.setFill(
                Color.TRANSPARENT
        );

        progressArc.setStroke(
                Color.web("#cccccc")
        );

        progressArc.setStrokeWidth(3);

        progressArc.setStrokeLineCap(
                javafx.scene.shape.StrokeLineCap.ROUND
        );

        displayLabel = new Label("25:00");

        displayLabel.setFont(
                Font.font(
                        "Segoe UI",
                        FontWeight.BOLD,
                        18
                )
        );

        displayLabel.setTextFill(
                Color.WHITE
        );

        ringStack.getChildren().addAll(
                backgroundCircle,
                progressArc,
                displayLabel
        );

        // ============================================================
        // CONTROLS
        // ============================================================

        startPauseBtn = new Button("▶ START");

        startPauseBtn.setFocusTraversable(false);

        startPauseBtn.setOnAction(
                e -> toggleTimer()
        );

        styleStartButton(false);

        resetBtn = new Button("RESET");

        resetBtn.setFocusTraversable(false);

        resetBtn.setOnAction(
                e -> resetTimer()
        );

        styleResetButton();

        HBox controls = new HBox(
                6,
                startPauseBtn,
                resetBtn
        );

        controls.setAlignment(
                Pos.CENTER
        );

        controls.setMinHeight(27);
        controls.setPrefHeight(27);

        // ============================================================
        // FINAL LAYOUT
        // ============================================================

        container.getChildren().addAll(
                headerLabel,
                ringStack,
                controls
        );

        updateDisplay();
    }

    // ================================================================
    // STYLING
    // ================================================================

    private void styleStartButton(boolean running) {

        if (running) {

            startPauseBtn.setStyle(
                    "-fx-background-color: #cc4444;" +
                    "-fx-text-fill: #ffffff;" +
                    "-fx-font-family: 'Segoe UI';" +
                    "-fx-font-weight: bold;" +
                    "-fx-font-size: 9px;" +
                    "-fx-padding: 4 11;" +
                    "-fx-background-radius: 4;" +
                    "-fx-cursor: hand;"
            );

        } else {

            startPauseBtn.setStyle(
                    "-fx-background-color: #ffffff;" +
                    "-fx-text-fill: #000000;" +
                    "-fx-font-family: 'Segoe UI';" +
                    "-fx-font-weight: bold;" +
                    "-fx-font-size: 9px;" +
                    "-fx-padding: 4 11;" +
                    "-fx-background-radius: 4;" +
                    "-fx-cursor: hand;"
            );
        }
    }

    private void styleResetButton() {

        resetBtn.setStyle(
                "-fx-background-color: #1a1a1a;" +
                "-fx-text-fill: #aaaaaa;" +
                "-fx-font-family: 'Segoe UI';" +
                "-fx-font-size: 9px;" +
                "-fx-padding: 4 9;" +
                "-fx-background-radius: 4;" +
                "-fx-border-color: #333333;" +
                "-fx-border-radius: 4;" +
                "-fx-cursor: hand;"
        );
    }

    // ================================================================
    // DESKTOP WIDGET
    // ================================================================

    @Override
    public String getWidgetType() {
        return "timer";
    }

    @Override
    public String getWidgetTitle() {
        return "Timer";
    }

    @Override
    public Node getWidgetNode() {
        return container;
    }

    // ================================================================
    // CONFIGURATION
    // ================================================================

    @Override
    public void onInitialize(Map<String, Object> config) {

        applyConfig(config);

        if (timeline == null) {

            timeline = new Timeline(
                    new KeyFrame(
                            Duration.seconds(1),
                            e -> tick()
                    )
            );

            timeline.setCycleCount(
                    Animation.INDEFINITE
            );
        }
    }

    public void applyConfig(
            Map<String, Object> config
    ) {

        if (config == null) {
            return;
        }

        if (!config.containsKey("preset")) {
            return;
        }

        Object preset = config.get("preset");

        if (preset instanceof Number) {

            setPresetMinutes(
                    ((Number) preset).intValue()
            );

        } else if (preset != null) {

            try {

                setPresetMinutes(
                        Integer.parseInt(
                                preset.toString()
                        )
                );

            } catch (NumberFormatException ignored) {
                // Ignore invalid configuration.
            }
        }
    }

    public void setPresetMinutes(int minutes) {

        if (timeline != null) {
            timeline.stop();
        }

        targetPresetMinutes = minutes;
        isRunning = false;

        if (minutes > 0) {

            isCountdownMode = true;
            secondsRemaining = minutes * 60;

        } else {

            isCountdownMode = false;
            secondsElapsed = 0;
        }

        updateDisplay();

        startPauseBtn.setText(
                "▶ START"
        );

        styleStartButton(false);
    }

    public int getPresetMinutes() {
        return targetPresetMinutes;
    }

    // ================================================================
    // TIMER LOGIC
    // ================================================================

    private void tick() {

        if (isCountdownMode) {

            if (secondsRemaining > 0) {

                secondsRemaining--;

                updateDisplay();

            } else {

                timeline.stop();

                isRunning = false;

                startPauseBtn.setText(
                        "▶ START"
                );

                styleStartButton(false);
            }

        } else {

            secondsElapsed++;

            updateDisplay();
        }
    }

    private void toggleTimer() {

        if (timeline == null) {

            timeline = new Timeline(
                    new KeyFrame(
                            Duration.seconds(1),
                            e -> tick()
                    )
            );

            timeline.setCycleCount(
                    Animation.INDEFINITE
            );
        }

        if (isRunning) {

            timeline.pause();

            isRunning = false;

            startPauseBtn.setText(
                    "RESUME"
            );

            styleStartButton(false);

        } else {

            timeline.play();

            isRunning = true;

            startPauseBtn.setText(
                    "PAUSE"
            );

            styleStartButton(true);
        }
    }

    private void resetTimer() {

        if (timeline != null) {
            timeline.stop();
        }

        isRunning = false;

        if (isCountdownMode) {

            secondsRemaining =
                    targetPresetMinutes * 60;

        } else {

            secondsElapsed = 0;
        }

        updateDisplay();

        startPauseBtn.setText(
                "▶ START"
        );

        styleStartButton(false);
    }

    // ================================================================
    // DISPLAY
    // ================================================================

    private void updateDisplay() {

        int totalSeconds =
                targetPresetMinutes * 60;

        int displaySeconds =
                isCountdownMode
                        ? secondsRemaining
                        : secondsElapsed;

        int minutes =
                displaySeconds / 60;

        int seconds =
                displaySeconds % 60;

        displayLabel.setText(
                String.format(
                        "%02d:%02d",
                        minutes,
                        seconds
                )
        );

        if (isCountdownMode && totalSeconds > 0) {

            double ratio =
                    (double) secondsRemaining
                            / totalSeconds;

            ratio = Math.max(
                    0,
                    Math.min(1, ratio)
            );

            progressArc.setLength(
                    -360 * ratio
            );

        } else {

            progressArc.setLength(-360);
        }
    }

    // ================================================================
    // CLOSE
    // ================================================================

    @Override
    public void onClose() {

        if (timeline != null) {
            timeline.stop();
        }

        isRunning = false;
    }
}