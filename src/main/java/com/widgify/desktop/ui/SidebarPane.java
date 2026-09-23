package com.widgify.desktop.ui;

import com.widgify.desktop.net.ServerApiClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.function.Consumer;

/**
 * SidebarPane displays the application branding, navigation links, and the 7 canonical widget library catalog.
 * Styled after the design reference mockup.
 */
public class SidebarPane extends VBox {

    private final ServerApiClient apiClient;
    private final Consumer<String> navigationHandler;
    private Button activeNavBtn = null;

    public SidebarPane(ServerApiClient apiClient, Consumer<String> navigationHandler, Runnable logoutHandler) {
        this.apiClient = apiClient;
        this.navigationHandler = navigationHandler;

        setPrefWidth(240);
        setMinWidth(220);
        setMaxWidth(260);
        setPadding(new Insets(18, 14, 18, 14));
        setSpacing(16);
        setStyle("-fx-background-color: #0a0a0a; -fx-border-color: #222222; -fx-border-width: 0 1 0 0;");

        // 1. Branding Header
        VBox brandBox = new VBox(2);
        Label titleLabel = new Label("W I D G I F Y");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.web("#ffffff"));

        Label subtitleLabel = new Label("Your Desktop. Your Widgets.");
        subtitleLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
        subtitleLabel.setTextFill(Color.web("#888888"));

        brandBox.getChildren().addAll(titleLabel, subtitleLabel);

        // 2. Navigation List
        VBox navBox = new VBox(4);
        Button canvasNav = createNavButton("⊞  Desktop Canvas", "canvas");
        Button presetsNav = createNavButton("⧉  Layout Presets", "presets");
        Button settingsNav = createNavButton("⚙  Settings", "settings");
        Button aboutNav = createNavButton("ℹ  About", "about");

        navBox.getChildren().addAll(canvasNav, presetsNav, settingsNav, aboutNav);
        setActiveNav(canvasNav);

        // 3. Widget Library Catalog Section
        Label libTitle = new Label("WIDGET LIBRARY");
        libTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        libTitle.setTextFill(Color.web("#888888"));

        Label libSub = new Label("Drag widgets to the canvas");
        libSub.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 9));
        libSub.setTextFill(Color.web("#555555"));

        VBox libHeader = new VBox(2, libTitle, libSub);

        VBox libraryList = new VBox(6);
        libraryList.getChildren().addAll(
                new WidgetPreviewCard("clock", "Clock", "Time and date"),
                new WidgetPreviewCard("timer", "Timer", "Focus countdown"),
                new WidgetPreviewCard("system", "System", "CPU, memory and battery"),
                new WidgetPreviewCard("weather", "Weather", "Current conditions and forecast"),
                new WidgetPreviewCard("notes", "Notes", "Quick desktop notes"),
                new WidgetPreviewCard("tasks", "Tasks", "Your active tasks"),
                new WidgetPreviewCard("launcher", "Launcher", "Quick shortcuts")
        );

        ScrollPane libScroll = new ScrollPane(libraryList);
        libScroll.setFitToWidth(true);
        libScroll.setStyle("-fx-background: #0a0a0a; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(libScroll, Priority.ALWAYS);

        // 4. Logout / Bottom Status Footer
        Button logoutBtn = new Button("LOGOUT");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setStyle("-fx-background-color: #1a1a1a; -fx-text-fill: #ff5555; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 6 12; -fx-border-color: #333333; -fx-border-radius: 4; -fx-cursor: hand;");
        logoutBtn.setOnAction(e -> {
            if (logoutHandler != null) logoutHandler.run();
        });

        Label versionLabel = new Label("Widgify v2.0 • Make your desktop yours.");
        versionLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 8));
        versionLabel.setTextFill(Color.web("#444444"));

        getChildren().addAll(brandBox, navBox, libHeader, libScroll, logoutBtn, versionLabel);
    }

    private Button createNavButton(String labelText, String navKey) {
        Button btn = new Button(labelText);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #aaaaaa; -fx-font-size: 11px; -fx-padding: 8 12; -fx-background-radius: 4; -fx-cursor: hand;");

        btn.setOnAction(e -> {
            setActiveNav(btn);
            if (navigationHandler != null) navigationHandler.accept(navKey);
        });
        return btn;
    }

    private void setActiveNav(Button btn) {
        if (activeNavBtn != null) {
            activeNavBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #aaaaaa; -fx-font-size: 11px; -fx-padding: 8 12; -fx-background-radius: 4; -fx-cursor: hand;");
        }
        activeNavBtn = btn;
        activeNavBtn.setStyle("-fx-background-color: #1a1a1a; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 8 12; -fx-background-radius: 4; -fx-cursor: hand;");
    }
}
