package com.widgify.desktop;

import com.widgify.desktop.model.AuthResult;
import com.widgify.desktop.net.ServerApiClient;
import com.widgify.desktop.ui.WidgetManagerStage;
import com.widgify.desktop.ui.WidgetWindowManager;
import com.widgify.desktop.ui.WidgifyMainWindow;
import java.awt.Desktop;
import java.net.URI;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.control.*;
import java.util.List;
import java.util.Map;

public class WidgifyDesktopClient extends Application {

    private ServerApiClient apiClient;
    private WidgetWindowManager widgetWindowManager;
    private Stage primaryStage;

    // Login View Components
    private TextField emailField;
    private PasswordField passwordField;
    private Button loginButton;
    private Label errorLabel;
    private ProgressIndicator progressSpinner;

    // Authenticated Control Panel Components
    private Label userLabel;
    private Label sessionLabel;
    private Label widgetCountLabel;
    private ListView<String> widgetListView;
    private Label statusDetailLabel;
    private Button refreshButton;
    private Button logoutButton;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Platform.setImplicitExit(false);
        this.primaryStage = stage;
        this.apiClient = new ServerApiClient();
        this.apiClient.setOnUnauthorizedCallback(() -> Platform.runLater(this::handleSessionExpiration));
        this.widgetWindowManager = new WidgetWindowManager(apiClient);

        Parameters params = getParameters();
        if (params != null && !params.getRaw().isEmpty()) {
            apiClient.setServerBaseUrl(params.getRaw().get(0));
        }

        stage.setTitle("Widgify — Desktop Widget System");
        stage.setMinWidth(420);
        stage.setMinHeight(560);
        stage.setWidth(450);
        stage.setHeight(580);

        showLoginScene();
        stage.show();
    }

    @Override
    public void stop() {
        if (widgetWindowManager != null) {
            widgetWindowManager.shutdown();
        }
    }

    /**
     * Renders the Nothing OS / Rainmeter inspired Login Interface.
     */
    private void showLoginScene() {
        VBox root = new VBox(20);
        root.setStyle("-fx-background-color: #0a0a0a; -fx-padding: 36 30 36 30;");
        root.setAlignment(Pos.TOP_CENTER);

        // Header Section
        VBox headerBox = new VBox(6);
        headerBox.setAlignment(Pos.CENTER);

        Text brandTitle = new Text("W I D G I F Y");
        brandTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        brandTitle.setFill(Color.web("#ffffff"));

        Text brandSubtitle = new Text("Your Desktop. Your Widgets.");
        brandSubtitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        brandSubtitle.setFill(Color.web("#888888"));

        headerBox.getChildren().addAll(brandTitle, brandSubtitle);

        // Form Container Card
        VBox card = new VBox(16);
        card.setStyle("-fx-background-color: #121212; -fx-border-color: #222222; -fx-border-width: 1; -fx-background-radius: 8; -fx-border-radius: 8; -fx-padding: 24;");
        card.setMaxWidth(380);

        // Email Input
        VBox emailBox = createInputGroup("EMAIL ADDRESS", emailField = new TextField());
        emailField.setPromptText("user@example.com");

        // Password Input
        VBox passwordBox = createInputGroup("PASSWORD", passwordField = new PasswordField());
        passwordField.setPromptText("••••••••");

        // Error Message Label
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ff5555; -fx-font-size: 11px; -fx-font-family: 'Segoe UI';");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Progress Spinner
        progressSpinner = new ProgressIndicator();
        progressSpinner.setMaxSize(20, 20);
        progressSpinner.setStyle("-fx-progress-color: #ffffff;");
        progressSpinner.setVisible(false);
        progressSpinner.setManaged(false);

        // Login Button
        loginButton = new Button("LOG IN");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setStyle(
                "-fx-background-color: #ffffff; " +
                "-fx-text-fill: #000000; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 12px; " +
                "-fx-padding: 10 16; " +
                "-fx-background-radius: 4; " +
                "-fx-cursor: hand;"
        );
        loginButton.setOnAction(e -> handleLogin());

        passwordField.setOnAction(e -> handleLogin());
        emailField.setOnAction(e -> handleLogin());

        card.getChildren().addAll(emailBox, passwordBox, errorLabel, progressSpinner, loginButton);

        // Registration footer
        Label footerPrefix = new Label("Need an account?");
        footerPrefix.setStyle(
                "-fx-text-fill: #555555;" +
                "-fx-font-size: 10px;"
        );

        Hyperlink registerLink = new Hyperlink("Register new accounts via browser.");
        registerLink.setStyle(
                "-fx-text-fill: #ffffff;" +
                "-fx-font-size: 10px;" +
                "-fx-padding: 2px;" +
                "-fx-background-color: transparent;" +
                "-fx-cursor: hand;"
        );

        registerLink.setOnMouseEntered(e -> {
            registerLink.setStyle(
                    "-fx-text-fill: #aaaaaa;" +
                    "-fx-font-size: 10px;" +
                    "-fx-padding: 2px;" +
                    "-fx-background-color: transparent;" +
                    "-fx-cursor: hand;"
            );
        });

        registerLink.setOnMouseExited(e -> {
            registerLink.setStyle(
                    "-fx-text-fill: #ffffff;" +
                    "-fx-font-size: 10px;" +
                    "-fx-padding: 2px;" +
                    "-fx-background-color: transparent;" +
                    "-fx-cursor: hand;"
            );
        });

        registerLink.setOnMouseClicked(e -> {
            System.out.println("[Widgify] REGISTER LINK CLICKED");

            try {
                String url = apiClient.getServerBaseUrl() + "/register.jsp";
                System.out.println("[Widgify] Opening: " + url);

                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(url));
                } else {
                    System.out.println("[Widgify] Desktop browser is not supported.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        HBox footerBox = new HBox(5);
        footerBox.setAlignment(Pos.CENTER);
        footerBox.setMinHeight(30);
        footerBox.setPrefHeight(30);
        footerBox.setMaxHeight(30);

        footerBox.getChildren().addAll(footerPrefix, registerLink);

        // Make sure footer is above other nodes for mouse interaction
        footerBox.toFront();

        root.getChildren().addAll(headerBox, card, footerBox);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
    }

    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        setLoginLoadingState(true);
        hideError();

        apiClient.login(email, password).thenAcceptAsync(result -> {
            Platform.runLater(() -> {
                setLoginLoadingState(false);
                if (result.isSuccess()) {
                    loadAuthenticatedView();
                } else {
                    showError(result.getMessage());
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                setLoginLoadingState(false);
                showError("Connection error: " + ex.getMessage());
            });
            return null;
        });
    }

    private WidgifyMainWindow mainWindow;

    /**
     * Renders the Single Main Application Window (WidgifyMainWindow).
     * Launches with 0 active transparent desktop stages.
     */
    private void loadAuthenticatedView() {
        if (primaryStage != null) {
            primaryStage.hide();
        }
        if (mainWindow != null) {
            mainWindow.close();
        }
        widgetWindowManager.closeAllWidgets(); // 0 transparent stages on initial launch

        mainWindow = new WidgifyMainWindow(apiClient, widgetWindowManager, this::handleLogout);
        mainWindow.show();

        // Setup optional system tray
        setupSystemTray();
    }

    private void handleSessionExpiration() {
        if (mainWindow != null) {
            mainWindow.close();
        }
        widgetWindowManager.closeAllWidgets();
        showLoginScene();
        primaryStage.show();
        showError("Session expired (HTTP 401). Please log in again.");
    }

    private void handleLogout() {
        if (mainWindow != null) {
            mainWindow.close();
        }
        widgetWindowManager.closeAllWidgets();
        apiClient.logout().thenAcceptAsync(v -> {
            Platform.runLater(() -> {
                showLoginScene();
                primaryStage.show();
            });
        });
    }

    private void setupSystemTray() {
        if (!java.awt.SystemTray.isSupported()) {
            return;
        }
        try {
            java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
            java.awt.PopupMenu popup = new java.awt.PopupMenu();

            java.awt.MenuItem managerItem = new java.awt.MenuItem("Open Main Window");
            managerItem.addActionListener(e -> Platform.runLater(() -> {
                if (mainWindow != null) {
                    mainWindow.show();
                    mainWindow.toFront();
                }
            }));

            java.awt.MenuItem logoutItem = new java.awt.MenuItem("Logout");
            logoutItem.addActionListener(e -> Platform.runLater(this::handleLogout));

            java.awt.MenuItem exitItem = new java.awt.MenuItem("Exit Widgify");
            exitItem.addActionListener(e -> Platform.runLater(() -> {
                widgetWindowManager.shutdown();
                Platform.exit();
                System.exit(0);
            }));

            popup.add(managerItem);
            popup.addSeparator();
            popup.add(logoutItem);
            popup.add(exitItem);

            // Simple 16x16 tray icon image
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = img.createGraphics();
            g.setColor(java.awt.Color.BLACK);
            g.fillRect(0, 0, 16, 16);
            g.setColor(java.awt.Color.WHITE);
            g.drawString("W", 2, 12);
            g.dispose();

            java.awt.TrayIcon trayIcon = new java.awt.TrayIcon(img, "Widgify Engine", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> Platform.runLater(() -> {
                if (mainWindow != null) {
                    mainWindow.show();
                    mainWindow.toFront();
                }
            }));

            tray.add(trayIcon);
        } catch (Throwable ignored) {
            // Optional system tray feature handles any desktop environment exceptions gracefully
        }
    }

    private VBox createInputGroup(String labelText, TextField inputField) {
        VBox box = new VBox(4);
        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: #888888; -fx-font-size: 9px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");

        inputField.setStyle(
                "-fx-background-color: #181818; " +
                "-fx-text-fill: #ffffff; " +
                "-fx-border-color: #2a2a2a; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4; " +
                "-fx-padding: 8; " +
                "-fx-font-family: 'Segoe UI';"
        );

        box.getChildren().addAll(label, inputField);
        return box;
    }

    private void setLoginLoadingState(boolean loading) {
        emailField.setDisable(loading);
        passwordField.setDisable(loading);
        loginButton.setDisable(loading);
        loginButton.setText(loading ? "CONNECTING..." : "LOG IN");
        progressSpinner.setVisible(loading);
        progressSpinner.setManaged(loading);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
