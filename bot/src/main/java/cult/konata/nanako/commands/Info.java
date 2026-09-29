package cult.konata.nanako.commands;

import cult.konata.nanako.Main;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class Info extends ListenerAdapter {

    private static final String BUILD_TIME = loadBuildTime();

    private static String loadBuildTime() {
        try (InputStream in = Info.class.getClassLoader().getResourceAsStream("build.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                String value = props.getProperty("build.time", "").trim();
                if (!value.isEmpty() && !value.contains("${")) {
                    return value;
                }
            }
        } catch (Exception ignored) {
        }
        return "unknown (dev build)";
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        Message message = event.getMessage();
        String content = message.getContentRaw();
        if (!content.equals("n^info")) return;

        Duration uptime = Duration.ofMillis(System.currentTimeMillis() - Main.getStartTimeMillis());

        event.getChannel().sendMessage("🤖 **Nanako Info**\n"
                + "**Built:** " + formatBuildTime() + "\n"
                + "**Uptime:** " + formatUptime(uptime)).queue();
    }

    private static String formatBuildTime() {
        try {
            Instant instant = Instant.parse(BUILD_TIME);
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC).format(instant);
        } catch (Exception ignored) {
            return BUILD_TIME;
        }
    }

    private static String formatUptime(Duration uptime) {
        long days = uptime.toDays();
        long hours = uptime.toHoursPart();
        long minutes = uptime.toMinutesPart();
        long seconds = uptime.toSecondsPart();

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (days > 0 || hours > 0) sb.append(hours).append("h ");
        if (days > 0 || hours > 0 || minutes > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}
