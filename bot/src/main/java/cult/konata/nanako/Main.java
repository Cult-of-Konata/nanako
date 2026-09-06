package cult.konata.nanako;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class Main {
    public static void main(String[] args) {
        /**
         * Pass the token as "-Dtoken=...".
         */
        String token = System.getProperty("token");

        if (token == "") {
            System.out.println("No token passed. Stopping.");
            System.out.println("To pass a token, pass -Dtoken=[token].");
        } else {
            System.out.println("Token passed.");
            JDA api = JDABuilder.createDefault(token).build();
        }
    }
}
