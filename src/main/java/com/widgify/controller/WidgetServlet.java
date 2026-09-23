package com.widgify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.widgify.dao.WidgetDAO;
import com.widgify.model.User;
import com.widgify.model.Widget;

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

@WebServlet("/widgets")
public class WidgetServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private WidgetDAO widgetDAO;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        widgetDAO = new WidgetDAO();
        objectMapper = new ObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        User user = getAuthenticatedUser(request, response);
        if (user == null) return;

        String action = request.getParameter("action");
        String idParam = request.getParameter("id");

        Map<String, Object> jsonResponse = new HashMap<>();

        if ("get".equalsIgnoreCase(action) && idParam != null) {
            try {
                int id = Integer.parseInt(idParam);
                Widget w = widgetDAO.getWidgetById(id, user.getId());
                if (w != null) {
                    jsonResponse.put("success", true);
                    jsonResponse.put("widget", w);
                } else {
                    jsonResponse.put("success", false);
                    jsonResponse.put("message", "Widget not found or access denied.");
                }
            } catch (NumberFormatException e) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Invalid widget ID.");
            }
        } else {
            List<Widget> widgets = widgetDAO.getWidgetsByUser(user.getId());
            jsonResponse.put("success", true);
            jsonResponse.put("widgets", widgets);
        }

        objectMapper.writeValue(response.getWriter(), jsonResponse);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        User user = getAuthenticatedUser(request, response);
        if (user == null) return;

        String action = request.getParameter("action");
        if (action == null) action = "create";

        Map<String, Object> jsonResponse = new HashMap<>();

        try {
            switch (action.toLowerCase()) {
                case "create": {
                    String widgetType = request.getParameter("widget_type");
                    if (widgetType == null) widgetType = request.getParameter("widgetType");
                    String title = request.getParameter("title");
                    String posXStr = request.getParameter("position_x");
                    if (posXStr == null) posXStr = request.getParameter("positionX");
                    String posYStr = request.getParameter("position_y");
                    if (posYStr == null) posYStr = request.getParameter("positionY");
                    String widthStr = request.getParameter("width");
                    String heightStr = request.getParameter("height");
                    String config = request.getParameter("config");

                    if (widgetType == null || widgetType.trim().isEmpty()) {
                        jsonResponse.put("success", false);
                        jsonResponse.put("message", "Widget type is required.");
                        break;
                    }

                    int posX = posXStr != null ? Integer.parseInt(posXStr) : 0;
                    int posY = posYStr != null ? Integer.parseInt(posYStr) : 0;
                    int width = widthStr != null ? Integer.parseInt(widthStr) : 2;
                    int height = heightStr != null ? Integer.parseInt(heightStr) : 2;
                    if (title == null || title.trim().isEmpty()) {
                        title = widgetType.substring(0, 1).toUpperCase() + widgetType.substring(1);
                    }
                    if (config == null) config = "{}";

                    Widget newWidget = new Widget(user.getId(), widgetType.trim(), title.trim(), posX, posY, width, height, config, true);
                    Widget created = widgetDAO.createWidget(newWidget);

                    if (created != null) {
                        jsonResponse.put("success", true);
                        jsonResponse.put("message", "Widget created successfully.");
                        jsonResponse.put("widget", created);
                    } else {
                        jsonResponse.put("success", false);
                        jsonResponse.put("message", "Failed to create widget.");
                    }
                    break;
                }

                case "position":
                case "move": {
                    int id = Integer.parseInt(request.getParameter("id"));
                    String posXStr = request.getParameter("position_x");
                    if (posXStr == null) posXStr = request.getParameter("positionX");
                    String posYStr = request.getParameter("position_y");
                    if (posYStr == null) posYStr = request.getParameter("positionY");

                    int posX = Integer.parseInt(posXStr);
                    int posY = Integer.parseInt(posYStr);

                    boolean moved = widgetDAO.updateWidgetPosition(id, user.getId(), posX, posY);
                    jsonResponse.put("success", moved);
                    jsonResponse.put("message", moved ? "Widget moved." : "Failed to move widget.");
                    break;
                }

                case "resize": {
                    int id = Integer.parseInt(request.getParameter("id"));
                    int width = Integer.parseInt(request.getParameter("width"));
                    int height = Integer.parseInt(request.getParameter("height"));

                    boolean resized = widgetDAO.updateWidgetSize(id, user.getId(), width, height);
                    jsonResponse.put("success", resized);
                    jsonResponse.put("message", resized ? "Widget resized." : "Failed to resize widget.");
                    break;
                }

                case "toggle": {
                    int id = Integer.parseInt(request.getParameter("id"));
                    boolean enabled = Boolean.parseBoolean(request.getParameter("enabled"));

                    boolean toggled = widgetDAO.updateWidgetEnabled(id, user.getId(), enabled);
                    jsonResponse.put("success", toggled);
                    jsonResponse.put("message", toggled ? "Widget status updated." : "Failed to update widget status.");
                    break;
                }

                case "config": {
                    int id = Integer.parseInt(request.getParameter("id"));
                    String config = request.getParameter("config");
                    Widget existing = widgetDAO.getWidgetById(id, user.getId());
                    if (existing != null) {
                        if (config != null) existing.setConfig(config);
                        boolean updated = widgetDAO.updateWidget(existing);
                        jsonResponse.put("success", updated);
                        jsonResponse.put("message", updated ? "Widget config updated." : "Failed to update widget config.");
                    } else {
                        jsonResponse.put("success", false);
                        jsonResponse.put("message", "Widget not found or access denied.");
                    }
                    break;
                }

                case "update": {
                    int id = Integer.parseInt(request.getParameter("id"));
                    Widget existing = widgetDAO.getWidgetById(id, user.getId());
                    if (existing != null) {
                        String title = request.getParameter("title");
                        String config = request.getParameter("config");
                        if (title != null) existing.setTitle(title.trim());
                        if (config != null) existing.setConfig(config);

                        boolean updated = widgetDAO.updateWidget(existing);
                        jsonResponse.put("success", updated);
                        jsonResponse.put("message", updated ? "Widget updated." : "Failed to update widget.");
                    } else {
                        jsonResponse.put("success", false);
                        jsonResponse.put("message", "Widget not found or access denied.");
                    }
                    break;
                }

                case "reset": {
                    List<Widget> resetWidgets = widgetDAO.resetUserWidgetsToDefaults(user.getId());
                    jsonResponse.put("success", true);
                    jsonResponse.put("message", "Reset to canonical default widgets.");
                    jsonResponse.put("widgets", resetWidgets);
                    break;
                }

                case "delete": {
                    int id = Integer.parseInt(request.getParameter("id"));
                    boolean deleted = widgetDAO.deleteWidget(id, user.getId());
                    jsonResponse.put("success", deleted);
                    jsonResponse.put("message", deleted ? "Widget deleted." : "Failed to delete widget.");
                    break;
                }

                default:
                    jsonResponse.put("success", false);
                    jsonResponse.put("message", "Invalid action.");
                    break;
            }
        } catch (Exception e) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Invalid request parameters.");
        }

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
