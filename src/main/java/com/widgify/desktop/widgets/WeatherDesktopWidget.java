package com.widgify.desktop.widgets;

import com.widgify.desktop.net.ServerApiClient;
import com.widgify.desktop.ui.ExpandedWeatherDialog;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Map;

/**
 * WeatherDesktopWidget provides a compact Rainmeter / Nothing OS inspired weather widget.
 * Permanent city inputs and SET buttons have been removed.
 * Clicking the compact widget opens the ExpandedWeatherDialog for forecast details and city search.
 */
public class WeatherDesktopWidget implements DesktopWidget {

    private final VBox container;
    private final Label tempLabel;
    private final Label conditionLabel;
    private final Label detailsLabel;
    private final Label locationLabel;

    private ServerApiClient apiClient;
    private String currentLocation = "Berhampur, India";

    public WeatherDesktopWidget(ServerApiClient apiClient) {
        this.apiClient = apiClient;

        container = new VBox(6);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(12, 16, 12, 16));
        container.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

        locationLabel = new Label("📍 BERHAMPUR, INDIA");
        locationLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        locationLabel.setTextFill(Color.web("#888888"));

        tempLabel = new Label("24.8°C");
        tempLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 30));
        tempLabel.setTextFill(Color.web("#ffffff"));

        conditionLabel = new Label("Drizzle");
        conditionLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        conditionLabel.setTextFill(Color.web("#cccccc"));

        detailsLabel = new Label("Feels 29.8°  |  Humidity 98%  |  Wind 9.5 km/h");
        detailsLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 9));
        detailsLabel.setTextFill(Color.web("#888888"));

        container.getChildren().addAll(locationLabel, tempLabel, conditionLabel, detailsLabel);

        // Click opens Expanded Weather Dialog
        container.setOnMouseClicked(e -> openExpandedWeatherDialog());
    }

    public WeatherDesktopWidget() {
        this(null);
    }

    public void setApiClient(ServerApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public String getWidgetType() {
        return "weather";
    }

    @Override
    public String getWidgetTitle() {
        return "Weather";
    }

    @Override
    public Node getWidgetNode() {
        return container;
    }

    @Override
    public void onInitialize(Map<String, Object> config) {
        if (config != null && config.containsKey("location")) {
            Object loc = config.get("location");
            if (loc != null && !loc.toString().trim().isEmpty()) {
                currentLocation = loc.toString().trim();
            }
        }
        fetchWeather();
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void openExpandedWeatherDialog() {
        ExpandedWeatherDialog dialog = new ExpandedWeatherDialog(apiClient, currentLocation, newLoc -> {
            this.currentLocation = newLoc;
            fetchWeather();
        });
        dialog.show();
    }

    private void fetchWeather() {
        if (apiClient == null) {
            return;
        }

        apiClient.getWeather(currentLocation).thenAcceptAsync(result -> {
            Platform.runLater(() -> {
                boolean success = Boolean.TRUE.equals(result.get("success"));
                if (success) {
                    String loc = (String) result.getOrDefault("location", currentLocation);
                    double temp = ((Number) result.getOrDefault("temperature", 24.8)).doubleValue();
                    double feels = ((Number) result.getOrDefault("feelsLike", temp)).doubleValue();
                    int humidity = ((Number) result.getOrDefault("humidity", 98)).intValue();
                    double wind = ((Number) result.getOrDefault("windSpeed", 9.5)).doubleValue();
                    String cond = (String) result.getOrDefault("condition", "Drizzle");
                    String unit = (String) result.getOrDefault("unit", "°C");

                    this.currentLocation = loc;
                    locationLabel.setText("📍 " + loc.toUpperCase());
                    tempLabel.setText(temp + unit);
                    conditionLabel.setText(cond);
                    detailsLabel.setText(String.format("Feels %.1f%s  |  Humidity %d%%  |  Wind %.1f km/h", feels, unit, humidity, wind));
                }
            });
        });
    }

    @Override
    public void onClose() {
    }
}
