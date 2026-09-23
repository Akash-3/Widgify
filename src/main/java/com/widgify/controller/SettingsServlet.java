package com.widgify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.widgify.dao.UserSettingsDAO;
import com.widgify.model.User;
import com.widgify.model.UserSettings;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/settings")
public class SettingsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private UserSettingsDAO userSettingsDAO;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        userSettingsDAO = new UserSettingsDAO();
        objectMapper = new ObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        User user = getAuthenticatedUser(request, response);
        if (user == null) return;

        UserSettings settings = userSettingsDAO.getSettingsByUser(user.getId());
        Map<String, Object> jsonResponse = new HashMap<>();
        jsonResponse.put("success", true);
        jsonResponse.put("settings", settings);

        objectMapper.writeValue(response.getWriter(), jsonResponse);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        User user = getAuthenticatedUser(request, response);
        if (user == null) return;

        String theme = request.getParameter("theme");
        String layoutMode = request.getParameter("layoutMode");
        if (layoutMode == null) {
            layoutMode = request.getParameter("layout_mode");
        }
        String settingsJson = request.getParameter("settingsJson");
        if (settingsJson == null) {
            settingsJson = request.getParameter("settings_json");
        }

        UserSettings currentSettings = userSettingsDAO.getSettingsByUser(user.getId());

        if (theme != null && !theme.trim().isEmpty()) {
            currentSettings.setTheme(theme.trim());
        }
        if (layoutMode != null && !layoutMode.trim().isEmpty()) {
            currentSettings.setLayoutMode(layoutMode.trim());
        }
        if (settingsJson != null) {
            currentSettings.setSettingsJson(settingsJson);
        }

        boolean updated = userSettingsDAO.updateSettings(currentSettings);
        Map<String, Object> jsonResponse = new HashMap<>();
        jsonResponse.put("success", updated);
        jsonResponse.put("message", updated ? "Settings updated successfully." : "Failed to update settings.");
        jsonResponse.put("settings", currentSettings);

        objectMapper.writeValue(response.getWriter(), jsonResponse);
    }

    private User getAuthenticatedUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Unauthorized access. Please log in.");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            objectMapper.writeValue(response.getWriter(), err);
            return null;
        }
        return user;
    }
}
