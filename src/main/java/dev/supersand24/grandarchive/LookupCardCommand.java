package dev.supersand24.grandarchive;

import dev.supersand24.DataStore;
import dev.supersand24.GuildSettings;
import dev.supersand24.ICommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;

public class LookupCardCommand implements ICommand {

    @Override
    public String getName() { return "lookup"; }

    @Override
    public CommandData getCommandData() {

        OptionData elementOption = new OptionData(OptionType.STRING, "element", "Filter by Element", false);
        OptionData classOption = new OptionData(OptionType.STRING, "class", "Filter by Class", false);
        OptionData typeOption = new OptionData(OptionType.STRING, "type", "Filter by Type", false);

        for (CardCache.OptionItem elementName : CardCache.getElements()) {
            elementOption.addChoice(elementName.getText(), elementName.getValue());
        }

        for (CardCache.OptionItem className : CardCache.getClasses()) {
            classOption.addChoice(className.getText(), className.getValue());
        }

        for (CardCache.OptionItem typeName : CardCache.getTypes()) {
            typeOption.addChoice(typeName.getText(), typeName.getValue());
        }

        return Commands.slash(getName(), "Look up a Grand Archive card.")
                .addOptions(new OptionData(OptionType.STRING, "name", "The exact name of the card", true).setAutoComplete(true))
                .addOptions(elementOption, classOption, typeOption);

    }

    @Override
    public void handleSlashCommand(SlashCommandInteractionEvent e) {
        if (e.getGuild() != null) {
            GuildSettings settings = DataStore.get(e.getGuild().getId(), "settings");
            if (!settings.enabledModules.contains("grand-archive")) {
                e.reply("The Grand Archive module is currently disabled in this server.").setEphemeral(true).queue();
                return;
            }
        }

        String cardName = e.getOption("name").getAsString();
        Card card = CardCache.getCard(cardName);

        if (card == null){
            e.reply("❌ Could not find a card named `" + cardName + "`.").setEphemeral(true).queue();
            return;
        }

        CardEdition defaultEdition = card.getDefaultEdition();
        EmbedBuilder eb = buildCardEmbed(card, defaultEdition);

        if (card.getEditions() == null || card.getEditions().size() <= 1){
            e.replyEmbeds(eb.build()).queue();
            return;
        }

        ActionRow menuRow = buildEditionMenu(card, defaultEdition);
        e.replyEmbeds(eb.build())
                .setComponents(menuRow)
                .queue();

    }

    @Override
    public void handleStringSelectInteraction(StringSelectInteractionEvent e) {
        if (e.getComponentId().startsWith(getName() + "/edition_select:")) {

            String cardUuid = e.getComponentId().split(":")[1];
            String selectedEditionUuid = e.getValues().get(0);

            Card card = CardCache.getCardByUuid(cardUuid);

            if (card == null) {
                e.reply("❌ Error: Card no longer exists in cache.").setEphemeral(true).queue();
                return;
            }

            CardEdition selectedEdition = card.getEditions().stream()
                    .filter(ed -> ed.getUuid().equals(selectedEditionUuid))
                    .findFirst()
                    .orElse(null);

            if (selectedEdition == null) {
                e.reply("❌ Error: Could not find that edition.").setEphemeral(true).queue();
                return;
            }

            LookupCardCommand cmd = new LookupCardCommand();
            EmbedBuilder updatedEmbed = cmd.buildCardEmbed(card, selectedEdition);
            ActionRow updatedMenu = cmd.buildEditionMenu(card, selectedEdition);

            e.editMessageEmbeds(updatedEmbed.build())
                    .setComponents(updatedMenu)
                    .queue();
        }
    }

    @Override
    public void handleAutoComplete(CommandAutoCompleteInteractionEvent e) {
        if (e.getFocusedOption().getName().equals("name")) {

            String typedName = e.getFocusedOption().getValue().toLowerCase();

            String elementFilter = e.getOption("element") != null ? e.getOption("element").getAsString() : null;
            String typeFilter = e.getOption("type") != null ? e.getOption("type").getAsString() : null;

            List<Command.Choice> choices = CardCache.getAllCards().values().stream()
                    // Filter 1: Element (If the user selected an element, the card MUST match it)
                    .filter(card -> {
                        if (elementFilter == null) return true; // Skip filter if they didn't set it

                        if (card.getElements() != null && !card.getElements().isEmpty()) {
                            return card.getElements().stream().anyMatch(el -> el.equalsIgnoreCase(elementFilter));
                        }

                        // Fallback to singular string
                        return card.getElement() != null && card.getElement().equalsIgnoreCase(elementFilter);
                    })
                    // Filter 2: Type (If the user selected a type, the card's types array MUST contain it)
                    .filter(card -> {
                        if (typeFilter == null) return true; // Skip filter if they didn't set it

                        if (card.getTypes() == null) return false;

                        // Check if any of the card's types match the filter (case-insensitive)
                        return card.getTypes().stream().anyMatch(t -> t.equalsIgnoreCase(typeFilter));
                    })
                    // Filter 3: Name (What they are actively typing)
                    .filter(card -> card.getName().toLowerCase().contains(typedName))

                    // Final mapping to Discord Choices
                    .map(Card::getName)
                    .distinct()
                    .limit(25)
                    .map(name -> new Command.Choice(name, name))
                    .collect(Collectors.toList());

            e.replyChoices(choices).queue();
        }
    }

    public EmbedBuilder buildCardEmbed(Card card, CardEdition edition) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(card.getName());
        eb.setColor(new Color(43, 45, 49));

        if (card.getEffect() != null && !card.getEffect().isEmpty()) {
            eb.setDescription(card.getEffect());
        }

        if (card.getCost() != null && card.getCost().getType() != null && card.getCost().getValue() != null) {
            String typeStr = card.getCost().getType();
            String valueStr = card.getCost().getValue();
            String formattedType = typeStr.substring(0, 1).toUpperCase() + typeStr.substring(1).toLowerCase();
            eb.addField("Cost", valueStr + " " + formattedType, true);
        }

        // --- Custom Type Line Formatting ---
        List<String> SUPER_TYPES = java.util.Arrays.asList("REGALIA", "UNIQUE", "TOKEN");
        StringBuilder typeLine = new StringBuilder();

        if (card.getTypes() != null) {
            List<String> supers = card.getTypes().stream()
                    .filter(t -> SUPER_TYPES.contains(t.toUpperCase()))
                    .map(this::capitalize)
                    .collect(java.util.stream.Collectors.toList());

            List<String> normalTypes = card.getTypes().stream()
                    .filter(t -> !SUPER_TYPES.contains(t.toUpperCase()))
                    .map(this::capitalize)
                    .collect(java.util.stream.Collectors.toList());

            if (!supers.isEmpty()) typeLine.append(String.join(" ", supers)).append(" ");
            if (!normalTypes.isEmpty()) typeLine.append(String.join(" ", normalTypes));
        }

        boolean hasClasses = card.getClasses() != null && !card.getClasses().isEmpty();
        List<String> filteredSubtypes = new java.util.ArrayList<>();

        if (card.getSubtypes() != null) {
            filteredSubtypes = card.getSubtypes().stream()
                    .filter(sub -> card.getClasses() == null || !card.getClasses().contains(sub))
                    .map(this::capitalize)
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());
        }

        if (hasClasses || !filteredSubtypes.isEmpty()) {
            if (typeLine.length() > 0) typeLine.append(" - ");
            if (hasClasses) {
                typeLine.append(card.getClasses().stream().map(this::capitalize).collect(java.util.stream.Collectors.joining("/")));
            }
            if (!filteredSubtypes.isEmpty()) {
                if (hasClasses) typeLine.append(" ");
                typeLine.append(String.join(" ", filteredSubtypes));
            }
        }

        if (typeLine.length() > 0) eb.addField("Type", typeLine.toString().trim(), true);

        if (card.getElements() != null && !card.getElements().isEmpty()) {
            String elementStr = card.getElements().stream()
                    .map(this::capitalize)
                    .collect(java.util.stream.Collectors.joining(" / "));
            eb.addField("Element", elementStr, true);
        } else if (card.getElement() != null) {
            eb.addField("Element", capitalize(card.getElement()), true);
        }

        if (card.getLife() != null) eb.addField("Life", String.valueOf(card.getLife()), true);
        if (card.getPower() != null) eb.addField("Power", String.valueOf(card.getPower()), true);
        if (card.getDurability() != null) eb.addField("Durability", String.valueOf(card.getDurability()), true);

        // Map the specific edition passed into the method
        if (edition != null) {
            if (edition.getFullImageUrl() != null) {
                eb.setImage(edition.getFullImageUrl());
            }
            if (edition.getSet() != null) {
                eb.setFooter(edition.getSet().getName() + " (" + edition.getSet().getPrefix() + " · " + edition.getCollectorNumber() + ")");
            }
        }

        return eb;
    }

    public ActionRow buildEditionMenu(Card card, CardEdition currentEdition) {
        StringSelectMenu.Builder menuBuilder = StringSelectMenu.create(getName() + "/edition_select:" + card.getUuid())
            .setPlaceholder("Select a different printing...");

        int limit = Math.min(card.getEditions().size(), 25);
        for (int i = 0; i < limit; i++) {
            CardEdition ed = card.getEditions().get(i);

            String label = ed.getSet().getName();
            String description = ed.getSet().getPrefix() + " · " + ed.getCollectorNumber();

            if (label.length() > 100) label = label.substring(0, 97) + "...";
            if (description.length() > 100) description = description.substring(0, 97) + "...";

            SelectOption option = SelectOption.of(label, ed.getUuid()).withDescription(description);

            // Flag the currently viewed edition as the default option in the UI
            if (ed.getUuid().equals(currentEdition.getUuid())) {
                option = option.withDefault(true);
            }

            menuBuilder.addOptions(option);
        }

        return ActionRow.of(menuBuilder.build());
    }

    private String capitalize(String input) {
        if (input == null || input.isEmpty()) return "";
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

}
