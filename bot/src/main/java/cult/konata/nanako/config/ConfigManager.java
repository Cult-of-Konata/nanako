package cult.konata.nanako.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private static final Map<Long, List<String>> adminRoles = new HashMap<>();
    private static final Map<Long, String> reportRoles = new HashMap<>();
    private static final Map<Long, String> reportChannels = new HashMap<>();

    public static void addAdminRole(long guildId, String roleId) {
        adminRoles.computeIfAbsent(guildId, k -> new ArrayList<>()).add(roleId);
    }

    public static void removeAdminRole(long guildId, String roleId) {
        List<String> roles = adminRoles.get(guildId);
        if (roles != null) {
            roles.remove(roleId);
        }
    }

    public static List<String> getAdminRoles(long guildId) {
        return adminRoles.getOrDefault(guildId, new ArrayList<>());
    }

    public static boolean isAdmin(long guildId, List<String> userRoleIds) {
        List<String> roles = adminRoles.get(guildId);
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
        reportRoles.put(guildId, roleId);
    }

    public static String getReportRole(long guildId) {
        return reportRoles.get(guildId);
    }

    public static void removeReportRole(long guildId) {
        reportRoles.remove(guildId);
    }

    public static void setReportChannel(long guildId, String channelId) {
        reportChannels.put(guildId, channelId);
    }

    public static String getReportChannel(long guildId) {
        return reportChannels.get(guildId);
    }

    public static void removeReportChannel(long guildId) {
        reportChannels.remove(guildId);
    }
}
