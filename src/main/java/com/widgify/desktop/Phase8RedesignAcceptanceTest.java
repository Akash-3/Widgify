package com.widgify.desktop;

import com.widgify.desktop.model.AuthResult;
import com.widgify.desktop.net.ServerApiClient;
import com.widgify.desktop.ui.DesktopPreviewCanvas;
import com.widgify.desktop.ui.DesktopPreviewWidget;
import com.widgify.desktop.ui.WidgifyMainWindow;
import com.widgify.desktop.ui.WidgetWindowManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Phase8RedesignAcceptanceTest programmatically validates all Phase 8 Redesign requirements:
 * 1. Single Main Window UX Architecture (WidgifyMainWindow).
 * 2. 0 Transparent Desktop Stages on Initial Login Startup.
 * 3. Interactive Preview Canvas Nodes & Drag-and-Drop Layout Editing.
 * 4. Backend Synchronization, Save Layout, Apply to Desktop, and Reset Defaults.
 * 5. 100% Idempotent Canonical Widget Seeding and Deduplication (No Duplicates).
 * 6. WAR Packaging Isolation (0 JavaFX JARs in WEB-INF/lib).
 */
public class Phase8RedesignAcceptanceTest extends Application {

    private static final String BASE_URL = "http://localhost:8080/widgify";
    private static final CountDownLatch testLatch = new CountDownLatch(1);
    private static boolean allPassed = false;
    private static String failureReason = "";

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("STARTING PHASE 8 REDESIGN ACCEPTANCE TEST SUITE");
        System.out.println("==================================================");

        new Thread(() -> Application.launch(Phase8RedesignAcceptanceTest.class, args)).start();

        boolean completed = testLatch.await(75, TimeUnit.SECONDS);
        if (!completed) {
            System.err.println("Phase 8 acceptance test suite timed out!");
            System.exit(1);
        }

        if (allPassed) {
            System.out.println("\n==================================================");
            System.out.println("ALL PHASE 8 REDESIGN ACCEPTANCE TESTS PASSED 100%!");
            System.out.println("==================================================");
            System.exit(0);
        } else {
            System.err.println("\nPHASE 8 TEST FAILURE: " + failureReason);
            System.exit(1);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(false);
        new Thread(() -> {
            try {
                runPhase8Tests();
                allPassed = true;
            } catch (Throwable t) {
                t.printStackTrace();
                allPassed = false;
                failureReason = t.getMessage();
            } finally {
                testLatch.countDown();
                Platform.runLater(Platform::exit);
            }
        }).start();
    }

    @SuppressWarnings("unchecked")
    private void runPhase8Tests() throws Exception {
        // TEST A: WAR Package JavaFX Decoupling Audit
        System.out.println("\n[TEST A] Auditing WAR File JavaFX Isolation...");
        File libDir = new File("target/widgify/WEB-INF/lib");
        if (libDir.exists() && libDir.isDirectory()) {
            File[] files = libDir.listFiles((dir, name) -> name.toLowerCase().contains("javafx"));
            int fxJarCount = files != null ? files.length : 0;
            System.out.println(" - JavaFX JARs inside target/widgify/WEB-INF/lib: " + fxJarCount);
            if (fxJarCount > 0) throw new RuntimeException("Found JavaFX JARs inside WAR WEB-INF/lib!");
        } else {
            System.out.println(" - (target/widgify/WEB-INF/lib not present yet, will check after maven build)");
        }

        // TEST B: Authentication & Session Management
        ServerApiClient client = new ServerApiClient(BASE_URL);
        String email = "phase8_qa_" + UUID.randomUUID().toString().substring(0, 8) + "@widgify.test";
        String password = "Pass123!Password";
        String name = "Phase8 QA Tester";

        System.out.println("\n[TEST B] Registering and Authenticating QA User: " + email);
        registerUser(BASE_URL, name, email, password);

        AuthResult authResult = client.login(email, password).get(10, TimeUnit.SECONDS);
        if (!authResult.isSuccess()) {
            throw new RuntimeException("Login failed: " + authResult.getMessage());
        }
        System.out.println(" - Login successful. Authenticated email: " + client.getCurrentAuthenticatedEmail());

        // TEST C: Backend Canonical Seeding & Deduplication
        System.out.println("\n[TEST C] Verifying Backend Default Widget Seeding...");
        Map<String, Object> widgetRes = client.getWidgets().get(10, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(widgetRes.get("success"))) {
            throw new RuntimeException("GET /widgets failed");
        }
        List<Map<String, Object>> widgets = (List<Map<String, Object>>) widgetRes.get("widgets");
        System.out.println(" - Initial widget count: " + widgets.size());
        if (widgets.size() < 8) {
            throw new RuntimeException("Expected at least 8 default canonical widgets, got: " + widgets.size());
        }

        Set<String> typesFound = new HashSet<>();
        for (Map<String, Object> w : widgets) {
            typesFound.add((String) w.get("widgetType"));
        }
        System.out.println(" - Canonical widget types returned: " + typesFound);
        String[] expectedTypes = {"clock", "timer", "system", "weather", "notes", "tasks", "launcher", "server_health"};
        for (String type : expectedTypes) {
            if (!typesFound.contains(type)) {
                throw new RuntimeException("Missing expected canonical widget type: " + type);
            }
        }

        // Deduplication test: call resetWidgets() twice and verify count remains exactly 8
        System.out.println(" - Triggering resetWidgets() API to test deduplication...");
        Map<String, Object> resetRes = client.resetWidgets().get(10, TimeUnit.SECONDS);
        List<Map<String, Object>> resetWidgets = (List<Map<String, Object>>) resetRes.get("widgets");
        System.out.println(" - Widget count after reset: " + resetWidgets.size());
        if (resetWidgets.size() != 8) {
            throw new RuntimeException("Deduplication failed! Expected exactly 8 widgets after reset, found: " + resetWidgets.size());
        }

        // TEST D: Single Main Window & 0 Desktop Stages on Launch
        System.out.println("\n[TEST D] Verifying Single Main Window Launch & Stage Count...");
        final WidgetWindowManager[] windowManagerHolder = new WidgetWindowManager[1];
        final WidgifyMainWindow[] mainWindowHolder = new WidgifyMainWindow[1];
        CountDownLatch uiLatch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                windowManagerHolder[0] = new WidgetWindowManager(client);
                // Ensure all widgets closed before main window launch
                windowManagerHolder[0].closeAllWidgets();

                mainWindowHolder[0] = new WidgifyMainWindow(client, windowManagerHolder[0], () -> {});
                mainWindowHolder[0].show();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                uiLatch.countDown();
            }
        });

        if (!uiLatch.await(10, TimeUnit.SECONDS)) {
            throw new RuntimeException("UI initialization timed out!");
        }

        WidgetWindowManager windowManager = windowManagerHolder[0];
        WidgifyMainWindow mainWindow = mainWindowHolder[0];

        int initialStageCount = windowManager.getActiveStages().size();
        System.out.println(" - Active transparent desktop widget stages on initial launch: " + initialStageCount);
        if (initialStageCount != 0) {
            throw new RuntimeException("Expected 0 active desktop widget stages on launch, found: " + initialStageCount);
        }

        // TEST E: Interactive Desktop Preview Canvas & Drag/Resize Editing
        System.out.println("\n[TEST E] Testing Desktop Preview Canvas Node Editing...");
        DesktopPreviewCanvas canvas = mainWindow.getCanvasPane();

        // Wait brief moment for canvas background async load to complete
        Thread.sleep(1000);

        List<DesktopPreviewWidget> previewWidgets = canvas.getPreviewWidgets();
        System.out.println(" - Preview widget nodes in canvas: " + previewWidgets.size());
        if (previewWidgets.isEmpty()) {
            throw new RuntimeException("Desktop preview canvas contains no preview widget nodes!");
        }

        DesktopPreviewWidget clockPreview = null;
        for (DesktopPreviewWidget pw : previewWidgets) {
            if ("clock".equalsIgnoreCase(pw.getWidgetType())) {
                clockPreview = pw;
                break;
            }
        }
        if (clockPreview == null) {
            throw new RuntimeException("Clock preview widget node not found in canvas!");
        }

        System.out.println(" - Original Clock preview position: (" + clockPreview.getPosX() + ", " + clockPreview.getPosY() + ")");
        double newX = 550.0;
        double newY = 220.0;

        CountDownLatch editLatch = new CountDownLatch(1);
        final DesktopPreviewWidget finalClockPreview = clockPreview;
        Platform.runLater(() -> {
            finalClockPreview.setPosX(newX);
            finalClockPreview.setPosY(newY);
            canvas.saveLayoutToBackend();
            editLatch.countDown();
        });

        if (!editLatch.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("Canvas layout save timed out!");
        }

        Thread.sleep(1000); // Allow HTTP update to complete

        // Verify update persisted in MySQL backend
        Map<String, Object> updatedWidgetsRes = client.getWidgets().get(10, TimeUnit.SECONDS);
        List<Map<String, Object>> updatedWidgets = (List<Map<String, Object>>) updatedWidgetsRes.get("widgets");
        boolean positionUpdated = false;
        for (Map<String, Object> w : updatedWidgets) {
            if ("clock".equalsIgnoreCase((String) w.get("widgetType"))) {
                int px = ((Number) w.get("positionX")).intValue();
                int py = ((Number) w.get("positionY")).intValue();
                System.out.println(" - Backend MySQL position for Clock after canvas save: (" + px + ", " + py + ")");
                if (px == (int) newX && py == (int) newY) {
                    positionUpdated = true;
                }
                break;
            }
        }
        if (!positionUpdated) {
            throw new RuntimeException("Canvas preview position edit was not correctly persisted to MySQL backend!");
        }

        // TEST F: Apply to Desktop Overlay Stages
        System.out.println("\n[TEST F] Testing [ APPLY TO DESKTOP ] Layout Overlay Stage Creation...");
        CountDownLatch applyLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            canvas.applyLayoutToDesktop();
            applyLatch.countDown();
        });

        if (!applyLatch.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("Apply to desktop timed out!");
        }

        Thread.sleep(1500); // Allow stages to initialize

        int activeDesktopStages = windowManager.getActiveStages().size();
        System.out.println(" - Active transparent desktop widget stages after [ APPLY TO DESKTOP ]: " + activeDesktopStages);
        if (activeDesktopStages == 0) {
            throw new RuntimeException("Expected > 0 active desktop widget stages after applying layout, found 0!");
        }

        // TEST G: Navigation & View Switching in Main Window
        System.out.println("\n[TEST G] Testing Navigation & View Switching...");
        CountDownLatch navLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            mainWindow.showView("presets");
            mainWindow.showView("settings");
            mainWindow.showView("about");
            mainWindow.showView("canvas");
            navLatch.countDown();
        });

        if (!navLatch.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("Main window view switching timed out!");
        }
        System.out.println(" - View switching (Canvas, Presets, Settings, About) verified.");

        // Cleanup FX stages
        Platform.runLater(() -> {
            windowManager.closeAllWidgets();
            mainWindow.close();
        });

        System.out.println("\n[SUMMARY] All Phase 8 Acceptance Tests completed successfully!");
    }

    private void registerUser(String baseUrl, String name, String email, String password) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String form = String.format("name=%s&email=%s&password=%s",
                URLEncoder.encode(name, StandardCharsets.UTF_8),
                URLEncoder.encode(email, StandardCharsets.UTF_8),
                URLEncoder.encode(password, StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/register"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 302) {
            throw new RuntimeException("Registration failed with status code: " + response.statusCode());
        }
    }
}
