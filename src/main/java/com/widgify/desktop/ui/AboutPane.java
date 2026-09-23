package com.widgify.desktop.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

/**
 * AboutPane displays system information, architecture design, and version details
 * for the Widgify desktop widget platform.
 */
public class AboutPane extends VBox {

    public AboutPane() {
        setSpacing(20);
        setPadding(new Insets(30));
        setStyle("-fx-background-color: #0a0a0a;");

        // Header
        Label header = new Label("ABOUT WIDGIFY");
        header.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Label subtitle = new Label("Nothing OS & Rainmeter Inspired Modular Desktop Engine");
        subtitle.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 13px; -fx-text-fill: #888888;");

        VBox headerBox = new VBox(5, header, subtitle);

        // Architecture Card
        VBox archCard = createCard("ARCHITECTURE & DESIGN", 
            "• Architecture: Client-Server Modular Java Desktop Application\n" +
            "• Desktop Client: JavaFX Single-Window Designer Canvas + Transparent Overlay Stages\n" +
            "• Backend: External Apache Tomcat 10.1 (Jakarta Servlets, RESTful APIs, Session Auth)\n" +
            "• Persistence: JDBC, MySQL 8.0, 300ms Debounced Layout Synchronization\n" +
            "• Supported Widgets: Clock, Timer, System, Weather, Notes, Tasks, Launcher, Server Health"
        );

        // Tech Specs Card
        VBox specsCard = createCard("SPECIFICATIONS", 
            "• Version: 2.0.0-PHASE8-RELEASE\n" +
            "• Java Standard: Java 17 LTS / OpenJDK 17\n" +
            "• UI Framework: OpenJFX 17 (JavaFX)\n" +
            "• Tomcat WAR Package: 0 JavaFX JARs in WEB-INF/lib (Strict Decoupling)\n" +
            "• Security: JSESSIONID Cookie Authentication & Filter Security Scoping"
        );

        // Footer / Developer info
        Label footer = new Label("Developed for Advanced Java Project • Widgify Inc.");
        footer.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 11px; -fx-text-fill: #555555;");

        getChildren().addAll(headerBox, archCard, specsCard, footer);
    }

    private VBox createCard(String titleText, String bodyText) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setStyle(
            "-fx-background-color: #121212; " +
            "-fx-border-color: #222222; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 6px; " +
            "-fx-background-radius: 6px;"
        );

        Label title = new Label(titleText);
        title.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #dddddd;");

        Label body = new Label(bodyText);
        body.setWrapText(true);
        body.setStyle("-fx-font-family: 'Consolas', 'Segoe UI', monospace; -fx-font-size: 12px; -fx-text-fill: #aaaaaa; -fx-line-spacing: 4px;");

        card.getChildren().addAll(title, body);
        return card;
    }
}
