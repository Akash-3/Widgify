package com.widgify.desktop.widgets;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Map;

public class GenericPlaceholderDesktopWidget implements DesktopWidget {

    private final VBox container;
    private final String widgetType;
    private final Label titleLabel;

    public GenericPlaceholderDesktopWidget(String widgetType) {
        this.widgetType = widgetType;

        container = new VBox(4);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(14));
        container.setStyle("-fx-background-color: transparent;");

        titleLabel = new Label(widgetType.toUpperCase() + " WIDGET");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.web("#ffffff"));

        Label noteLabel = new Label("Desktop widget engine active");
        noteLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
        noteLabel.setTextFill(Color.web("#777777"));

        container.getChildren().addAll(titleLabel, noteLabel);
    }

    @Override
    public String getWidgetType() {
        return widgetType;
    }

    @Override
    public String getWidgetTitle() {
        return widgetType.toUpperCase();
    }

    @Override
    public Node getWidgetNode() {
        return container;
    }

    @Override
    public void onInitialize(Map<String, Object> config) {
    }

    @Override
    public void onClose() {
    }
}
