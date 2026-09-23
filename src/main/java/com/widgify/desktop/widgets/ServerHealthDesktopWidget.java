package com.widgify.desktop.widgets;

import com.widgify.desktop.net.ServerApiClient;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.Map;

public class ServerHealthDesktopWidget implements DesktopWidget {

    private final VBox container;
    private final Label serverCpuLabel;
    private final Label serverRamLabel;
    private final Label serverStatusLabel;
    private Timeline timeline;

    private ServerApiClient apiClient;

    public ServerHealthDesktopWidget(ServerApiClient apiClient) {
        this.apiClient = apiClient;

        container = new VBox(6);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(12));
        container.setStyle("-fx-background-color: transparent;");

        Label headerLabel = new Label("SERVER HEALTH (TOMCAT / JVM)");
        headerLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        headerLabel.setTextFill(Color.web("#00ff88"));

        serverCpuLabel = createMetricLabel("SERVER CPU: --%");
        serverRamLabel = createMetricLabel("SERVER RAM: -- MB");
        serverStatusLabel = new Label("STATUS: Connecting to Tomcat...");
        serverStatusLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 9));
        serverStatusLabel.setTextFill(Color.web("#888888"));

        container.getChildren().addAll(headerLabel, serverCpuLabel, serverRamLabel, serverStatusLabel);
    }

    public ServerHealthDesktopWidget() {
        this(null);
    }

    public void setApiClient(ServerApiClient apiClient) {
        this.apiClient = apiClient;
    }

    private Label createMetricLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        l.setTextFill(Color.web("#ffffff"));
        return l;
    }

    @Override
    public String getWidgetType() {
        return "server_health";
    }

    @Override
    public String getWidgetTitle() {
        return "SERVER HEALTH (TOMCAT)";
    }

    @Override
    public Node getWidgetNode() {
        return container;
    }

    @Override
    public void onInitialize(Map<String, Object> config) {
        fetchServerHealth();
        timeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> fetchServerHealth()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void fetchServerHealth() {
        if (apiClient == null) {
            serverStatusLabel.setText("STATUS: API client uninitialized");
            return;
        }

        apiClient.getServerHealth().thenAcceptAsync(result -> {
            Platform.runLater(() -> {
                boolean success = Boolean.TRUE.equals(result.get("success"));
                if (success) {
                    double cpu = parseSafeDouble(result.get("cpuUsage"));
                    Object memUsedObj = result.getOrDefault("memoryUsed", "0 MB");
                    Object memTotalObj = result.getOrDefault("memoryTotal", "0 MB");
                    String label = String.valueOf(result.getOrDefault("serverLabel", "Apache Tomcat / JVM"));

                    serverCpuLabel.setText(String.format("SERVER CPU: %.1f%%", cpu));
                    serverRamLabel.setText("SERVER MEMORY: " + memUsedObj + " / " + memTotalObj);
                    serverStatusLabel.setText("STATUS: " + label + " Online");
                    serverStatusLabel.setTextFill(Color.web("#00ff88"));
                } else {
                    String msg = (String) result.getOrDefault("message", "Server unavailable");
                    serverCpuLabel.setText("SERVER CPU: N/A");
                    serverRamLabel.setText("SERVER MEMORY: N/A");
                    serverStatusLabel.setText("STATUS: " + msg);
                    serverStatusLabel.setTextFill(Color.web("#ff5555"));
                }
            });
        });
    }

    private double parseSafeDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    @Override
    public void onClose() {
        if (timeline != null) {
            timeline.stop();
        }
    }
}
