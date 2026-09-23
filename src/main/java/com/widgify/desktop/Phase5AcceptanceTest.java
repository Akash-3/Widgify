package com.widgify.desktop;

import com.widgify.desktop.model.AuthResult;
import com.widgify.desktop.net.ServerApiClient;
import com.widgify.desktop.ui.DesktopWidgetStage;
import com.widgify.desktop.ui.WidgetWindowManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

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

public class Phase5AcceptanceTest extends Application {

    private static final String BASE_URL = "http://localhost:8080/widgify";
    private static final CountDownLatch testLatch = new CountDownLatch(1);
    private static boolean allPassed = false;
    private static String failureReason = "";

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("STARTING PHASE 5 FULL WIDGET ACCEPTANCE TEST SUITE");
        System.out.println("==================================================");

        new Thread(() -> Application.launch(Phase5AcceptanceTest.class, args)).start();

        boolean completed = testLatch.await(45, TimeUnit.SECONDS);
        if (!completed) {
            System.err.println("Phase 5 acceptance test suite timed out!");
            System.exit(1);
        }

        if (allPassed) {
            System.out.println("\n==================================================");
            System.out.println("ALL PHASE 5 ACCEPTANCE TESTS PASSED 100%!");
            System.out.println("==================================================");
            System.exit(0);
        } else {
            System.err.println("\nPHASE 5 TEST FAILURE: " + failureReason);
            System.exit(1);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(false);
        new Thread(() -> {
            try {
                runPhase5Tests();
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
    private void runPhase5Tests() throws Exception {
        ServerApiClient client = new ServerApiClient(BASE_URL);

        // --- SETUP: Register QA user ---
        String email = "phase5_qa_" + UUID.randomUUID().toString().substring(0, 8) + "@widgify.test";
        String password = "Pass123!Password";
        String name = "Phase5 QA Tester";

        System.out.println("[SETUP] Registering QA user: " + email);
        registerUser(BASE_URL, name, email, password);

        // TEST A: Login
        AuthResult auth = client.login(email, password).get();
        System.out.println("[TEST A] Desktop Login: " + auth.isSuccess());
        if (!auth.isSuccess()) throw new RuntimeException("Login failed");

        // TEST B, C, D: Widgets Creation & Verification on Desktop
        Map<String, Object> widgetData = client.getWidgets().get();
        List<Map<String, Object>> widgetList = (List<Map<String, Object>>) widgetData.get("widgets");

        WidgetWindowManager manager = new WidgetWindowManager(client);
        CountDownLatch stageLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                manager.loadWidgets(widgetList);
            } finally {
                stageLatch.countDown();
            }
        });
        stageLatch.await(5, TimeUnit.SECONDS);

        Map<Integer, DesktopWidgetStage> stages = manager.getActiveStages();
        System.out.println("[TEST B-F] Active Desktop Widget Stages: " + stages.size());

        DesktopWidgetStage clockStage = null;
        DesktopWidgetStage timerStage = null;
        DesktopWidgetStage systemStage = null;

        for (DesktopWidgetStage s : stages.values()) {
            String type = s.getWidget().getWidgetType();
            if ("clock".equalsIgnoreCase(type)) clockStage = s;
            if ("timer".equalsIgnoreCase(type)) timerStage = s;
            if ("system".equalsIgnoreCase(type)) systemStage = s;
        }

        System.out.println(" - Clock Stage Active: " + (clockStage != null));
        System.out.println(" - Timer Stage Active: " + (timerStage != null));
        System.out.println(" - Local System Stage Active: " + (systemStage != null));

        if (clockStage == null || timerStage == null || systemStage == null) {
            throw new RuntimeException("Clock, Timer, or System stage missing!");
        }

        // TEST E: Server Health (/system-health)
        System.out.println("\n[TEST E] Testing Server Health Endpoint (/system-health)");
        Map<String, Object> serverHealth = client.getServerHealth().get();
        System.out.println(" - Success: " + serverHealth.get("success"));
        System.out.println(" - Server Label: " + serverHealth.get("serverLabel"));
        System.out.println(" - CPU Usage: " + serverHealth.get("cpuUsage") + "%");
        System.out.println(" - Memory Used/Total: " + serverHealth.get("memoryUsed") + " / " + serverHealth.get("memoryTotal"));

        if (!Boolean.TRUE.equals(serverHealth.get("success"))) {
            throw new RuntimeException("Server Health endpoint failed!");
        }
        if (!Boolean.TRUE.equals(serverHealth.get("isServerMetrics"))) {
            throw new RuntimeException("Server Health output missing isServerMetrics flag!");
        }

        // TEST F: Weather Endpoint (/weather -> Open-Meteo)
        System.out.println("\n[TEST F] Testing Weather Endpoint (/weather -> Open-Meteo)");
        Map<String, Object> weatherResult = client.getWeather("Bhubaneswar").get();
        System.out.println(" - Success: " + weatherResult.get("success"));
        if (Boolean.TRUE.equals(weatherResult.get("success"))) {
            System.out.println(" - Location: " + weatherResult.get("location"));
            System.out.println(" - Temperature: " + weatherResult.get("temperature") + weatherResult.get("unit"));
            System.out.println(" - Condition: " + weatherResult.get("condition"));
            System.out.println(" - Feels Like: " + weatherResult.get("feelsLike") + weatherResult.get("unit"));
        } else {
            System.out.println(" - Weather Unavailable Notice: " + weatherResult.get("message"));
        }

        // TEST G: Notes CRUD Persistence (/notes)
        System.out.println("\n[TEST G] Testing Notes CRUD Persistence (/notes)");
        Map<String, Object> createNoteRes = client.createNote("QA Meeting Notes", "Discuss Phase 5 deliverables.").get();
        System.out.println(" - Create Note Success: " + createNoteRes.get("success"));
        Map<String, Object> noteObj = (Map<String, Object>) createNoteRes.get("note");
        int noteId = ((Number) noteObj.get("id")).intValue();

        // Edit Note
        Map<String, Object> updateNoteRes = client.updateNote(noteId, "QA Meeting Notes (Updated)", "Discuss Phase 5 & 6.").get();
        System.out.println(" - Update Note Success: " + updateNoteRes.get("success"));

        // Verify in list
        Map<String, Object> notesListRes = client.getNotes().get();
        List<Map<String, Object>> notesList = (List<Map<String, Object>>) notesListRes.get("notes");
        boolean noteFound = notesList.stream().anyMatch(n -> ((Number) n.get("id")).intValue() == noteId && "QA Meeting Notes (Updated)".equals(n.get("title")));
        System.out.println(" - Note Verified in GET /notes: " + noteFound);
        if (!noteFound) throw new RuntimeException("Updated note not found in GET /notes!");

        // Delete Note
        Map<String, Object> deleteNoteRes = client.deleteNote(noteId).get();
        System.out.println(" - Delete Note Success: " + deleteNoteRes.get("success"));

        // TEST H: Tasks CRUD Persistence (/tasks)
        System.out.println("\n[TEST H] Testing Tasks CRUD Persistence (/tasks)");
        Map<String, Object> createTaskRes = client.createTask("Complete Phase 5 Audit").get();
        System.out.println(" - Create Task Success: " + createTaskRes.get("success"));
        Map<String, Object> taskObj = (Map<String, Object>) createTaskRes.get("task");
        int taskId = ((Number) taskObj.get("id")).intValue();

        // Toggle Task Completion
        Map<String, Object> toggleTaskRes = client.toggleTask(taskId).get();
        System.out.println(" - Toggle Task Success: " + toggleTaskRes.get("success"));

        // Update Task Title
        Map<String, Object> updateTaskRes = client.updateTask(taskId, "Complete Phase 5 Audit (Verified)", true).get();
        System.out.println(" - Update Task Success: " + updateTaskRes.get("success"));

        // Verify in list
        Map<String, Object> tasksListRes = client.getTasks().get();
        List<Map<String, Object>> tasksList = (List<Map<String, Object>>) tasksListRes.get("tasks");
        boolean taskFound = tasksList.stream().anyMatch(t -> ((Number) t.get("id")).intValue() == taskId && Boolean.TRUE.equals(t.get("completed")));
        System.out.println(" - Task Verified in GET /tasks: " + taskFound);
        if (!taskFound) throw new RuntimeException("Updated completed task not found in GET /tasks!");

        // Delete Task
        Map<String, Object> deleteTaskRes = client.deleteTask(taskId).get();
        System.out.println(" - Delete Task Success: " + deleteTaskRes.get("success"));

        // TEST J & K: Position/Size Debounced Persistence & Restoration
        System.out.println("\n[TEST J & K] Testing Debounced Position Persistence & Restoration");
        int clockId = clockStage.getWidgetId();
        manager.onWidgetMoved(clockId, 640, 320);
        manager.onWidgetResized(clockId, 360, 240);
        Thread.sleep(600); // Allow 300ms debounce timer to persist

        Map<String, Object> updatedWidgetData = client.getWidgets().get();
        List<Map<String, Object>> updatedWidgets = (List<Map<String, Object>>) updatedWidgetData.get("widgets");
        Map<String, Object> clockRec = updatedWidgets.stream().filter(w -> ((Number) w.get("id")).intValue() == clockId).findFirst().orElse(null);
        int posX = ((Number) clockRec.getOrDefault("positionX", clockRec.get("position_x"))).intValue();
        int posY = ((Number) clockRec.getOrDefault("positionY", clockRec.get("position_y"))).intValue();
        System.out.println(" - Persisted MySQL Coordinates: X=" + posX + ", Y=" + posY);
        if (posX != 640 || posY != 320) throw new RuntimeException("Position persistence failed!");

        // TEST L & M: Logout Cleanup & Invalidation
        System.out.println("\n[TEST L & M] Testing Logout Cleanup & Session Invalidation");
        CountDownLatch shutdownLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            manager.closeAllWidgets();
            shutdownLatch.countDown();
        });
        shutdownLatch.await(2, TimeUnit.SECONDS);

        boolean logoutRes = client.logout().get();
        System.out.println(" - Client Logout Success: " + logoutRes);
        System.out.println(" - Remaining Desktop Stages: " + manager.getActiveStages().size());
        if (manager.getActiveStages().size() != 0) throw new RuntimeException("Widget stages remained open after logout!");

        Map<String, Object> postLogoutRes = client.getWidgets().get();
        System.out.println(" - Post-logout GET /widgets: Success=" + postLogoutRes.get("success") + ", StatusCode=" + postLogoutRes.get("statusCode"));
        if (Boolean.TRUE.equals(postLogoutRes.get("success"))) throw new RuntimeException("Protected API allowed access post-logout!");
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
