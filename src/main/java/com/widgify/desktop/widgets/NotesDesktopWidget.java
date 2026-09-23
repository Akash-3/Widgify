package com.widgify.desktop.widgets;

import com.widgify.desktop.net.ServerApiClient;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
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
 * NotesDesktopWidget provides a minimalist desktop sticky notes representation
 * matching the Rainmeter / Nothing OS aesthetic.
 */
public class NotesDesktopWidget implements DesktopWidget {

    private final VBox container;
    private final VBox notesListBox;

    private ServerApiClient apiClient;

    public NotesDesktopWidget(ServerApiClient apiClient) {
        this.apiClient = apiClient;

        container = new VBox(8);
        container.setPadding(new Insets(12, 16, 12, 16));
        container.setStyle("-fx-background-color: transparent;");

        // Header
        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("NOTES");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        title.setTextFill(Color.web("#888888"));
        HBox.setHgrow(title, Priority.ALWAYS);

        Button addBtn = new Button("+");
        addBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #aaaaaa; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        addBtn.setOnAction(e -> createDefaultNote());

        header.getChildren().addAll(title, addBtn);

        // Notes List Box
        notesListBox = new VBox(4);
        notesListBox.setStyle("-fx-background-color: transparent;");

        container.getChildren().addAll(header, notesListBox);
    }

    public NotesDesktopWidget() {
        this(null);
    }

    public void setApiClient(ServerApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public String getWidgetType() {
        return "notes";
    }

    @Override
    public String getWidgetTitle() {
        return "Notes";
    }

    @Override
    public Node getWidgetNode() {
        return container;
    }

    @Override
    public void onInitialize(Map<String, Object> config) {
        fetchNotes();
    }

    @SuppressWarnings("unchecked")
    private void fetchNotes() {
        if (apiClient == null) {
            renderFallbackNotes();
            return;
        }

        apiClient.getNotes().thenAcceptAsync(result -> {
            Platform.runLater(() -> {
                boolean success = Boolean.TRUE.equals(result.get("success"));
                if (success) {
                    List<Map<String, Object>> notes = (List<Map<String, Object>>) result.get("notes");
                    renderNotesList(notes);
                } else {
                    renderFallbackNotes();
                }
            });
        });
    }

    private void renderNotesList(List<Map<String, Object>> notes) {
        notesListBox.getChildren().clear();

        if (notes == null || notes.isEmpty()) {
            renderFallbackNotes();
            return;
        }

        int count = 0;
        for (Map<String, Object> note : notes) {
            if (count >= 3) {
                Label moreLbl = new Label("+" + (notes.size() - 3) + " more...");
                moreLbl.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 9));
                moreLbl.setTextFill(Color.web("#666666"));
                notesListBox.getChildren().add(moreLbl);
                break;
            }

            String content = (String) note.getOrDefault("content", note.getOrDefault("title", "Untitled"));
            Label bulletItem = new Label("•  " + content);
            bulletItem.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
            bulletItem.setTextFill(Color.web("#dddddd"));
            bulletItem.setWrapText(true);

            notesListBox.getChildren().add(bulletItem);
            count++;
        }
    }

    private void renderFallbackNotes() {
        notesListBox.getChildren().clear();
        String[] defaults = {
            "•  Buy groceries",
            "•  Finish project report",
            "•  Call Kavya"
        };
        for (String d : defaults) {
            Label item = new Label(d);
            item.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
            item.setTextFill(Color.web("#dddddd"));
            notesListBox.getChildren().add(item);
        }
    }

    private void createDefaultNote() {
        if (apiClient != null) {
            apiClient.createNote("New Note", "Quick sticky note...").thenAcceptAsync(res -> {
                Platform.runLater(this::fetchNotes);
            });
        }
    }

    @Override
    public void onClose() {
    }
}
