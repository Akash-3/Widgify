package com.widgify.desktop.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.widgify.desktop.model.AuthResult;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ServerApiClient {

    private String serverBaseUrl;
    private final HttpClient httpClient;
    private final CookieManager cookieManager;
    private final ObjectMapper objectMapper;
    private String currentAuthenticatedEmail;

    private Runnable onUnauthorizedCallback;

    public void setOnUnauthorizedCallback(Runnable callback) {
        this.onUnauthorizedCallback = callback;
    }

    public ServerApiClient() {
        this("http://localhost:8080/widgify");
    }

    public ServerApiClient(String serverBaseUrl) {
        this.serverBaseUrl = sanitizeUrl(serverBaseUrl);
        this.cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        this.httpClient = HttpClient.newBuilder()
                .cookieHandler(this.cookieManager)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String getServerBaseUrl() {
        return serverBaseUrl;
    }

    public void setServerBaseUrl(String serverBaseUrl) {
        this.serverBaseUrl = sanitizeUrl(serverBaseUrl);
    }

    public String getCurrentAuthenticatedEmail() {
        return currentAuthenticatedEmail;
    }

    private String sanitizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "http://localhost:8080/widgify";
        }
        String trimmed = url.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    /**
     * Asynchronously authenticates with the Tomcat backend using POST /login.
     * LoginServlet returns HTTP 302 Redirect to dashboard.jsp on success, or HTTP 200 on failure.
     */
    public CompletableFuture<AuthResult> login(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                    new AuthResult(false, "Please enter both email and password.", email)
            );
        }

        String trimmedEmail = email.trim().toLowerCase();

        String formData = "email=" + URLEncoder.encode(trimmedEmail, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverBaseUrl + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    int statusCode = response.statusCode();
                    String location = response.headers().firstValue("Location").orElse("");

                    if (statusCode == 302 || statusCode == 301 || location.contains("dashboard")) {
                        // Login accepted by Tomcat! Verify session via GET /widgets
                        this.currentAuthenticatedEmail = trimmedEmail;
                        return getWidgets().thenApply(widgetResult -> {
                            if (Boolean.TRUE.equals(widgetResult.get("success"))) {
                                return new AuthResult(true, "Authentication successful", trimmedEmail);
                            } else {
                                return new AuthResult(false, "Session verification failed after login.", trimmedEmail);
                            }
                        });
                    } else {
                        this.currentAuthenticatedEmail = null;
                        return CompletableFuture.completedFuture(
                                new AuthResult(false, "Invalid email address or password.", trimmedEmail)
                        );
                    }
                })
                .exceptionally(ex -> {
                    this.currentAuthenticatedEmail = null;
                    return new AuthResult(false, "Unable to connect to server: " + ex.getMessage(), trimmedEmail);
                });
    }

    /**
     * Asynchronously retrieves the authenticated user's widgets via GET /widgets.
     * Attach JSESSIONID stored in CookieManager. Returns Map containing success flag and widget list/count.
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<Map<String, Object>> getWidgets() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverBaseUrl + "/widgets"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    Map<String, Object> result = new HashMap<>();
                    int statusCode = response.statusCode();
                    result.put("statusCode", statusCode);

                    if (statusCode == 200) {
                        try {
                            Map<String, Object> jsonMap = objectMapper.readValue(response.body(), Map.class);
                            result.putAll(jsonMap);
                            result.put("success", jsonMap.getOrDefault("success", false));
                        } catch (Exception e) {
                            result.put("success", false);
                            result.put("message", "Error parsing widget response JSON.");
                        }
                    } else {
                        if (statusCode == 401 && onUnauthorizedCallback != null) {
                            onUnauthorizedCallback.run();
                        }
                        result.put("success", false);
                        result.put("message", "Unauthorized access. Server returned HTTP " + statusCode);
                    }
                    return result;
                })
                .exceptionally(ex -> {
                    Map<String, Object> errResult = new HashMap<>();
                    errResult.put("success", false);
                    errResult.put("statusCode", 0);
                    errResult.put("message", "Connection failure: " + ex.getMessage());
                    return errResult;
                });
    }

    /**
     * Asynchronously logs out via POST /logout and clears client-side JSESSIONID cookie store.
     */
    public CompletableFuture<Boolean> logout() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverBaseUrl + "/logout"))
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    cookieManager.getCookieStore().removeAll();
                    this.currentAuthenticatedEmail = null;
                    return true;
                })
                .exceptionally(ex -> {
                    cookieManager.getCookieStore().removeAll();
                    this.currentAuthenticatedEmail = null;
                    return true;
                });
    }

    /**
     * Asynchronously updates a widget's desktop position (position_x, position_y) via POST /widgets.
     */
    public CompletableFuture<Boolean> updateWidgetPosition(int widgetId, int posX, int posY) {
        String formData = "action=position"
                + "&id=" + widgetId
                + "&position_x=" + posX
                + "&position_y=" + posY;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverBaseUrl + "/widgets"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() == 200)
                .exceptionally(ex -> false);
    }

    /**
     * Asynchronously updates a widget's desktop size (width, height) via POST /widgets.
     */
    public CompletableFuture<Boolean> updateWidgetSize(int widgetId, int width, int height) {
        String formData = "action=resize"
                + "&id=" + widgetId
                + "&width=" + width
                + "&height=" + height;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverBaseUrl + "/widgets"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() == 200)
                .exceptionally(ex -> false);
    }

    /**
     * Asynchronously toggles a widget's enabled/disabled status via POST /widgets.
     */
    public CompletableFuture<Boolean> updateWidgetEnabled(int widgetId, boolean enabled) {
        String formData = "action=toggle"
                + "&id=" + widgetId
                + "&enabled=" + enabled;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverBaseUrl + "/widgets"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() == 200)
                .exceptionally(ex -> false);
    }

    /**
     * Asynchronously updates a widget's config JSON string via POST /widgets (action=config).
     */
    public CompletableFuture<Boolean> updateWidgetConfig(int widgetId, String configJson) {
        String formData = "action=update"
                + "&id=" + widgetId
                + "&config=" + URLEncoder.encode(configJson != null ? configJson : "{}", StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverBaseUrl + "/widgets"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() == 200)
                .exceptionally(ex -> false);
    }

    /**
     * Asynchronously resets widgets to canonical defaults via POST /widgets (action=reset).
     */
    public CompletableFuture<Map<String, Object>> resetWidgets() {
        return sendPostRequest("/widgets", "action=reset");
    }

    /**
     * Asynchronously creates a new widget record via POST /widgets (action=create).
     */
    public CompletableFuture<Map<String, Object>> createWidget(String widgetType, String title, int posX, int posY, int width, int height, String config) {
        String formData = "action=create"
                + "&widget_type=" + URLEncoder.encode(widgetType != null ? widgetType : "", StandardCharsets.UTF_8)
                + "&title=" + URLEncoder.encode(title != null ? title : "", StandardCharsets.UTF_8)
                + "&position_x=" + posX
                + "&position_y=" + posY
                + "&width=" + width
                + "&height=" + height
                + "&config=" + URLEncoder.encode(config != null ? config : "{}", StandardCharsets.UTF_8);

        return sendPostRequest("/widgets", formData);
    }

    /**
     * Helper method to manually clear client cookie store for testing unauthenticated state.
     */
    public void clearSessionCookies() {
        cookieManager.getCookieStore().removeAll();
        this.currentAuthenticatedEmail = null;
    }

    // --- NOTES API ENDPOINTS (/notes) ---

    @SuppressWarnings("unchecked")
    public CompletableFuture<Map<String, Object>> getNotes() {
        return sendGetRequest("/notes");
    }

    public CompletableFuture<Map<String, Object>> createNote(String title, String content) {
        String formData = "action=create"
                + "&title=" + URLEncoder.encode(title != null ? title : "", StandardCharsets.UTF_8)
                + "&content=" + URLEncoder.encode(content != null ? content : "", StandardCharsets.UTF_8);
        return sendPostRequest("/notes", formData);
    }

    public CompletableFuture<Map<String, Object>> updateNote(int id, String title, String content) {
        String formData = "action=update"
                + "&id=" + id
                + "&title=" + URLEncoder.encode(title != null ? title : "", StandardCharsets.UTF_8)
                + "&content=" + URLEncoder.encode(content != null ? content : "", StandardCharsets.UTF_8);
        return sendPostRequest("/notes", formData);
    }

    public CompletableFuture<Map<String, Object>> deleteNote(int id) {
        String formData = "action=delete&id=" + id;
        return sendPostRequest("/notes", formData);
    }

    // --- TASKS API ENDPOINTS (/tasks) ---

    @SuppressWarnings("unchecked")
    public CompletableFuture<Map<String, Object>> getTasks() {
        return sendGetRequest("/tasks");
    }

    public CompletableFuture<Map<String, Object>> createTask(String title) {
        String formData = "action=create"
                + "&title=" + URLEncoder.encode(title != null ? title : "", StandardCharsets.UTF_8);
        return sendPostRequest("/tasks", formData);
    }

    public CompletableFuture<Map<String, Object>> toggleTask(int id) {
        String formData = "action=toggle&id=" + id;
        return sendPostRequest("/tasks", formData);
    }

    public CompletableFuture<Map<String, Object>> updateTask(int id, String title, boolean completed) {
        String formData = "action=update"
                + "&id=" + id
                + "&title=" + URLEncoder.encode(title != null ? title : "", StandardCharsets.UTF_8)
                + "&completed=" + completed;
        return sendPostRequest("/tasks", formData);
    }

    public CompletableFuture<Map<String, Object>> deleteTask(int id) {
        String formData = "action=delete&id=" + id;
        return sendPostRequest("/tasks", formData);
    }

    // --- SERVER HEALTH API ENDPOINT (/system-health) ---

    public CompletableFuture<Map<String, Object>> getServerHealth() {
        return sendGetRequest("/system-health");
    }

    // --- WEATHER API ENDPOINT (/weather) ---

    public CompletableFuture<Map<String, Object>> getWeather(String location) {
        String endpoint = "/weather";
        if (location != null && !location.trim().isEmpty()) {
            endpoint += "?location=" + URLEncoder.encode(location.trim(), StandardCharsets.UTF_8);
        }
        return sendGetRequest(endpoint);
    }

    // --- HELPER HTTP METHODS ---

    @SuppressWarnings("unchecked")
    private CompletableFuture<Map<String, Object>> sendGetRequest(String path) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverBaseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    Map<String, Object> result = new HashMap<>();
                    int statusCode = response.statusCode();
                    result.put("statusCode", statusCode);

                    if (statusCode == 200) {
                        try {
                            Map<String, Object> jsonMap = objectMapper.readValue(response.body(), Map.class);
                            result.putAll(jsonMap);
                            result.put("success", jsonMap.getOrDefault("success", true));
                        } catch (Exception e) {
                            result.put("success", false);
                            result.put("message", "Error parsing JSON response.");
                        }
                    } else {
                        if (statusCode == 401 && onUnauthorizedCallback != null) {
                            onUnauthorizedCallback.run();
                        }
                        result.put("success", false);
                        result.put("message", "HTTP " + statusCode + " - Access denied or server error.");
                    }
                    return result;
                })
                .exceptionally(ex -> {
                    Map<String, Object> errResult = new HashMap<>();
                    errResult.put("success", false);
                    errResult.put("statusCode", 0);
                    errResult.put("message", "Connection failure: " + ex.getMessage());
                    return errResult;
                });
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<Map<String, Object>> sendPostRequest(String path, String formData) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverBaseUrl + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    Map<String, Object> result = new HashMap<>();
                    int statusCode = response.statusCode();
                    result.put("statusCode", statusCode);

                    if (statusCode == 200) {
                        try {
                            Map<String, Object> jsonMap = objectMapper.readValue(response.body(), Map.class);
                            result.putAll(jsonMap);
                            result.put("success", jsonMap.getOrDefault("success", true));
                        } catch (Exception e) {
                            result.put("success", false);
                            result.put("message", "Error parsing JSON response.");
                        }
                    } else {
                        if (statusCode == 401 && onUnauthorizedCallback != null) {
                            onUnauthorizedCallback.run();
                        }
                        result.put("success", false);
                        result.put("message", "HTTP " + statusCode + " - Request failed.");
                    }
                    return result;
                })
                .exceptionally(ex -> {
                    Map<String, Object> errResult = new HashMap<>();
                    errResult.put("success", false);
                    errResult.put("statusCode", 0);
                    errResult.put("message", "Connection failure: " + ex.getMessage());
                    return errResult;
                });
    }
}
