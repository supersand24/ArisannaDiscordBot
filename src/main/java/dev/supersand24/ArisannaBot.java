package dev.supersand24;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

public class ArisannaBot {

    static JDA jda;

    public static void main(String[] args) {
        
        JDABuilder builder = JDABuilder.create(
            System.getenv("ARISANNA_DISCORD_BOT_TOKEN"),
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.SCHEDULED_EVENTS
        ).setMemberCachePolicy(MemberCachePolicy.ALL);

        builder.addEventListeners( new Listener() );

        try {
            jda = builder.build();
            jda.awaitReady();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}