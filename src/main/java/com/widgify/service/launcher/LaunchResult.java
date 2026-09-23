package com.widgify.service.launcher;

public class LaunchResult {
    private final boolean success;
    private final String message;

    public LaunchResult(boolean success, String message) {
        this.success = success;
        this.message = message != null ? message : "";
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
