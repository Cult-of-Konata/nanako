package cult.konata.nanako.commands.moderation;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.TimeUnit;

public class Ban extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        Message message = event.getMessage();
        String content = message.getContentRaw();

        if (content.startsWith("n^b")) {
            // Split by spaces
            String[] args = content.split("\\s+");

            //args[0] is always the command itself
            if (args.length < 3) {
                event.getChannel().sendMessage("Missing args! Use: `n^b [userid] [reason]`").queue();
                return;
            }

            String userId = args[1];
            String reason = args[2];

            if (userId.isEmpty()) {
                event.getChannel().sendMessage("Missing userId param!").queue();
                return;
            }
            
            if (reason.isEmpty()) {
                reason = "No reason provided";
            }
            
            // Now hit them with the banhammer!
            event.getGuild().ban(UserSnowflake.fromId(userId), 0, TimeUnit.SECONDS)
                    .reason(reason)
                    .queue(
                        success -> event.getChannel().sendMessage("Successfully banned user " + userId).queue(),
                        error -> event.getChannel().sendMessage("Failed to ban user: " + error.getMessage()).queue()
                    );
        }
    }
}
