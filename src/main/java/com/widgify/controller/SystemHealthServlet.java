package com.widgify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.widgify.model.SystemHealth;
import com.widgify.model.User;
import com.widgify.service.SystemMonitorService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/system-health")
public class SystemHealthServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private SystemMonitorService systemMonitorService;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        systemMonitorService = new SystemMonitorService();
        objectMapper = new ObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Unauthorized access. Please log in.");
            objectMapper.writeValue(response.getWriter(), err);
            return;
        }

        SystemHealth health = systemMonitorService.getSystemHealth();

        Map<String, Object> jsonResponse = new HashMap<>();
        jsonResponse.put("success", true);
        jsonResponse.put("isServerMetrics", true);
        jsonResponse.put("serverLabel", "Server Health (Tomcat)");
        jsonResponse.put("cpuUsage", health.getCpuUsage());
        jsonResponse.put("memoryUsage", health.getMemoryUsage());
        jsonResponse.put("memoryUsed", health.getMemoryUsed());
        jsonResponse.put("memoryTotal", health.getMemoryTotal());
        jsonResponse.put("batteryLevel", health.getBatteryLevel());
        jsonResponse.put("batteryAvailable", health.isBatteryAvailable());
        jsonResponse.put("batteryStatus", health.getBatteryStatus());

        objectMapper.writeValue(response.getWriter(), jsonResponse);
    }
}
