package cult.konata.nanako.worker;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class StatusWorker extends ListenerAdapter {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "StatusWorker-Thread");
        thread.setDaemon(true);
        return thread;
    });

    private static final AtomicBoolean started = new AtomicBoolean(false);

    public static void start(JDA jda) {
        if (started.compareAndSet(false, true)) {
            StatusTask task = new StatusTask(jda);
            scheduler.scheduleAtFixedRate(task, 0, 3, TimeUnit.MINUTES);
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        start(event.getJDA());
    }
}

