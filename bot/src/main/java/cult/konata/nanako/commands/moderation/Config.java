package cult.konata.nanako.commands.moderation;

import cult.konata.nanako.config.ConfigManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

public class Config extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromGuild()) return;

        Message message = event.getMessage();
        String content = message.getContentRaw();

        if (content.startsWith("n^config")) {
            // Check for ADMINISTRATOR permission to use config
            if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                event.getChannel().sendMessage("You need `ADMINISTRATOR` permission to use this command.").queue();
                return;
            }

            String[] args = content.split("\\s+");
            if (args.length < 2) {
                event.getChannel().sendMessage("Use: `n^config adminrole [add/remove/list] [roleId]`").queue();
                return;
            }

            String subCommand = args[1].toLowerCase();

            if (subCommand.equals("adminrole")) {
                if (args.length < 3) {
                    event.getChannel().sendMessage("Use: `n^config adminrole [add/remove/list] [roleId]`").queue();
                    return;
                }

                String action = args[2].toLowerCase();
                long guildId = event.getGuild().getIdLong();

                if (action.equals("add")) {
                    if (args.length < 4) {
                        event.getChannel().sendMessage("Please provide a role ID.").queue();
                        return;
                    }
                    String roleId = args[3];
                    ConfigManager.addAdminRole(guildId, roleId);
                    event.getChannel().sendMessage("Added admin role: " + roleId).queue();
                } else if (action.equals("remove")) {
                    if (args.length < 4) {
                        event.getChannel().sendMessage("Please provide a role ID.").queue();
                        return;
                    }
                    String roleId = args[3];
                    ConfigManager.removeAdminRole(guildId, roleId);
                    event.getChannel().sendMessage("Removed admin role: " + roleId).queue();
                } else if (action.equals("list")) {
                    List<String> roles = ConfigManager.getAdminRoles(guildId);
                    if (roles.isEmpty()) {
                        event.getChannel().sendMessage("No admin roles configured.").queue();
                    } else {
                        event.getChannel().sendMessage("Configured admin roles: " + String.join(", ", roles)).queue();
                    }
                } else {
                    event.getChannel().sendMessage("Unknown action. Use `add`, `remove`, or `list`.").queue();
                }
            } else {
                event.getChannel().sendMessage("Unknown subcommand. Use `adminrole`.").queue();
            }
        }
    }
}
