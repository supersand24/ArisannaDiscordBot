package dev.supersand24;

import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import dev.supersand24.counters.CounterManager;
import dev.supersand24.events.Event;
import dev.supersand24.events.EventManager;
import dev.supersand24.expenses.ExpenseData;
import dev.supersand24.expenses.ExpenseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.selections.StringSelectMenu.Builder;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;

public class Listener extends ListenerAdapter {

    private final Logger log = LoggerFactory.getLogger(Listener.class);

    private final Map<String, Long> SELF_ASSIGNABLE_ROLES = Map.of(
            "ascent-boston", 1357375256498802900L,
            "ascent-niagara-falls", 1389686731565174884L,
            "ascent-los-angeles", 1389686814813720647L,
            "ascent-las-vegas", 1389686984431239319L
    );

    @Override
    public void onReady(ReadyEvent ev) {
        log.info("Listener is Ready.");
    }

    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent e) {
        if (e.getFocusedOption().getName().equals("counter")) {
            List<Command.Choice> options = CounterManager.getCounterNames().stream()
                    .filter(counterName -> counterName.startsWith(e.getFocusedOption().getValue()))
                    .map(counterName -> new Command.Choice(counterName, counterName))
                    .collect(Collectors.toList());
            e.replyChoices(options).queue();
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {

        User commandUser = e.getUser();

        switch (e.getName()) {
            case "counter" -> {

                //If trying to create a Counter, handle first
                if (e.getFullCommandName().equals("counter create")) {
                    String optionName = e.getOption("name").getAsString();
                    if (CounterManager.getCounterNames().contains(optionName)) {
                        e.reply(optionName + " counter already exists!").setEphemeral(true).queue();
                    } else {
                        String optionDescription = e.getOption("description").getAsString();
                        int initialValue = e.getOption("initial-value") != null ? e.getOption("initial-value").getAsInt() : 0;
                        int minValue = e.getOption("min-value") != null ? e.getOption("min-value").getAsInt() : 0;
                        int maxValue = e.getOption("max-value") != null ? e.getOption("max-value").getAsInt() : Integer.MAX_VALUE;
                        CounterManager.createCounter(optionName, optionDescription, initialValue, minValue, maxValue, commandUser.getId());
                        e.reply(optionName + " counter was created!").queue();
                    }
                    return;
                }

                String counterName = e.getOption("counter").getAsString();

                //Check to see if Counter exists
                if (!CounterManager.getCounterNames().contains(counterName)) {
                    e.reply("I can't find a counter named " + counterName + "!").setEphemeral(true).queue();
                    return;
                }

                //Check to see if user has editing access
                if (!CounterManager.canEdit(counterName, commandUser.getId())) {
                    e.reply("You don't have editing access on " + counterName + " counter.").setEphemeral(true).queue();
                    return;
                }

                //Different Command Groups
                switch (e.getSubcommandGroup()) {
                    case null -> {
                        switch (e.getSubcommandName()) {
                            case "increment" -> {
                                CounterManager.increment(counterName);
                                e.reply(counterName + " counter incremented to " + CounterManager.getValue(counterName) + ".").queue();
                            }
                            case "decrement" -> {
                                CounterManager.decrement(counterName);
                                e.reply(counterName + " counter decremented to " + CounterManager.getValue(counterName) + ".").queue();
                            }
                            case "set" -> {
                                int value = e.getOption("value").getAsInt();
                                CounterManager.setValue(counterName, value);
                                e.reply(counterName + " counter set to " + value + ".").queue();
                            }
                            case "display" -> e.replyEmbeds(CounterManager.getCounterEmbed(counterName)).queue();
                            case "delete" -> {
                                CounterManager.deleteCounter(counterName);
                                e.reply(counterName + " counter was deleted!").queue();
                            }
                        }
                    }
                    case "editor" -> {
                        User editor = e.getOption("editor").getAsUser();

                        switch (e.getSubcommandName()) {
                            case "add" -> {
                                if (CounterManager.canEdit(counterName, editor.getId())) {
                                    e.reply(editor.getName() + " is already authorized on " + counterName + " counter.").setEphemeral(true).queue();
                                } else {
                                    CounterManager.addEditor(counterName, editor.getId());
                                    e.reply(editor.getName() + " is now an editor of " + counterName + " counter.").setEphemeral(true).queue();
                                }
                            }
                            case "remove" -> {
                                if (CounterManager.canEdit(counterName, editor.getId())) {
                                    CounterManager.removeEditor(counterName, editor.getId());
                                    e.reply(editor.getName() + " is no longer an editor of " + counterName + " counter.").setEphemeral(true).queue();
                                } else {
                                    e.reply(editor.getName() + " is not currently authorized on " + counterName + " counter.").setEphemeral(true).queue();
                                }
                            }
                        }
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + e.getSubcommandGroup());
                }
            }
            case "expense" -> {

                switch (e.getSubcommandName()) {
                    case "add" -> {
                        String optionName = e.getOption("name").getAsString();
                        double optionAmount = e.getOption("amount").getAsDouble();

                        long expenseId = ExpenseManager.createExpense(optionName, optionAmount, e.getUser().getId(), 0);

                        e.replyComponents(Container.of(
                                        TextDisplay.of("Created " + CurrencyUtils.formatAsUSD(optionAmount) + " expense."),
                                        Separator.createDivider(Separator.Spacing.SMALL),
                                        TextDisplay.of("Choose who benefited from " + optionName + "."),
                                        ActionRow.of(EntitySelectMenu.create("expense-beneficiary-select:" + expenseId, EntitySelectMenu.SelectTarget.USER)
                                                .setDefaultValues(EntitySelectMenu.DefaultValue.user(e.getUser().getId()))
                                                .setMaxValues(20)
                                                .build())
                                )).useComponentsV2().queue();
                    }
                    case "remove" -> {
                        e.reply("coming soon.").setEphemeral(true).queue();
                    }
                    case "view" -> {
                        long expenseId = e.getOption("id") != null ? e.getOption("id").getAsLong() : -1;
                        e.deferReply().queue();

                        List<ExpenseData> sortedExpenses = ExpenseManager.getExpensesSorted();
                        int initialIndex = -1;

                        for (int i = 0; i < sortedExpenses.size(); i++) {
                            if (sortedExpenses.get(i).expenseId == expenseId) {
                                initialIndex = i;
                                break;
                            }
                        }

                        if (initialIndex == -1) {
                            e.getHook().sendMessage("This expense doesn't exist in my library!").setEphemeral(true).queue();
                            return;
                        }

                        MessageCreateData messageData = ExpenseManager.buildSingleExpenseView(initialIndex, e.getUser().getId());
                        e.getHook().sendMessage(messageData).queue();
                    }
                    case "list" -> {
                        e.deferReply().queue();
                        User userFilter = e.getOption("user") != null ? e.getOption("user").getAsUser() : null;
                        String targetId = (userFilter == null) ? "all" : userFilter.getId();

                        MessageCreateData messageData = ExpenseManager.buildExpenseListPage(0, e.getUser().getId(), targetId);
                        e.getHook().sendMessage(messageData).queue();
                    }
                    case "settleup" -> {
                        e.deferReply().queue();
                        MessageCreateData messageData = ExpenseManager.buildSettlementView();
                        e.getHook().sendMessage(messageData).queue();
                    }
                }
            }
            case "payment" -> {
                switch (e.getSubcommandName()) {
                    case "add" -> {
                        String appName = e.getOption("app").getAsString();
                        String details = e.getOption("details").getAsString();
                        String userId = e.getUser().getId();

                        ExpenseManager.addPaymentInfo(userId, appName, details);

                        e.reply("Successfully added payment method:\n**" + appName + ":** `" + details + "`")
                                .setEphemeral(true)
                                .queue();
                    }
                    case "remove" -> {
                        String appName = e.getOption("app").getAsString();
                        String userId = e.getUser().getId();

                        boolean wasRemoved = ExpenseManager.removePaymentInfo(userId, appName);

                        if (wasRemoved) {
                            e.reply("I removed your **" + appName + "** information from my library.").setEphemeral(true).queue();
                        } else {
                            e.reply("I could not find your **" + appName + "** information to from my library.").setEphemeral(true).queue();
                        }
                    }
                    case "view" -> {
                        e.deferReply().queue();
                        User targetUser = e.getOption("user").getAsUser();
                        MessageCreateData messageData = ExpenseManager.buildPaymentMethodView(targetUser);
                        e.getHook().sendMessage(messageData).queue();
                    }
                }
            }
            case "debt" -> {
                switch (e.getSubcommandName()) {
                    case "list" -> {
                        e.deferReply().queue();
                        MessageCreateData messageData = ExpenseManager.buildDebtList();
                        e.getHook().sendMessage(messageData).queue();
                    }
                    case "markpaid" -> {
                        long debtId = e.getOption("id").getAsLong();
                        String actioningUserId = e.getUser().getId();
                        String resultMessage = ExpenseManager.markDebtAsPaid(debtId, actioningUserId);
                        e.reply(resultMessage).setEphemeral(true).queue();
                    }
                }
            }
            case "roles" -> {

                Builder menu = StringSelectMenu.create("role-select-menu")
                        .setPlaceholder("Select the roles you want...")
                        .setRequiredRange(0, SELF_ASSIGNABLE_ROLES.size());

                menu.addOption("Ascent Boston", "ascent-boston", "If you are interested in going to Ascent Boston.");
                menu.addOption("Ascent Niagara Falls", "ascent-niagara-falls", "If you are interested in going to Ascent Niagara Falls.");
                menu.addOption("Ascent Los Angeles", "ascent-los-angeles", "If you are interested in going to Ascent Los Angeles.");
                menu.addOption("Ascent Las Vegas", "ascent-las-vegas", "If you are interested in going to Ascent Las Vegas.");

                e.reply("Sending Role Selection now!").setEphemeral(true).queue();

                Container container = Container.of(
                        TextDisplay.of("## Role Selection"),
                        Separator.createDivider(Separator.Spacing.SMALL),
                        TextDisplay.of("Select any roles you'd like to receive from the dropdown menu below. Selecting a role you already have will remove it."),
                        ActionRow.of(menu.build())
                );

                e.getChannel().sendMessageComponents(container)
                        .useComponentsV2()
                        .queue();

            }
            case "event" -> {
                switch (e.getSubcommandName()) {
                    case "create" -> {
                        String eventName = e.getOption("name").getAsString();
                        EventManager.createEvent(eventName);
                        e.reply("Created new event: **" + eventName + "**").setEphemeral(true).queue();
                    }
                    case "list" -> {
                        e.deferReply().queue();
                        MessageCreateData messageData = EventManager.sendListView(e.getUser().getId(), 0);
                        e.getHook().sendMessage(messageData).queue();
                    }
                    case "edit" -> {
                        long eventId = e.getOption("id").getAsLong();
                        Event event = EventManager.getEventById(eventId);

                        if (event == null) {
                            e.reply("I can't find that Event in my calendar").setEphemeral(true).queue();
                            return;
                        }

                        StringBuilder response = new StringBuilder("## Updated Event #" + eventId + "\n");
                        boolean changed = false;

                        OptionMapping nameOpt = e.getOption("name");
                        if (nameOpt != null) {
                            String newName = nameOpt.getAsString();
                            event.setName(newName);
                            response.append("- Name set to: **").append(newName).append("**\n");
                            changed = true;
                        }

                        OptionMapping startDateOpt = e.getOption("start-date");
                        if (startDateOpt != null) {
                            long timestamp = parseDateToEpochMilli(startDateOpt.getAsString());
                            if (timestamp == -1) {
                                e.reply("Invalid start date format. Please use `MM/DD/YYYY`.").setEphemeral(true).queue();
                                return;
                            }
                            event.setStartDate(timestamp);
                            response.append("- Start date set to: <t:").append(timestamp / 1000).append(":D>\n");
                            changed = true;
                        }

                        OptionMapping endDateOpt = e.getOption("end-date");
                        if (endDateOpt != null) {
                            long timestamp = parseDateToEpochMilli(endDateOpt.getAsString());
                            if (timestamp == -1) {
                                e.reply("Invalid end date format. Please use `MM/DD/YYYY`.").setEphemeral(true).queue();
                                return;
                            }
                            event.setEndDate(timestamp);
                            response.append("- End date set to: <t:").append(timestamp / 1000).append(":D>\n");
                            changed = true;
                        }

                        OptionMapping roleOpt = e.getOption("role");
                        if (roleOpt != null) {
                            Role role = roleOpt.getAsRole();
                            event.setRoleId(role.getIdLong());
                            response.append("- Role set to: ").append(role.getAsMention()).append("\n");
                            changed = true;
                        }

                        OptionMapping channelOpt = e.getOption("channel");
                        if (channelOpt != null) {
                            GuildChannel channel = channelOpt.getAsChannel();
                            event.setChannelId(channel.getIdLong());
                            response.append("- Channel set to: ").append(channel.getAsMention()).append("\n");
                            changed = true;
                        }

                        OptionMapping addressOpt = e.getOption("address");
                        if (addressOpt != null) {
                            String address = addressOpt.getAsString();
                            event.setAddress(address);
                            response.append("- Address set to: `").append(address).append("`\n");
                            changed = true;
                        }

                        OptionMapping linkOpt = e.getOption("omnidex-link");
                        if (linkOpt != null) {
                            String link = linkOpt.getAsString();
                            event.setOmnidexLink(link);
                            response.append("- Link set to: ").append(link).append("\n");
                            changed = true;
                        }

                        if (changed) {
                            DataStore.markDirty("events");
                            e.reply(response.toString()).setEphemeral(true).queue();
                        } else {
                            e.reply("No changes were provided.").setEphemeral(true).queue();
                        }
                    }
                }
            }
        }
    }

    private long parseDateToEpochMilli(String dateStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            LocalDate localDate = LocalDate.parse(dateStr, formatter);
            return localDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        } catch (DateTimeParseException e) {
            return -1;
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent e) {
        System.out.println(e.getComponentId());
        String[] parts = e.getComponentId().split(":");
        String prefix = parts[0];
        String authorId = parts.length > 1 ? parts[1] : "";

        if (prefix.equals("role-select-menu")) {
            Member member = e.getMember();
            Guild guild = e.getGuild();
            if (member == null || guild == null) return;

            e.deferReply(true).queue();

            List<Role> currentMemberRoles = member.getRoles();
            List<String> selectedRoleValues = e.getValues();

            List<Role> allPossibleRoles = SELF_ASSIGNABLE_ROLES.values().stream()
                    .map(guild::getRoleById)
                    .filter(Objects::nonNull)
                    .toList();

            List<Role> rolesToAdd = new ArrayList<>();
            List<Role> rolesToRemove = new ArrayList<>();

            for (Role role : allPossibleRoles) {
                boolean wasSelected = selectedRoleValues.contains(getRoleKey(role.getIdLong()));
                boolean hasRole = currentMemberRoles.contains(role);

                if (wasSelected != hasRole && !guild.getSelfMember().canInteract(role)) {
                    e.getHook().sendMessage("I don't have permissions to manage the `" + role.getName() + "` role. Please contact an admin to check my role position.").setEphemeral(true).queue();
                    return;
                }

                if (wasSelected && !hasRole) {
                    rolesToAdd.add(role);
                } else if (!wasSelected && hasRole) {
                    rolesToRemove.add(role);
                }

            }

            if (rolesToAdd.isEmpty() && rolesToRemove.isEmpty()) {
                e.getHook().sendMessage("Your roles have not changed").setEphemeral(true).queue();
                return;
            }

            guild.modifyMemberRoles(member, rolesToAdd, rolesToRemove).queue(
                    success -> {
                        StringBuilder response = new StringBuilder("I updated your roles!\n");
                        if (!rolesToAdd.isEmpty()) {
                            response.append(rolesToAdd.stream().map(Role::getName).collect(Collectors.joining(", "))).append(" was added.\n");
                        }
                        if (!rolesToRemove.isEmpty()) {
                            response.append(rolesToRemove.stream().map(Role::getName).collect(Collectors.joining(", "))).append(" was removed.");
                        }
                        e.getHook().sendMessage(response.toString()).setEphemeral(true).queue();
                    },
                    error -> {
                        e.getHook().sendMessage("I was unable to update your roles!").setEphemeral(true).queue();
                    }
            );
        } else if (prefix.equals("event-list-zoom")) {
            if (!e.getUser().getId().equals(authorId)) {
                e.reply("You cannot use these buttons.").setEphemeral(true).queue();
                return;
            }

            e.deferEdit().queue();

            int index = Integer.parseInt(e.getValues().get(0));
            MessageCreateData data = EventManager.editDetailView(authorId, index);
            e.getHook().editOriginalEmbeds(data.getEmbeds())
                    .setComponents(data.getComponents())
                    .queue();
        } else if (prefix.equals("expense-list-zoom")) {
            if (!e.getUser().getId().equals(authorId)) {
                e.reply("You cannot use these buttons.").setEphemeral(true).queue();
                return;
            }

            e.deferEdit().queue();

            int index = Integer.parseInt(e.getValues().get(0));
            MessageCreateData data = ExpenseManager.editExpenseDetailView(authorId, index);
            e.getHook().editOriginalEmbeds(data.getEmbeds())
                    .setComponents(data.getComponents())
                    .queue();
        }
    }

    private String getRoleKey(long roleId) {
        return SELF_ASSIGNABLE_ROLES.entrySet().stream()
                .filter(entry -> entry.getValue().equals(roleId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    @Override
    public void onEntitySelectInteraction(EntitySelectInteractionEvent e) {
        log.info(e.getComponentId() + " was interacted with.");

        String[] parts = e.getComponentId().split(":");
        String prefix = parts[0];

        if (prefix.equals("expense-beneficiary-select")) {
            long expenseId = Long.parseLong(parts[1]);

            if (!ExpenseManager.exists(expenseId)) {
                log.error("Could not find expense: " + expenseId);
                e.reply("This expense doesn't exist in my library!").setEphemeral(true).queue();
                return;
            }

            List<String> beneficiaryIds = e.getMentions().getUsers().stream()
                    .map(IMentionable::getId)
                    .toList();

            ExpenseManager.addBenefactors(expenseId, beneficiaryIds);

            if (beneficiaryIds.size() == 1) {
                e.reply("Added 1 person to " + ExpenseManager.getName(expenseId) + " expense.").setEphemeral(true).queue();
            } else {
                e.reply("Added " + beneficiaryIds.size() + " people to " + ExpenseManager.getName(expenseId) + " expense.").setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent e) {
        log.info(e.getComponentId() + " was pressed.");

        String[] parts = e.getComponentId().split(":");
        String prefix = parts[0];

        if (prefix.equals("settleup-explain")) {
            e.deferReply(true).queue();

            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.BLUE);
            embed.setTitle("How the Settlement is Calculated");

            embed.addField("Totaling the Bills 💵",
                    "First, I look at every single expense and add up the total amount of money each person spent. This shows who contributed money to the trip.",
                    false);

            embed.addField("Finding the 'Fair Share' ➗",
                    "Next, for each shared expense, I calculate a \"fair share.\" For example, if a $30 pizza was shared by 3 people, everyone's fair share of that pizza is $10.",
                    false);

            embed.addField("Checking the Balance 👍",
                    "Then, I compare how much you *spent* versus your total *fair share*.\n" +
                            "• If you spent **more** than your share, you are **owed money**.\n" +
                            "• If you spent **less** than your share, you **need to pay** money.",
                    false);

            embed.addField("Simplifying the Payments ➡️",
                    "So, instead of a messy web of payments, I figure out the simplest way to get everyone even. I tell people who they need to pay and exactly how much, minimizing the number of payments required until all debts are cleared.",
                    false);

            e.getHook().sendMessageEmbeds(embed.build()).queue();
        } else if (prefix.startsWith("event-")) {
            String authorId = parts[1];

            if (!e.getUser().getId().equals(authorId)) {
                e.reply("You cannot use these buttons.").setEphemeral(true).queue();
                return;
            }

            e.deferEdit().queue();

            MessageCreateData data = new MessageCreateBuilder().setContent("No events.").build();

            switch (prefix) {
                case "event-list-prev", "event-list-next" -> {
                    int currentPage = Integer.parseInt(parts[2]);
                    int newPage = prefix.equals("event-list-next") ? currentPage + 1 : currentPage - 1;
                    data = EventManager.editListView(authorId, newPage);
                }
                case "event-list-zoom" -> {
                    int index = Integer.parseInt(parts[2]);
                    data = EventManager.editDetailView(authorId, index);
                }
                case "event-detail-prev", "event-detail-next" -> {
                    int currentIndex = Integer.parseInt(parts[2]);
                    int newIndex = prefix.equals("event-detail-next") ? currentIndex + 1 : currentIndex - 1;
                    data = EventManager.editDetailView(authorId, newIndex);
                }
                case "event-detail-back" -> {
                    int page = Integer.parseInt(parts[2]);
                    data = EventManager.editListView(authorId, page);
                }
            }

            e.getHook().editOriginalEmbeds(data.getEmbeds())
                    .setComponents(data.getComponents())
                    .queue();
        } else if (prefix.startsWith("expense-")) {
            String authorId = parts[1];

            if (!e.getUser().getId().equals(authorId)) {
                e.reply("You cannot use these buttons.").setEphemeral(true).queue();
                return;
            }

            e.deferEdit().queue();

            MessageCreateData data = new MessageCreateBuilder().setContent("No events.").build();

            switch (prefix) {
                case "expense-list-prev", "expense-list-next" -> {
                    int currentPage = Integer.parseInt(parts[2]);
                    int newPage = prefix.equals("expense-list-next") ? currentPage + 1 : currentPage - 1;
                    data = ExpenseManager.editExpenseListView(authorId, newPage);
                }
                case "expense-list-zoom" -> {
                    int index = Integer.parseInt(parts[2]);
                    data = ExpenseManager.editExpenseDetailView(authorId, index);
                }
                case "expense-detail-prev", "expense-detail-next" -> {
                    int currentIndex = Integer.parseInt(parts[2]);
                    int newIndex = prefix.equals("expense-detail-next") ? currentIndex + 1 : currentIndex - 1;
                    data = ExpenseManager.editExpenseDetailView(authorId, newIndex);
                }
                case "expense-detail-back" -> {
                    int page = Integer.parseInt(parts[2]);
                    data = ExpenseManager.editExpenseListView(authorId, page);
                }
            }

            e.getHook().editOriginalEmbeds(data.getEmbeds())
                    .setComponents(data.getComponents())
                    .queue();
        }  else if (prefix.startsWith("debt-")) {
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
                    data = ExpenseManager.editDebtListView(authorId, newPage);
                }
                case "debt-list-zoom" -> {
                    int index = Integer.parseInt(parts[2]);
                    data = ExpenseManager.editDebtDetailView(authorId, index, e.getJDA());
                }
                case "debt-detail-prev", "debt-detail-next" -> {
                    int currentIndex = Integer.parseInt(parts[2]);
                    int newIndex = prefix.equals("debt-detail-next") ? currentIndex + 1 : currentIndex - 1;
                    data = ExpenseManager.editDebtDetailView(authorId, newIndex, e.getJDA());
                }
                case "debt-detail-back" -> {
                    int page = Integer.parseInt(parts[2]);
                    data = ExpenseManager.editDebtListView(authorId, page);
                }
            }

            e.getHook().editOriginalEmbeds(data.getEmbeds())
                    .setComponents(data.getComponents())
                    .queue();
        }
    }

}
