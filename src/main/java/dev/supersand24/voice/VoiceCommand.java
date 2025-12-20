package dev.supersand24.voice;

import dev.supersand24.ArisannaBot;
import dev.supersand24.ICommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.managers.channel.concrete.VoiceChannelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

import static dev.supersand24.voice.VoiceManager.*;

public class VoiceCommand implements ICommand {

    private final Logger log = LoggerFactory.getLogger(VoiceCommand.class);

    @Override
    public String getName() { return "vc"; }

    @Override
    public CommandData getCommandData() { return null; }

    @Override
    public void handleSlashCommand(SlashCommandInteractionEvent e) {

    }

    @Override
    public void handleButtonInteraction(ButtonInteractionEvent e) {
        String[] parts = e.getComponentId().split(":");

        String prefix = parts[1];

        switch (prefix) {
            case "hideChannel" -> {
                Long channelId = Long.parseLong(parts[2]);

                AriVoiceChannel ariVC = channels.get(channelId);
                if (ariVC == null) {
                    log.error("I can't find {}.", channelId);
                    return;
                }

                hideChannel(ariVC);
                e.replyComponents(Container.of(
                        TextDisplay.of("### Visibility Setting"),
                        Separator.createDivider(Separator.Spacing.SMALL),
                        TextDisplay.of("Select who you want the channel to be visible to."),
                        ActionRow.of(
                                EntitySelectMenu.create("vc:hideChannelUserSelected:" + channelId, EntitySelectMenu.SelectTarget.USER)
                                        //.setDefaultValues(EntitySelectMenu.DefaultValue.user(e.getMember().getId()))
                                        .setRequiredRange(0, 10)
                                        .setPlaceholder("Select any Users here.")
                                        .build()
                        ),
                        ActionRow.of(
                                EntitySelectMenu.create("vc:hideChannelRoleSelected:" + channelId, EntitySelectMenu.SelectTarget.ROLE)
                                        .setPlaceholder("Select any Roles here.")
                                        .setRequiredRange(0, 10)
                                        .build()
                        ),
                        ActionRow.of(
                                Button.danger("vc:resetHideChannelPermissions:" + channelId, "Reset Visibility")
                        )
                )).useComponentsV2().setEphemeral(true).queue(message -> ariVC.updateControlPanel());

            }
            case "showChannel" -> {
                Long channelId = Long.parseLong(parts[2]);

                AriVoiceChannel ariVC = channels.get(channelId);
                if (ariVC == null) {
                    log.error("I can't find {}.", channelId);
                    return;
                }

                showChannel(ariVC);
                e.reply("Channel revealed " + ArisannaBot.emojiHeartArisanna.getAsMention()).setEphemeral(true).queue(message -> ariVC.updateControlPanel());
            }
            case "sendVisibility" -> {
                Long channelId = Long.parseLong(parts[2]);

                AriVoiceChannel ariVC = channels.get(channelId);
                if (ariVC == null) {
                    log.error("I can't find {}.", channelId);
                    return;
                }

                e.replyComponents(Container.of(
                        TextDisplay.of("### Visibility Setting"),
                        Separator.createDivider(Separator.Spacing.SMALL),
                        TextDisplay.of("Select who you want the channel to be visible to."),
                        ActionRow.of(
                                EntitySelectMenu.create("vc:hideChannelUserSelected:" + channelId, EntitySelectMenu.SelectTarget.USER)
                                        //.setDefaultValues(EntitySelectMenu.DefaultValue.user(e.getMember().getId()))
                                        .setRequiredRange(0, 10)
                                        .setPlaceholder("Select any Users here.")
                                        .build()
                        ),
                        ActionRow.of(
                                EntitySelectMenu.create("vc:hideChannelRoleSelected:" + channelId, EntitySelectMenu.SelectTarget.ROLE)
                                        .setPlaceholder("Select any Roles here.")
                                        .setRequiredRange(0, 10)
                                        .build()
                        ),
                        ActionRow.of(
                                Button.danger("vc:resetHideChannelPermissions:" + channelId, "Reset Visibility")
                        )
                )).useComponentsV2().setEphemeral(true).queue();
            }
            case "resetHideChannelPermissions" -> {
                Long channelId = Long.parseLong(parts[2]);

                AriVoiceChannel ariVC = channels.get(channelId);
                if (ariVC == null) {
                    log.error("I can't find {}.", channelId);
                    return;
                }

                e.deferReply().setEphemeral(true).queue();

                VoiceChannelManager manager = ariVC.getVoiceChannel().getManager();
                List<Member> membersInVC = ariVC.getVoiceChannel().getMembers();

                long selfId = e.getJDA().getSelfUser().getIdLong();

                if (canManagePermission(ariVC.getVoiceChannel())) {
                    for (PermissionOverride override : ariVC.getVoiceChannel().getPermissionOverrides()) {
                        IPermissionHolder holder = override.getPermissionHolder();
                        if (holder == null) continue;

                        if (holder instanceof Member && membersInVC.contains(holder)) continue;
                        if (holder instanceof Member && holder.getIdLong() == selfId) continue;
                        if (holder instanceof Role && ((Role) holder).getTags().isBot()) continue;

                        manager = manager.putPermissionOverride(
                                holder,
                                EnumSet.noneOf(Permission.class),
                                EnumSet.of(Permission.VIEW_CHANNEL)
                        );
                    }

                    manager.queue(success -> {
                        ariVC.updateControlPanel();
                        e.getHook().sendMessage("Reset Visibility").queue();
                    });
                } else {
                    e.getHook().sendMessage("I couldn't reset the Visibility").queue();
                }

            }
            default -> {
                log.warn("Unhandled button prefix: {}", prefix);
                e.reply("Something went wrong!").setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void handleStringSelectInteraction(StringSelectInteractionEvent e) {

    }

    @Override
    public void handleEntitySelectInteraction(EntitySelectInteractionEvent e) {

        String[] parts = e.getComponentId().split(":");
        if (!parts[0].equals("vc")) return;

        String prefix = parts[1];

        if (prefix.equals("hideChannelUserSelected")) {
            Long channelId = Long.parseLong(parts[2]);

            AriVoiceChannel ariVC = channels.get(channelId);
            if (ariVC == null) { log.error("I can't find {}.", channelId); return; }

            e.deferReply().setEphemeral(true).queue();

            VoiceChannelManager manager = ariVC.getVoiceChannel().getManager();

            for (Member member : e.getMentions().getMembers()) {
                manager = manager.putMemberPermissionOverride(
                        member.getIdLong(),
                        EnumSet.of(Permission.VIEW_CHANNEL),
                        EnumSet.noneOf(Permission.class)
                );
            }

            manager.queue( success -> {
                e.getHook().sendMessage("Added " + e.getMentions().getMembers().size() + " users to the list.").queue();
                ariVC.updateControlPanel();
            });
        } else if (prefix.equals("hideChannelRoleSelected")) {
            Long channelId = Long.parseLong(parts[2]);

            AriVoiceChannel ariVC = channels.get(channelId);
            if (ariVC == null) { log.error("I can't find {}.", channelId); return; }

            e.deferReply().setEphemeral(true).queue();

            VoiceChannelManager manager = ariVC.getVoiceChannel().getManager();

            for (Role role : e.getMentions().getRoles()) {
                manager = manager.putRolePermissionOverride(
                        role.getIdLong(),
                        EnumSet.of(Permission.VIEW_CHANNEL),
                        EnumSet.noneOf(Permission.class)
                );
            }

            manager.queue( success -> {
                e.getHook().sendMessage("Added " + e.getMentions().getRoles().size() + " roles to the list.").queue();
                ariVC.updateControlPanel();
            });
        } else {
            e.reply("Something went wrong!").setEphemeral(true).queue();
        }

    }

    @Override
    public void handleModalInteraction(ModalInteractionEvent e) {

    }
}
