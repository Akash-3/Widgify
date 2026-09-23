package com.widgify.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class UserSettings implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private String theme;
    private String layoutMode;
    private String settingsJson;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public UserSettings() {
        this.theme = "dark";
        this.layoutMode = "grid";
    }

    public UserSettings(int userId, String theme, String layoutMode, String settingsJson) {
        this.userId = userId;
        this.theme = theme;
        this.layoutMode = layoutMode;
        this.settingsJson = settingsJson;
    }

    public UserSettings(int id, int userId, String theme, String layoutMode, String settingsJson, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.userId = userId;
        this.theme = theme;
        this.layoutMode = layoutMode;
        this.settingsJson = settingsJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getLayoutMode() {
        return layoutMode;
    }

    public void setLayoutMode(String layoutMode) {
        this.layoutMode = layoutMode;
    }

    public String getSettingsJson() {
        return settingsJson;
    }

    public void setSettingsJson(String settingsJson) {
        this.settingsJson = settingsJson;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
