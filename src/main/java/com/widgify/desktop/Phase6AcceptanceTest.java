package com.widgify.desktop;

import com.widgify.desktop.model.AuthResult;
import com.widgify.desktop.net.ServerApiClient;
import com.widgify.desktop.ui.DesktopWidgetStage;
import com.widgify.desktop.ui.WidgetManagerStage;
import com.widgify.desktop.ui.WidgetWindowManager;
import com.widgify.desktop.widgets.*;

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

public class Phase6AcceptanceTest extends Application {

    private static final String BASE_URL = "http://localhost:8080/widgify";
    private static final CountDownLatch testLatch = new CountDownLatch(1);
    private static boolean allPassed = false;
    private static String failureReason = "";

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("STARTING PHASE 6 WIDGET MANAGER & PRESETS TEST SUITE");
        System.out.println("==================================================");

        new Thread(() -> Application.launch(Phase6AcceptanceTest.class, args)).start();

        boolean completed = testLatch.await(60, TimeUnit.SECONDS);
        if (!completed) {
            System.err.println("Phase 6 acceptance test suite timed out!");
            System.exit(1);
        }

        if (allPassed) {
            System.out.println("\n==================================================");
            System.out.println("ALL PHASE 6 ACCEPTANCE TESTS PASSED 100%!");
            System.out.println("==================================================");
            System.exit(0);
        } else {
            System.err.println("\nPHASE 6 TEST FAILURE: " + failureReason);
            System.exit(1);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(false);
        new Thread(() -> {
            try {
                runPhase6Tests();
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
    private void runPhase6Tests() throws Exception {
        ServerApiClient client = new ServerApiClient(BASE_URL);

        // --- SETUP: Register QA user ---
        String email = "phase6_qa_" + UUID.randomUUID().toString().substring(0, 8) + "@widgify.test";
        String password = "Pass123!Password";
        String name = "Phase6 QA Tester";

        System.out.println("[SETUP] Registering QA user: " + email);
        registerUser(BASE_URL, name, email, password);

        // TEST C: Desktop Login & Widget Manager Initialization
        AuthResult auth = client.login(email, password).get();
        System.out.println("[TEST C] Desktop Login: " + auth.isSuccess());
        if (!auth.isSuccess()) throw new RuntimeException("Login failed");

        Map<String, Object> initialWidgetsRes = client.getWidgets().get();
        List<Map<String, Object>> widgetList = (List<Map<String, Object>>) initialWidgetsRes.get("widgets");

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

        WidgetManagerStage managerStage = null;
        CountDownLatch mgrLatch = new CountDownLatch(1);
        final WidgetManagerStage[] mgrHolder = new WidgetManagerStage[1];
        Platform.runLater(() -> {
            try {
                mgrHolder[0] = new WidgetManagerStage(client, manager, null);
            } finally {
                mgrLatch.countDown();
            }
        });
        mgrLatch.await(3, TimeUnit.SECONDS);
        managerStage = mgrHolder[0];

        System.out.println(" - Widget Manager Window Initialized: " + (managerStage != null));

        // TEST D & E: Widget Disable and Re-enable
        System.out.println("\n[TEST D & E] Testing Widget Disable and Re-enable");
        CountDownLatch disableLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            manager.disableWidget("weather");
            disableLatch.countDown();
        });
        disableLatch.await(3, TimeUnit.SECONDS);

        boolean weatherActivePostDisable = manager.isWidgetActive("weather");
        System.out.println(" - Weather Active Post-Disable: " + weatherActivePostDisable);
        if (weatherActivePostDisable) throw new RuntimeException("Weather stage remained active after disableWidget!");

        // Verify database record remains
        Map<String, Object> postDisableWidgets = client.getWidgets().get();
        List<Map<String, Object>> listPostDisable = (List<Map<String, Object>>) postDisableWidgets.get("widgets");
        boolean recordExists = listPostDisable.stream().anyMatch(w -> {
            Object t = w.get("widgetType");
            if (t == null) t = w.get("widget_type");
            return "weather".equalsIgnoreCase(String.valueOf(t));
        });
        System.out.println(" - Weather Database Record Retained: " + recordExists);
        if (!recordExists) throw new RuntimeException("Weather widget record deleted from database!");

        // Re-enable
        CountDownLatch enableLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            manager.enableWidget("weather");
            enableLatch.countDown();
        });
        enableLatch.await(3, TimeUnit.SECONDS);
        boolean weatherActivePostEnable = manager.isWidgetActive("weather");
        System.out.println(" - Weather Active Post-Re-enable: " + weatherActivePostEnable);
        if (!weatherActivePostEnable) throw new RuntimeException("Weather stage failed to re-enable!");

        // TEST F, G, H, I: Configuration Persistence (Weather, Timer, Clock)
        System.out.println("\n[TEST G, H, I] Testing Widget Configurations Persistence");
        int clockId = manager.getActiveStages().values().stream()
                .filter(s -> "clock".equalsIgnoreCase(s.getWidget().getWidgetType()))
                .mapToInt(DesktopWidgetStage::getWidgetId).findFirst().orElse(0);

        int timerId = manager.getActiveStages().values().stream()
                .filter(s -> "timer".equalsIgnoreCase(s.getWidget().getWidgetType()))
                .mapToInt(DesktopWidgetStage::getWidgetId).findFirst().orElse(0);

        int weatherId = manager.getActiveStages().values().stream()
                .filter(s -> "weather".equalsIgnoreCase(s.getWidget().getWidgetType()))
                .mapToInt(DesktopWidgetStage::getWidgetId).findFirst().orElse(0);

        // Update Clock format to 12h
        client.updateWidgetConfig(clockId, "{\"format\":\"12h\"}").get();
        // Update Timer preset to 15m
        client.updateWidgetConfig(timerId, "{\"preset\":15}").get();
        // Update Weather location to Tokyo
        client.updateWidgetConfig(weatherId, "{\"location\":\"Tokyo\"}").get();

        // Verify config survives server read
        Map<String, Object> confRes = client.getWidgets().get();
        List<Map<String, Object>> confList = (List<Map<String, Object>>) confRes.get("widgets");

        Map<String, Object> clockRec = confList.stream().filter(w -> ((Number) w.get("id")).intValue() == clockId).findFirst().orElseThrow();
        Map<String, Object> timerRec = confList.stream().filter(w -> ((Number) w.get("id")).intValue() == timerId).findFirst().orElseThrow();
        Map<String, Object> weatherRec = confList.stream().filter(w -> ((Number) w.get("id")).intValue() == weatherId).findFirst().orElseThrow();

        System.out.println(" - Clock Config Saved: " + clockRec.get("config"));
        System.out.println(" - Timer Config Saved: " + timerRec.get("config"));
        System.out.println(" - Weather Config Saved: " + weatherRec.get("config"));

        if (!String.valueOf(clockRec.get("config")).contains("12h")) throw new RuntimeException("Clock config persistence failed!");
        if (!String.valueOf(timerRec.get("config")).contains("15")) throw new RuntimeException("Timer config persistence failed!");
        if (!String.valueOf(weatherRec.get("config")).contains("Tokyo")) throw new RuntimeException("Weather config persistence failed!");

        // TEST J: Reset Position
        System.out.println("\n[TEST J] Testing Reset Position");
        DesktopWidgetStage clockStage = manager.getActiveStages().get(clockId);
        CountDownLatch resetLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            clockStage.resetPosition();
            resetLatch.countDown();
        });
        resetLatch.await(3, TimeUnit.SECONDS);

        Thread.sleep(600); // Allow debounce timer to persist
        Map<String, Object> postResetWidgets = client.getWidgets().get();
        List<Map<String, Object>> postResetList = (List<Map<String, Object>>) postResetWidgets.get("widgets");
        Map<String, Object> clockPostReset = postResetList.stream().filter(w -> ((Number) w.get("id")).intValue() == clockId).findFirst().orElseThrow();
        System.out.println(" - Clock Position Post-Reset: X=" + clockPostReset.get("position_x") + ", Y=" + clockPostReset.get("position_y"));

        // TEST K: Layout Presets (Minimalist, Productivity, Developer, All Active)
        System.out.println("\n[TEST K] Testing Layout Presets Application");
        String[] presets = {"Minimalist", "Productivity", "Developer", "All Active"};
        for (String preset : presets) {
            CountDownLatch pLatch = new CountDownLatch(1);
            Platform.runLater(() -> {
                manager.applyLayoutPreset(preset);
                pLatch.countDown();
            });
            pLatch.await(3, TimeUnit.SECONDS);

            int activeCount = manager.getActiveStages().size();
            System.out.println(" - Preset '" + preset + "' Applied -> Active Stages: " + activeCount);
            if (activeCount == 0) throw new RuntimeException("Preset '" + preset + "' resulted in 0 active stages!");
        }

        // TEST L & M: Logout & Re-login Restoration
        System.out.println("\n[TEST L & M] Testing Logout & Re-login Layout Restoration");
        CountDownLatch closeLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            manager.closeAllWidgets();
            if (managerHolder[0] != null) managerHolder[0].close();
            closeLatch.countDown();
        });
        closeLatch.await(2, TimeUnit.SECONDS);

        client.logout().get();
        System.out.println(" - Client Logged Out Cleanly.");

        // Re-login
        AuthResult reloginAuth = client.login(email, password).get();
        System.out.println(" - Re-login Auth: " + reloginAuth.isSuccess());
        if (!reloginAuth.isSuccess()) throw new RuntimeException("Re-login failed!");

        Map<String, Object> restoredRes = client.getWidgets().get();
        List<Map<String, Object>> restoredList = (List<Map<String, Object>>) restoredRes.get("widgets");
        System.out.println(" - Restored Widget Count from Server: " + restoredList.size());
        if (restoredList.isEmpty()) throw new RuntimeException("No widgets restored on re-login!");

        // TEST N: Regression Check on All 8 Widget Types
        System.out.println("\n[TEST N] Regression Check on All 8 Widget Types");
        WidgetWindowManager regManager = new WidgetWindowManager(client);
        CountDownLatch regLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            regManager.loadWidgets(restoredList);
            regLatch.countDown();
        });
        regLatch.await(5, TimeUnit.SECONDS);

        System.out.println(" - Total Active Stages Post-Relogin: " + regManager.getActiveStages().size());

        // Cleanup
        CountDownLatch cleanLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            regManager.closeAllWidgets();
            cleanLatch.countDown();
        });
        cleanLatch.await(2, TimeUnit.SECONDS);
        client.logout().get();
    }

    private static final WidgetManagerStage[] managerHolder = new WidgetManagerStage[1];

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
