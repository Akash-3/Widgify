<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.widgify.model.User" %>
<%
    User loggedInUser = (User) session.getAttribute("user");
    if (loggedInUser != null) {
        response.sendRedirect("dashboard.jsp");
        return;
    }
    String successMsg = (String) session.getAttribute("successMessage");
    if (successMsg != null) {
        session.removeAttribute("successMessage");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Widgify - Login</title>
    <link rel="stylesheet" href="css/style.css">
    <link rel="stylesheet" href="css/auth.css">
</head>
<body>
    <div class="auth-container">
        <div class="auth-card">
            <h1 class="logo">W I D G I F Y</h1>
            <p class="subtitle">Personal Workspace Suite</p>
            <h2>Sign In</h2>

            <% if (request.getAttribute("error") != null) { %>
                <div class="alert alert-error">
                    <%= request.getAttribute("error") %>
                </div>
            <% } %>

            <% if (successMsg != null) { %>
                <div class="alert alert-success">
                    <%= successMsg %>
                </div>
            <% } %>

            <% if ("true".equals(request.getParameter("logout"))) { %>
                <div class="alert alert-info">
                    You have been successfully logged out.
                </div>
            <% } %>

            <form action="login" method="post">
                <div class="form-group">
                    <label for="email">Email Address</label>
                    <input type="email" id="email" name="email" required placeholder="name@example.com" value="<%= request.getParameter("email") != null ? request.getParameter("email") : "" %>">
                </div>
                <div class="form-group">
                    <label for="password">Password</label>
                    <input type="password" id="password" name="password" required placeholder="Enter password">
                </div>
                <button type="submit" class="btn btn-primary">Login</button>
            </form>
            <p class="auth-footer">
                Don't have an account? <a href="register.jsp">Register here</a>
            </p>
        </div>
    </div>
</body>
</html>
