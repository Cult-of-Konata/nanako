package cult.konata.nanako.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SQLite-backed guild settings store.
 *
 * <p>Two tables: {@code guild_config} for single-value settings (one row per
 * guild/key) and {@code guild_admin_roles} for the multi-value admin role list.
 * Every operation opens its own connection and all methods are synchronized,
 * so this is safe to call from JDA event threads and scheduler threads.
 * The database file defaults to {@code nanako.db} in the working directory
 * and can be overridden with {@code -Ddb=<path>}.</p>
 */
public final class GuildConfigStore {
    public static final String KEY_REPORT_ROLE = "report_role";
    public static final String KEY_REPORT_CHANNEL = "report_channel";
    public static final String KEY_ART_NAME = "art_name";
    public static final String KEY_ART_SUBMISSION_CHANNEL = "art_submission_channel";
    public static final String KEY_ART_STAFF_CHANNEL = "art_staff_channel";
    public static final String KEY_ART_LEADERBOARD_MESSAGE = "art_leaderboard_message";

    private static final String DB_PATH = System.getProperty("db", "nanako.db");
    private static final String URL = "jdbc:sqlite:" + DB_PATH;
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    private GuildConfigStore() {
    }

    private static void init() {
        if (initialized.compareAndSet(false, true)) {
            try (Connection conn = connect();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS guild_config"
                        + " (guild_id INTEGER NOT NULL, key TEXT NOT NULL, value TEXT NOT NULL,"
                        + " PRIMARY KEY (guild_id, key))");
                stmt.execute("CREATE TABLE IF NOT EXISTS guild_admin_roles"
                        + " (guild_id INTEGER NOT NULL, role_id TEXT NOT NULL,"
                        + " PRIMARY KEY (guild_id, role_id))");
            } catch (SQLException e) {
                initialized.set(false);
                throw new IllegalStateException("Failed to initialize SQLite database at " + DB_PATH, e);
            }
        }
    }

    private static Connection connect() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA busy_timeout = 5000");
        }
        return conn;
    }

    public static synchronized String get(long guildId, String key) {
        init();
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT value FROM guild_config WHERE guild_id = ? AND key = ?")) {
            ps.setLong(1, guildId);
            ps.setString(2, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read guild config (" + key + ")", e);
        }
    }

    public static synchronized void set(long guildId, String key, String value) {
        init();
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO guild_config (guild_id, key, value) VALUES (?, ?, ?)"
                             + " ON CONFLICT (guild_id, key) DO UPDATE SET value = excluded.value")) {
            ps.setLong(1, guildId);
            ps.setString(2, key);
            ps.setString(3, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to write guild config (" + key + ")", e);
        }
    }

    public static synchronized void remove(long guildId, String key) {
        init();
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM guild_config WHERE guild_id = ? AND key = ?")) {
            ps.setLong(1, guildId);
            ps.setString(2, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete guild config (" + key + ")", e);
        }
    }

    public static synchronized List<String> getAdminRoles(long guildId) {
        init();
        List<String> roles = new ArrayList<>();
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT role_id FROM guild_admin_roles WHERE guild_id = ? ORDER BY role_id")) {
            ps.setLong(1, guildId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    roles.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read admin roles", e);
        }
        return roles;
    }

    public static synchronized void addAdminRole(long guildId, String roleId) {
        init();
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT OR IGNORE INTO guild_admin_roles (guild_id, role_id) VALUES (?, ?)")) {
            ps.setLong(1, guildId);
            ps.setString(2, roleId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to add admin role", e);
        }
    }

    public static synchronized void removeAdminRole(long guildId, String roleId) {
        init();
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM guild_admin_roles WHERE guild_id = ? AND role_id = ?")) {
            ps.setLong(1, guildId);
            ps.setString(2, roleId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to remove admin role", e);
        }
    }
}
