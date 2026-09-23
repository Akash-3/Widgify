package com.widgify.desktop.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * WidgetPreviewCard represents a miniature drag-and-drop catalog entry in the Sidebar Widget Library.
 * Recreates the exact visual catalog layout from the design mockup.
 */
public class WidgetPreviewCard extends VBox {

    private final String widgetType;
    private final String widgetTitle;

    public WidgetPreviewCard(String widgetType, String widgetTitle, String description) {
        this.widgetType = widgetType;
        this.widgetTitle = widgetTitle;

        setPadding(new Insets(8, 10, 8, 10));
        setSpacing(4);
        setStyle("-fx-background-color: #121212; -fx-border-color: #222222; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");

        HBox cardRow = new HBox(10);
        cardRow.setAlignment(Pos.CENTER_LEFT);

        // Miniature Visual Preview Box
        VBox miniBox = new VBox();
        miniBox.setAlignment(Pos.CENTER);
        miniBox.setMinSize(52, 38);
        miniBox.setMaxSize(52, 38);
        miniBox.setStyle("-fx-background-color: #080808; -fx-border-color: #1a1a1a; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label miniLabel = new Label(getMiniatureVisual(widgetType));
        miniLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        miniLabel.setTextFill(Color.web("#ffffff"));
        miniBox.getChildren().add(miniLabel);

        // Title and Description Box
        VBox textGroup = new VBox(2);
        Label titleLabel = new Label(widgetTitle);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        titleLabel.setTextFill(Color.web("#ffffff"));

        Label descLabel = new Label(description);
        descLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 9));
        descLabel.setTextFill(Color.web("#888888"));

        textGroup.getChildren().addAll(titleLabel, descLabel);
        HBox.setHgrow(textGroup, Priority.ALWAYS);

        cardRow.getChildren().addAll(miniBox, textGroup);
        getChildren().add(cardRow);

        // Hover Effect
        setOnMouseEntered(e -> setStyle("-fx-background-color: #1a1a1a; -fx-border-color: #333333; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;"));
        setOnMouseExited(e -> setStyle("-fx-background-color: #121212; -fx-border-color: #222222; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;"));

        // Native JavaFX Drag-and-Drop Handler
        setOnDragDetected(event -> {
            Dragboard db = startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString(widgetType);
            db.setContent(content);
            event.consume();
        });
    }

    private String getMiniatureVisual(String type) {
        switch (type.toLowerCase()) {
            case "clock": return "12:48";
            case "timer": return "25:00";
            case "system": return "CPU 42%";
            case "weather": return "☁ 24°";
            case "notes": return "≡ Notes";
            case "tasks": return "☑ Tasks";
            case "launcher": return ":: Apps";
            default: return type.toUpperCase();
        }
    }

    public String getWidgetType() {
        return widgetType;
    }

    public String getWidgetTitle() {
        return widgetTitle;
    }
}
