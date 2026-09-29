package cult.konata.nanako.commands.contest;

import cult.konata.nanako.config.ConfigManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ArtContest extends ListenerAdapter {

    private static final String STAR = "⭐";
    private static final String CONFIRM_PREFIX = "art-confirm:";
    private static final String CANCEL_PREFIX = "art-cancel:";
    private static final int HISTORY_LIMIT = 100;
    private static final long PENDING_EXPIRY_MINUTES = 15;
    public static final int DEFAULT_TOP_COUNT = 3;

    private final Map<String, PendingSubmission> pending = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final Pattern MENTION_PATTERN = Pattern.compile("<@!?(\\d+)>");

    private static class PendingSubmission {
        final long guildId;
        final long userId;
        final List<String> imageUrls;
        final List<String> fileNames;
        final String caption;

        PendingSubmission(long guildId, long userId, List<String> imageUrls, List<String> fileNames, String caption) {
            this.guildId = guildId;
            this.userId = userId;
            this.imageUrls = imageUrls;
            this.fileNames = fileNames;
            this.caption = caption;
        }
    }

    private static class RankedEntry {
        final Message message;
        final int stars;
        final String entrantId;

        RankedEntry(Message message, int stars, String entrantId) {
            this.message = message;
            this.stars = stars;
            this.entrantId = entrantId;
        }
    }

    // ---------- Guild commands: n^art info / leaderboard / top ----------

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        if (event.isFromGuild()) {
            handleGuildCommand(event);
        } else {
            handleDirectMessage(event);
        }
    }

    private void handleGuildCommand(MessageReceivedEvent event) {
        String content = event.getMessage().getContentRaw();
        // NOTE: check n^artrefresh first — it also starts with the "n^art" prefix.
        if (content.startsWith("n^artrefresh")) {
            handleArtRefresh(event);
            return;
        }
        if (!content.startsWith("n^art")) return;

        String[] args = content.split("\\s+");
        String sub = args.length > 1 ? args[1].toLowerCase() : "info";
        long guildId = event.getGuild().getIdLong();

        switch (sub) {
            case "info":
                sendArtInfo(event, guildId);
                break;
            case "leaderboard":
            case "top-public":
            case "ranking":
                int boardCount = parseCount(args, 2, 5);
                sendLeaderboard(event, guildId, boardCount);
                break;
            case "top":
                if (!isStaff(event)) {
                    event.getChannel().sendMessage("You do not have permission to use this command.").queue();
                    return;
                }
                int topCount = parseCount(args, 2, 3);
                sendTopToStaffChannel(event, guildId, topCount);
                break;
            default:
                event.getChannel().sendMessage(
                        "Use: `n^art info` — show the current competition.\n"
                                + "`n^art leaderboard [count]` — show the top-voted art here.\n"
                                + "`n^art top [count]` — send the top ⭐-voted art to the staff channel (staff only).\n"
                                + "`n^artrefresh [count]` — refresh the live staff leaderboard now (staff only, also auto-refreshes every 5 min).\n"
                                + "Enter by DMing the bot an image attachment.").queue();
                break;
        }
    }

    private void handleArtRefresh(MessageReceivedEvent event) {
        if (!isStaff(event)) {
            event.getChannel().sendMessage("You do not have permission to use this command.").queue();
            return;
        }
        String[] args = event.getMessage().getContentRaw().split("\\s+");
        int count = parseCount(args, 1, DEFAULT_TOP_COUNT);
        event.getChannel().sendMessage("Refreshing staff leaderboard...").queue(statusMsg ->
                refreshStaffLeaderboard(event.getJDA(), event.getGuild().getIdLong(), count, result -> {
                    if (result == null) {
                        statusMsg.editMessage("Staff leaderboard refreshed.").queue();
                    } else {
                        statusMsg.editMessage(result).queue();
                    }
                }));
    }

    /**
     * Recomputes the top ⭐-voted art and updates the single live leaderboard message
     * in the staff channel (posts it first if there isn't one yet).
     *
     * @param feedback receives {@code null} on success or an error message on failure.
     *                 May be {@code null} for silent (scheduled) refreshes.
     */
    public static void refreshStaffLeaderboard(JDA jda, long guildId, int count, Consumer<String> feedback) {
        Consumer<String> reply = feedback != null ? feedback : msg -> {};
        String subChannelId = ConfigManager.getArtSubmissionChannel(guildId);
        String staffChannelId = ConfigManager.getArtStaffChannel(guildId);

        if (subChannelId == null || subChannelId.isEmpty()
                || staffChannelId == null || staffChannelId.isEmpty()) {
            return; // contest not (fully) configured — nothing to refresh.
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;
        GuildMessageChannel subChannel = guild.getChannelById(GuildMessageChannel.class, subChannelId);
        GuildMessageChannel staffChannel = guild.getChannelById(GuildMessageChannel.class, staffChannelId);
        if (subChannel == null || staffChannel == null) {
            reply.accept("Configured art channel could not be found.");
            return;
        }

        String competitionName = ConfigManager.getArtCompetitionName(guildId);
        String title = (competitionName != null && !competitionName.isEmpty()) ? competitionName : "Art Contest";

        subChannel.getHistory().retrievePast(HISTORY_LIMIT).queue(
                messages -> {
                    List<RankedEntry> ranked = rankEntries(jda, messages);
                    int n = Math.min(count, ranked.size());

                    StringBuilder sb = new StringBuilder("🏆 **Top-rated art — ").append(title).append("**");
                    List<MessageEmbed> embeds = new ArrayList<>();
                    if (ranked.isEmpty()) {
                        sb.append("\nNo entries yet. React with ⭐ in <#").append(subChannelId).append("> to vote!");
                    } else {
                        sb.append(" (top ").append(n).append(" by ⭐)\n");
                        for (int i = 0; i < n; i++) {
                            RankedEntry entry = ranked.get(i);
                            sb.append("`#").append(i + 1).append("` ")
                                    .append(entry.stars).append(" ⭐ — ")
                                    .append("<@").append(entry.entrantId).append("> ")
                                    .append(entry.message.getJumpUrl()).append("\n");

                            String imageUrl = firstImageUrl(entry.message);
                            EmbedBuilder eb = new EmbedBuilder()
                                    .setTitle("#" + (i + 1) + " — " + entry.stars + " ⭐ — " + title)
                                    .setDescription("<@" + entry.entrantId + "> — " + entry.message.getJumpUrl())
                                    .setColor(Color.MAGENTA)
                                    .setFooter("Entrant ID: " + entry.entrantId);
                            if (imageUrl != null) {
                                eb.setImage(imageUrl);
                            }
                            embeds.add(eb.build());
                        }
                    }
                    long nowEpoch = Instant.now().getEpochSecond();
                    sb.append("\nLast updated: <t:").append(nowEpoch).append(":R> • auto-refreshes every 5 min (`n^artrefresh` to refresh now).");

                    String text = sb.toString();
                    String existingId = ConfigManager.getArtLeaderboardMessage(guildId);
                    if (existingId != null && !existingId.isEmpty()) {
                        staffChannel.retrieveMessageById(existingId).queue(
                                existing -> existing.editMessage(text).setEmbeds(embeds).queue(
                                        msg -> reply.accept(null),
                                        err -> postNewLeaderboard(staffChannel, guildId, text, embeds, reply)),
                                err -> postNewLeaderboard(staffChannel, guildId, text, embeds, reply));
                    } else {
                        postNewLeaderboard(staffChannel, guildId, text, embeds, reply);
                    }
                },
                error -> reply.accept("Failed to load entries: " + error.getMessage()));
    }

    private static void postNewLeaderboard(GuildMessageChannel staffChannel, long guildId,
                                           String text, List<MessageEmbed> embeds, Consumer<String> reply) {
        staffChannel.sendMessage(text).addEmbeds(embeds).queue(
                posted -> {
                    ConfigManager.setArtLeaderboardMessage(guildId, posted.getId());
                    reply.accept(null);
                },
                error -> reply.accept("Failed to post staff leaderboard: " + error.getMessage()));
    }

    private void sendArtInfo(MessageReceivedEvent event, long guildId) {
        String name = ConfigManager.getArtCompetitionName(guildId);
        String subChannelId = ConfigManager.getArtSubmissionChannel(guildId);

        if ((name == null || name.isEmpty()) && (subChannelId == null || subChannelId.isEmpty())) {
            event.getChannel().sendMessage("No art competition is currently configured. Staff can set one up with `n^config artname set <name>`, `n^config artchannel set <#channel>` and `n^config artstaffchannel set <#channel>`.").queue();
            return;
        }

        StringBuilder sb = new StringBuilder("🎨 **Art Contest**");
        if (name != null && !name.isEmpty()) {
            sb.append(": **").append(name).append("**");
        }
        sb.append("\n");
        if (subChannelId != null && !subChannelId.isEmpty()) {
            sb.append("Entries are posted in <#").append(subChannelId).append(">.\n");
        }
        sb.append("To enter, **DM me** your art as an image attachment and confirm with the ✅ button.\n")
                .append("Vote for entries with the ⭐ reaction — the top-voted art can be sent to staff with `n^art top`.");
        event.getChannel().sendMessage(sb.toString()).queue();
    }

    private void sendLeaderboard(MessageReceivedEvent event, long guildId, int count) {
        String subChannelId = ConfigManager.getArtSubmissionChannel(guildId);
        if (subChannelId == null || subChannelId.isEmpty()) {
            event.getChannel().sendMessage("No art submission channel configured (`n^config artchannel set <#channel>`).").queue();
            return;
        }
        GuildMessageChannel subChannel = event.getGuild().getChannelById(GuildMessageChannel.class, subChannelId);
        if (subChannel == null) {
            event.getChannel().sendMessage("Configured art submission channel could not be found.").queue();
            return;
        }

        subChannel.getHistory().retrievePast(HISTORY_LIMIT).queue(
                messages -> {
                    List<RankedEntry> ranked = rankEntries(event.getJDA(), messages);
                    if (ranked.isEmpty()) {
                        event.getChannel().sendMessage("No art entries found yet.").queue();
                        return;
                    }
                    int n = Math.min(count, ranked.size());
                    StringBuilder sb = new StringBuilder("🏆 **Art Contest Leaderboard** (top ").append(n).append(" by ⭐)\n");
                    for (int i = 0; i < n; i++) {
                        RankedEntry entry = ranked.get(i);
                        sb.append("`#").append(i + 1).append("` ")
                                .append(entry.stars).append(" ⭐ — ")
                                .append("<@").append(entry.entrantId).append("> ")
                                .append(entry.message.getJumpUrl()).append("\n");
                    }
                    event.getChannel().sendMessage(sb.toString()).queue();
                },
                error -> event.getChannel().sendMessage("Failed to load entries: " + error.getMessage()).queue()
        );
    }

    private void sendTopToStaffChannel(MessageReceivedEvent event, long guildId, int count) {
        String subChannelId = ConfigManager.getArtSubmissionChannel(guildId);
        String staffChannelId = ConfigManager.getArtStaffChannel(guildId);

        if (subChannelId == null || subChannelId.isEmpty()) {
            event.getChannel().sendMessage("No art submission channel configured (`n^config artchannel set <#channel>`).").queue();
            return;
        }
        if (staffChannelId == null || staffChannelId.isEmpty()) {
            event.getChannel().sendMessage("No art staff channel configured (`n^config artstaffchannel set <#channel>`).").queue();
            return;
        }

        GuildMessageChannel subChannel = event.getGuild().getChannelById(GuildMessageChannel.class, subChannelId);
        GuildMessageChannel staffChannel = event.getGuild().getChannelById(GuildMessageChannel.class, staffChannelId);
        if (subChannel == null) {
            event.getChannel().sendMessage("Configured art submission channel could not be found.").queue();
            return;
        }
        if (staffChannel == null) {
            event.getChannel().sendMessage("Configured art staff channel could not be found.").queue();
            return;
        }

        String competitionName = ConfigManager.getArtCompetitionName(guildId);
        String title = (competitionName != null && !competitionName.isEmpty()) ? competitionName : "Art Contest";

        subChannel.getHistory().retrievePast(HISTORY_LIMIT).queue(
                messages -> {
                    List<RankedEntry> ranked = rankEntries(event.getJDA(), messages);
                    if (ranked.isEmpty()) {
                        event.getChannel().sendMessage("No art entries found to send.").queue();
                        return;
                    }
                    int n = Math.min(count, ranked.size());
                    List<RankedEntry> top = ranked.subList(0, n);

                    StringBuilder summary = new StringBuilder("🏆 **Top-rated art — ").append(title).append("** (top ").append(n).append(" by ⭐)\n");
                    List<MessageEmbed> embeds = new ArrayList<>();
                    for (int i = 0; i < top.size(); i++) {
                        RankedEntry entry = top.get(i);
                        summary.append("`#").append(i + 1).append("` ")
                                .append(entry.stars).append(" ⭐ — ")
                                .append("<@").append(entry.entrantId).append("> ")
                                .append(entry.message.getJumpUrl()).append("\n");

                        String imageUrl = firstImageUrl(entry.message);
                        EmbedBuilder eb = new EmbedBuilder()
                                .setTitle("#" + (i + 1) + " — " + entry.stars + " ⭐ — " + title)
                                .setDescription("<@" + entry.entrantId + "> — " + entry.message.getJumpUrl())
                                .setColor(Color.MAGENTA)
                                .setFooter("Entrant ID: " + entry.entrantId);
                        if (imageUrl != null) {
                            eb.setImage(imageUrl);
                        }
                        embeds.add(eb.build());
                        if (embeds.size() == 10) break;
                    }

                    staffChannel.sendMessage(summary.toString()).addEmbeds(embeds).queue(
                            success -> event.getChannel().sendMessage("Sent top " + n + " art to <#" + staffChannelId + ">.").queue(),
                            error -> event.getChannel().sendMessage("Failed to send to staff channel: " + error.getMessage()).queue()
                    );
                },
                error -> event.getChannel().sendMessage("Failed to load entries: " + error.getMessage()).queue()
        );
    }

    // ---------- DM submissions ----------

    private void handleDirectMessage(MessageReceivedEvent event) {
        Message message = event.getMessage();
        String content = message.getContentRaw();

        // Ignore bare commands with no attachment; point users at the flow.
        List<Message.Attachment> images = message.getAttachments().stream()
                .filter(Message.Attachment::isImage)
                .collect(Collectors.toList());

        if (images.isEmpty()) {
            if (content.startsWith("n^")) return; // let guild-style commands be ignored in DMs
            event.getChannel().sendMessage("To enter the art contest, DM me your art as an **image attachment** (you can add a caption in the message). I'll ask you to confirm before posting it.").queue();
            return;
        }

        if (!message.getAttachments().isEmpty() && images.size() != message.getAttachments().size()) {
            event.getChannel().sendMessage("Only image attachments can be entered into the art contest. Please resend with image file(s).").queue();
            return;
        }

        resolveCandidateGuilds(event.getJDA(), event.getAuthor(), content, candidates -> {
            if (candidates.isEmpty()) {
                event.getChannel().sendMessage("I couldn't find an art contest for you. Make sure you are a member of a server where staff have set one up (`n^config artname` / `n^config artchannel`), then DM me the art again.").queue();
                return;
            }

            Guild target = selectTargetGuild(candidates, content);
            if (target == null) {
                StringBuilder sb = new StringBuilder("I found art contests in multiple servers. Please resend your art with the server's name or ID in the message, e.g. `MyServer <caption>`:\n");
                for (Guild g : candidates) {
                    String cname = ConfigManager.getArtCompetitionName(g.getIdLong());
                    sb.append("• **").append(g.getName()).append("** (ID: `").append(g.getId()).append("`)");
                    if (cname != null && !cname.isEmpty()) sb.append(" — ").append(cname);
                    sb.append("\n");
                }
                event.getChannel().sendMessage(sb.toString()).queue();
                return;
            }

            long guildId = target.getIdLong();
            String competitionName = ConfigManager.getArtCompetitionName(guildId);
            String displayName = (competitionName != null && !competitionName.isEmpty()) ? competitionName : target.getName();

            List<String> urls = images.stream().map(Message.Attachment::getUrl).collect(Collectors.toList());
            List<String> names = images.stream().map(Message.Attachment::getFileName).collect(Collectors.toList());
            String caption = content != null ? content.trim() : "";
            if (caption.length() > 1000) caption = caption.substring(0, 1000);

            String token = UUID.randomUUID().toString().substring(0, 8);
            PendingSubmission pendingSubmission = new PendingSubmission(guildId, event.getAuthor().getIdLong(), urls, names, caption);
            pending.put(token, pendingSubmission);
            scheduler.schedule(() -> pending.remove(token), PENDING_EXPIRY_MINUTES, TimeUnit.MINUTES);

            EmbedBuilder eb = new EmbedBuilder()
                    .setTitle("Confirm art submission")
                    .setDescription("Is this art for **" + displayName + "** in **" + target.getName() + "**?\nClick ✅ to post it, or ❌ to cancel.")
                    .setColor(Color.CYAN)
                    .setFooter("This confirmation expires in " + PENDING_EXPIRY_MINUTES + " minutes.");
            if (!urls.isEmpty()) {
                eb.setImage(urls.get(0));
            }

            event.getChannel().sendMessageEmbeds(eb.build())
                    .addComponents(ActionRow.of(
                            Button.success(CONFIRM_PREFIX + token, "✅ Yes, submit"),
                            Button.danger(CANCEL_PREFIX + token, "❌ Cancel")))
                    .queue();
        });
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        boolean confirm = componentId.startsWith(CONFIRM_PREFIX);
        boolean cancel = componentId.startsWith(CANCEL_PREFIX);
        if (!confirm && !cancel) return;

        String token = componentId.substring(componentId.indexOf(':') + 1);
        PendingSubmission submission = pending.get(token);
        if (submission == null) {
            event.reply("This submission has expired or was already handled. Please DM me your art again.").queue();
            return;
        }
        if (event.getUser().getIdLong() != submission.userId) {
            event.reply("Only the person who submitted this art can confirm it.").queue();
            return;
        }

        if (cancel) {
            pending.remove(token);
            clearButtons(event);
            event.getHook().sendMessage("Submission cancelled. Your art was not posted.").queue();
            return;
        }

        // Confirm: remove first to prevent double-submits, then post.
        pending.remove(token);
        event.deferEdit().queue();
        clearButtons(event);

        JDA jda = event.getJDA();
        Guild guild = jda.getGuildById(submission.guildId);
        if (guild == null) {
            event.getHook().sendMessage("I could no longer access that server. Your art was not posted.").queue();
            return;
        }
        String subChannelId = ConfigManager.getArtSubmissionChannel(submission.guildId);
        if (subChannelId == null || subChannelId.isEmpty()) {
            event.getHook().sendMessage("The art contest is no longer configured on that server. Your art was not posted.").queue();
            return;
        }
        GuildMessageChannel subChannel = guild.getChannelById(GuildMessageChannel.class, subChannelId);
        if (subChannel == null) {
            event.getHook().sendMessage("The configured submission channel could not be found. Your art was not posted.").queue();
            return;
        }

        String competitionName = ConfigManager.getArtCompetitionName(submission.guildId);
        String title = (competitionName != null && !competitionName.isEmpty()) ? competitionName : guild.getName();

        List<MessageEmbed> embeds = new ArrayList<>();
        for (int i = 0; i < submission.imageUrls.size() && i < 10; i++) {
            EmbedBuilder eb = new EmbedBuilder()
                    .setTitle(i == 0 ? "🎨 Art Contest Entry — " + title : "🎨 Entry (cont.) — " + title)
                    .setDescription("<@" + submission.userId + ">" + (submission.caption.isEmpty() ? "" : "\n" + submission.caption))
                    .setColor(Color.MAGENTA)
                    .setFooter("Entrant ID: " + submission.userId + " | React with ⭐ to vote!")
                    .setImage(submission.imageUrls.get(i));
            embeds.add(eb.build());
        }
        if (embeds.isEmpty()) {
            event.getHook().sendMessage("No images found on this submission. Please try again.").queue();
            return;
        }

        subChannel.sendMessageEmbeds(embeds).queue(
                posted -> {
                    posted.addReaction(Emoji.fromUnicode(STAR)).queue(
                            unused -> {},
                            err -> {});
                    event.getHook().sendMessage("Your art was submitted to **" + title + "**! View it here: " + posted.getJumpUrl()).queue();
                },
                error -> event.getHook().sendMessage("Failed to post your art: " + error.getMessage()).queue()
        );
    }

    // ---------- Helpers ----------

    private void clearButtons(ButtonInteractionEvent event) {
        try {
            event.getMessage().editMessageComponents().queue(unused -> {}, err -> {});
        } catch (Exception ignored) {
            // Buttons stay but the pending entry is gone, so re-clicks are rejected.
        }
    }

    private boolean isStaff(MessageReceivedEvent event) {
        if (event.getMember() != null && event.getMember().hasPermission(Permission.ADMINISTRATOR)) return true;
        if (event.getMember() == null) return false;
        List<String> roleIds = event.getMember().getRoles().stream().map(Role::getId).collect(Collectors.toList());
        return ConfigManager.isAdmin(event.getGuild().getIdLong(), roleIds);
    }

    private static int parseCount(String[] args, int index, int def) {
        if (args.length > index) {
            try {
                int n = Integer.parseInt(args[index].replaceAll("[^0-9]", ""));
                return Math.min(10, Math.max(1, n));
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    private static List<RankedEntry> rankEntries(JDA jda, List<Message> messages) {
        String selfId = jda.getSelfUser().getId();
        List<RankedEntry> entries = new ArrayList<>();
        for (Message m : messages) {
            if (!m.getAuthor().getId().equals(selfId)) continue;
            if (!looksLikeEntry(m)) continue;
            entries.add(new RankedEntry(m, countStars(m), extractEntrantId(m)));
        }
        entries.sort(Comparator.comparingInt((RankedEntry e) -> e.stars).reversed());
        return entries;
    }

    private static boolean looksLikeEntry(Message message) {
        for (MessageEmbed embed : message.getEmbeds()) {
            String title = embed.getTitle() != null ? embed.getTitle() : "";
            MessageEmbed.Footer footer = embed.getFooter();
            String footerText = footer != null && footer.getText() != null ? footer.getText() : "";
            if (title.contains("Art Contest Entry") || footerText.contains("Entrant ID")) return true;
        }
        return false;
    }

    private static int countStars(Message message) {
        return message.getReactions().stream()
                .filter(r -> {
                    try {
                        return STAR.equals(r.getEmoji().getName());
                    } catch (Exception e) {
                        return false;
                    }
                })
                .mapToInt(r -> Math.max(0, r.getCount() - 1)) // exclude the bot's seed ⭐
                .sum();
    }

    private static String extractEntrantId(Message message) {
        for (MessageEmbed embed : message.getEmbeds()) {
            if (embed.getDescription() != null) {
                Matcher matcher = MENTION_PATTERN.matcher(embed.getDescription());
                if (matcher.find()) return matcher.group(1);
            }
            if (embed.getFooter() != null && embed.getFooter().getText() != null) {
                Matcher matcher = Pattern.compile("(\\d{5,})").matcher(embed.getFooter().getText());
                if (matcher.find()) return matcher.group(1);
            }
        }
        return message.getAuthor().getId();
    }

    private static String firstImageUrl(Message message) {
        for (MessageEmbed embed : message.getEmbeds()) {
            if (embed.getImage() != null && embed.getImage().getUrl() != null) return embed.getImage().getUrl();
        }
        if (!message.getAttachments().isEmpty()) return message.getAttachments().get(0).getUrl();
        return null;
    }

    private void resolveCandidateGuilds(JDA jda, User user, String content, Consumer<List<Guild>> callback) {
        List<Guild> configured = jda.getGuilds().stream()
                .filter(g -> {
                    String ch = ConfigManager.getArtSubmissionChannel(g.getIdLong());
                    return ch != null && !ch.isEmpty();
                })
                .collect(Collectors.toList());

        if (configured.isEmpty()) {
            callback.accept(Collections.emptyList());
            return;
        }

        // Fast path: cached members only.
        List<Guild> cached = configured.stream()
                .filter(g -> {
                    try {
                        return g.getMemberById(user.getIdLong()) != null;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
        if (!cached.isEmpty()) {
            callback.accept(cached);
            return;
        }

        // Fallback: async membership lookups (works without the GUILD_MEMBERS cache).
        List<Guild> found = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger remaining = new AtomicInteger(configured.size());
        for (Guild guild : configured) {
            guild.retrieveMemberById(user.getId()).queue(
                    member -> {
                        found.add(guild);
                        if (remaining.decrementAndGet() == 0) callback.accept(new ArrayList<>(found));
                    },
                    err -> {
                        if (remaining.decrementAndGet() == 0) callback.accept(new ArrayList<>(found));
                    }
            );
        }
    }

    private Guild selectTargetGuild(List<Guild> candidates, String content) {
        if (candidates.size() == 1) return candidates.get(0);
        if (content != null) {
            String digits = content.replaceAll("[^0-9 ]", " ");
            for (String token : digits.split("\\s+")) {
                if (token.length() >= 5) {
                    for (Guild g : candidates) {
                        if (g.getId().equals(token)) return g;
                    }
                }
            }
            String lowered = content.toLowerCase();
            for (Guild g : candidates) {
                if (!g.getName().isEmpty() && lowered.contains(g.getName().toLowerCase())) return g;
            }
        }
        return null;
    }
}
