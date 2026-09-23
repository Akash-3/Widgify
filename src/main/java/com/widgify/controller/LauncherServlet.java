package com.widgify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.widgify.model.AppShortcut;
import com.widgify.model.User;
import com.widgify.service.AppLauncherService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/launcher")
public class LauncherServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private AppLauncherService appLauncherService;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        appLauncherService = new AppLauncherService();
        objectMapper = new ObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        User user = getAuthenticatedUser(request, response);
        if (user == null) return;

        List<AppShortcut> shortcuts = appLauncherService.getSupportedShortcuts();
        Map<String, Object> jsonResponse = new HashMap<>();
        jsonResponse.put("success", true);
        jsonResponse.put("shortcuts", shortcuts);

        objectMapper.writeValue(response.getWriter(), jsonResponse);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        User user = getAuthenticatedUser(request, response);
        if (user == null) return;

        String appId = request.getParameter("appId");
        if (appId == null || appId.trim().isEmpty()) {
            appId = request.getParameter("id");
        }

        Map<String, Object> jsonResponse = new HashMap<>();

        if (appId == null || appId.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Application ID is required.");
            objectMapper.writeValue(response.getWriter(), jsonResponse);
            return;
        }

        AppShortcut shortcut = appLauncherService.getShortcutById(appId.trim());

        if (shortcut != null && shortcut.getUrl() != null) {
            jsonResponse.put("success", true);
            jsonResponse.put("url", shortcut.getUrl());
            jsonResponse.put("name", shortcut.getName());
            jsonResponse.put("message", "Opening " + shortcut.getName() + "...");
        } else {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Shortcut not found.");
        }

        objectMapper.writeValue(response.getWriter(), jsonResponse);
    }

    private User getAuthenticatedUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Unauthorized access. Please log in.");
            objectMapper.writeValue(response.getWriter(), err);
            return null;
        }
        return user;
    }
}
