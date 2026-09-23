<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.widgify.model.User" %>
<%
    User loggedInUser = (User) session.getAttribute("user");
    if (loggedInUser != null) {
        response.sendRedirect("dashboard.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Widgify - Register</title>
    <link rel="stylesheet" href="css/style.css">
    <link rel="stylesheet" href="css/auth.css">
</head>
<body>
    <div class="auth-container">
        <div class="auth-card">
            <h1 class="logo">W I D G I F Y</h1>
            <p class="subtitle">Personal Workspace Suite</p>
            <h2>Create Account</h2>

            <% if (request.getAttribute("error") != null) { %>
                <div class="alert alert-error">
                    <%= request.getAttribute("error") %>
                </div>
            <% } %>

            <form action="register" method="post">
                <div class="form-group">
                    <label for="name">Full Name</label>
                    <input type="text" id="name" name="name" required placeholder="John Doe" value="<%= request.getParameter("name") != null ? request.getParameter("name") : "" %>">
                </div>
                <div class="form-group">
                    <label for="email">Email Address</label>
                    <input type="email" id="email" name="email" required placeholder="name@example.com" value="<%= request.getParameter("email") != null ? request.getParameter("email") : "" %>">
                </div>
                <div class="form-group">
                    <label for="password">Password</label>
                    <input type="password" id="password" name="password" required placeholder="Choose a password">
                </div>
                <button type="submit" class="btn btn-primary">Register Account</button>
            </form>
            <p class="auth-footer">
                Already have an account? <a href="login.jsp">Sign in here</a>
            </p>
        </div>
    </div>
</body>
</html>
