package com.widgify.controller;

import com.widgify.dao.UserDAO;
import com.widgify.model.User;
import com.widgify.util.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private UserDAO userDAO;

    @Override
    public void init() throws ServletException {
        userDAO = new UserDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendRedirect("register.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (name == null || name.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            request.setAttribute("error", "All fields are required.");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }

        email = email.trim().toLowerCase();

        if (userDAO.isEmailExists(email)) {
            request.setAttribute("error", "An account with this email address already exists.");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }

        String hashedPassword = PasswordUtil.hashPassword(password);
        User newUser = new User(name.trim(), email, hashedPassword);

        boolean success = userDAO.registerUser(newUser);

        if (success) {
            HttpSession session = request.getSession();
            session.setAttribute("successMessage", "Registration successful! Please sign in below.");
            response.sendRedirect("login.jsp");
        } else {
            request.setAttribute("error", "Registration failed due to a system error. Please try again.");
            request.getRequestDispatcher("register.jsp").forward(request, response);
        }
    }
}
