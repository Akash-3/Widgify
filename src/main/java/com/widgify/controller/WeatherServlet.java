package com.widgify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.widgify.model.User;
import com.widgify.service.WeatherService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/weather")
public class WeatherServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private WeatherService weatherService;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        weatherService = new WeatherService();
        objectMapper = new ObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        User user = getAuthenticatedUser(request, response);
        if (user == null) return;

        String location = request.getParameter("location");
        Map<String, Object> weatherData = weatherService.getWeatherForLocation(location);

        objectMapper.writeValue(response.getWriter(), weatherData);
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
