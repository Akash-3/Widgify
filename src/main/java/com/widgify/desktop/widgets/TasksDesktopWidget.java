package com.widgify.desktop.widgets;

import com.widgify.desktop.net.ServerApiClient;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.Map;

/**
 * TasksDesktopWidget provides a minimalist desktop task checklist
 * matching the Rainmeter / Nothing OS aesthetic.
 */
public class TasksDesktopWidget implements DesktopWidget {

    private final VBox container;
    private final VBox tasksListBox;

    private ServerApiClient apiClient;

    public TasksDesktopWidget(ServerApiClient apiClient) {
        this.apiClient = apiClient;

        container = new VBox(8);
        container.setPadding(new Insets(12, 16, 12, 16));
        container.setStyle("-fx-background-color: transparent;");

        // Header
        Label title = new Label("TODAY");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        title.setTextFill(Color.web("#888888"));

        // Tasks List Container
        tasksListBox = new VBox(4);
        tasksListBox.setStyle("-fx-background-color: transparent;");

        // Add task button
        Button addBtn = new Button("+ Add task");
        addBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #888888; -fx-font-size: 10px; -fx-padding: 4 0; -fx-cursor: hand;");
        addBtn.setOnAction(e -> createDefaultTask());

        container.getChildren().addAll(title, tasksListBox, addBtn);
    }

    public TasksDesktopWidget() {
        this(null);
    }

    public void setApiClient(ServerApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public String getWidgetType() {
        return "tasks";
    }

    @Override
    public String getWidgetTitle() {
        return "Tasks";
    }

    @Override
    public Node getWidgetNode() {
        return container;
    }

    @Override
    public void onInitialize(Map<String, Object> config) {
        fetchTasks();
    }

    @SuppressWarnings("unchecked")
    private void fetchTasks() {
        if (apiClient == null) {
            renderFallbackTasks();
            return;
        }

        apiClient.getTasks().thenAcceptAsync(result -> {
            Platform.runLater(() -> {
                boolean success = Boolean.TRUE.equals(result.get("success"));
                if (success) {
                    List<Map<String, Object>> tasks = (List<Map<String, Object>>) result.get("tasks");
                    renderTasksList(tasks);
                } else {
                    renderFallbackTasks();
                }
            });
        });
    }

    private void renderTasksList(List<Map<String, Object>> tasks) {
        tasksListBox.getChildren().clear();

        if (tasks == null || tasks.isEmpty()) {
            renderFallbackTasks();
            return;
        }

        for (Map<String, Object> task : tasks) {
            int id = ((Number) task.get("id")).intValue();
            String title = (String) task.getOrDefault("title", "Untitled Task");
            boolean completed = Boolean.TRUE.equals(task.getOrDefault("completed", false));

            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);

            CheckBox cb = new CheckBox();
            cb.setSelected(completed);
            cb.setStyle("-fx-cursor: hand;");
            cb.setOnAction(e -> toggleTask(id));

            Label itemLbl = new Label(title);
            itemLbl.setFont(Font.font("Segoe UI", completed ? FontWeight.NORMAL : FontWeight.BOLD, 10));
            if (completed) {
                itemLbl.setTextFill(Color.web("#666666"));
                itemLbl.setStyle("-fx-strikethrough: true;");
            } else {
                itemLbl.setTextFill(Color.web("#dddddd"));
            }
            HBox.setHgrow(itemLbl, Priority.ALWAYS);

            row.getChildren().addAll(cb, itemLbl);
            tasksListBox.getChildren().add(row);
        }
    }

    private void renderFallbackTasks() {
        tasksListBox.getChildren().clear();
        String[][] defaults = {
            {"Finish documentation", "false"},
            {"Test desktop widgets", "false"},
            {"Complete backend", "true"},
            {"Prepare presentation", "false"}
        };

        for (String[] def : defaults) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);

            boolean done = Boolean.parseBoolean(def[1]);
            CheckBox cb = new CheckBox();
            cb.setSelected(done);

            Label itemLbl = new Label(def[0]);
            itemLbl.setFont(Font.font("Segoe UI", done ? FontWeight.NORMAL : FontWeight.BOLD, 10));
            if (done) {
                itemLbl.setTextFill(Color.web("#666666"));
                itemLbl.setStyle("-fx-strikethrough: true;");
            } else {
                itemLbl.setTextFill(Color.web("#dddddd"));
            }

            row.getChildren().addAll(cb, itemLbl);
            tasksListBox.getChildren().add(row);
        }
    }

    private void toggleTask(int id) {
        if (apiClient != null) {
            apiClient.toggleTask(id).thenAcceptAsync(res -> Platform.runLater(this::fetchTasks));
        }
    }

    private void createDefaultTask() {
        if (apiClient != null) {
            apiClient.createTask("New Task Item").thenAcceptAsync(res -> Platform.runLater(this::fetchTasks));
        }
    }

    @Override
    public void onClose() {
    }
}
