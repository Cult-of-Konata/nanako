package cult.konata.nanako.leveling;

import java.util.List;

public interface XPRepository {
    XPUser getUser(long guildId, long userId);

    void addXp(long guildId, long userId, long amount);

    void setXp(long guildId, long userId, long xp);

    List<XPUser> getLeaderboard(long guildId, int limit);
}
