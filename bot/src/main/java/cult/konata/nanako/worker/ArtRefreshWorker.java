package cult.konata.nanako.worker;

import net.dv8tion.jda.api.JDA;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ArtRefreshWorker {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "ArtRefreshWorker-Thread");
        thread.setDaemon(true);
        return thread;
    });

    private static final AtomicBoolean started = new AtomicBoolean(false);

    public static void start(JDA jda) {
        if (started.compareAndSet(false, true)) {
            scheduler.scheduleAtFixedRate(new ArtRefreshTask(jda), 1, 5, TimeUnit.MINUTES);
        }
    }
}
