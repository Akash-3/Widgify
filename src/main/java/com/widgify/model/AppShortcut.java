package com.widgify.model;

import java.io.Serializable;

public class AppShortcut implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String icon;
    private String url;
    private String description;

    public AppShortcut() {
    }

    public AppShortcut(String id, String name, String icon) {
        this(id, name, icon, "", "");
    }

    public AppShortcut(String id, String name, String icon, String url, String description) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.url = url;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
