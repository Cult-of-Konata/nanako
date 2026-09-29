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
                event.getChannel().sendMessage("Use: `n^config adminrole [add/remove/list] [roleId]`\n`n^config reportrole [set/remove/get] [roleId]`\n`n^config reportchannel [set/remove/get] [channelId]`\n`n^config artname [set <name>/remove/get]`\n`n^config artchannel [set/remove/get] [channelId]`\n`n^config artstaffchannel [set/remove/get] [channelId]`").queue();
                return;
            }

            String subCommand = args[1].toLowerCase();
            long guildId = event.getGuild().getIdLong();

            if (subCommand.equals("adminrole")) {
                if (args.length < 3) {
                    event.getChannel().sendMessage("Use: `n^config adminrole [add/remove/list] [roleId]`").queue();
                    return;
                }

                String action = args[2].toLowerCase();

                if (action.equals("add")) {
                    if (args.length < 4) {
                        event.getChannel().sendMessage("Please provide a role ID.").queue();
                        return;
                    }
                    String roleId = args[3].replaceAll("[^0-9]", "");
                    if (roleId.isEmpty()) {
                        event.getChannel().sendMessage("Invalid role ID.").queue();
                        return;
                    }
                    ConfigManager.addAdminRole(guildId, roleId);
                    event.getChannel().sendMessage("Added admin role: " + roleId).queue();
                } else if (action.equals("remove")) {
                    if (args.length < 4) {
                        event.getChannel().sendMessage("Please provide a role ID.").queue();
                        return;
                    }
                    String roleId = args[3].replaceAll("[^0-9]", "");
                    if (roleId.isEmpty()) {
                        event.getChannel().sendMessage("Invalid role ID.").queue();
                        return;
                    }
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
            } else if (subCommand.equals("reportrole")) {
                if (args.length < 3) {
                    event.getChannel().sendMessage("Use: `n^config reportrole [set/remove/get] [roleId]`").queue();
                    return;
                }

                String action = args[2].toLowerCase();

                if (action.equals("set")) {
                    if (args.length < 4) {
                        event.getChannel().sendMessage("Please provide a role ID.").queue();
                        return;
                    }
                    String roleId = args[3].replaceAll("[^0-9]", "");
                    if (roleId.isEmpty()) {
                        event.getChannel().sendMessage("Invalid role ID.").queue();
                        return;
                    }
                    ConfigManager.setReportRole(guildId, roleId);
                    event.getChannel().sendMessage("Set report role to: " + roleId).queue();
                } else if (action.equals("remove") || action.equals("clear")) {
                    ConfigManager.removeReportRole(guildId);
                    event.getChannel().sendMessage("Removed report role.").queue();
                } else if (action.equals("get") || action.equals("list")) {
                    String roleId = ConfigManager.getReportRole(guildId);
                    if (roleId == null || roleId.isEmpty()) {
                        event.getChannel().sendMessage("No report role configured.").queue();
                    } else {
                        event.getChannel().sendMessage("Configured report role: " + roleId).queue();
                    }
                } else {
                    String possibleRoleId = args[2].replaceAll("[^0-9]", "");
                    if (!possibleRoleId.isEmpty()) {
                        ConfigManager.setReportRole(guildId, possibleRoleId);
                        event.getChannel().sendMessage("Set report role to: " + possibleRoleId).queue();
                    } else {
                        event.getChannel().sendMessage("Unknown action. Use `set`, `remove`, or `get`.").queue();
                    }
                }
            } else if (subCommand.equals("reportchannel")) {
                if (args.length < 3) {
                    event.getChannel().sendMessage("Use: `n^config reportchannel [set/remove/get] [channelId]`").queue();
                    return;
                }

                String action = args[2].toLowerCase();

                if (action.equals("set")) {
                    if (args.length < 4) {
                        event.getChannel().sendMessage("Please provide a channel ID.").queue();
                        return;
                    }
                    String channelId = args[3].replaceAll("[^0-9]", "");
                    if (channelId.isEmpty()) {
                        event.getChannel().sendMessage("Invalid channel ID.").queue();
                        return;
                    }
                    ConfigManager.setReportChannel(guildId, channelId);
                    event.getChannel().sendMessage("Set report channel to: " + channelId).queue();
                } else if (action.equals("remove") || action.equals("clear")) {
                    ConfigManager.removeReportChannel(guildId);
                    event.getChannel().sendMessage("Removed report channel.").queue();
                } else if (action.equals("get") || action.equals("list")) {
                    String channelId = ConfigManager.getReportChannel(guildId);
                    if (channelId == null || channelId.isEmpty()) {
                        event.getChannel().sendMessage("No report channel configured.").queue();
                    } else {
                        event.getChannel().sendMessage("Configured report channel: " + channelId).queue();
                    }
                } else {
                    String possibleChannelId = args[2].replaceAll("[^0-9]", "");
                    if (!possibleChannelId.isEmpty()) {
                        ConfigManager.setReportChannel(guildId, possibleChannelId);
                        event.getChannel().sendMessage("Set report channel to: " + possibleChannelId).queue();
                    } else {
                        event.getChannel().sendMessage("Unknown action. Use `set`, `remove`, or `get`.").queue();
                    }
                }
            } else if (subCommand.equals("artname")) {
                if (args.length < 3) {
                    event.getChannel().sendMessage("Use: `n^config artname [set <competition name>/remove/get]`").queue();
                    return;
                }

                String action = args[2].toLowerCase();

                if (action.equals("set")) {
                    if (args.length < 4) {
                        event.getChannel().sendMessage("Please provide a competition name. Use: `n^config artname set <name>`").queue();
                        return;
                    }
                    StringBuilder nameBuilder = new StringBuilder();
                    for (int i = 3; i < args.length; i++) {
                        nameBuilder.append(args[i]).append(" ");
                    }
                    String name = nameBuilder.toString().trim();
                    if (name.isEmpty()) {
                        event.getChannel().sendMessage("Competition name cannot be empty.").queue();
                        return;
                    }
                    ConfigManager.setArtCompetitionName(guildId, name);
                    event.getChannel().sendMessage("Set art competition name to: " + name).queue();
                } else if (action.equals("remove") || action.equals("clear")) {
                    ConfigManager.removeArtCompetitionName(guildId);
                    event.getChannel().sendMessage("Removed art competition name.").queue();
                } else if (action.equals("get") || action.equals("list")) {
                    String name = ConfigManager.getArtCompetitionName(guildId);
                    if (name == null || name.isEmpty()) {
                        event.getChannel().sendMessage("No art competition name configured.").queue();
                    } else {
                        event.getChannel().sendMessage("Configured art competition: " + name).queue();
                    }
                } else {
                    event.getChannel().sendMessage("Unknown action. Use `set`, `remove`, or `get`.").queue();
                }
            } else if (subCommand.equals("artchannel")) {
                if (args.length < 3) {
                    event.getChannel().sendMessage("Use: `n^config artchannel [set/remove/get] [channelId]`").queue();
                    return;
                }

                String action = args[2].toLowerCase();

                if (action.equals("set")) {
                    if (args.length < 4) {
                        event.getChannel().sendMessage("Please provide a channel ID.").queue();
                        return;
                    }
                    String channelId = args[3].replaceAll("[^0-9]", "");
                    if (channelId.isEmpty()) {
                        event.getChannel().sendMessage("Invalid channel ID.").queue();
                        return;
                    }
                    ConfigManager.setArtSubmissionChannel(guildId, channelId);
                    event.getChannel().sendMessage("Set art submission channel to: " + channelId).queue();
                } else if (action.equals("remove") || action.equals("clear")) {
                    ConfigManager.removeArtSubmissionChannel(guildId);
                    event.getChannel().sendMessage("Removed art submission channel.").queue();
                } else if (action.equals("get") || action.equals("list")) {
                    String channelId = ConfigManager.getArtSubmissionChannel(guildId);
                    if (channelId == null || channelId.isEmpty()) {
                        event.getChannel().sendMessage("No art submission channel configured.").queue();
                    } else {
                        event.getChannel().sendMessage("Configured art submission channel: " + channelId).queue();
                    }
                } else {
                    String possibleChannelId = args[2].replaceAll("[^0-9]", "");
                    if (!possibleChannelId.isEmpty()) {
                        ConfigManager.setArtSubmissionChannel(guildId, possibleChannelId);
                        event.getChannel().sendMessage("Set art submission channel to: " + possibleChannelId).queue();
                    } else {
                        event.getChannel().sendMessage("Unknown action. Use `set`, `remove`, or `get`.").queue();
                    }
                }
            } else if (subCommand.equals("artstaffchannel")) {
                if (args.length < 3) {
                    event.getChannel().sendMessage("Use: `n^config artstaffchannel [set/remove/get] [channelId]`").queue();
                    return;
                }

                String action = args[2].toLowerCase();

                if (action.equals("set")) {
                    if (args.length < 4) {
                        event.getChannel().sendMessage("Please provide a channel ID.").queue();
                        return;
                    }
                    String channelId = args[3].replaceAll("[^0-9]", "");
                    if (channelId.isEmpty()) {
                        event.getChannel().sendMessage("Invalid channel ID.").queue();
                        return;
                    }
                    ConfigManager.setArtStaffChannel(guildId, channelId);
                    event.getChannel().sendMessage("Set art staff (top-rated) channel to: " + channelId).queue();
                } else if (action.equals("remove") || action.equals("clear")) {
                    ConfigManager.removeArtStaffChannel(guildId);
                    event.getChannel().sendMessage("Removed art staff channel.").queue();
                } else if (action.equals("get") || action.equals("list")) {
                    String channelId = ConfigManager.getArtStaffChannel(guildId);
                    if (channelId == null || channelId.isEmpty()) {
                        event.getChannel().sendMessage("No art staff channel configured.").queue();
                    } else {
                        event.getChannel().sendMessage("Configured art staff channel: " + channelId).queue();
                    }
                } else {
                    String possibleChannelId = args[2].replaceAll("[^0-9]", "");
                    if (!possibleChannelId.isEmpty()) {
                        ConfigManager.setArtStaffChannel(guildId, possibleChannelId);
                        event.getChannel().sendMessage("Set art staff channel to: " + possibleChannelId).queue();
                    } else {
                        event.getChannel().sendMessage("Unknown action. Use `set`, `remove`, or `get`.").queue();
                    }
                }
            } else {
                event.getChannel().sendMessage("Unknown subcommand. Use `adminrole`, `reportrole`, `reportchannel`, `artname`, `artchannel`, or `artstaffchannel`.").queue();
            }
        }
    }
}
