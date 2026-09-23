package com.widgify.desktop.widgets;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * QuickLauncherDesktopWidget provides a clean application shortcut dock
 * matching the Rainmeter / Nothing OS aesthetic.
 */
public class QuickLauncherDesktopWidget implements DesktopWidget {

    private final VBox container;

    private static class WebShortcut {
        final String name;
        final String iconText;
        final String bgStyle;
        final String url;

        WebShortcut(String name, String iconText, String bgStyle, String url) {
            this.name = name;
            this.iconText = iconText;
            this.bgStyle = bgStyle;
            this.url = url;
        }
    }

    private final List<WebShortcut> allowlist;

    public QuickLauncherDesktopWidget() {
        container = new VBox(10);
        container.setPadding(new Insets(12, 16, 12, 16));
        container.setStyle("-fx-background-color: transparent;");

        // Predefined safe allowlist of web shortcuts
        allowlist = new ArrayList<>();
        allowlist.add(new WebShortcut("VS Code", "⚡", "-fx-background-color: #007acc;", "https://vscode.dev/"));
        allowlist.add(new WebShortcut("GitHub", "🐱", "-fx-background-color: #24292e;", "https://github.com/"));
        allowlist.add(new WebShortcut("Google", "G", "-fx-background-color: #4285f4;", "https://www.google.com/"));
        allowlist.add(new WebShortcut("MDN", "M", "-fx-background-color: #1b1b1b;", "https://developer.mozilla.org/"));
        allowlist.add(new WebShortcut("ChatGPT", "🤖", "-fx-background-color: #10a37f;", "https://chatgpt.com/"));
        allowlist.add(new WebShortcut("Stack Overflow", "🥞", "-fx-background-color: #f48024;", "https://stackoverflow.com/"));

        // Header
        Label title = new Label("QUICK LAUNCHER");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        title.setTextFill(Color.web("#888888"));

        // Shortcut Dock HBox
        HBox dockBox = new HBox(16);
        dockBox.setAlignment(Pos.CENTER_LEFT);

        for (WebShortcut shortcut : allowlist) {
            VBox tileBox = createShortcutTile(shortcut);
            dockBox.getChildren().add(tileBox);
        }

        container.getChildren().addAll(title, dockBox);
    }

    private VBox createShortcutTile(WebShortcut shortcut) {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-cursor: hand;");

        VBox iconTile = new VBox();
        iconTile.setAlignment(Pos.CENTER);
        iconTile.setMinSize(42, 42);
        iconTile.setMaxSize(42, 42);
        iconTile.setStyle(shortcut.bgStyle + "; -fx-border-color: #333333; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label iconLabel = new Label(shortcut.iconText);
        iconLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        iconLabel.setTextFill(Color.web("#ffffff"));
        iconTile.getChildren().add(iconLabel);

        Label nameLabel = new Label(shortcut.name);
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 9));
        nameLabel.setTextFill(Color.web("#aaaaaa"));

        box.getChildren().addAll(iconTile, nameLabel);
        box.setOnMouseClicked(e -> openUrlInBrowser(shortcut.url));
        return box;
    }

    private void openUrlInBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public String getWidgetType() {
        return "launcher";
    }

    @Override
    public String getWidgetTitle() {
        return "Quick Launcher";
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
