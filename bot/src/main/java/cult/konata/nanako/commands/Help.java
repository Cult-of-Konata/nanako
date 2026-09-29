package cult.konata.nanako.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Help extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        Message message = event.getMessage();
        String content = message.getContentRaw();
        if (!content.equals("n^help")) return;

        event.getChannel().sendMessage(
                "🤖 **Nanako Commands** (prefix `n^`)\n"
                        + "\n**General**\n"
                        + "`n^help` — show this list.\n"
                        + "`n^info` — show build date and bot uptime.\n"
                        + "`n^ping` — check the bot is alive.\n"
                        + "`n^getMembers` — show this server's member count.\n"
                        + "\n**Art contest**\n"
                        + "`n^art info` — show the current competition and how to enter.\n"
                        + "`n^art leaderboard [count]` — show the top ⭐-voted art here.\n"
                        + "To enter: **DM the bot** your art as an image attachment, then confirm with ✅.\n"
                        + "\n**Moderation**\n"
                        + "`n^b [userid] [reason]` — ban a user (staff).\n"
                        + "`n^k [userid] [reason]` — kick a user (staff).\n"
                        + "`n^report <reason>` (as a reply) — report the replied-to message to staff.\n"
                        + "\n**Staff setup** (`ADMINISTRATOR` only)\n"
                        + "`n^config adminrole [add/remove/list] [role]`\n"
                        + "`n^config reportrole [set/remove/get] [role]`\n"
                        + "`n^config reportchannel [set/remove/get] [#channel]`\n"
                        + "`n^config artname [set <name>/remove/get]` — competition name.\n"
                        + "`n^config artchannel [set/remove/get] [#channel]` — where entries are posted for ⭐ voting.\n"
                        + "`n^config artstaffchannel [set/remove/get] [#channel]` — where the live top-rated board is kept.\n"
                        + "`n^art top [count]` — post the top ⭐ art to the staff channel (staff).\n"
                        + "`n^artrefresh [count]` — refresh the live staff board now; it also auto-refreshes every 5 min (staff)."
        ).queue();
    }
}
