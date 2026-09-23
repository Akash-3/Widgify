package com.widgify.desktop.widgets;

import com.widgify.service.battery.BatteryInfo;
import com.widgify.service.battery.BatteryProvider;
import com.widgify.service.battery.NullBatteryProvider;
import com.widgify.service.battery.WindowsBatteryProvider;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.lang.management.ManagementFactory;
import java.util.Map;

/**
 * SystemDesktopWidget displays local PC Health metrics:
 * CPU Load %, RAM Usage GB, and Battery status (real data via BatteryProvider).
 * Battery row is hidden when no battery is present.
 * All JVM Heap, server, and developer metrics are completely excluded.
 */
public class SystemDesktopWidget implements DesktopWidget {

    private final VBox container;
    private final Label cpuValLabel;
    private final ProgressBar cpuBar;

    private final Label ramValLabel;
    private final ProgressBar ramBar;

    private final HBox batteryRow;
    private final Label batteryValLabel;
    private final ProgressBar batteryBar;

    private final BatteryProvider batteryProvider;
    private Timeline timeline;

    public SystemDesktopWidget() {
        // Resolve battery provider at construction time
        batteryProvider = resolveBatteryProvider();

        container = new VBox(10);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(12, 16, 12, 16));
        container.setStyle("-fx-background-color: transparent;");

        Label headerLabel = new Label("PC HEALTH");
        headerLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        headerLabel.setTextFill(Color.web("#888888"));

        // CPU Row
        HBox cpuRow = new HBox(8);
        cpuRow.setAlignment(Pos.CENTER_LEFT);
        Label cpuTitle = new Label("CPU");
        cpuTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        cpuTitle.setTextFill(Color.web("#aaaaaa"));
        cpuTitle.setMinWidth(56);

        cpuBar = createProgressBar();
        HBox.setHgrow(cpuBar, Priority.ALWAYS);

        cpuValLabel = new Label("–");
        cpuValLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        cpuValLabel.setTextFill(Color.web("#ffffff"));
        cpuValLabel.setMinWidth(45);
        cpuValLabel.setAlignment(Pos.CENTER_RIGHT);

        cpuRow.getChildren().addAll(cpuTitle, cpuBar, cpuValLabel);

        // RAM Row
        HBox ramRow = new HBox(8);
        ramRow.setAlignment(Pos.CENTER_LEFT);
        Label ramTitle = new Label("RAM");
        ramTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        ramTitle.setTextFill(Color.web("#aaaaaa"));
        ramTitle.setMinWidth(56);

        ramBar = createProgressBar();
        HBox.setHgrow(ramBar, Priority.ALWAYS);

        ramValLabel = new Label("–");
        ramValLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        ramValLabel.setTextFill(Color.web("#ffffff"));
        ramValLabel.setMinWidth(75);
        ramValLabel.setAlignment(Pos.CENTER_RIGHT);

        ramRow.getChildren().addAll(ramTitle, ramBar, ramValLabel);

        // Battery Row
        batteryRow = new HBox(8);
        batteryRow.setAlignment(Pos.CENTER_LEFT);
        Label batTitle = new Label("BATTERY");
        batTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        batTitle.setTextFill(Color.web("#aaaaaa"));
        batTitle.setMinWidth(56);

        batteryBar = createProgressBar();
        HBox.setHgrow(batteryBar, Priority.ALWAYS);

        batteryValLabel = new Label("–");
        batteryValLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        batteryValLabel.setTextFill(Color.web("#ffffff"));
        batteryValLabel.setMinWidth(75);
        batteryValLabel.setAlignment(Pos.CENTER_RIGHT);

        batteryRow.getChildren().addAll(batTitle, batteryBar, batteryValLabel);

        container.getChildren().addAll(headerLabel, cpuRow, ramRow, batteryRow);

        // Check battery availability now — hide row if unavailable
        BatteryInfo initialBattery = batteryProvider.getBatteryInfo();
        if (!initialBattery.isAvailable()) {
            batteryRow.setVisible(false);
            batteryRow.setManaged(false);
        }
    }

    private ProgressBar createProgressBar() {
        ProgressBar bar = new ProgressBar(0.0);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(6);
        bar.setStyle("-fx-accent: #ffffff; -fx-control-inner-background: #222222; -fx-border-color: transparent;");
        return bar;
    }

    /**
     * Resolves the correct BatteryProvider for the current OS.
     */
    private BatteryProvider resolveBatteryProvider() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return new WindowsBatteryProvider();
        }
        // macOS provider not in scope here; use null provider
        return new NullBatteryProvider();
    }

    // ── DesktopWidget interface ─────────────────────────────────────

    @Override
    public String getWidgetType() { return "system"; }

    @Override
    public String getWidgetTitle() { return "PC Health"; }

    @Override
    public Node getWidgetNode() { return container; }

    @Override
    public void onInitialize(Map<String, Object> config) {
        updateMetrics();
        timeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> updateMetrics()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

        private void updateMetrics() {
            // ── CPU & RAM ────────────────────────────────────────────────
            try {
                double systemCpu = -1;
                long totalRamMb = 0;
                long freeRamMb = 0;

                java.lang.management.OperatingSystemMXBean osBean =
                        ManagementFactory.getOperatingSystemMXBean();

                if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                    com.sun.management.OperatingSystemMXBean sunBean =
                            (com.sun.management.OperatingSystemMXBean) osBean;

                    systemCpu = sunBean.getCpuLoad() * 100;
                    totalRamMb = sunBean.getTotalMemorySize() / (1024 * 1024);
                    freeRamMb = sunBean.getFreeMemorySize() / (1024 * 1024);
                }

                if (systemCpu >= 0) {
                    cpuBar.setProgress(Math.max(0, Math.min(1, systemCpu / 100.0)));
                    cpuValLabel.setText(String.format("%.0f%%", systemCpu));
                } else {
                    cpuBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                    cpuValLabel.setText("–");
                }

                if (totalRamMb > 0) {
                    double totalGb = totalRamMb / 1024.0;
                    double usedGb = (totalRamMb - freeRamMb) / 1024.0;

                    ramBar.setProgress(
                            Math.max(0, Math.min(1, usedGb / totalGb))
                    );

                    ramValLabel.setText(
                            String.format("%.1f / %.0f GB", usedGb, totalGb)
                    );
                } else {
                    ramBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                    ramValLabel.setText("–");
                }

            } catch (Exception e) {
                cpuValLabel.setText("–");
                ramValLabel.setText("–");

                System.err.println(
                        "[Widgify] System metrics error: " + e.getMessage()
                );
            }

            // ── Battery ─────────────────────────────────────────────────
            // Keep battery completely independent from CPU/RAM.
            try {
                BatteryInfo battery = batteryProvider.getBatteryInfo();

                System.out.println(
                        "[Widgify] Battery: available=" + battery.isAvailable()
                                + " level=" + battery.getLevel()
                                + " status=" + battery.getStatus()
                );

                if (battery.isAvailable()) {
                    int level = Math.max(0, Math.min(100, battery.getLevel()));

                    batteryRow.setVisible(true);
                    batteryRow.setManaged(true);

                    batteryBar.setProgress(level / 100.0);

                    String status = battery.getStatus();

                    if (status == null || status.isBlank()) {
                        batteryValLabel.setText(level + "%");
                    } else {
                        batteryValLabel.setText(level + "% " + status);
                    }

                } else {
                    batteryRow.setVisible(false);
                    batteryRow.setManaged(false);
                }

            } catch (Exception e) {
                System.err.println(
                        "[Widgify] Battery detection error: " + e
                );

                batteryRow.setVisible(false);
                batteryRow.setManaged(false);
            }
        }

    @Override
    public void onClose() {
        if (timeline != null) {
            timeline.stop();
        }
    }
}
