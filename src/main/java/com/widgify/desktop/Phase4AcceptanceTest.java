package com.widgify.desktop;

import com.widgify.desktop.model.AuthResult;
import com.widgify.desktop.net.ServerApiClient;
import com.widgify.desktop.ui.DesktopWidgetStage;
import com.widgify.desktop.ui.WidgetWindowManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Screen;
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

public class Phase4AcceptanceTest extends Application {

    private static final String BASE_URL = "http://localhost:8080/widgify";
    private static CountDownLatch testLatch = new CountDownLatch(1);
    private static boolean allPassed = false;
    private static String failureReason = "";

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("STARTING PHASE 4 REAL-WORLD ACCEPTANCE TEST SUITE");
        System.out.println("==================================================");

        // Launch JavaFX Application in background thread for stage testing
        new Thread(() -> Application.launch(Phase4AcceptanceTest.class, args)).start();

        boolean completed = testLatch.await(30, TimeUnit.SECONDS);
        if (!completed) {
            System.err.println("Acceptance test suite timed out!");
            System.exit(1);
        }

        if (allPassed) {
            System.out.println("\n==================================================");
            System.out.println("ALL PHASE 4 ACCEPTANCE TESTS PASSED 100%!");
            System.out.println("==================================================");
            System.exit(0);
        } else {
            System.err.println("\nACCEPTANCE TEST FAILURE: " + failureReason);
            System.exit(1);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(false);
        // Run tests on JavaFX application thread
        new Thread(() -> {
            try {
                runAcceptanceTests();
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
    private void runAcceptanceTests() throws Exception {
        ServerApiClient client = new ServerApiClient(BASE_URL);

        // --- SETUP: Register unique test user ---
        String email = "phase4_qa_" + UUID.randomUUID().toString().substring(0, 8) + "@widgify.test";
        String password = "Pass123!Password";
        String name = "Phase4 QA Tester";

        System.out.println("[SETUP] Registering QA user: " + email);
        registerUser(BASE_URL, name, email, password);

        // 1. LOGIN
        AuthResult auth = client.login(email, password).get();
        System.out.println("[TEST 1.1] Desktop Login: " + auth.isSuccess());
        if (!auth.isSuccess()) throw new RuntimeException("Login failed");

        // 2. FETCH WIDGETS
        Map<String, Object> widgetData = client.getWidgets().get();
        List<Map<String, Object>> widgetList = (List<Map<String, Object>>) widgetData.get("widgets");
        System.out.println("[TEST 1.2] Fetched Widgets Count: " + (widgetList != null ? widgetList.size() : 0));
        if (widgetList == null || widgetList.isEmpty()) throw new RuntimeException("No widgets returned");

        // 3. INITIALIZE WIDGET WINDOW MANAGER & DESKTOP STAGES ON JAVAFX THREAD
        CountDownLatch stageLatch = new CountDownLatch(1);
        WidgetWindowManager manager = new WidgetWindowManager(client);

        Platform.runLater(() -> {
            manager.loadWidgets(widgetList);
            stageLatch.countDown();
        });
        stageLatch.await(5, TimeUnit.SECONDS);

        Map<Integer, DesktopWidgetStage> stages = manager.getActiveStages();
        System.out.println("[TEST 1.3] Active Desktop Stages Count: " + stages.size());

        // Find Clock, Timer, System stages
        DesktopWidgetStage clockStage = null;
        DesktopWidgetStage timerStage = null;
        DesktopWidgetStage systemStage = null;

        for (DesktopWidgetStage s : stages.values()) {
            String type = s.getWidget().getWidgetType();
            if ("clock".equalsIgnoreCase(type)) clockStage = s;
            if ("timer".equalsIgnoreCase(type)) timerStage = s;
            if ("system".equalsIgnoreCase(type)) systemStage = s;
        }

        System.out.println("[TEST 1.4] Independent Stages Check:");
        System.out.println(" - Clock Stage Present: " + (clockStage != null));
        System.out.println(" - Timer Stage Present: " + (timerStage != null));
        System.out.println(" - System Stage Present: " + (systemStage != null));

        if (clockStage == null || timerStage == null || systemStage == null) {
            throw new RuntimeException("Clock, Timer, or System stage missing");
        }

        // Verify separate Stage instances
        if (clockStage == timerStage || clockStage == systemStage || timerStage == systemStage) {
            throw new RuntimeException("Widget stages are not independent instances!");
        }

        // --- TEST 1: INDEPENDENT MOVEMENT & CLOSING ---
        final DesktopWidgetStage fClock = clockStage;
        final DesktopWidgetStage fTimer = timerStage;

        CountDownLatch moveLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            double initialTimerX = fTimer.getX();
            fClock.setX(520);
            fClock.setY(280);
            System.out.println("   [MOVE] Set Clock X=520, Y=280. Timer X remains: " + fTimer.getX());
            if (fTimer.getX() != initialTimerX) {
                throw new RuntimeException("Moving Clock moved Timer stage!");
            }
            moveLatch.countDown();
        });
        moveLatch.await(2, TimeUnit.SECONDS);

        // --- TEST 2: DEBOUNCED POSITION & SIZE PERSISTENCE TO MYSQL ---
        int clockId = clockStage.getWidgetId();
        System.out.println("\n[TEST 2] Testing Debounced Position & Size Persistence for Widget #" + clockId);

        // Trigger move & resize
        manager.onWidgetMoved(clockId, 520, 280);
        manager.onWidgetResized(clockId, 340, 220);

        // Wait 600ms to allow 300ms debounce timer to execute HTTP update to Tomcat
        Thread.sleep(600);

        // Query server GET /widgets to verify MySQL persistence
        Map<String, Object> updatedData = client.getWidgets().get();
        List<Map<String, Object>> updatedList = (List<Map<String, Object>>) updatedData.get("widgets");

        Map<String, Object> clockRecord = null;
        for (Map<String, Object> w : updatedList) {
            if (((Number) w.get("id")).intValue() == clockId) {
                clockRecord = w;
                break;
            }
        }

        if (clockRecord == null) throw new RuntimeException("Clock record not found on server");

        int persistedX = ((Number) clockRecord.getOrDefault("positionX", clockRecord.get("position_x"))).intValue();
        int persistedY = ((Number) clockRecord.getOrDefault("positionY", clockRecord.get("position_y"))).intValue();
        int persistedW = ((Number) clockRecord.getOrDefault("width", 0)).intValue();
        int persistedH = ((Number) clockRecord.getOrDefault("height", 0)).intValue();

        System.out.println("   [PERSISTED IN MYSQL] position_x=" + persistedX + ", position_y=" + persistedY + ", width=" + persistedW + ", height=" + persistedH);

        if (persistedX != 520 || persistedY != 280 || persistedW != 340 || persistedH != 220) {
            throw new RuntimeException("Debounced persistence values in MySQL mismatch!");
        }

        // --- TEST 3: RESTART & RESTORATION SIMULATION ---
        System.out.println("\n[TEST 3] Testing Restart & Position Restoration");
        // Shutdown manager and simulate clean restart
        CountDownLatch shutdownLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            manager.shutdown();
            shutdownLatch.countDown();
        });
        shutdownLatch.await(2, TimeUnit.SECONDS);

        // Re-fetch widgets from server and re-create stages
        Map<String, Object> freshData = client.getWidgets().get();
        System.out.println("   [FRESH GET /widgets] Success: " + freshData.get("success") + ", StatusCode: " + freshData.get("statusCode"));
        List<Map<String, Object>> freshList = (List<Map<String, Object>>) freshData.get("widgets");
        System.out.println("   [FRESH WIDGETS COUNT] " + (freshList != null ? freshList.size() : 0));
        if (freshList != null) {
            for (Map<String, Object> item : freshList) {
                System.out.println("     Item: " + item);
            }
        }

        WidgetWindowManager newManager = new WidgetWindowManager(client);
        CountDownLatch restoreLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                newManager.loadWidgets(freshList);
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                restoreLatch.countDown();
            }
        });
        restoreLatch.await(5, TimeUnit.SECONDS);

        System.out.println("   [NEW MANAGER ACTIVE STAGES] Count: " + newManager.getActiveStages().size());
        DesktopWidgetStage restoredClock = null;
        for (DesktopWidgetStage s : newManager.getActiveStages().values()) {
            System.out.println("    - Active stage #" + s.getWidgetId() + " type: " + s.getWidget().getWidgetType());
            if ("clock".equalsIgnoreCase(s.getWidget().getWidgetType())) {
                restoredClock = s;
                break;
            }
        }

        if (restoredClock == null) throw new RuntimeException("Restored Clock stage missing!");

        System.out.println("   [RESTORED STAGE] X=" + (int) restoredClock.getX() + ", Y=" + (int) restoredClock.getY() + ", W=" + (int) restoredClock.getWidth() + ", H=" + (int) restoredClock.getHeight());

        if ((int) restoredClock.getX() != 520 || (int) restoredClock.getY() != 280) {
            throw new RuntimeException("Restored Stage coordinates do not match saved MySQL position!");
        }

        // --- TEST 4: LOGOUT & SESSION INVALIDATION ---
        System.out.println("\n[TEST 4] Testing Logout Cleanup & Session Invalidation");
        CountDownLatch logoutLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            newManager.closeAllWidgets();
            logoutLatch.countDown();
        });
        logoutLatch.await(2, TimeUnit.SECONDS);

        boolean loggedOut = client.logout().get();
        System.out.println("   [LOGOUT] Client logout completed: " + loggedOut);
        System.out.println("   [ACTIVE STAGES] Stages remaining after logout: " + newManager.getActiveStages().size());

        if (newManager.getActiveStages().size() != 0) {
            throw new RuntimeException("Widget stages remained open after logout!");
        }

        Map<String, Object> postLogoutData = client.getWidgets().get();
        System.out.println("   [POST-LOGOUT GET /widgets] Success: " + postLogoutData.get("success") + ", StatusCode: " + postLogoutData.get("statusCode"));

        if (Boolean.TRUE.equals(postLogoutData.get("success"))) {
            throw new RuntimeException("Protected /widgets request succeeded after logout!");
        }

        System.out.println("\n[TEST 5] Multi-Monitor Environment Info:");
        List<Screen> screens = Screen.getScreens();
        System.out.println("   Available Screens Count: " + screens.size());
        for (int i = 0; i < screens.size(); i++) {
            System.out.println("   Screen #" + i + " bounds: " + screens.get(i).getVisualBounds());
        }
        if (screens.size() == 1) {
            System.out.println("   Note: Single monitor detected. Multi-monitor secondary screen restoration test could not be physically performed.");
        }
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
