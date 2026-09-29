package cult.konata.nanako.worker;

import cult.konata.nanako.commands.contest.ArtContest;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

/**
 * Refreshes the live staff art leaderboard in every guild every 5 minutes.
 * Guilds without a fully configured art contest are skipped silently.
 */
public class ArtRefreshTask implements Runnable {
    private final JDA jda;

    public ArtRefreshTask(JDA jda) {
        this.jda = jda;
    }

    @Override
    public void run() {
        if (jda == null) return;
        try {
            for (Guild guild : jda.getGuilds()) {
                try {
                    ArtContest.refreshStaffLeaderboard(jda, guild.getIdLong(), ArtContest.DEFAULT_TOP_COUNT, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
