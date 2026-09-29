package cult.konata.nanako.config;

import java.util.List;

/**
 * Guild settings facade. All values are persisted in SQLite via
 * {@link GuildConfigStore}, so they survive bot restarts. The public API is
 * unchanged from the previous in-memory version.
 */
public class ConfigManager {
    public static void addAdminRole(long guildId, String roleId) {
        GuildConfigStore.addAdminRole(guildId, roleId);
    }

    public static void removeAdminRole(long guildId, String roleId) {
        GuildConfigStore.removeAdminRole(guildId, roleId);
    }

    public static List<String> getAdminRoles(long guildId) {
        return GuildConfigStore.getAdminRoles(guildId);
    }

    public static boolean isAdmin(long guildId, List<String> userRoleIds) {
        List<String> roles = GuildConfigStore.getAdminRoles(guildId);
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        for (String roleId : userRoleIds) {
            if (roles.contains(roleId)) {
                return true;
            }
        }
        return false;
    }

    public static void setReportRole(long guildId, String roleId) {
        GuildConfigStore.set(guildId, GuildConfigStore.KEY_REPORT_ROLE, roleId);
    }

    public static String getReportRole(long guildId) {
        return GuildConfigStore.get(guildId, GuildConfigStore.KEY_REPORT_ROLE);
    }

    public static void removeReportRole(long guildId) {
        GuildConfigStore.remove(guildId, GuildConfigStore.KEY_REPORT_ROLE);
    }

    public static void setReportChannel(long guildId, String channelId) {
        GuildConfigStore.set(guildId, GuildConfigStore.KEY_REPORT_CHANNEL, channelId);
    }

    public static String getReportChannel(long guildId) {
        return GuildConfigStore.get(guildId, GuildConfigStore.KEY_REPORT_CHANNEL);
    }

    public static void removeReportChannel(long guildId) {
        GuildConfigStore.remove(guildId, GuildConfigStore.KEY_REPORT_CHANNEL);
    }

    // --- Art contest settings ---

    public static void setArtCompetitionName(long guildId, String name) {
        GuildConfigStore.set(guildId, GuildConfigStore.KEY_ART_NAME, name);
    }

    public static String getArtCompetitionName(long guildId) {
        return GuildConfigStore.get(guildId, GuildConfigStore.KEY_ART_NAME);
    }

    public static void removeArtCompetitionName(long guildId) {
        GuildConfigStore.remove(guildId, GuildConfigStore.KEY_ART_NAME);
    }

    public static void setArtSubmissionChannel(long guildId, String channelId) {
        GuildConfigStore.set(guildId, GuildConfigStore.KEY_ART_SUBMISSION_CHANNEL, channelId);
    }

    public static String getArtSubmissionChannel(long guildId) {
        return GuildConfigStore.get(guildId, GuildConfigStore.KEY_ART_SUBMISSION_CHANNEL);
    }

    public static void removeArtSubmissionChannel(long guildId) {
        GuildConfigStore.remove(guildId, GuildConfigStore.KEY_ART_SUBMISSION_CHANNEL);
    }

    public static void setArtStaffChannel(long guildId, String channelId) {
        GuildConfigStore.set(guildId, GuildConfigStore.KEY_ART_STAFF_CHANNEL, channelId);
    }

    public static String getArtStaffChannel(long guildId) {
        return GuildConfigStore.get(guildId, GuildConfigStore.KEY_ART_STAFF_CHANNEL);
    }

    public static void removeArtStaffChannel(long guildId) {
        GuildConfigStore.remove(guildId, GuildConfigStore.KEY_ART_STAFF_CHANNEL);
    }

    // Message ID of the live staff leaderboard message (edited in place on refresh).

    public static void setArtLeaderboardMessage(long guildId, String messageId) {
        GuildConfigStore.set(guildId, GuildConfigStore.KEY_ART_LEADERBOARD_MESSAGE, messageId);
    }

    public static String getArtLeaderboardMessage(long guildId) {
        return GuildConfigStore.get(guildId, GuildConfigStore.KEY_ART_LEADERBOARD_MESSAGE);
    }

    public static void removeArtLeaderboardMessage(long guildId) {
        GuildConfigStore.remove(guildId, GuildConfigStore.KEY_ART_LEADERBOARD_MESSAGE);
    }
}
