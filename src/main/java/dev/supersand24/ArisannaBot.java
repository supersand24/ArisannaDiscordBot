package dev.supersand24;

import com.google.gson.reflect.TypeToken;
import dev.supersand24.counters.CounterData;
import dev.supersand24.events.EventData;
import dev.supersand24.expenses.DebtData;
import dev.supersand24.expenses.ExpenseData;
import dev.supersand24.expenses.PaymentInfo;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ArisannaBot {

    static JDA jda;

    public static Emoji emojiLoadingArisanna;
    public static Emoji emojiBonkArisanna;
    public static CustomEmoji emojiHeartArisanna;

    public static void main(String[] args) {

        DataStore.register(
                "counters",
                "counters.json",
                new TypeToken<ConcurrentHashMap<String, CounterData>>() {}.getType(),
                ConcurrentHashMap::new
        );
        DataStore.register(
                "paymentMethods",
                "paymentMethods.json",
                new TypeToken<ConcurrentHashMap<String, List<PaymentInfo>>>() {}.getType(),
                ConcurrentHashMap::new
        );

        DataStore.register(
                "expenses",
                "expenses.json",
                new TypeToken<DataPartition<ExpenseData>>() {}.getType(),
                DataPartition::new
        );
        DataStore.register(
                "debts",
                "debts.json",
                new TypeToken<DataPartition<DebtData>>() {}.getType(),
                DataPartition::new
        );
        DataStore.register(
                "events",
                "events.json",
                new TypeToken<DataPartition<EventData>>() {}.getType(),
                DataPartition::new
        );

        DataStore.initialize(10);

        Listener listener = new Listener();

        JDABuilder builder = JDABuilder.create(
            System.getenv("ARISANNA_DISCORD_BOT_TOKEN"),
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.GUILD_VOICE_STATES,
            GatewayIntent.SCHEDULED_EVENTS
        ).setMemberCachePolicy(MemberCachePolicy.ALL);

        builder.addEventListeners(listener);

        try {
            jda = builder.build();
            jda.awaitReady();

            List<CommandData> commandDataList = listener.getAllCommands().stream()
                .map(ICommand::getCommandData)
                .filter(Objects::nonNull)
                .toList();

            ArisannaBot.getAriGuild().updateCommands()
                .addCommands(commandDataList)
                .queue();

            emojiLoadingArisanna = Emoji.fromCustom("loading_arisanna", 1163570216018653316L, false);
            emojiBonkArisanna = Emoji.fromCustom("bonk_arisanna", 1163570214147993731L, false);
            emojiHeartArisanna = Emoji.fromCustom("heart_arisanna", 1163570212872933486L, false);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static Guild getAriGuild() {
        return jda.getGuildById("1295469711773138984");
    }
}