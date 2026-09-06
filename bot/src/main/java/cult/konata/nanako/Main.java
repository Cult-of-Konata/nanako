package cult.konata.nanako;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import cult.konata.nanako.commands.PingPong;
import cult.konata.nanako.commands.MemberCount;
import cult.konata.nanako.listeners.ReadyListener;

public class Main {
    public static void main(String[] args) {
        String token = System.getProperty("token");

        // Fallback to program arguments if -Dtoken is missing or misplaced
        if (token == null || token.isEmpty()) {
            for (String arg : args) {
                if (arg.startsWith("-Dtoken=")) {
                    token = arg.substring("-Dtoken=".length());
                    break;
                } else if (arg.startsWith("token=")) {
                    token = arg.substring("token=".length());
                    break;
                }
            }
        }

        if (token == null || token.isEmpty()) {
            System.out.println("No token passed. Stopping.");
            System.out.println("To pass a token, add '-Dtoken=[token]' to VM Options in your Run Configuration.");
        } else {
            System.out.println("Token passed.");
            JDA api = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(new PingPong(), new MemberCount(), new ReadyListener())
                    .build();
        }
    }
}
