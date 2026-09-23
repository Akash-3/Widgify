package com.widgify.dao;

import com.widgify.model.UserSettings;
import com.widgify.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UserSettingsDAO {

    public UserSettings getSettingsByUser(int userId) {
        String sql = "SELECT id, user_id, theme, layout_mode, settings_json, created_at, updated_at FROM user_settings WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUserSettings(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Initialize default settings if none exist
        UserSettings defaultSettings = new UserSettings(userId, "dark", "grid", "{}");
        return createSettings(defaultSettings);
    }

    public UserSettings createSettings(UserSettings settings) {
        String sql = "INSERT INTO user_settings (user_id, theme, layout_mode, settings_json) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, settings.getUserId());
            stmt.setString(2, settings.getTheme());
            stmt.setString(3, settings.getLayoutMode());
            stmt.setString(4, settings.getSettingsJson());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        settings.setId(rs.getInt(1));
                        return settings;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateSettings(UserSettings settings) {
        String sql = "UPDATE user_settings SET theme = ?, layout_mode = ?, settings_json = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, settings.getTheme());
            stmt.setString(2, settings.getLayoutMode());
            stmt.setString(3, settings.getSettingsJson());
            stmt.setInt(4, settings.getUserId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private UserSettings mapResultSetToUserSettings(ResultSet rs) throws SQLException {
        return new UserSettings(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getString("theme"),
            rs.getString("layout_mode"),
            rs.getString("settings_json"),
            rs.getTimestamp("created_at"),
            rs.getTimestamp("updated_at")
        );
    }
}
