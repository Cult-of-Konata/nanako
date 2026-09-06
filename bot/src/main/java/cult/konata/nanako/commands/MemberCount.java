package cult.konata.nanako.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MemberCount extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        Message message = event.getMessage();
        String content = message.getContentRaw();

        if (content.equals("n^getMembers")) {
            Guild guild = event.getGuild();
            if (guild != null) {
                int memberCount = guild.getMemberCount();
                MessageChannel channel = event.getChannel();
                channel.sendMessage("Total members of this server: " + memberCount);
            }
        }
    }
}
