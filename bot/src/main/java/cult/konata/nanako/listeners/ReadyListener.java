package cult.konata.nanako.listeners;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;

import java.util.EventListener;

public class ReadyListener implements EventListener {
        public void setupBot(String token) {
            JDA jda = JDABuilder.createDefault(token)
                    .addEventListeners(new ReadyListener())
                    .build();

            // block until JDA is ready
            try {
                jda.awaitReady();
            } catch (Exception e) {
                System.out.println("Could not awaitReady! Printing stack trace!");
                e.printStackTrace();
            }
        }

}
