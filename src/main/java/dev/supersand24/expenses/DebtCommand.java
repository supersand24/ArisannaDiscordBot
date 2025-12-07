package dev.supersand24.expenses;

import dev.supersand24.ICommand;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

public class DebtCommand implements ICommand {
    @Override
    public String getName() { return "debt"; }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("debt", "Manage outstanding debts from a settlement.")
                .addSubcommands(
                        new SubcommandData("list", "List all outstanding (unpaid) debts."),
                        new SubcommandData("markpaid", "Mark a debt as paid.")
                                .addOption(OptionType.INTEGER, "id", "The ID of the debt to mark as paid.", true)
                );
    }

    @Override
    public void handleSlashCommand(SlashCommandInteractionEvent e) {
        switch (e.getSubcommandName()) {
            case "list" ->
                    e.reply(ExpenseManager.generateDebtListMessage(e.getUser().getId(), 0))
                            .useComponentsV2()
                            .queue();
            case "markpaid" -> {
                long debtId = e.getOption("id").getAsLong();
                String actioningUserId = e.getUser().getId();
                String resultMessage = ExpenseManager.markDebtAsPaid(debtId, actioningUserId);
                e.reply(resultMessage).setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void handleButtonInteraction(ButtonInteractionEvent e) {
        String[] parts = e.getComponentId().split(":");
        String prefix = parts[0];

        if (prefix.startsWith("debt-")) {
            String authorId = parts[1];

            if (!e.getUser().getId().equals(authorId)) {
                e.reply("You cannot use these buttons.").setEphemeral(true).queue();
                return;
            }

            e.deferEdit().queue();

            MessageCreateData data = new MessageCreateBuilder().setContent("No debts.").build();

            switch (prefix) {
                case "debt-list-prev", "debt-list-next" -> {
                    int currentPage = Integer.parseInt(parts[2]);
                    int newPage = prefix.equals("debt-list-next") ? currentPage + 1 : currentPage - 1;
                    data = ExpenseManager.generateDebtListMessage(authorId, newPage);
                }
                case "debt-list-zoom" -> {
                    int index = Integer.parseInt(parts[2]);
                    data = ExpenseManager.generateDebtDetailMessage(authorId, index, e.getJDA());
                }
                case "debt-detail-prev", "debt-detail-next" -> {
                    int currentIndex = Integer.parseInt(parts[2]);
                    int newIndex = prefix.equals("debt-detail-next") ? currentIndex + 1 : currentIndex - 1;
                    data = ExpenseManager.generateDebtDetailMessage(authorId, newIndex, e.getJDA());
                }
                case "debt-detail-back" -> {
                    int page = Integer.parseInt(parts[2]);
                    data = ExpenseManager.generateDebtListMessage(authorId, page);
                }
            }

            e.getHook().editOriginalEmbeds(data.getEmbeds())
                    .setComponents(data.getComponents())
                    .queue();
        }
    }

    @Override
    public void handleStringSelectInteraction(StringSelectInteractionEvent e) {

    }

    @Override
    public void handleEntitySelectInteraction(EntitySelectInteractionEvent e) {

    }

    @Override
    public void handleModalInteraction(ModalInteractionEvent e) {

    }
}
