package com.widgify.desktop.model;

public class AuthResult {
    private final boolean success;
    private final String message;
    private final String email;

    public AuthResult(boolean success, String message, String email) {
        this.success = success;
        this.message = message;
        this.email = email;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return "AuthResult{success=" + success + ", message='" + message + "', email='" + email + "'}";
    }
}
