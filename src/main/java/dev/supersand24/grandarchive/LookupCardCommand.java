package dev.supersand24.grandarchive;

import dev.supersand24.DataStore;
import dev.supersand24.GuildSettings;
import dev.supersand24.ICommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.awt.Color;
import java.util.ArrayList;
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

        for (CardCache.OptionItem element : CardCache.getElements()) {
            if (elementOption.getChoices().size() >= 25) break;
            elementOption.addChoice(element.getText(), element.getValue());
        }
        for (CardCache.OptionItem className : CardCache.getClasses()) {
            if (classOption.getChoices().size() >= 25) break;
            classOption.addChoice(className.getText(), className.getValue());
        }
        for (CardCache.OptionItem type : CardCache.getTypes()) {
            if (typeOption.getChoices().size() >= 25) break;
            typeOption.addChoice(type.getText(), type.getValue());
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

        if (card == null) {
            e.reply("❌ Could not find a card named `" + cardName + "`.").setEphemeral(true).queue();
            return;
        }

        CardEdition defaultEdition = card.getDefaultEdition();
        EmbedBuilder eb = buildCardEmbed(card, defaultEdition);
        List<ActionRow> components = buildComponents(card, defaultEdition);

        if (components.isEmpty()) {
            e.replyEmbeds(eb.build()).queue();
        } else {
            e.replyEmbeds(eb.build()).setComponents(components).queue();
        }
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
                    .orElse(card.getDefaultEdition());

            EmbedBuilder updatedEmbed = buildCardEmbed(card, selectedEdition);
            List<ActionRow> updatedComponents = buildComponents(card, selectedEdition);

            e.editMessageEmbeds(updatedEmbed.build()).setComponents(updatedComponents).queue();
        }
    }

    @Override
    public void handleButtonInteraction(ButtonInteractionEvent e) {
        if (e.getComponentId().startsWith(getName() + "/flip:")) {
            String[] parts = e.getComponentId().split(":");
            String targetCardUuid = parts[1];
            String targetEditionUuid = parts.length > 2 ? parts[2] : null;

            Card targetCard = CardCache.getCardByUuid(targetCardUuid);
            if (targetCard == null) {
                e.reply("❌ Error: Could not find the other side of this card.").setEphemeral(true).queue();
                return;
            }

            CardEdition targetEdition = targetCard.getDefaultEdition();
            if (targetEditionUuid != null && !targetEditionUuid.isEmpty() && targetCard.getEditions() != null) {
                targetEdition = targetCard.getEditions().stream()
                        .filter(ed -> ed.getUuid().equals(targetEditionUuid))
                        .findFirst()
                        .orElse(targetCard.getDefaultEdition());
            }

            EmbedBuilder updatedEmbed = buildCardEmbed(targetCard, targetEdition);
            List<ActionRow> updatedComponents = buildComponents(targetCard, targetEdition);

            e.editMessageEmbeds(updatedEmbed.build()).setComponents(updatedComponents).queue();
        }
    }

    @Override
    public void handleAutoComplete(CommandAutoCompleteInteractionEvent e) {
        if (e.getFocusedOption().getName().equals("name")) {
            String typedName = e.getFocusedOption().getValue().toLowerCase();
            String elementFilter = e.getOption("element") != null ? e.getOption("element").getAsString() : null;
            String typeFilter = e.getOption("type") != null ? e.getOption("type").getAsString() : null;

            List<Command.Choice> choices = CardCache.getAllCards().values().stream()
                    .filter(card -> {
                        if (elementFilter == null) return true;
                        if (card.getElements() != null && !card.getElements().isEmpty()) {
                            return card.getElements().stream().anyMatch(el -> el.equalsIgnoreCase(elementFilter));
                        }
                        return card.getElement() != null && card.getElement().equalsIgnoreCase(elementFilter);
                    })
                    .filter(card -> {
                        if (typeFilter == null) return true;
                        if (card.getTypes() == null) return false;
                        return card.getTypes().stream().anyMatch(t -> t.equalsIgnoreCase(typeFilter));
                    })
                    .filter(card -> card.getName().toLowerCase().contains(typedName))
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
            eb.addField("Cost", capitalize(card.getCost().getType()) + ": " + card.getCost().getValue(), true);
        }

        List<String> SUPER_TYPES = java.util.Arrays.asList("REGALIA", "UNIQUE", "TOKEN");
        StringBuilder typeLine = new StringBuilder();

        if (card.getTypes() != null) {
            List<String> supers = card.getTypes().stream()
                    .filter(t -> SUPER_TYPES.contains(t.toUpperCase()))
                    .map(this::capitalize).collect(Collectors.toList());

            List<String> normalTypes = card.getTypes().stream()
                    .filter(t -> !SUPER_TYPES.contains(t.toUpperCase()))
                    .map(this::capitalize).collect(Collectors.toList());

            if (!supers.isEmpty()) typeLine.append(String.join(" ", supers)).append(" ");
            if (!normalTypes.isEmpty()) typeLine.append(String.join(" ", normalTypes));
        }

        boolean hasClasses = card.getClasses() != null && !card.getClasses().isEmpty();
        List<String> filteredSubtypes = new ArrayList<>();

        if (card.getSubtypes() != null) {
            filteredSubtypes = card.getSubtypes().stream()
                    .filter(sub -> card.getClasses() == null || !card.getClasses().contains(sub))
                    .map(this::capitalize).sorted().collect(Collectors.toList());
        }

        if (hasClasses || !filteredSubtypes.isEmpty()) {
            if (!typeLine.isEmpty()) typeLine.append(" - ");
            if (hasClasses) {
                typeLine.append(card.getClasses().stream().map(this::capitalize).collect(Collectors.joining("/")));
            }
            if (!filteredSubtypes.isEmpty()) {
                if (hasClasses) typeLine.append(" ");
                typeLine.append(String.join(" ", filteredSubtypes));
            }
        }

        if (!typeLine.isEmpty()) eb.addField("Type", typeLine.toString().trim(), true);

        if (card.getElements() != null && !card.getElements().isEmpty()) {
            eb.addField("Element", card.getElements().stream().map(this::capitalize).collect(Collectors.joining(" ")), true);
        } else if (card.getElement() != null) {
            eb.addField("Element", capitalize(card.getElement()), true);
        }

        if (card.getLife() != null) eb.addField("Life", String.valueOf(card.getLife()), true);
        if (card.getPower() != null) eb.addField("Power", String.valueOf(card.getPower()), true);
        if (card.getDurability() != null) eb.addField("Durability", String.valueOf(card.getDurability()), true);

        if (edition != null) {
            if (edition.getFullImageUrl() != null) eb.setImage(edition.getFullImageUrl());
            if (edition.getSet() != null) {
                eb.setFooter(edition.getSet().getName() + " (" + edition.getSet().getPrefix() + " · " + edition.getCollectorNumber() + ")");
            }
        }

        return eb;
    }

    public List<ActionRow> buildComponents(Card card, CardEdition currentEdition) {
        List<ActionRow> rows = new ArrayList<>();

        // 1. Dropdown Menu
        if (card.getEditions() != null && card.getEditions().size() > 1) {
            StringSelectMenu.Builder menuBuilder = StringSelectMenu.create(getName() + "/edition_select:" + card.getUuid())
                    .setPlaceholder("Select a different printing...");

            int limit = Math.min(card.getEditions().size(), 25);
            for (int i = 0; i < limit; i++) {
                CardEdition ed = card.getEditions().get(i);

                String label = ed.getSet().getName().length() > 100 ? ed.getSet().getName().substring(0, 97) + "..." : ed.getSet().getName();
                String desc = ed.getSet().getPrefix() + " · " + ed.getCollectorNumber();

                SelectOption option = SelectOption.of(label, ed.getUuid()).withDescription(desc);
                if (ed.getUuid().equals(currentEdition.getUuid())) option = option.withDefault(true);
                menuBuilder.addOptions(option);
            }
            rows.add(ActionRow.of(menuBuilder.build()));
        }

        // 2. Flip Button
        if ("flip".equalsIgnoreCase(currentEdition.getConfiguration())) {
            String targetCardUuid = null;
            String targetEditionUuid = null;

            if (currentEdition.getOtherOrientations() != null && !currentEdition.getOtherOrientations().isEmpty()) {
                Card otherSideData = currentEdition.getOtherOrientations().get(0);
                targetCardUuid = otherSideData.getUuid();
                if (otherSideData.getSingularEdition() != null) {
                    targetEditionUuid = otherSideData.getSingularEdition().getUuid();
                }
            } else if (currentEdition.getParentCardUuid() != null) {
                targetCardUuid = currentEdition.getParentCardUuid();
                targetEditionUuid = currentEdition.getParentEditionUuid();
            }

            if (targetCardUuid != null) {
                String buttonId = getName() + "/flip:" + targetCardUuid + ":" + (targetEditionUuid != null ? targetEditionUuid : "");
                String buttonText = "back".equalsIgnoreCase(currentEdition.getOrientation()) ? "Flip to Front 🔄" : "Flip to Back 🔄";

                rows.add(ActionRow.of(Button.primary(buttonId, buttonText)));
            }
        }

        return rows;
    }

    private String capitalize(String input) {
        if (input == null || input.isEmpty()) return "";
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

}
