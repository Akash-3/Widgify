package com.widgify.desktop.net;

import com.widgify.desktop.model.AuthResult;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public class ServerApiClientTest {

    public static void main(String[] args) throws Exception {
        String baseUrl = "http://localhost:8080/widgify";
        ServerApiClient client = new ServerApiClient(baseUrl);

        System.out.println("==================================================");
        System.out.println("RUNNING PHASE 3 AUTHENTICATION & SESSION TESTS");
        System.out.println("==================================================");

        // 1. Create a dedicated verified test user via POST /register
        String testEmail = "testuser_" + UUID.randomUUID().toString().substring(0, 8) + "@widgify.test";
        String testPassword = "TestPassword123!";
        String testName = "Phase3 Test User";

        System.out.println("[SETUP] Registering test user: " + testEmail);
        boolean registered = registerTestUser(baseUrl, testName, testEmail, testPassword);
        if (!registered) {
            System.err.println("FAILED to register test user for testing!");
            System.exit(1);
        }
        System.out.println("[SETUP] Test user registered successfully.");

        // TEST A: Wrong credentials → rejected
        System.out.println("\n--- TEST A: Wrong Password ---");
        AuthResult resultWrong = client.login(testEmail, "WrongPassword999").get();
        System.out.println("Success: " + resultWrong.isSuccess() + ", Message: " + resultWrong.getMessage());
        assert !resultWrong.isSuccess() : "Test A failed: wrong password was accepted";

        // TEST B: Unknown account → rejected
        System.out.println("\n--- TEST B: Unknown Account ---");
        AuthResult resultUnknown = client.login("nonexistent_user_99999@widgify.test", "SomePass123").get();
        System.out.println("Success: " + resultUnknown.isSuccess() + ", Message: " + resultUnknown.getMessage());
        assert !resultUnknown.isSuccess() : "Test B failed: unknown account was accepted";

        // TEST E: /widgets without session → rejected (HTTP 401)
        System.out.println("\n--- TEST E: /widgets Without Session ---");
        client.clearSessionCookies();
        Map<String, Object> unauthWidgets = client.getWidgets().get();
        System.out.println("Success: " + unauthWidgets.get("success") + ", StatusCode: " + unauthWidgets.get("statusCode"));
        assert Boolean.FALSE.equals(unauthWidgets.get("success")) : "Test E failed: unauthenticated /widgets returned success";

        // TEST C: Correct credentials → authenticated
        System.out.println("\n--- TEST C: Correct Credentials ---");
        AuthResult resultCorrect = client.login(testEmail, testPassword).get();
        System.out.println("Success: " + resultCorrect.isSuccess() + ", Message: " + resultCorrect.getMessage());
        assert resultCorrect.isSuccess() : "Test C failed: correct credentials were rejected";

        // TEST D: /widgets immediately after login → authenticated response
        System.out.println("\n--- TEST D: /widgets Immediately After Login ---");
        Map<String, Object> authWidgets = client.getWidgets().get();
        System.out.println("Success: " + authWidgets.get("success") + ", StatusCode: " + authWidgets.get("statusCode") + ", Widgets: " + authWidgets.get("widgets"));
        assert Boolean.TRUE.equals(authWidgets.get("success")) : "Test D failed: authenticated /widgets failed";

        // TEST F & G: Logout → server session invalidated & /widgets after logout rejected
        System.out.println("\n--- TEST F & G: Logout & Post-Logout Access ---");
        boolean loggedOut = client.logout().get();
        System.out.println("Logged out: " + loggedOut);
        Map<String, Object> postLogoutWidgets = client.getWidgets().get();
        System.out.println("Post-logout GET /widgets -> Success: " + postLogoutWidgets.get("success") + ", StatusCode: " + postLogoutWidgets.get("statusCode"));
        assert Boolean.FALSE.equals(postLogoutWidgets.get("success")) : "Test G failed: /widgets succeeded after logout";

        // TEST H: Login again → new authenticated session works
        System.out.println("\n--- TEST H: Login Again ---");
        AuthResult resultReLogin = client.login(testEmail, testPassword).get();
        System.out.println("Re-login Success: " + resultReLogin.isSuccess());
        Map<String, Object> reLoginWidgets = client.getWidgets().get();
        System.out.println("Re-login GET /widgets -> Success: " + reLoginWidgets.get("success") + ", Widgets: " + reLoginWidgets.get("widgets"));
        assert Boolean.TRUE.equals(reLoginWidgets.get("success")) : "Test H failed: re-login GET /widgets failed";

        System.out.println("\n==================================================");
        System.out.println("ALL PHASE 3 AUTHENTICATION TESTS PASSED 100%!");
        System.out.println("==================================================");
    }

    private static boolean registerTestUser(String baseUrl, String name, String email, String password) throws IOException, InterruptedException {
        HttpClient tempClient = HttpClient.newHttpClient();
        String formData = "name=" + URLEncoder.encode(name, StandardCharsets.UTF_8)
                + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/register"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        HttpResponse<String> resp = tempClient.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.statusCode() == 200 || resp.statusCode() == 302;
    }
}
