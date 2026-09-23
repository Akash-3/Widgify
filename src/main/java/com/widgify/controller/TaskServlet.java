package com.widgify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.widgify.dao.TaskDAO;
import com.widgify.model.Task;
import com.widgify.model.User;

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

@WebServlet("/tasks")
public class TaskServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private TaskDAO taskDAO;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        taskDAO = new TaskDAO();
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
                Task task = taskDAO.getTaskById(id, user.getId());
                if (task != null) {
                    jsonResponse.put("success", true);
                    jsonResponse.put("task", task);
                } else {
                    jsonResponse.put("success", false);
                    jsonResponse.put("message", "Task not found or access denied.");
                }
            } catch (NumberFormatException e) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Invalid task ID.");
            }
        } else {
            List<Task> tasks = taskDAO.getTasksByUser(user.getId());
            jsonResponse.put("success", true);
            jsonResponse.put("tasks", tasks);
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
                    String title = request.getParameter("title");
                    if (title == null || title.trim().isEmpty()) {
                        title = "Untitled Task";
                    }

                    Task newTask = new Task(user.getId(), title.trim(), false);
                    Task created = taskDAO.createTask(newTask);

                    if (created != null) {
                        jsonResponse.put("success", true);
                        jsonResponse.put("message", "Task created successfully.");
                        jsonResponse.put("task", created);
                    } else {
                        jsonResponse.put("success", false);
                        jsonResponse.put("message", "Failed to create task.");
                    }
                    break;
                }

                case "toggle": {
                    int id = Integer.parseInt(request.getParameter("id"));
                    boolean toggled = taskDAO.toggleTaskCompletion(id, user.getId());
                    if (toggled) {
                        Task updated = taskDAO.getTaskById(id, user.getId());
                        jsonResponse.put("success", true);
                        jsonResponse.put("message", "Task completion toggled.");
                        jsonResponse.put("task", updated);
                    } else {
                        jsonResponse.put("success", false);
                        jsonResponse.put("message", "Task not found or access denied.");
                    }
                    break;
                }

                case "update": {
                    int id = Integer.parseInt(request.getParameter("id"));
                    String title = request.getParameter("title");
                    String completedParam = request.getParameter("completed");

                    Task existing = taskDAO.getTaskById(id, user.getId());
                    if (existing != null) {
                        if (title != null && !title.trim().isEmpty()) {
                            existing.setTitle(title.trim());
                        }
                        if (completedParam != null) {
                            existing.setCompleted(Boolean.parseBoolean(completedParam));
                        }

                        boolean updated = taskDAO.updateTask(existing);
                        jsonResponse.put("success", updated);
                        jsonResponse.put("message", updated ? "Task updated." : "Failed to update task.");
                        jsonResponse.put("task", existing);
                    } else {
                        jsonResponse.put("success", false);
                        jsonResponse.put("message", "Task not found or access denied.");
                    }
                    break;
                }

                case "delete": {
                    int id = Integer.parseInt(request.getParameter("id"));
                    boolean deleted = taskDAO.deleteTask(id, user.getId());
                    jsonResponse.put("success", deleted);
                    jsonResponse.put("message", deleted ? "Task deleted." : "Failed to delete task.");
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
