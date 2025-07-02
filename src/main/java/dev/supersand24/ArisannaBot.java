package dev.supersand24;

import dev.supersand24.counters.CounterManager;
import dev.supersand24.expenses.ExpenseManager;
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

public class ArisannaBot {

    static JDA jda;

    public static Emoji emojiLoadingArisanna;

    public static void main(String[] args) {

        CounterManager.ready(10);
        ExpenseManager.ready(10);

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

            emojiLoadingArisanna = Emoji.fromCustom("loading_arisanna", 1163570216018653316L, false);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static Guild getAriGuild() {
        return jda.getGuildById("1295469711773138984");
    }
}