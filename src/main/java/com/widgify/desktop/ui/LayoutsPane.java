package com.widgify.desktop.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.function.Consumer;

/**
 * LayoutsPane displays layout presets for loading organized widget arrangements into the canvas.
 */
public class LayoutsPane extends VBox {

    public LayoutsPane(Consumer<String> presetApplier) {
        setPadding(new Insets(24));
        setSpacing(16);
        setStyle("-fx-background-color: #060606;");

        Label titleLabel = new Label("LAYOUT PRESETS");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.web("#ffffff"));

        Label subLabel = new Label("Select a layout preset to populate your Desktop Preview Canvas.");
        subLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
        subLabel.setTextFill(Color.web("#888888"));

        VBox grid = new VBox(12);

        grid.getChildren().addAll(
                createPresetCard("Minimalist", "Clock + Weather", "Clean, distraction-free desktop focus.", presetApplier),
                createPresetCard("Productivity", "Clock + Tasks + Notes", "Designed for daily study and task management.", presetApplier),
                createPresetCard("Developer", "Clock + PC Health + Quick Launcher", "Tailored for software development and system monitoring.", presetApplier),
                createPresetCard("All Active", "All 7 Canonical Widgets", "Displays all supported widgets in an organized grid.", presetApplier)
        );

        getChildren().addAll(titleLabel, subLabel, grid);
    }

    private VBox createPresetCard(String presetName, String widgetsSummary, String description, Consumer<String> presetApplier) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #121212; -fx-border-color: #222222; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(presetName.toUpperCase());
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        nameLabel.setTextFill(Color.web("#ffffff"));
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        Button applyBtn = new Button("LOAD PRESET");
        applyBtn.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 12; -fx-cursor: hand;");
        applyBtn.setOnAction(e -> {
            if (presetApplier != null) presetApplier.accept(presetName);
        });

        header.getChildren().addAll(nameLabel, applyBtn);

        Label summary = new Label("Widgets: " + widgetsSummary);
        summary.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        summary.setTextFill(Color.web("#00ff88"));

        Label desc = new Label(description);
        desc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
        desc.setTextFill(Color.web("#aaaaaa"));

        card.getChildren().addAll(header, summary, desc);
        return card;
    }
}
