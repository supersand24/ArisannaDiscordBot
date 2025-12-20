package dev.supersand24;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import dev.supersand24.counters.CounterCommand;
import dev.supersand24.counters.CounterManager;
import dev.supersand24.events.EventCommand;
import dev.supersand24.events.RolesCommand;
import dev.supersand24.expenses.DebtCommand;
import dev.supersand24.expenses.ExpenseCommand;
import dev.supersand24.expenses.PaymentCommand;
import dev.supersand24.voice.AriVoiceChannel;
import dev.supersand24.voice.VoiceCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.StageChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;

import static dev.supersand24.voice.VoiceManager.*;

public class Listener extends ListenerAdapter {

    private final Logger log = LoggerFactory.getLogger(Listener.class);
    private final Map<String, ICommand> commands = new HashMap<>();

    public Collection<ICommand> getAllCommands() {
        return commands.values();
    }

    @Override
    public void onReady(ReadyEvent e) {
        commands.put("counter", new CounterCommand());
        commands.put("roles", new RolesCommand());
        commands.put("event", new EventCommand());
        commands.put("expense", new ExpenseCommand());
        commands.put("payment", new PaymentCommand());
        commands.put("debt", new DebtCommand());
        commands.put("vc", new VoiceCommand());

        for (Guild guild : e.getJDA().getGuilds()) {
            log.info(guild.getName());
            for (StageChannel stageChannel : guild.getStageChannels())
                if (stageChannel.getName().equals("New Channel")) AUTO_VOICE_NEW_CHANNEL_ID.put(guild.getIdLong(), stageChannel.getIdLong());

            for (VoiceChannel vc : guild.getVoiceChannels()) {
                log.info(vc.getName());

                if (guild.getAfkChannel() != null)
                    if (vc.getIdLong() == guild.getAfkChannel().getIdLong()) continue;

                if (!canViewChannel(vc)) continue;

                if (vc.getMembers().isEmpty()) {
                    deleteChannel(vc);
                    continue;
                }

                AriVoiceChannel ariVC = new AriVoiceChannel(vc);
                for (Member member : vc.getMembers()) {
                    log.info(member.getEffectiveName());
                    PermissionOverride perms = vc.getPermissionOverride(member);
                    if (perms == null) continue;
                    if (perms.getAllowed().contains(Permission.MANAGE_CHANNEL)) {
                        ariVC.addChannelAdmin(member);
                        log.info(member.getUser().getName() + " was made a Channel Admin for " + vc.getName() + " in " + guild.getName() + ".");
                    }
                }

                ariVC.sendControlPanel();
                channels.put(vc.getIdLong(), ariVC);

                System.out.println(ariVC);
            }
        }

        log.info("Listener is Ready.");
    }

    @Override
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
        log.info("{} slash command received.", e.getName());

        ICommand command = commands.get(e.getName());
        if (command != null)
            command.handleSlashCommand(e);
        else
            e.reply("Unknown Command!").setEphemeral(true).queue();
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent e) {
        log.info("{} was selected.", e.getComponentId());

        ICommand command = commands.get(e.getComponentId().split(":")[0]);
        if (command != null)
            command.handleStringSelectInteraction(e);
        else
            e.reply("Something went wrong!").setEphemeral(true).queue();
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
                    "Then, I compare how much you *spent* versus your total *fair share*.\n" +
                            "• If you spent **more** than your share, you are **owed money**.\n" +
                            "• If you spent **less** than your share, you **need to pay** money.",
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

    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent e) {
        Member member = e.getMember();
        AudioChannelUnion channelJoined = e.getChannelJoined();
        AudioChannelUnion channelLeft = e.getChannelLeft();

        if (channelJoined != null) {
            log.info(member.getUser().getName() + " joined " + channelJoined.getName() + ".");
            switch (channelJoined.getType()) {
                case STAGE -> newChannel(member);
                case VOICE -> {
                    VoiceChannel voiceChannel = channelJoined.asVoiceChannel();
                    if (isAfkChannel(channelJoined)) {
                        if (channelLeft != null) {
                            log.info("Went AFK");
                        }
                    }
                    else
                    {
                        if (canSendMessage(channelJoined))
                            voiceChannel.sendMessage(member.getAsMention() + " joined the voice call.")
                                    .setSuppressedNotifications(true)
                                    .setTTS(false)
                                    .mentionUsers("0")
                                    .queue();
                    }
                }
            }
        }

        if (channelLeft == null) return;

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
}
