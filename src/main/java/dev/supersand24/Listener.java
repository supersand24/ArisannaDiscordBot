package dev.supersand24;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import dev.supersand24.economy.cardStore.CardStoreCommand;
import dev.supersand24.counters.CounterCommand;
import dev.supersand24.counters.CounterManager;
import dev.supersand24.economy.events.EventCommand;
import dev.supersand24.economy.events.RolesCommand;
import dev.supersand24.economy.expenses.DebtCommand;
import dev.supersand24.economy.expenses.ExpenseCommand;
import dev.supersand24.economy.expenses.PaymentCommand;
import dev.supersand24.logging.ServerLogger;
import dev.supersand24.autovoice.voice.AriVoiceChannel;
import dev.supersand24.autovoice.voice.VoiceCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;

import static dev.supersand24.autovoice.voice.VoiceManager.*;

public class Listener extends ListenerAdapter {

    private final Logger log = LoggerFactory.getLogger(Listener.class);
    private final Map<String, ICommand> commands = new HashMap<>();

    public Listener() {
        /*
        commands.put("counter", new CounterCommand());
        commands.put("roles", new RolesCommand());
        commands.put("event", new EventCommand());
        commands.put("expense", new ExpenseCommand());
        commands.put("payment", new PaymentCommand());
        commands.put("debt", new DebtCommand());
        commands.put("vc", new VoiceCommand());
        commands.put("card-store", new CardStoreCommand());
         */
    }

    @Override
    public void onReady(ReadyEvent e) {
        for (Guild guild : e.getJDA().getGuilds()) {
            log.info("Loading server: {}", guild.getName());

            GuildSettings settings = DataStore.get(guild.getId(), "settings");

            for (VoiceChannel vc : guild.getVoiceChannels()) {

                // Protect the AFK channel
                if (guild.getAfkChannel() != null && vc.getIdLong() == guild.getAfkChannel().getIdLong()) continue;

                // Protect the Generator channel
                if (settings.autoVoiceChannelId != null && vc.getIdLong() == settings.autoVoiceChannelId) continue;

                if (!canViewChannel(vc)) continue;

                // Only manage channels the bot actually created!
                PermissionOverride botOverride = vc.getPermissionOverride(guild.getSelfMember());
                if (botOverride == null) continue;

                // Clean up orphaned auto-channels that are empty.
                if (vc.getMembers().isEmpty()) {
                    log.info("Cleaning up orphaned auto-channel: {}", vc.getName());
                    deleteChannel(vc);
                    continue;
                }

                // Re-initialize active auto-channels
                AriVoiceChannel ariVC = new AriVoiceChannel(vc);
                for (Member member : vc.getMembers()) {
                    PermissionOverride perms = vc.getPermissionOverride(member);
                    if (perms == null) continue;
                    if (perms.getAllowed().contains(Permission.MANAGE_CHANNEL)) {
                        ariVC.addChannelAdmin(member);
                        log.info("{} was made a Channel Admin for {} in {}.", member.getUser().getName(), vc.getName(), guild.getName());
                    }
                }

                ariVC.findOrSendControlPanel();
                channels.put(vc.getIdLong(), ariVC);
            }
        }

        log.info("Listener is Ready.");
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        updateGuildCommands(event.getGuild());
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        updateGuildCommands(event.getGuild());
    }

    public void updateGuildCommands(Guild guild) {
        GuildSettings settings = DataStore.get(guild.getId(), "settings");
        List<CommandData> activeCommands = new ArrayList<>();

        for (IModule module : ModuleManager.MODULES) {
            if (settings.enabledModules.contains(module.getName())) {
                for (ICommand cmd : module.getCommands()) {
                    if (cmd.getCommandData() != null) {
                        activeCommands.add(cmd.getCommandData());
                    }
                }
            }
        }

        guild.updateCommands().addCommands(activeCommands).queue(
                success -> log.info("Successfully loaded {} commands for server: {}", activeCommands.size(), guild.getName()),
                error -> log.error("Failed to load commands for server: {}", guild.getName(), error)
        );
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent e) {
        if (e.getGuild() == null) return;

        ICommand command = ModuleManager.getCommand(e.getName());
        if (command == null) {
            log.error("Unknown Auto Complete Received. {}", e.getName());
            return;
        }

        command.handleAutoComplete(e);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
        if (e.getGuild() == null) return;

        log.info("{} slash command received.", e.getName());

        ICommand command = ModuleManager.getCommand(e.getName());
        if (command == null) {
            log.error("Unknown Command Received! " + e.getName());
            e.reply("Unknown Command!").setEphemeral(true).queue();
            return;
        }

        IModule module = ModuleManager.getModuleForCommand(e.getName());
        GuildSettings settings = DataStore.get(e.getGuild().getId(), "settings");

        if (!settings.enabledModules.contains(module.getName())) {
            e.reply("❌ The **" + module.getName() + "** module is currently disabled.").setEphemeral(true).queue();
            return;
        }

        command.handleSlashCommand(e);
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent e) {
        log.info("{} was selected.", e.getComponentId());

        String[] split = e.getComponentId().split("/");

        ICommand command = ModuleManager.getCommand(split[0]);
        if (command == null) {
            log.error("Unknown Command Received! " + split[0]);
            e.reply("Unknown Command!").setEphemeral(true).queue();
            return;
        }

        IModule module = ModuleManager.getModuleForCommand(split[0]);
        GuildSettings settings = DataStore.get(e.getGuild().getId(), "settings");

        if (!settings.enabledModules.contains(module.getName())) {
            e.reply("❌ The **" + module.getName() + "** module is currently disabled.").setEphemeral(true).queue();
            return;
        }

        command.handleStringSelectInteraction(e);
    }

    @Override
    public void onEntitySelectInteraction(EntitySelectInteractionEvent e) {
        log.info("{} was selected.", e.getComponentId());

        ICommand command = commands.get(e.getComponentId().split(":")[0]);
        if (command != null)
            command.handleEntitySelectInteraction(e);
        else
            e.reply("Something went wrong!").setEphemeral(true).queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent e) {
        log.info("{} was pressed.", e.getComponentId());

        String prefix = e.getComponentId().split(":")[0];
        ICommand command = commands.get(prefix);

        if (command != null) {
            command.handleButtonInteraction(e);
        } else if (prefix.equals("settleup-explain")) {
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
                    """
                            Then, I compare how much you *spent* versus your total *fair share*.
                            • If you spent **more** than your share, you are **owed money**.
                            • If you spent **less** than your share, you **need to pay** money.""",
                    false);

            embed.addField("Simplifying the Payments ➡️",
                    "So, instead of a messy web of payments, I figure out the simplest way to get everyone even. I tell people who they need to pay and exactly how much, minimizing the number of payments required until all debts are cleared.",
                    false);

            e.getHook().sendMessageEmbeds(embed.build()).queue();
        } else
            e.reply("Something went wrong!").setEphemeral(true).queue();
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent e) {
        log.info("{} modal was submitted.", e.getModalId());

        ICommand command = commands.get(e.getModalId().split(":")[0]);
        if (command != null)
            command.handleModalInteraction(e);
        else
            e.reply("Something went wrong!").setEphemeral(true).queue();
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent e) {
        Member member = e.getMember();
        AudioChannelUnion channelJoined = e.getChannelJoined();
        AudioChannelUnion channelLeft = e.getChannelLeft();
        String guildId = member.getGuild().getId();

        GuildSettings settings = DataStore.get(guildId, "settings");

        if (!settings.enabledModules.contains("auto-vc")) {
            return;
        }

        if (channelJoined != null) {
            log.info(member.getUser().getName() + " joined " + channelJoined.getName() + ".");

            if (settings.autoVoiceChannelId != null && channelJoined.getIdLong() == settings.autoVoiceChannelId){
                newChannel(member);
            }
            else if (channelJoined.getType() == ChannelType.VOICE) {
                VoiceChannel voiceChannel = channelJoined.asVoiceChannel();
                if (isAfkChannel(channelJoined)) {
                    if (channelLeft != null)
                        log.info("Went AFK");
                } else {
                    if (canSendMessage(channelJoined))
                        voiceChannel.sendMessage(member.getAsMention() + " joined the voice call.")
                                .setSuppressedNotifications(true)
                                .setTTS(false)
                                .mentionUsers("0")
                                .queue();
                }
            }
        }

        if (channelLeft == null) return;

        if (settings.autoVoiceChannelId != null && channelLeft.getIdLong() == settings.autoVoiceChannelId)
            return;

        if (channelLeft.getType() == ChannelType.VOICE) {
            VoiceChannel voiceChannel = channelLeft.asVoiceChannel();
            if (isAfkChannel(channelLeft)) return;
            log.info(member.getUser().getName() + " left " + channelLeft.getName() + ".");
            if (canSendMessage(channelLeft)) {
                voiceChannel.sendMessage(member.getAsMention() + " left the voice call.")
                        .setSuppressedNotifications(true)
                        .setTTS(false)
                        .mentionUsers("0")
                        .queue(message -> {
                            if (channelLeft.getMembers().isEmpty()) {
                                deleteChannel(voiceChannel);
                            }
                        });
            } else {
                if (!voiceChannel.getMembers().isEmpty()) return;
                if (channelLeft.getMembers().isEmpty())
                    deleteChannel(voiceChannel);
            }
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent e) {
        GuildSettings settings = DataStore.get(e.getGuild().getId(), "settings");
        if (!settings.enabledModules.contains("logs")) return; // Module check

        ServerLogger.logEvent(
                e.getGuild(),
                "Member Joined",
                e.getMember().getAsMention() + " (`" + e.getUser().getName() + "`) has joined the server.\n" +
                        "Account created: <t:" + e.getUser().getTimeCreated().toEpochSecond() + ":R>",
                java.awt.Color.GREEN
        );
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent e) {
        GuildSettings settings = DataStore.get(e.getGuild().getId(), "settings");
        if (!settings.enabledModules.contains("logs")) return; // Module check

        ServerLogger.logEvent(
                e.getGuild(),
                "Member Left",
                "**" + e.getUser().getName() + "** has left the server.",
                java.awt.Color.RED
        );
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        if (e.isFromGuild()) return;
        if (e.getAuthor().isBot()) return;

        String OWNER_ID = "262982533157879810";
        if (!e.getAuthor().getId().equals(OWNER_ID)) return;

        String[] args = e.getMessage().getContentRaw().split("\\s+");
        if (args.length == 0) return;

        if (args[0].equalsIgnoreCase("!module")) {

            // Command: !module available (Lists every command the bot knows)
            if (args.length == 2 && args[1].equalsIgnoreCase("available")) {
                String allModules = ModuleManager.MODULES.stream().map(IModule::getName).collect(Collectors.joining(", "));
                e.getChannel().sendMessage("📦 **All Available Bot Modules:**\n`" + allModules + "`").queue();
                return;
            }

            // Command: !module list <guildId> (Lists what is enabled in a specific server)
            if (args.length == 3 && args[1].equalsIgnoreCase("list")) {
                String guildId = args[2];
                Guild guild = e.getJDA().getGuildById(guildId);
                if (guild == null) {
                    e.getChannel().sendMessage("❌ I am not currently in a server with ID: `" + guildId + "`").queue();
                    return;
                }

                GuildSettings settings = DataStore.get(guildId, "settings");
                String enabled = settings.enabledModules.isEmpty() ? "None" : String.join(", ", settings.enabledModules);
                e.getChannel().sendMessage("📜 **Enabled Modules in " + guild.getName() + ":**\n`" + enabled + "`").queue();
                return;
            }

            // Command: !module <guildId> <add/remove> <commandName>
            if (args.length == 4) {
                String guildId = args[1];
                String action = args[2].toLowerCase(); // "add" or "remove"
                String moduleName = args[3].toLowerCase();

                Guild guild = e.getJDA().getGuildById(guildId);
                if (guild == null) {
                    e.getChannel().sendMessage("❌ I am not currently in a server with ID: `" + guildId + "`").queue();
                    return;
                }

                if (!ModuleManager.MODULES.stream().anyMatch(m -> m.getName().equalsIgnoreCase(moduleName))){
                    e.getChannel().sendMessage("❌ Unknown command module: `" + moduleName + "`\nType `!module available` to see a list.").queue();
                    return;
                }

                GuildSettings settings = DataStore.get(guildId, "settings");

                if (action.equals("add")) {
                    if (!settings.enabledModules.contains(moduleName)) {
                        settings.enabledModules.add(moduleName);
                        DataStore.markDirty(guildId, "settings");
                        updateGuildCommands(guild);
                        e.getChannel().sendMessage("✅ Added `" + moduleName + "` to **" + guild.getName() + "**").queue();
                    } else {
                        e.getChannel().sendMessage("⚠️ `" + moduleName + "` is already enabled there.").queue();
                    }
                } else if (action.equals("remove")) {
                    if (settings.enabledModules.contains(moduleName)) {
                        settings.enabledModules.remove(moduleName);
                        DataStore.markDirty(guildId, "settings");
                        updateGuildCommands(guild);
                        e.getChannel().sendMessage("✅ Removed `" + moduleName + "` from **" + guild.getName() + "**").queue();
                    } else {
                        e.getChannel().sendMessage("⚠️ `" + moduleName + "` is not enabled there.").queue();
                    }
                } else {
                    e.getChannel().sendMessage("❌ Invalid action. Use `add` or `remove`.").queue();
                }
                return;
            }

        }

        // Help menu if they type an incomplete command
        e.getChannel().sendMessage("""
                🛠️ **Admin Panel Usage:**
                - `!module available` *(See all modules in the code)*
                - `!module list <guildId>` *(See what a server has enabled)*
                - `!module <guildId> add <module>` *(Turn on a module)*
                - `!module <guildId> remove <module>` *(Turn off a module)*
                """).queue();
    }
}
