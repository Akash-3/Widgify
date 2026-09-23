package com.widgify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.widgify.dao.NoteDAO;
import com.widgify.model.Note;
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

@WebServlet("/notes")
public class NoteServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private NoteDAO noteDAO;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        noteDAO = new NoteDAO();
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
                Note note = noteDAO.getNoteById(id, user.getId());
                if (note != null) {
                    jsonResponse.put("success", true);
                    jsonResponse.put("note", note);
                } else {
                    jsonResponse.put("success", false);
                    jsonResponse.put("message", "Note not found or access denied.");
                }
            } catch (NumberFormatException e) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Invalid note ID.");
            }
        } else {
            List<Note> notes = noteDAO.getNotesByUser(user.getId());
            jsonResponse.put("success", true);
            jsonResponse.put("notes", notes);
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
                    String content = request.getParameter("content");

                    if (title == null || title.trim().isEmpty()) {
                        title = "Untitled Note";
                    }
                    if (content == null) content = "";

                    Note newNote = new Note(user.getId(), title.trim(), content);
                    Note created = noteDAO.createNote(newNote);

                    if (created != null) {
                        jsonResponse.put("success", true);
                        jsonResponse.put("message", "Note created successfully.");
                        jsonResponse.put("note", created);
                    } else {
                        jsonResponse.put("success", false);
                        jsonResponse.put("message", "Failed to create note.");
                    }
                    break;
                }

                case "update": {
                    int id = Integer.parseInt(request.getParameter("id"));
                    String title = request.getParameter("title");
                    String content = request.getParameter("content");

                    Note existing = noteDAO.getNoteById(id, user.getId());
                    if (existing != null) {
                        if (title != null) existing.setTitle(title.trim());
                        if (content != null) existing.setContent(content);

                        boolean updated = noteDAO.updateNote(existing);
                        jsonResponse.put("success", updated);
                        jsonResponse.put("message", updated ? "Note updated." : "Failed to update note.");
                    } else {
                        jsonResponse.put("success", false);
                        jsonResponse.put("message", "Note not found or access denied.");
                    }
                    break;
                }

                case "delete": {
                    int id = Integer.parseInt(request.getParameter("id"));
                    boolean deleted = noteDAO.deleteNote(id, user.getId());
                    jsonResponse.put("success", deleted);
                    jsonResponse.put("message", deleted ? "Note deleted." : "Failed to delete note.");
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
