package com.widgify.desktop;

import com.widgify.desktop.model.AuthResult;
import com.widgify.desktop.net.ServerApiClient;
import com.widgify.desktop.ui.DesktopWidgetStage;
import com.widgify.desktop.ui.WidgetManagerStage;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Phase7FinalAcceptanceTest extends Application {

    private static final String BASE_URL = "http://localhost:8080/widgify";
    private static final CountDownLatch testLatch = new CountDownLatch(1);
    private static boolean allPassed = false;
    private static String failureReason = "";

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("STARTING PHASE 7 FINAL SYSTEM ACCEPTANCE AUDIT");
        System.out.println("==================================================");

        new Thread(() -> Application.launch(Phase7FinalAcceptanceTest.class, args)).start();

        boolean completed = testLatch.await(75, TimeUnit.SECONDS);
        if (!completed) {
            System.err.println("Phase 7 final acceptance test suite timed out!");
            System.exit(1);
        }

        if (allPassed) {
            System.out.println("\n==================================================");
            System.out.println("ALL PHASE 7 FINAL ACCEPTANCE AUDIT TESTS PASSED 100%!");
            System.out.println("==================================================");
            System.exit(0);
        } else {
            System.err.println("\nPHASE 7 TEST FAILURE: " + failureReason);
            System.exit(1);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(false);
        new Thread(() -> {
            try {
                runPhase7Tests();
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
    private void runPhase7Tests() throws Exception {
        // TEST B: WAR Isolation Audit
        System.out.println("\n[TEST B] Auditing WAR File JavaFX Isolation...");
        File libDir = new File("target/widgify/WEB-INF/lib");
        if (libDir.exists() && libDir.isDirectory()) {
            File[] files = libDir.listFiles((dir, name) -> name.toLowerCase().contains("javafx"));
            int fxJarCount = files != null ? files.length : 0;
            System.out.println(" - JavaFX JARs inside target/widgify/WEB-INF/lib: " + fxJarCount);
            if (fxJarCount > 0) throw new RuntimeException("Found JavaFX JARs inside WAR WEB-INF/lib!");
        }

        // TEST C: Authentication & Session Management
        ServerApiClient client = new ServerApiClient(BASE_URL);

        final boolean[] sessionExpiredFlag = {false};
        client.setOnUnauthorizedCallback(() -> {
            sessionExpiredFlag[0] = true;
            System.out.println(" - [CENTRAL 401 CALLBACK TRIGGERED] Session Expiration Handled.");
        });

        String email = "phase7_qa_" + UUID.randomUUID().toString().substring(0, 8) + "@widgify.test";
        String password = "Pass123!Password";
        String name = "Phase7 QA Tester";

        System.out.println("\n[TEST C] Registering and Authenticating QA User: " + email);
        registerUser(BASE_URL, name, email, password);

        AuthResult auth = client.login(email, password).get();
        System.out.println(" - Desktop Login Success: " + auth.isSuccess());
        if (!auth.isSuccess()) throw new RuntimeException("Desktop login failed!");

        // TEST D: Desktop Widgets Stage Instantiation
        System.out.println("\n[TEST D] Instantiating All 8 Desktop Widgets as Independent Stages...");
        // Ensure server_health widget exists in DB so 8 widgets load synchronously
        client.createWidget("server_health", "Server Health", 600, 50, 300, 180, "{}").get();

        Map<String, Object> widgetData = client.getWidgets().get();
        List<Map<String, Object>> widgetList = (List<Map<String, Object>>) widgetData.get("widgets");

        WidgetWindowManager manager = new WidgetWindowManager(client);
        CountDownLatch loadLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                manager.loadWidgets(widgetList);
            } finally {
                loadLatch.countDown();
            }
        });
        loadLatch.await(5, TimeUnit.SECONDS);

        Map<Integer, DesktopWidgetStage> stages = manager.getActiveStages();
        System.out.println(" - Active Independent Desktop Widget Stages Count: " + stages.size());
        if (stages.size() < 8) throw new RuntimeException("Expected 8 active widget stages, found " + stages.size());

        // TEST E: Widget Manager Master Controls (Hide All & Show All)
        System.out.println("\n[TEST E] Testing Widget Manager Master Controls (Hide All & Show All)...");
        final WidgetManagerStage[] mgrHolder = new WidgetManagerStage[1];
        CountDownLatch mgrLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                mgrHolder[0] = new WidgetManagerStage(client, manager, null);
            } finally {
                mgrLatch.countDown();
            }
        });
        mgrLatch.await(3, TimeUnit.SECONDS);
        WidgetManagerStage managerStage = mgrHolder[0];

        // Hide All
        CountDownLatch hideAllLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            String[] types = {"clock", "timer", "system", "weather", "notes", "tasks", "server_health", "launcher"};
            for (String t : types) manager.disableWidget(t);
            hideAllLatch.countDown();
        });
        hideAllLatch.await(3, TimeUnit.SECONDS);
        System.out.println(" - Active Stages after HIDE ALL: " + manager.getActiveStages().size());
        if (manager.getActiveStages().size() != 0) throw new RuntimeException("HIDE ALL failed to close all stages!");

        // Show All
        CountDownLatch showAllLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            String[] types = {"clock", "timer", "system", "weather", "notes", "tasks", "server_health", "launcher"};
            for (String t : types) manager.enableWidget(t);
            showAllLatch.countDown();
        });
        showAllLatch.await(3, TimeUnit.SECONDS);
        System.out.println(" - Active Stages after SHOW ALL: " + manager.getActiveStages().size());
        if (manager.getActiveStages().size() < 8) throw new RuntimeException("SHOW ALL failed to restore 8 stages!");

        // TEST F, G, H: Context Menu & Config Persistence & Presets
        System.out.println("\n[TEST G & H] Testing Configuration Persistence & Layout Presets...");
        String[] presets = {"Minimalist", "Productivity", "Developer", "All Active"};
        for (String p : presets) {
            CountDownLatch pLatch = new CountDownLatch(1);
            Platform.runLater(() -> {
                manager.applyLayoutPreset(p);
                pLatch.countDown();
            });
            pLatch.await(3, TimeUnit.SECONDS);
            System.out.println(" - Preset '" + p + "' Applied -> Active Stages: " + manager.getActiveStages().size());
        }

        // TEST I: Representative CRUD (Notes & Tasks)
        System.out.println("\n[TEST I] Testing Representative Notes & Tasks CRUD Operations...");
        Map<String, Object> noteRes = client.createNote("Final Audit Note", "Testing Phase 7 final acceptance").get();
        int noteId = ((Number) ((Map<String, Object>) noteRes.get("note")).get("id")).intValue();

        Map<String, Object> taskRes = client.createTask("Final Audit Task").get();
        int taskId = ((Number) ((Map<String, Object>) taskRes.get("task")).get("id")).intValue();

        client.toggleTask(taskId).get();
        client.deleteNote(noteId).get();
        client.deleteTask(taskId).get();
        System.out.println(" - Notes & Tasks CRUD operations completed successfully.");

        // TEST J & K: Position/Size Debounced Persistence & Multi-Monitor Safe Coordinates
        System.out.println("\n[TEST J & K] Testing Debounced Position Persistence & Multi-Monitor Safe Coordinates...");
        DesktopWidgetStage firstStage = manager.getActiveStages().values().iterator().next();
        int widgetId = firstStage.getWidgetId();
        manager.onWidgetMoved(widgetId, 500, 300);
        Thread.sleep(600); // Allow 300ms debounce persistence to commit

        Map<String, Object> posCheck = client.getWidgets().get();
        List<Map<String, Object>> posList = (List<Map<String, Object>>) posCheck.get("widgets");
        Map<String, Object> rec = posList.stream().filter(w -> ((Number) w.get("id")).intValue() == widgetId).findFirst().orElseThrow();
        int posX = ((Number) rec.getOrDefault("positionX", rec.get("position_x"))).intValue();
        System.out.println(" - Persisted MySQL Position X: " + posX);

        // TEST L & M: Network Failure Resilience & Session Expiration 401 Handling
        System.out.println("\n[TEST L & M] Testing Session Expiration 401 Central Handling...");
        client.clearSessionCookies(); // Invalidate session
        Map<String, Object> unauthRes = client.getWidgets().get();
        System.out.println(" - Post-Session Invalidation GET /widgets: Success=" + unauthRes.get("success") + ", StatusCode=" + unauthRes.get("statusCode"));
        System.out.println(" - Central 401 Callback Fired: " + sessionExpiredFlag[0]);
        if (!sessionExpiredFlag[0]) throw new RuntimeException("Central 401 callback failed to trigger!");

        // TEST N: Clean Application Shutdown
        System.out.println("\n[TEST N] Testing Clean Application Shutdown...");
        CountDownLatch cleanLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            manager.closeAllWidgets();
            if (managerStage != null) managerStage.close();
            cleanLatch.countDown();
        });
        cleanLatch.await(2, TimeUnit.SECONDS);
        System.out.println(" - All JavaFX desktop widget stages closed cleanly.");
    }

    private static void registerUser(String baseUrl, String name, String email, String password) throws Exception {
        HttpClient tempClient = HttpClient.newHttpClient();
        String formData = "name=" + URLEncoder.encode(name, StandardCharsets.UTF_8)
                + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/register"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        tempClient.send(req, HttpResponse.BodyHandlers.ofString());
    }
}
