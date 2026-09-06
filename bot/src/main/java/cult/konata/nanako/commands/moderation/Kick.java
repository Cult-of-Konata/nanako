package cult.konata.nanako.commands.moderation;

import cult.konata.nanako.config.ConfigManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;
import java.util.stream.Collectors;

public class Kick extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromGuild()) return;

        Message message = event.getMessage();
        String content = message.getContentRaw();

        if (content.startsWith("n^k")) {
            // Permission check: Administrator or one of the configured admin roles
            boolean isAllowed = event.getMember().hasPermission(Permission.ADMINISTRATOR);
            if (!isAllowed) {
                List<String> userRoleIds = event.getMember().getRoles().stream()
                        .map(Role::getId)
                        .collect(Collectors.toList());
                isAllowed = ConfigManager.isAdmin(event.getGuild().getIdLong(), userRoleIds);
            }

            if (!isAllowed) {
                event.getChannel().sendMessage("You do not have permission to use this command.").queue();
                return;
            }

            // Split by spaces
            String[] args = content.split("\\s+");

            //args[0] is always the command itself
            if (args.length < 3) {
                event.getChannel().sendMessage("Missing args! Use: `n^k [userid] [reason]`").queue();
                return;
            }

            final String userIdFinal = args[1];
            
            // Join all arguments from index 2 onwards as the reason
            StringBuilder reasonBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                reasonBuilder.append(args[i]).append(" ");
            }
            String reasonTemp = reasonBuilder.toString().trim();
            if (reasonTemp.isEmpty()) {
                reasonTemp = "No reason provided";
            }
            final String reasonFinal = reasonTemp;

            if (userIdFinal.equals(event.getJDA().getSelfUser().getId())) {
                event.getChannel().sendMessage("I cannot kick myself!").queue();
                return;
            }

            event.getJDA().retrieveUserById(userIdFinal).queue(user ->
                user.openPrivateChannel().queue(channel ->
                    channel.sendMessage("You have been kicked from " + event.getGuild().getName() + " for: " + reasonFinal).queue(
                            unused1 -> executeKick(event, userIdFinal, reasonFinal),
                            unused2 -> executeKick(event, userIdFinal, reasonFinal)
                    ), ignored1 -> executeKick(event, userIdFinal, reasonFinal)
                ), ignored2 -> event.getChannel().sendMessage("Failed to kick: User ID " + userIdFinal + " does not exist.").queue()
            );
        }
    }

    private void executeKick(MessageReceivedEvent event, String userId, String reason) {
        event.getGuild().kick(UserSnowflake.fromId(userId))
                .reason(reason)
                .queue(
                        unused -> event.getChannel().sendMessage("Successfully kicked user " + userId).queue(),
                        error -> event.getChannel().sendMessage("Failed to kick user: " + error.getMessage()).queue()
                );
    }
}
