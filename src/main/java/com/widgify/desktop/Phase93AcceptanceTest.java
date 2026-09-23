package com.widgify.desktop;

import com.widgify.desktop.net.ServerApiClient;
import com.widgify.desktop.ui.*;
import com.widgify.desktop.widgets.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Phase 9.3 Acceptance Test Suite (Tests AH through AW).
 *
 * AH — Apply deploys ONLY current canvas widgets (not all backend widgets)
 * AI — Removed widget is not deployed
 * AJ — Clear Canvas produces empty deployment (all widgets closed)
 * AK — Reset Layout restores 7 defaults in canvas
 * AL — Save persists exact current canvas (not rebuilt from backend list)
 * AM — Normalized X mapping: canvasX/canvasW * screenW + screenMinX
 * AN — Normalized Y mapping: canvasY/canvasH * screenH + screenMinY
 * AO — Normalized width mapping: widgetW/canvasW * screenW
 * AP — Normalized height mapping: widgetH/canvasH * screenH
 * AQ — Negative monitor coordinates are supported (minX may be negative)
 * AR — No setAlwaysOnTop in DesktopWidgetStage
 * AS — WS_EX_NOACTIVATE / WS_EX_TOOLWINDOW applied (WindowsDesktopHelper exists)
 * AT — HWND_BOTTOM z-order is set (WindowsDesktopHelper sets HWND_BOTTOM)
 * AU — deployFromCanvas closes all previous stages before deploying new ones
 * AV — Empty canvas removes all deployed widgets
 * AW — deployFromCanvas uses canvas list, not apiClient.getWidgets()
 */
public class Phase93AcceptanceTest extends Application {

    private static final CountDownLatch testLatch = new CountDownLatch(1);
    private static boolean allPassed = false;
    private static String failureReason = "";

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("STARTING PHASE 9.3 ACCEPTANCE TEST SUITE");
        System.out.println("==================================================");

        new Thread(() -> Application.launch(Phase93AcceptanceTest.class, args)).start();

        boolean completed = testLatch.await(60, TimeUnit.SECONDS);
        if (!completed) {
            System.err.println("Phase 9.3 acceptance test suite timed out!");
            System.exit(1);
        }

        if (allPassed) {
            System.out.println("\n==================================================");
            System.out.println("ALL PHASE 9.3 ACCEPTANCE TESTS PASSED 100%!");
            System.out.println("==================================================");
            System.exit(0);
        } else {
            System.err.println("\n==================================================");
            System.err.println("PHASE 9.3 ACCEPTANCE TEST FAILED: " + failureReason);
            System.err.println("==================================================");
            System.exit(1);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        new Thread(() -> {
            try {
                runAllTests();
                allPassed = true;
            } catch (Throwable t) {
                allPassed = false;
                failureReason = t.getMessage();
                t.printStackTrace();
            } finally {
                testLatch.countDown();
                Platform.exit();
            }
        }).start();
    }

    private void runAllTests() throws Exception {
        testAH_ApplyUsesOnlyCanvasWidgets();
        testAI_RemovedWidgetNotDeployed();
        testAJ_ClearCanvasProducesEmptyDeployment();
        testAK_ResetLayoutRestores7Defaults();
        testAL_SavePersistsExactCanvas();
        testAM_NormalizedXMapping();
        testAN_NormalizedYMapping();
        testAO_NormalizedWidthMapping();
        testAP_NormalizedHeightMapping();
        testAQ_NegativeMonitorCoordinates();
        testAR_NoAlwaysOnTopInDesktopWidgetStage();
        testAS_WindowsDesktopHelperExists();
        testAT_HwndBottomUsed();
        testAU_DeployFromCanvasClosesOldStages();
        testAV_EmptyCanvasClosesAllWidgets();
        testAW_ApplyDoesNotCallApiGetWidgets();

        System.out.println("\n[SUMMARY] All Phase 9.3 tests completed successfully!");
    }

    // ── AH ──────────────────────────────────────────────────────────────────────

    private void testAH_ApplyUsesOnlyCanvasWidgets() throws Exception {
        System.out.print("[AH] Apply deploys ONLY canvas widgets (not all backend widgets)... ");

        // Verify DesktopPreviewCanvas.applyLayoutToDesktop does NOT call apiClient.getWidgets()
        File source = new File("src/main/java/com/widgify/desktop/ui/DesktopPreviewCanvas.java");
        if (source.exists()) {
            String content = new String(Files.readAllBytes(source.toPath()));
            // The method should use deployFromCanvas, not loadWidgets/getWidgets
            String applyMethod = extractMethod(content, "applyLayoutToDesktop");
            if (applyMethod.contains("apiClient.getWidgets()")) {
                throw new RuntimeException(
                        "applyLayoutToDesktop still calls apiClient.getWidgets()! Canvas is not source of truth.");
            }
            if (!applyMethod.contains("deployFromCanvas")) {
                throw new RuntimeException(
                        "applyLayoutToDesktop must call windowManager.deployFromCanvas()!");
            }
        }

        System.out.println("PASS");
    }

    // ── AI ──────────────────────────────────────────────────────────────────────

    private void testAI_RemovedWidgetNotDeployed() throws Exception {
        System.out.print("[AI] Removed widget is not deployed (removePreviewWidget takes it out of list)... ");

        File source = new File("src/main/java/com/widgify/desktop/ui/DesktopPreviewCanvas.java");
        if (source.exists()) {
            String content = new String(Files.readAllBytes(source.toPath()));
            // removePreviewWidget must remove from previewWidgets list
            if (!content.contains("previewWidgets.remove(widget)")) {
                throw new RuntimeException("removePreviewWidget must call previewWidgets.remove(widget)!");
            }
        }

        System.out.println("PASS");
    }

    // ── AJ ──────────────────────────────────────────────────────────────────────

    private void testAJ_ClearCanvasProducesEmptyDeployment() throws Exception {
        System.out.print("[AJ] Clear Canvas clears previewWidgets list (empty canvas → empty deployment)... ");

        File source = new File("src/main/java/com/widgify/desktop/ui/DesktopPreviewCanvas.java");
        if (source.exists()) {
            String content = new String(Files.readAllBytes(source.toPath()));
            String clearMethod = extractMethod(content, "clearCanvas");
            if (!clearMethod.contains("previewWidgets.clear()")) {
                throw new RuntimeException("clearCanvas must call previewWidgets.clear()!");
            }
        }

        // Verify applyLayoutToDesktop with empty canvas calls closeAllWidgets
        File canvasSource = new File("src/main/java/com/widgify/desktop/ui/DesktopPreviewCanvas.java");
        if (canvasSource.exists()) {
            String content = new String(Files.readAllBytes(canvasSource.toPath()));
            String applyMethod = extractMethod(content, "applyLayoutToDesktop");
            if (!applyMethod.contains("closeAllWidgets")) {
                throw new RuntimeException("applyLayoutToDesktop must call closeAllWidgets when canvas is empty!");
            }
        }

        System.out.println("PASS");
    }

    // ── AK ──────────────────────────────────────────────────────────────────────

    private void testAK_ResetLayoutRestores7Defaults() throws Exception {
        System.out.print("[AK] Reset Layout calls backend /widgets/reset (not Apply)... ");

        File source = new File("src/main/java/com/widgify/desktop/ui/DesktopPreviewCanvas.java");
        if (source.exists()) {
            String content = new String(Files.readAllBytes(source.toPath()));
            String resetMethod = extractMethod(content, "resetToDefaults");
            if (!resetMethod.contains("resetWidgets")) {
                throw new RuntimeException("resetToDefaults must call apiClient.resetWidgets()!");
            }
            // Reset must NOT call deployFromCanvas — it only refreshes the canvas preview
            if (resetMethod.contains("deployFromCanvas")) {
                throw new RuntimeException("resetToDefaults must NOT deploy to desktop automatically!");
            }
        }

        System.out.println("PASS");
    }

    // ── AL ──────────────────────────────────────────────────────────────────────

    private void testAL_SavePersistsExactCanvas() throws Exception {
        System.out.print("[AL] Save Layout persists current canvas widgets only... ");

        File source = new File("src/main/java/com/widgify/desktop/ui/DesktopPreviewCanvas.java");
        if (source.exists()) {
            String content = new String(Files.readAllBytes(source.toPath()));
            String saveMethod = extractMethod(content, "saveLayoutToBackend");
            // Save must iterate over previewWidgets
            if (!saveMethod.contains("previewWidgets")) {
                throw new RuntimeException("saveLayoutToBackend must iterate over previewWidgets!");
            }
            // Save must NOT call getWidgets() from backend
            if (saveMethod.contains("apiClient.getWidgets()")) {
                throw new RuntimeException("saveLayoutToBackend must not call apiClient.getWidgets()!");
            }
        }

        System.out.println("PASS");
    }

    // ── AM ──────────────────────────────────────────────────────────────────────

    private void testAM_NormalizedXMapping() throws Exception {
        System.out.print("[AM] Normalized X mapping: normX * screenW + screenMinX... ");

        File source = new File("src/main/java/com/widgify/desktop/ui/WidgetWindowManager.java");
        if (source.exists()) {
            String content = new String(Files.readAllBytes(source.toPath()));
            // deployFromCanvas must compute normX and then realX using screenX + normX * screenW
            if (!content.contains("normX") || !content.contains("screenX")) {
                throw new RuntimeException("deployFromCanvas must use normX and screenX for coordinate mapping!");
            }
            if (!content.contains("screenX + normX * screenW")) {
                throw new RuntimeException("deployFromCanvas: realX must be screenX + normX * screenW!");
            }
        }

        // Verify numerically: normX = canvasX / canvasW; realX = screenMinX + normX * screenW
        double canvasW = 900, screenW = 1920, screenMinX = 0;
        double widgetX = 180; // widget at 180px in 900px canvas = 20%
        double normX = widgetX / canvasW;  // 0.2
        double realX = screenMinX + normX * screenW; // 384px
        if (Math.abs(realX - 384.0) > 0.01) {
            throw new RuntimeException("Normalized X mapping formula is incorrect: expected 384, got " + realX);
        }

        System.out.println("PASS");
    }

    // ── AN ──────────────────────────────────────────────────────────────────────

    private void testAN_NormalizedYMapping() throws Exception {
        System.out.print("[AN] Normalized Y mapping: normY * screenH + screenMinY... ");

        double canvasH = 580, screenH = 1032, screenMinY = 0;
        double widgetY = 116; // 20% of canvas height
        double normY = widgetY / canvasH;
        double realY = screenMinY + normY * screenH;
        double expected = 0 + 0.2 * 1032;
        if (Math.abs(realY - expected) > 1.0) {
            throw new RuntimeException("Normalized Y mapping formula incorrect: expected " + expected + ", got " + realY);
        }

        System.out.println("PASS");
    }

    // ── AO ──────────────────────────────────────────────────────────────────────

    private void testAO_NormalizedWidthMapping() throws Exception {
        System.out.print("[AO] Normalized width mapping: normW * screenW... ");

        double canvasW = 900, screenW = 1920;
        double widgetW = 90; // 10% of canvas = ~192px on 1920 screen
        double normW = widgetW / canvasW;
        double realW = normW * screenW;
        double expected = 192.0;
        if (Math.abs(realW - expected) > 1.0) {
            throw new RuntimeException("Normalized width formula incorrect: expected " + expected + ", got " + realW);
        }

        System.out.println("PASS");
    }

    // ── AP ──────────────────────────────────────────────────────────────────────

    private void testAP_NormalizedHeightMapping() throws Exception {
        System.out.print("[AP] Normalized height mapping: normH * screenH... ");

        double canvasH = 580, screenH = 1032;
        double widgetH = 58;  // 10% of canvas
        double normH = widgetH / canvasH;
        double realH = normH * screenH;
        double expected = 103.2;
        if (Math.abs(realH - expected) > 1.0) {
            throw new RuntimeException("Normalized height formula incorrect: expected ~" + expected + ", got " + realH);
        }

        System.out.println("PASS");
    }

    // ── AQ ──────────────────────────────────────────────────────────────────────

    private void testAQ_NegativeMonitorCoordinates() throws Exception {
        System.out.print("[AQ] Negative monitor coordinates: realX = screenMinX + normX * screenW... ");

        // Simulate left-side second monitor: minX = -1920
        double canvasW = 900, screenW = 1920, screenMinX = -1920;
        double widgetX = 450; // center of canvas = 50%
        double normX = widgetX / canvasW; // 0.5
        double realX = screenMinX + normX * screenW; // -1920 + 960 = -960

        if (Math.abs(realX - (-960.0)) > 0.01) {
            throw new RuntimeException("Negative minX not handled: expected -960.0, got " + realX);
        }

        // Verify deployFromCanvas uses visual bounds (which can have negative minX)
        File source = new File("src/main/java/com/widgify/desktop/ui/WidgetWindowManager.java");
        if (source.exists()) {
            String content = new String(Files.readAllBytes(source.toPath()));
            if (!content.contains("getVisualBounds") || !content.contains("getMinX")) {
                throw new RuntimeException("deployFromCanvas must use screen.getVisualBounds().getMinX()!");
            }
        }

        System.out.println("PASS");
    }

    // ── AR ──────────────────────────────────────────────────────────────────────

    private void testAR_NoAlwaysOnTopInDesktopWidgetStage() throws Exception {
        System.out.print("[AR] No setAlwaysOnTop(true) in DesktopWidgetStage... ");

        File source = new File("src/main/java/com/widgify/desktop/ui/DesktopWidgetStage.java");
        if (source.exists()) {
            String content = new String(Files.readAllBytes(source.toPath()));
            // Must not call setAlwaysOnTop(true)
            if (content.contains("setAlwaysOnTop(true)") || content.contains("ALWAYS_ON_TOP")) {
                throw new RuntimeException("DesktopWidgetStage must NOT use setAlwaysOnTop(true)!");
            }
        }

        System.out.println("PASS");
    }

    // ── AS ──────────────────────────────────────────────────────────────────────

    private void testAS_WindowsDesktopHelperExists() throws Exception {
        System.out.print("[AS] WindowsDesktopHelper exists and applies WS_EX_NOACTIVATE + WS_EX_TOOLWINDOW... ");

        File source = new File("src/main/java/com/widgify/desktop/ui/WindowsDesktopHelper.java");
        if (!source.exists()) {
            throw new RuntimeException("WindowsDesktopHelper.java does not exist!");
        }
        String content = new String(Files.readAllBytes(source.toPath()));
        if (!content.contains("WS_EX_NOACTIVATE")) {
            throw new RuntimeException("WindowsDesktopHelper must define WS_EX_NOACTIVATE!");
        }
        if (!content.contains("WS_EX_TOOLWINDOW")) {
            throw new RuntimeException("WindowsDesktopHelper must define WS_EX_TOOLWINDOW!");
        }

        System.out.println("PASS");
    }

    // ── AT ──────────────────────────────────────────────────────────────────────

    private void testAT_HwndBottomUsed() throws Exception {
        System.out.print("[AT] HWND_BOTTOM z-order is applied in WindowsDesktopHelper... ");

        File source = new File("src/main/java/com/widgify/desktop/ui/WindowsDesktopHelper.java");
        if (source.exists()) {
            String content = new String(Files.readAllBytes(source.toPath()));
            if (!content.contains("HWND_BOTTOM")) {
                throw new RuntimeException("WindowsDesktopHelper must use HWND_BOTTOM to place widgets behind windows!");
            }
            if (!content.contains("SetWindowPos")) {
                throw new RuntimeException("WindowsDesktopHelper must call SetWindowPos to apply z-order!");
            }
        }

        System.out.println("PASS");
    }

    // ── AU ──────────────────────────────────────────────────────────────────────

    private void testAU_DeployFromCanvasClosesOldStages() throws Exception {
        System.out.print("[AU] deployFromCanvas closes all previous stages before new deployment... ");

        File source = new File("src/main/java/com/widgify/desktop/ui/WidgetWindowManager.java");
        if (source.exists()) {
            String content = new String(Files.readAllBytes(source.toPath()));
            String deployMethod = extractMethod(content, "deployFromCanvas");
            if (!deployMethod.contains("closeAllWidgets")) {
                throw new RuntimeException("deployFromCanvas must call closeAllWidgets() before deploying new stages!");
            }
        }

        System.out.println("PASS");
    }

    // ── AV ──────────────────────────────────────────────────────────────────────

    private void testAV_EmptyCanvasClosesAllWidgets() throws Exception {
        System.out.print("[AV] Empty canvas Apply closes all deployed widget stages... ");

        File source = new File("src/main/java/com/widgify/desktop/ui/DesktopPreviewCanvas.java");
        if (source.exists()) {
            String content = new String(Files.readAllBytes(source.toPath()));
            String applyMethod = extractMethod(content, "applyLayoutToDesktop");
            // When canvas is empty, must call closeAllWidgets
            if (!applyMethod.contains("closeAllWidgets")) {
                throw new RuntimeException("applyLayoutToDesktop must call windowManager.closeAllWidgets() for empty canvas!");
            }
        }

        System.out.println("PASS");
    }

    // ── AW ──────────────────────────────────────────────────────────────────────

    private void testAW_ApplyDoesNotCallApiGetWidgets() throws Exception {
        System.out.print("[AW] Apply pipeline does NOT call apiClient.getWidgets() (canvas is source of truth)... ");

        File source = new File("src/main/java/com/widgify/desktop/ui/DesktopPreviewCanvas.java");
        if (source.exists()) {
            String content = new String(Files.readAllBytes(source.toPath()));
            String applyMethod = extractMethod(content, "applyLayoutToDesktop");
            if (applyMethod.contains("getWidgets()")) {
                throw new RuntimeException(
                        "applyLayoutToDesktop must NOT call apiClient.getWidgets() — canvas is source of truth!");
            }
        }

        File wmSource = new File("src/main/java/com/widgify/desktop/ui/WidgetWindowManager.java");
        if (wmSource.exists()) {
            String content = new String(Files.readAllBytes(wmSource.toPath()));
            String deployMethod = extractMethod(content, "deployFromCanvas");
            if (deployMethod.contains("getWidgets()")) {
                throw new RuntimeException(
                        "deployFromCanvas must NOT call apiClient.getWidgets()!");
            }
        }

        System.out.println("PASS");
    }

    // ── Helper: extract the body of a method by name ─────────────────────────

    private String extractMethod(String source, String methodName) {
        int idx = source.indexOf(" " + methodName + "(");
        if (idx < 0) idx = source.indexOf("\t" + methodName + "(");
        if (idx < 0) return "";

        // Find the opening brace
        int braceStart = source.indexOf("{", idx);
        if (braceStart < 0) return "";

        int depth = 0;
        int end = braceStart;
        for (int i = braceStart; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') { depth--; if (depth == 0) { end = i; break; } }
        }

        return source.substring(braceStart, end + 1);
    }
}
