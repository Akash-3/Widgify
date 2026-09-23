package com.widgify.dao;

import com.widgify.model.Widget;
import com.widgify.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class WidgetDAO {

    public Widget createWidget(Widget widget) {
        String sql = "INSERT INTO widgets (user_id, widget_type, title, position_x, position_y, width, height, config, enabled) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, widget.getUserId());
            stmt.setString(2, widget.getWidgetType());
            stmt.setString(3, widget.getTitle());
            stmt.setInt(4, widget.getPositionX());
            stmt.setInt(5, widget.getPositionY());
            stmt.setInt(6, widget.getWidth());
            stmt.setInt(7, widget.getHeight());
            stmt.setString(8, widget.getConfig());
            stmt.setBoolean(9, widget.isEnabled());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        widget.setId(rs.getInt(1));
                        return widget;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Widget> getWidgetsByUser(int userId) {
        List<Widget> list = new ArrayList<>();
        String sql = "SELECT id, user_id, widget_type, title, position_x, position_y, width, height, config, enabled, created_at, updated_at FROM widgets WHERE user_id = ? ORDER BY id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToWidget(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (list.isEmpty()) {
            return initializeDefaultWidgets(userId);
        }

        return list;
    }

    public Widget getWidgetById(int id, int userId) {
        String sql = "SELECT id, user_id, widget_type, title, position_x, position_y, width, height, config, enabled, created_at, updated_at FROM widgets WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToWidget(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateWidget(Widget widget) {
        String sql = "UPDATE widgets SET title = ?, position_x = ?, position_y = ?, width = ?, height = ?, config = ?, enabled = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, widget.getTitle());
            stmt.setInt(2, widget.getPositionX());
            stmt.setInt(3, widget.getPositionY());
            stmt.setInt(4, widget.getWidth());
            stmt.setInt(5, widget.getHeight());
            stmt.setString(6, widget.getConfig());
            stmt.setBoolean(7, widget.isEnabled());
            stmt.setInt(8, widget.getId());
            stmt.setInt(9, widget.getUserId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateWidgetPosition(int id, int userId, int posX, int posY) {
        String sql = "UPDATE widgets SET position_x = ?, position_y = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, posX);
            stmt.setInt(2, posY);
            stmt.setInt(3, id);
            stmt.setInt(4, userId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateWidgetSize(int id, int userId, int width, int height) {
        String sql = "UPDATE widgets SET width = ?, height = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, width);
            stmt.setInt(2, height);
            stmt.setInt(3, id);
            stmt.setInt(4, userId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateWidgetEnabled(int id, int userId, boolean enabled) {
        String sql = "UPDATE widgets SET enabled = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBoolean(1, enabled);
            stmt.setInt(2, id);
            stmt.setInt(3, userId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteWidget(int id, int userId) {
        String sql = "DELETE FROM widgets WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            stmt.setInt(2, userId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Widget> initializeDefaultWidgets(int userId) {
        List<Widget> list = new ArrayList<>();
        String sql = "SELECT id, user_id, widget_type, title, position_x, position_y, width, height, config, enabled, created_at, updated_at FROM widgets WHERE user_id = ? ORDER BY id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToWidget(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (!list.isEmpty()) {
            return deduplicateCanonicalWidgets(userId, list);
        }

        return resetUserWidgetsToDefaults(userId);
    }

    public List<Widget> resetUserWidgetsToDefaults(int userId) {
        // Delete existing widgets for this specific user
        String deleteSql = "DELETE FROM widgets WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        List<Widget> defaults = new ArrayList<>();
        defaults.add(new Widget(userId, "clock", "Clock", 50, 50, 240, 150, "{\"format\":\"24h\",\"display\":0,\"normX\":0.05,\"normY\":0.06,\"normW\":0.20,\"normH\":0.20}", true));
        defaults.add(new Widget(userId, "timer", "Timer", 310, 50, 240, 150, "{\"preset\":25,\"display\":0,\"normX\":0.27,\"normY\":0.06,\"normW\":0.20,\"normH\":0.20}", true));
        defaults.add(new Widget(userId, "system", "PC Health", 570, 50, 260, 160, "{\"display\":0,\"normX\":0.49,\"normY\":0.06,\"normW\":0.22,\"normH\":0.21}", true));
        defaults.add(new Widget(userId, "weather", "Weather", 50, 220, 300, 180, "{\"location\":\"Berhampur, India\",\"display\":0,\"normX\":0.05,\"normY\":0.28,\"normW\":0.25,\"normH\":0.24}", true));
        defaults.add(new Widget(userId, "notes", "Notes", 370, 220, 240, 180, "{\"display\":0,\"normX\":0.32,\"normY\":0.28,\"normW\":0.20,\"normH\":0.24}", true));
        defaults.add(new Widget(userId, "tasks", "Tasks", 630, 220, 200, 180, "{\"display\":0,\"normX\":0.54,\"normY\":0.28,\"normW\":0.17,\"normH\":0.24}", true));
        defaults.add(new Widget(userId, "launcher", "Quick Launcher", 50, 420, 500, 130, "{\"display\":0,\"normX\":0.05,\"normY\":0.54,\"normW\":0.42,\"normH\":0.18}", true));

        List<Widget> savedList = new ArrayList<>();
        for (Widget w : defaults) {
            Widget created = createWidget(w);
            if (created != null) {
                savedList.add(created);
            }
        }
        return savedList;
    }

    private List<Widget> deduplicateCanonicalWidgets(int userId, List<Widget> existingList) {
        java.util.Set<String> seenTypes = new java.util.HashSet<>();
        List<Widget> uniqueList = new ArrayList<>();

        for (Widget w : existingList) {
            String type = w.getWidgetType() != null ? w.getWidgetType().trim().toLowerCase() : "";
            // Safely delete server_health or unknown/duplicate types
            if ("server_health".equalsIgnoreCase(type) || "server".equalsIgnoreCase(type)) {
                if (w.getId() > 0) {
                    deleteWidget(w.getId(), userId);
                }
                continue;
            }

            if (!type.isEmpty() && !seenTypes.contains(type)) {
                seenTypes.add(type);
                uniqueList.add(w);
            } else if (w.getId() > 0) {
                deleteWidget(w.getId(), userId);
            }
        }
        return uniqueList;
    }

    private Widget mapResultSetToWidget(ResultSet rs) throws SQLException {
        return new Widget(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getString("widget_type"),
            rs.getString("title"),
            rs.getInt("position_x"),
            rs.getInt("position_y"),
            rs.getInt("width"),
            rs.getInt("height"),
            rs.getString("config"),
            rs.getBoolean("enabled"),
            rs.getTimestamp("created_at"),
            rs.getTimestamp("updated_at")
        );
    }
}
