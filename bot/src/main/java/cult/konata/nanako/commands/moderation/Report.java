package cult.konata.nanako.commands.moderation;

import cult.konata.nanako.config.ConfigManager;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReference;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Report extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromGuild()) return;

        Message message = event.getMessage();
        String content = message.getContentRaw();

        if (content.startsWith("n^report")) {
            long guildId = event.getGuild().getIdLong();

            MessageReference messageReference = message.getMessageReference();
            if (messageReference == null) {
                event.getChannel().sendMessage("You must reply to the message you want to report with `n^report <reason>`.").queue();
                return;
            }

            String[] args = content.split("\\s+");
            if (args.length < 2) {
                event.getChannel().sendMessage("Please provide a reason for the report. Use: `n^report <reason>`").queue();
                return;
            }

            StringBuilder reasonBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                reasonBuilder.append(args[i]).append(" ");
            }
            String reason = reasonBuilder.toString().trim();
            if (reason.isEmpty()) {
                event.getChannel().sendMessage("Please provide a reason for the report. Use: `n^report <reason>`").queue();
                return;
            }

            String reportChannelId = ConfigManager.getReportChannel(guildId);
            if (reportChannelId == null || reportChannelId.isEmpty()) {
                event.getChannel().sendMessage("No report channel has been configured. Please ask an administrator to configure one with `n^config reportchannel [channelId]`.").queue();
                return;
            }

            GuildMessageChannel reportChannel = event.getGuild().getChannelById(GuildMessageChannel.class, reportChannelId);
            if (reportChannel == null) {
                event.getChannel().sendMessage("Configured report channel could not be found.").queue();
                return;
            }

            long targetChannelId = messageReference.getChannelIdLong();
            GuildMessageChannel sourceChannel = event.getGuild().getChannelById(GuildMessageChannel.class, targetChannelId);
            if (sourceChannel == null && event.getChannel() instanceof GuildMessageChannel) {
                sourceChannel = (GuildMessageChannel) event.getChannel();
            }

            if (sourceChannel == null) {
                event.getChannel().sendMessage("Failed to locate channel containing reported message.").queue();
                return;
            }

            String referencedMessageId = messageReference.getMessageId();
            sourceChannel.retrieveMessageById(referencedMessageId).queue(
                    reportedMessage -> {
                        String reportRoleId = ConfigManager.getReportRole(guildId);
                        String rolePing = (reportRoleId != null && !reportRoleId.isEmpty()) ? "<@&" + reportRoleId + "> " : "";

                        String reportedContent = reportedMessage.getContentRaw();
                        if (reportedContent.isEmpty()) {
                            if (!reportedMessage.getAttachments().isEmpty()) {
                                reportedContent = "[Attachment(s): " + reportedMessage.getAttachments().size() + "]";
                            } else {
                                reportedContent = "[No text content]";
                            }
                        }

                        StringBuilder reportMsg = new StringBuilder();
                        if (!rolePing.isEmpty()) {
                            reportMsg.append(rolePing).append("\n");
                        }
                        reportMsg.append("⚠️ **New Content Report** ⚠️\n")
                                .append("**Reported Message:** ").append(reportedContent).append("\n")
                                .append("**Reported User:** ").append(reportedMessage.getAuthor().getAsMention())
                                .append(" (").append(reportedMessage.getAuthor().getName()).append(" - ID: ").append(reportedMessage.getAuthor().getId()).append(")\n")
                                .append("**Reported By:** ").append(event.getAuthor().getAsMention())
                                .append(" (").append(event.getAuthor().getName()).append(" - ID: ").append(event.getAuthor().getId()).append(")\n")
                                .append("**Reason:** ").append(reason).append("\n")
                                .append("**Channel:** ").append(event.getChannel().getAsMention()).append("\n")
                                .append("**Message Link:** ").append(reportedMessage.getJumpUrl());

                        reportChannel.sendMessage(reportMsg.toString()).queue(
                                success -> event.getChannel().sendMessage("Report submitted successfully.").queue(),
                                error -> event.getChannel().sendMessage("Failed to send report to report channel: " + error.getMessage()).queue()
                        );
                    },
                    error -> event.getChannel().sendMessage("Failed to retrieve the reported message: " + error.getMessage()).queue()
            );
        }
    }
}
