package com.widgify.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class Widget implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private String widgetType;
    private String title;
    private int positionX;
    private int positionY;
    private int width;
    private int height;
    private String config;
    private boolean enabled;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Widget() {
        this.positionX = 0;
        this.positionY = 0;
        this.width = 2;
        this.height = 2;
        this.enabled = true;
    }

    public Widget(int userId, String widgetType, String title, int positionX, int positionY, int width, int height, String config, boolean enabled) {
        this.userId = userId;
        this.widgetType = widgetType;
        this.title = title;
        this.positionX = positionX;
        this.positionY = positionY;
        this.width = width;
        this.height = height;
        this.config = config;
        this.enabled = enabled;
    }

    public Widget(int id, int userId, String widgetType, String title, int positionX, int positionY, int width, int height, String config, boolean enabled, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.userId = userId;
        this.widgetType = widgetType;
        this.title = title;
        this.positionX = positionX;
        this.positionY = positionY;
        this.width = width;
        this.height = height;
        this.config = config;
        this.enabled = enabled;
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

    public String getWidgetType() {
        return widgetType;
    }

    public void setWidgetType(String widgetType) {
        this.widgetType = widgetType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPositionX() {
        return positionX;
    }

    public void setPositionX(int positionX) {
        this.positionX = positionX;
    }

    public int getPositionY() {
        return positionY;
    }

    public void setPositionY(int positionY) {
        this.positionY = positionY;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
