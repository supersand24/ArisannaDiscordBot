package dev.supersand24.voice;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Hashtable;

public class VoiceManager {

    private static final Logger log = LoggerFactory.getLogger(VoiceManager.class);

    public static final Hashtable<Long, AriVoiceChannel> channels = new Hashtable<>();

    public static final HashMap<Long, Long> AUTO_VOICE_NEW_CHANNEL_ID = new HashMap<>();

    public static void newChannel(Member member) {
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

    public static void hideChannel(AriVoiceChannel voiceChannel) {
        if (canManageChannel(voiceChannel.getVoiceChannel()))
            voiceChannel.getVoiceChannel().upsertPermissionOverride(
                            voiceChannel.getVoiceChannel().getGuild().getPublicRole()).deny(Permission.VIEW_CHANNEL)
                    .queue();
        else log.error("I can't manage " + voiceChannel.getVoiceChannel().getName() + " in " + voiceChannel.getVoiceChannel().getGuild().getName() + ".");
    }

    public static void showChannel(AriVoiceChannel voiceChannel) {
        if (canManageChannel(voiceChannel.getVoiceChannel()))
            voiceChannel.getVoiceChannel().upsertPermissionOverride(
                            voiceChannel.getVoiceChannel().getGuild().getPublicRole()).grant(Permission.VIEW_CHANNEL)
                    .queue();
        else log.error("I can't manage " + voiceChannel.getVoiceChannel().getName() + " in " + voiceChannel.getVoiceChannel().getGuild().getName() + ".");
    }

    public static void deleteChannel(AriVoiceChannel ariVoiceChannel) {
        deleteChannel(ariVoiceChannel.getVoiceChannel());
    }

    public static void deleteChannel(VoiceChannel voiceChannel) {
        if (canManageChannel(voiceChannel)) voiceChannel.delete().queue();
        else log.error("I can't manage " + voiceChannel.getName() + " in " + voiceChannel.getGuild().getName() + ".");
    }

    public static boolean isAfkChannel(AudioChannelUnion voiceChannel) {
        if (voiceChannel.getGuild().getAfkChannel() == null) return false;
        return voiceChannel.getGuild().getAfkChannel().getIdLong() == voiceChannel.getIdLong();
    }

    public static boolean canViewChannel(VoiceChannel voiceChannel) {
        return voiceChannel.getGuild().getSelfMember().hasPermission(voiceChannel, Permission.VIEW_CHANNEL);
    }

    public static boolean canSendMessage(AudioChannelUnion voiceChannel) {
        return voiceChannel.getGuild().getSelfMember().hasPermission(voiceChannel, Permission.MESSAGE_SEND);
    }

    public static boolean canManageChannel(AudioChannelUnion voiceChannel) {
        return voiceChannel.getGuild().getSelfMember().hasPermission(voiceChannel, Permission.MANAGE_CHANNEL);
    }

    public static boolean canManageChannel(VoiceChannel voiceChannel) {
        return voiceChannel.getGuild().getSelfMember().hasPermission(voiceChannel, Permission.MANAGE_CHANNEL);
    }

    public static boolean canManagePermission(VoiceChannel voiceChannel) {
        return voiceChannel.getGuild().getSelfMember().hasPermission(voiceChannel, Permission.MANAGE_PERMISSIONS);
    }

    public static boolean canMoveMembers(AudioChannelUnion voiceChannel) {
        return voiceChannel.getGuild().getSelfMember().hasPermission(voiceChannel, Permission.VOICE_MOVE_OTHERS);
    }

}
