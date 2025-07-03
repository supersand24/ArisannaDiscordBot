package dev.supersand24;

import com.google.gson.reflect.TypeToken;
import dev.supersand24.counters.CounterData;
import dev.supersand24.expenses.Debt;
import dev.supersand24.expenses.ExpenseData;
import dev.supersand24.expenses.ExpenseManager;
import dev.supersand24.expenses.PaymentInfo;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ArisannaBot {

    static JDA jda;

    public static Emoji emojiLoadingArisanna;
    public static Emoji emojiBonkArisanna;

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
                new TypeToken<DataPartition<Debt>>() {}.getType(),
                DataPartition::new
        );

        DataStore.initialize(10);

        JDABuilder builder = JDABuilder.create(
            System.getenv("ARISANNA_DISCORD_BOT_TOKEN"),
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.SCHEDULED_EVENTS
        ).setMemberCachePolicy(MemberCachePolicy.ALL);

        builder.addEventListeners( new Listener() );

        try {
            jda = builder.build();
            jda.awaitReady();

            ArisannaBot.getAriGuild().upsertCommand("counter", "Modifies Counters.")
                .addSubcommandGroups(
                    new SubcommandGroupData("editor", "Modifies Editors on a Counter.")
                        .addSubcommands(
                            new SubcommandData("add", "Adds a new editor to a Counter.")
                                .addOptions(
                                    new OptionData(OptionType.STRING, "counter", "Counter to Modify.", true, true),
                                    new OptionData(OptionType.USER, "editor", "Who to add as an Editor.").setRequired(true)
                                ),
                            new SubcommandData("remove", "Removes an editor from a Counter.")
                                .addOptions(
                                    new OptionData(OptionType.STRING, "counter", "Counter to Modify.", true, true),
                                    new OptionData(OptionType.USER, "editor", "Who to remove as an Editor.").setRequired(true)
                                )
                    )
                )
                .addSubcommands(
                    new SubcommandData("increment", "Increments a Counter.")
                        .addOptions(
                            new OptionData(OptionType.STRING, "counter", "Counter to Modify.", true, true)
                        ),
                    new SubcommandData("decrement", "Decrements a Counter.")
                        .addOptions(
                            new OptionData(OptionType.STRING, "counter", "Counter to Modify.", true, true)
                        ),
                    new SubcommandData("set", "Sets the Value of a Counter.")
                        .addOptions(
                            new OptionData(OptionType.STRING, "counter", "Counter to Modify.", true, true),
                            new OptionData(OptionType.INTEGER, "value", "New value of the Counter.", true)
                        ),
                    new SubcommandData("display", "Displays an existing Counter.")
                            .addOptions(
                                    new OptionData(OptionType.STRING, "counter", "Counter to Display.", true, true)
                            ),
                    new SubcommandData("create", "Creates a new Counter.")
                        .addOptions(
                            new OptionData(OptionType.STRING, "name", "Name of the Counter.", true),
                            new OptionData(OptionType.STRING, "description", "Description of the Counter.", true),
                            new OptionData(OptionType.INTEGER, "initial-value", "Starting Value."),
                            new OptionData(OptionType.INTEGER, "min-value", "Minimum Value."),
                            new OptionData(OptionType.INTEGER, "max-value", "Maximum Value.")
                        ),
                    new SubcommandData("delete", "Deletes an existing Counter.")
                        .addOptions(
                            new OptionData(OptionType.STRING, "counter", "Counter to Delete.", true, true)
                        )
                )
            .queue();

            ArisannaBot.getAriGuild().upsertCommand("expense", "Modifies Expenses.")
                    .addSubcommands(
                            new SubcommandData("add", "Add a new expense you paid for.")
                                    .addOptions(
                                            new OptionData(OptionType.STRING, "name", "What was this expense for?", true),
                                            new OptionData(OptionType.NUMBER, "amount", "How much did it cost?", true)
                                    ),
                            new SubcommandData("list", "List expenses.")
                                    .addOption(OptionType.USER, "user", "Filter expenses involving a specific user."),
                            new SubcommandData("view", "View the details of a single expense.")
                                    .addOption(OptionType.INTEGER, "id", "The ID of the expense to view.", true),
                            new SubcommandData("remove", "Remove an expense you added.")
                                    .addOption(OptionType.INTEGER, "id", "The ID of the expense to remove.", true),
                            new SubcommandData("settleup", "Calculate who owes who to settle all debts.")
                    )
            .queue();

            ArisannaBot.getAriGuild().upsertCommand("payment", "Manage your payment information.")
                    .addSubcommands(
                            new SubcommandData("add", "Add one of your payment methods (e.g., PayPal, Venmo).")
                                    .addOptions(
                                            new OptionData(OptionType.STRING, "app", "The name of the app (e.g., PayPal).", true)
                                                    .addChoice("PayPal", "PayPal")
                                                    .addChoice("Venmo", "Venmo")
                                                    .addChoice("Cash App", "Cash App")
                                                    .addChoice("Zelle", "Zelle"),
                                            new OptionData(OptionType.STRING, "details", "Your username, email, or phone number for the app.", true)
                                    ),
                            new SubcommandData("remove", "Remove one of your payment methods.")
                                    .addOptions(
                                            new OptionData(OptionType.STRING, "app", "The name of the app to remove (e.g., PayPal).", true)
                                                    .addChoice("PayPal", "PayPal")
                                                    .addChoice("Venmo", "Venmo")
                                                    .addChoice("Cash App", "Cash App")
                                                    .addChoice("Zelle", "Zelle")
                                    ),
                            new SubcommandData("view", "View a user's saved payment methods.")
                                    .addOption(OptionType.USER, "user", "The user whose payment info you want to see.", true)
                    )
            .queue();

            ArisannaBot.getAriGuild().upsertCommand("debt", "Manage outstanding debts from a settlement.")
                    .addSubcommands(
                            new SubcommandData("list", "List all outstanding (unpaid) debts."),
                            new SubcommandData("markpaid", "Mark a debt as paid.")
                                    .addOption(OptionType.INTEGER, "id", "The ID of the debt to mark as paid.", true)
                    ).queue();

            ArisannaBot.getAriGuild().upsertCommand("roles", "Commands for managing roles").queue();

            emojiLoadingArisanna = Emoji.fromCustom("loading_arisanna", 1163570216018653316L, false);
            emojiBonkArisanna = Emoji.fromCustom("bonk_arisanna", 1163570214147993731L, false);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static Guild getAriGuild() {
        return jda.getGuildById("1295469711773138984");
    }
}