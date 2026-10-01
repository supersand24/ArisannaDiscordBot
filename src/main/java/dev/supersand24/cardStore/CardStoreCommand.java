package dev.supersand24.cardStore;

import dev.supersand24.ICommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CardStoreCommand implements ICommand {

    private final Logger log = LoggerFactory.getLogger(CardStoreCommand.class);

    @Override
    public String getName() { return "card-store"; }

    @Override
    public CommandData getCommandData() {
        return Commands.slash(getName(), "Manage Card Stores.")
                .addSubcommands(
                        new SubcommandData("create", "Creates a new card store.")
                                .addOption(OptionType.STRING, "name", "The name of the card store.", true),
                        new SubcommandData("list", "List all the card stores.")
                ).setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR));
    }

    @Override
    public void handleSlashCommand(SlashCommandInteractionEvent e) {

        if (e.getGuild() == null) return;
        String guildId = e.getGuild().getId();

        switch (e.getSubcommandName()) {
            case "create" -> {
                String storeName = e.getOption("name").getAsString();
                CardStoreManager.createCardStore(guildId, storeName);
                e.reply("Created new store: **" + storeName + "**").setEphemeral(true).queue();
            }
            case "list" -> {
                e.deferReply().queue();
                MessageCreateData messageData = CardStoreManager.generateListMessage(guildId, e.getUser().getId(), 0);
                e.getHook().sendMessage(messageData).useComponentsV2().queue();
            }
        }
    }

    @Override
    public void handleButtonInteraction(ButtonInteractionEvent e) {

        if (e.getGuild() == null) return;
        String guildId = e.getGuild().getId();

        String[] parts = e.getComponentId().split(":");
        String prefix = parts[1];

        log.info("Processing {} button interaction.", prefix);

        String authorId = parts[2];

        if (!e.getUser().getId().equals(authorId)) {
            e.reply("You cannot use these buttons.").setEphemeral(true).queue();
            return;
        }

        if (prefix.equals("edit")) {

            int index = Integer.parseInt(parts[3]);

            e.editComponents(CardStoreManager.generateEditContainer(guildId, index, authorId))
                    .useComponentsV2()
                    .queue();

        }
        else if (prefix.startsWith("edit-")) {

            int index = Integer.parseInt(parts[3]);

            switch (prefix) {
                case "edit-name" -> e.replyModal(CardStoreManager.generateEditNameModal(guildId, index)).queue();
                case "edit-address" -> e.replyModal(CardStoreManager.generateEditAddressModal(guildId, index)).queue();
                case "edit-website" -> e.replyModal(CardStoreManager.generateEditWebsiteModal(guildId, index)).queue();
                case "edit-delete" -> {
                    Modal modal = CardStoreManager.generateDeleteCardStoreModel(guildId, index);
                    if (modal == null)
                        e.reply("Could not delete non existing card store!").setEphemeral(true).queue();
                    else
                        e.replyModal(modal).queue();
                }
                case "edit-view" -> e.editComponents(CardStoreManager.buildDetailContainer(guildId, index, authorId))
                        .useComponentsV2()
                        .queue();
                case "edit-view-list" ->
                        e.editComponents(CardStoreManager.buildListContainer(CardStoreManager.getAllCardStores(guildId), 0, authorId))
                                .useComponentsV2()
                                .queue();
                default -> {
                    log.error("Unexpected Card Store Edit Button Pressed!");
                    e.reply("Something went wrong!").setEphemeral(true).queue();
                }
            }

        }
        else {
            e.deferEdit().queue();

            MessageCreateData data = new MessageCreateBuilder().setContent("No stores.").build();

            switch (prefix) {
                case "list-prev", "list-next" -> {
                    int currentPage = Integer.parseInt(parts[3]);
                    int newPage = prefix.equals("list-next") ? currentPage + 1 : currentPage - 1;
                    data = CardStoreManager.generateListMessage(guildId, authorId, newPage);
                }
            }

            e.getHook().editOriginalComponents(data.getComponents())
                    .useComponentsV2()
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
