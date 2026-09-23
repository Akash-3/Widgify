package com.widgify.desktop.ui;

import com.widgify.desktop.net.ServerApiClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Map;
import java.util.function.Consumer;

/**
 * ExpandedWeatherDialog — full weather forecast + city search.
 * Uses StageStyle.UNDECORATED with a custom dark chrome header
 * matching the Widgify design language. Launched on compact widget click.
 */
public class ExpandedWeatherDialog extends Stage {

    private final ServerApiClient apiClient;
    private final Consumer<String> onCityChangedCallback;
    private String currentLocation;

    private final Label cityTitleLabel;
    private final Label tempLabel;
    private final Label conditionLabel;
    private final Label detailsLabel;
    private final TextField searchInput;
    private final Label statusLabel;
    private final VBox forecastContainer;

    // Drag state
    private double dragX, dragY;

    public ExpandedWeatherDialog(ServerApiClient apiClient, String initialLocation, Consumer<String> onCityChangedCallback) {
        this.apiClient = apiClient;
        this.currentLocation = (initialLocation != null && !initialLocation.trim().isEmpty())
                ? initialLocation.trim() : "Berhampur, India";
        this.onCityChangedCallback = onCityChangedCallback;

        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UNDECORATED);

        // ── Custom Title Bar ───────────────────────────────────────
        HBox titleBar = buildTitleBar();

        // ── Main Weather Card ──────────────────────────────────────
        cityTitleLabel = new Label(currentLocation.toUpperCase());
        cityTitleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        cityTitleLabel.setTextFill(Color.web("#ffffff"));

        // Weather icon + temperature row
        HBox tempRow = new HBox(12);
        tempRow.setAlignment(Pos.CENTER_LEFT);
        Label weatherIcon = new Label("🌤");
        weatherIcon.setFont(Font.font(38));
        tempLabel = new Label("24.8°C");
        tempLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 40));
        tempLabel.setTextFill(Color.web("#ffffff"));
        tempRow.getChildren().addAll(weatherIcon, tempLabel);

        conditionLabel = new Label("Drizzle");
        conditionLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        conditionLabel.setTextFill(Color.web("#cccccc"));

        detailsLabel = new Label("Feels like 29.8°  |  Humidity 98%  |  Wind 9.5 km/h");
        detailsLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
        detailsLabel.setTextFill(Color.web("#777777"));

        VBox mainCard = new VBox(6, cityTitleLabel, tempRow, conditionLabel, detailsLabel);
        mainCard.setPadding(new Insets(16));
        mainCard.setStyle("-fx-background-color: #141414; -fx-border-color: #222222; "
                + "-fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");

        // ── Forecast Section ───────────────────────────────────────
        Label forecastTitle = new Label("DAILY FORECAST");
        forecastTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        forecastTitle.setTextFill(Color.web("#666666"));

        forecastContainer = new VBox(6);
        forecastContainer.getChildren().addAll(
                createForecastRow("Today",     "Drizzle",      "28°C", "22°C"),
                createForecastRow("Tomorrow",  "Partly Cloudy","30°C", "23°C"),
                createForecastRow("Friday",    "Clear Sky",    "31°C", "24°C"),
                createForecastRow("Saturday",  "Rain Showers", "27°C", "21°C")
        );

        // ── City Search Bar ────────────────────────────────────────
        Label searchLabel = new Label("CHANGE LOCATION");
        searchLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        searchLabel.setTextFill(Color.web("#666666"));

        searchInput = new TextField(currentLocation);
        searchInput.setPromptText("City name or country...");
        searchInput.setStyle("-fx-background-color: #141414; -fx-text-fill: #ffffff; "
                + "-fx-border-color: #333333; -fx-border-radius: 4; "
                + "-fx-padding: 7 10; -fx-font-size: 11px;");
        HBox.setHgrow(searchInput, Priority.ALWAYS);

        Button searchBtn = new Button("SEARCH");
        searchBtn.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #000000; "
                + "-fx-font-weight: bold; -fx-font-size: 9px; "
                + "-fx-padding: 7 14; -fx-background-radius: 4; -fx-cursor: hand;");
        searchBtn.setOnAction(e -> performCitySearch());
        searchInput.setOnAction(e -> performCitySearch());

        HBox searchBox = new HBox(8, searchInput, searchBtn);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("");
        statusLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
        statusLabel.setTextFill(Color.web("#777777"));

        // ── Root ───────────────────────────────────────────────────
        VBox body = new VBox(14, mainCard, forecastTitle, forecastContainer, searchLabel, searchBox, statusLabel);
        body.setPadding(new Insets(16, 20, 20, 20));
        body.setStyle("-fx-background-color: #0d0d0d;");

        VBox root = new VBox(0, titleBar, body);
        root.setStyle("-fx-background-color: #0d0d0d; -fx-border-color: #282828; -fx-border-width: 1;");

        Scene scene = new Scene(root, 440, 520);
        scene.setFill(Color.web("#0d0d0d"));
        setScene(scene);

        // Dragging the custom title bar
        titleBar.setOnMousePressed(e -> {
            dragX = e.getScreenX() - getX();
            dragY = e.getScreenY() - getY();
        });
        titleBar.setOnMouseDragged(e -> {
            setX(e.getScreenX() - dragX);
            setY(e.getScreenY() - dragY);
        });

        fetchLiveWeather(currentLocation);
    }

    /**
     * Builds the custom dialog title bar with WIDGIFY branding and close button.
     */
    private HBox buildTitleBar() {
        HBox bar = new HBox(0);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPrefHeight(38);
        bar.setMinHeight(38);
        bar.setMaxHeight(38);
        bar.setStyle("-fx-background-color: #111111; -fx-border-color: #222222; -fx-border-width: 0 0 1 0;");
        bar.setCursor(Cursor.MOVE);

        Label icon = new Label("🌤");
        icon.setFont(Font.font(14));
        icon.setPadding(new Insets(0, 6, 0, 14));

        Label titleLbl = new Label("WEATHER FORECAST");
        titleLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        titleLbl.setTextFill(Color.web("#ffffff"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
        closeBtn.setTextFill(Color.web("#aaaaaa"));
        closeBtn.setPrefWidth(38);
        closeBtn.setPrefHeight(38);
        closeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
                "-fx-background-color: #cc4444; -fx-cursor: hand; -fx-padding: 0;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;"));
        closeBtn.setOnAction(e -> close());
        closeBtn.setCursor(Cursor.HAND);

        bar.getChildren().addAll(icon, titleLbl, spacer, closeBtn);
        return bar;
    }

    private HBox createForecastRow(String day, String cond, String high, String low) {
        HBox row = new HBox(12);
        row.setPadding(new Insets(7, 12, 7, 12));
        row.setStyle("-fx-background-color: #121212; -fx-border-color: #1e1e1e; "
                + "-fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");
        row.setAlignment(Pos.CENTER_LEFT);

        Label dayL = new Label(day);
        dayL.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        dayL.setTextFill(Color.web("#ffffff"));
        dayL.setMinWidth(80);

        Label condL = new Label(cond);
        condL.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
        condL.setTextFill(Color.web("#888888"));
        HBox.setHgrow(condL, Priority.ALWAYS);

        Label tempsL = new Label(high + " / " + low);
        tempsL.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        tempsL.setTextFill(Color.web("#cccccc"));

        row.getChildren().addAll(dayL, condL, tempsL);
        return row;
    }

    private void performCitySearch() {
        String query = searchInput.getText().trim();
        if (!query.isEmpty()) {
            fetchLiveWeather(query);
        }
    }

    private void fetchLiveWeather(String loc) {
        statusLabel.setText("Updating weather data...");
        statusLabel.setTextFill(Color.web("#666666"));

        if (apiClient != null) {
            apiClient.getWeather(loc).thenAcceptAsync(res -> {
                Platform.runLater(() -> {
                    if (Boolean.TRUE.equals(res.get("success"))) {
                        String resolvedLoc = (String) res.getOrDefault("location", loc);
                        double temp = ((Number) res.getOrDefault("temperature", 24.8)).doubleValue();
                        double feels = ((Number) res.getOrDefault("feelsLike", temp)).doubleValue();
                        int humidity = ((Number) res.getOrDefault("humidity", 98)).intValue();
                        double wind = ((Number) res.getOrDefault("windSpeed", 9.5)).doubleValue();
                        String cond = (String) res.getOrDefault("condition", "Drizzle");
                        String unit = (String) res.getOrDefault("unit", "°C");

                        this.currentLocation = resolvedLoc;
                        cityTitleLabel.setText(resolvedLoc.toUpperCase());
                        tempLabel.setText(temp + unit);
                        conditionLabel.setText(cond);
                        detailsLabel.setText(String.format(
                                "Feels like %.1f%s  |  Humidity %d%%  |  Wind %.1f km/h",
                                feels, unit, humidity, wind));

                        statusLabel.setText("Updated: " + resolvedLoc);
                        statusLabel.setTextFill(Color.web("#555555"));

                        if (onCityChangedCallback != null) {
                            onCityChangedCallback.accept(resolvedLoc);
                        }
                    } else {
                        statusLabel.setText("Location not found. Check the city name.");
                        statusLabel.setTextFill(Color.web("#cc4444"));
                    }
                });
            });
        }
    }
}
