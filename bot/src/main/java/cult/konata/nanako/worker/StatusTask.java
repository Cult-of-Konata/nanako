package cult.konata.nanako.worker;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;

import java.util.Random;

public class StatusTask implements Runnable {
    private static final Activity[] STATUSES = {
            Activity.playing("authored by meowcat767"),
            Activity.watching("the vent channels"),
            Activity.playing("with Konata"),
            Activity.listening("n^config transRights true"),
            Activity.watching("IntelliJ IDEA"),
            Activity.playing("open source is <3"),
            Activity.listening("to your messages")
    };

    private final JDA jda;
    private final Activity[] activities;
    private final Random random;

    public StatusTask(JDA jda) {
        this(jda, STATUSES);
    }

    public StatusTask(JDA jda, Activity[] activities) {
        this.jda = jda;
        this.activities = activities != null && activities.length > 0 ? activities : STATUSES;
        this.random = new Random();
    }

    public static void start(JDA jda) {
        StatusWorker.start(jda);
    }

    @Override
    public void run() {
        if (jda == null || activities.length == 0) return;
        try {
            int index = random.nextInt(activities.length);
            Activity nextActivity = activities[index];
            jda.getPresence().setActivity(nextActivity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Activity[] getActivities() {
        return activities;
    }
}
