package dev.supersand24.voice;

import dev.supersand24.ArisannaBot;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.StageChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.channel.ChannelManager;
import net.dv8tion.jda.api.managers.channel.concrete.VoiceChannelManager;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class VoiceManager extends ListenerAdapter {

    protected static final Logger log = LoggerFactory.getLogger(VoiceManager.class);

    protected final static Hashtable<Long, AriVoiceChannel> channels = new Hashtable<>();

    private final HashMap<Long, Long> AUTO_VOICE_NEW_CHANNEL_ID = new HashMap<>();

    @Override
    public void onReady(@NotNull ReadyEvent e) {
        for (Guild guild : e.getJDA().getGuilds()) {
            log.info(guild.getName());
            for (StageChannel stageChannel : guild.getStageChannels())
                if (stageChannel.getName().equals("New Channel")) AUTO_VOICE_NEW_CHANNEL_ID.put(guild.getIdLong(), stageChannel.getIdLong());

            for (VoiceChannel vc : guild.getVoiceChannels()) {
                log.info(vc.getName());
                if (guild.getAfkChannel() != null)
                    if (vc.getIdLong() == guild.getAfkChannel().getIdLong()) continue;

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

    @Override
    public void onButtonInteraction(ButtonInteractionEvent e) {
        String[] parts = e.getComponentId().split(":");
        if (!parts[0].equals("vc")) return;

        String prefix = parts[1];

        if (prefix.equals("hideChannel")) {
            Long channelId = Long.parseLong(parts[2]);

            AriVoiceChannel ariVC = channels.get(channelId);
            if (ariVC == null) { log.error("I can't find {}.", channelId); return; }

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

        } else if (prefix.equals("showChannel")) {
            Long channelId = Long.parseLong(parts[2]);

            AriVoiceChannel ariVC = channels.get(channelId);
            if (ariVC == null) { log.error("I can't find {}.", channelId); return; }

            showChannel(ariVC);
            e.reply("Channel revealed " + ArisannaBot.emojiHeartArisanna.getAsMention()).setEphemeral(true).queue(message -> ariVC.updateControlPanel());
        } else if (prefix.equals("sendVisibility")) {
            Long channelId = Long.parseLong(parts[2]);

            AriVoiceChannel ariVC = channels.get(channelId);
            if (ariVC == null) { log.error("I can't find {}.", channelId); return; }

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
        } else if (prefix.equals("resetHideChannelPermissions")) {
            Long channelId = Long.parseLong(parts[2]);

            AriVoiceChannel ariVC = channels.get(channelId);
            if (ariVC == null) { log.error("I can't find {}.", channelId); return; }

            e.deferReply().setEphemeral(true).queue();

            VoiceChannelManager manager = ariVC.voiceChannel.getManager();

            List<Member> membersInVC = ariVC.getVoiceChannel().getMembers();

            long selfId = e.getJDA().getSelfUser().getIdLong();

            if (canManagePermission(ariVC.voiceChannel)) {
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
    }

    @Override
    public void onEntitySelectInteraction(@NotNull EntitySelectInteractionEvent e) {
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
        }
    }

    private void newChannel(Member member) {
        if (member.getVoiceState() == null) return;
        if (member.getVoiceState().getChannel() == null) return;

        Guild guild = member.getGuild();

        AudioChannelUnion vc = member.getVoiceState().getChannel();
        if (vc.getIdLong() != AUTO_VOICE_NEW_CHANNEL_ID.get(guild.getIdLong())) return;

        if (!canManageChannel(vc) || !canMoveMembers(vc)) return;

        ChannelAction<VoiceChannel> newChannel;
        if (vc.getParentCategory() == null)
            newChannel = guild.createVoiceChannel(member.getEffectiveName() + "'s Channel");
        else
            newChannel = guild.createVoiceChannel(member.getEffectiveName() + "'s Channel", vc.getParentCategory());
        newChannel.setPosition(0).setBitrate(guild.getMaxBitrate()).queue(voiceChannel -> {
            guild.moveVoiceMember(member, voiceChannel).queue();
            log.info("{} created a New Voice Channel in {}.", member.getUser().getName(), guild.getName());

            AriVoiceChannel ariVC = new AriVoiceChannel(voiceChannel);
            ariVC.addChannelAdmin(member);
            channels.put(voiceChannel.getIdLong(), ariVC);

            voiceChannel.getManager().putMemberPermissionOverride(
                    member.getIdLong(),
                    EnumSet.of(Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL),
                    EnumSet.noneOf(Permission.class)
            ).putRolePermissionOverride(
                    guild.getPublicRole().getIdLong(),
                    EnumSet.of(Permission.VIEW_CHANNEL),
                    EnumSet.noneOf(Permission.class)
            ).queue(success -> ariVC.sendControlPanel());
        });
    }

    private void hideChannel(AriVoiceChannel voiceChannel) {
        if (canManageChannel(voiceChannel.voiceChannel)) voiceChannel.voiceChannel.upsertPermissionOverride(voiceChannel.voiceChannel.getGuild().getPublicRole()).deny(Permission.VIEW_CHANNEL).queue();
        else log.error("I can't manage " + voiceChannel.voiceChannel.getName() + " in " + voiceChannel.voiceChannel.getGuild().getName() + ".");
    }

    private void showChannel(AriVoiceChannel voiceChannel) {
        if (canManageChannel(voiceChannel.voiceChannel)) voiceChannel.voiceChannel.upsertPermissionOverride(voiceChannel.voiceChannel.getGuild().getPublicRole()).grant(Permission.VIEW_CHANNEL).queue();
        else log.error("I can't manage " + voiceChannel.voiceChannel.getName() + " in " + voiceChannel.voiceChannel.getGuild().getName() + ".");
    }

    private void deleteChannel(AriVoiceChannel ariVoiceChannel) {
        deleteChannel(ariVoiceChannel.voiceChannel);
    }

    private void deleteChannel(VoiceChannel voiceChannel) {
        if (canManageChannel(voiceChannel)) voiceChannel.delete().queue();
        else log.error("I can't manage " + voiceChannel.getName() + " in " + voiceChannel.getGuild().getName() + ".");
    }

    private boolean isAfkChannel(AudioChannelUnion voiceChannel) {
        if (voiceChannel.getGuild().getAfkChannel() == null) return false;
        return voiceChannel.getGuild().getAfkChannel().getIdLong() == voiceChannel.getIdLong();
    }

    private boolean canSendMessage(AudioChannelUnion voiceChannel) {
        return voiceChannel.getGuild().getSelfMember().hasPermission(voiceChannel, Permission.MESSAGE_SEND);
    }

    private boolean canManageChannel(AudioChannelUnion voiceChannel) {
        return voiceChannel.getGuild().getSelfMember().hasPermission(voiceChannel, Permission.MANAGE_CHANNEL);
    }

    private boolean canManageChannel(VoiceChannel voiceChannel) {
        return voiceChannel.getGuild().getSelfMember().hasPermission(voiceChannel, Permission.MANAGE_CHANNEL);
    }

    private boolean canManagePermission(VoiceChannel voiceChannel) {
        return voiceChannel.getGuild().getSelfMember().hasPermission(voiceChannel, Permission.MANAGE_PERMISSIONS);
    }

    private boolean canMoveMembers(AudioChannelUnion voiceChannel) {
        return voiceChannel.getGuild().getSelfMember().hasPermission(voiceChannel, Permission.VOICE_MOVE_OTHERS);
    }

}
