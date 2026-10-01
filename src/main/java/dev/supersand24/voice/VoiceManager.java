package dev.supersand24.voice;

import dev.supersand24.DataStore;
import dev.supersand24.GuildSettings;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
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

        GuildSettings settings = DataStore.get(guild.getId(), "settings");

        if (settings.autoVoiceChannelId == null || vc.getIdLong() != settings.autoVoiceChannelId)
            return;

        // Channel Limit Check
        if (guild.getChannels().size() >= 500) {
            log.error("Channel Issue: Cannot create Auto-VC in '{}'. The server has hit Discord's 500 channel limit!", guild.getName());
            return;
        }

        // Category Channel Limit Check
        Category category = vc.getParentCategory();
        if (category != null && category.getChannels().size() >= 50) {
            log.error("Channel Issue: Cannot create Auto-VC in '{}'. The category '{}' has hit Discord's 50 channel limit!", guild.getName(), category.getName());
            return;
        }

        Member self = guild.getSelfMember();

        boolean hasView = self.hasPermission(vc, Permission.VIEW_CHANNEL);
        boolean hasConnect = self.hasPermission(vc, Permission.VOICE_CONNECT);
        boolean hasMove = self.hasPermission(vc, Permission.VOICE_MOVE_OTHERS);

        boolean hasManageChannel = (category != null)
                ? self.hasPermission(category, Permission.MANAGE_CHANNEL)
                : self.hasPermission(Permission.MANAGE_CHANNEL);

        boolean hasManagePerms = (category != null)
                ? self.hasPermission(category, Permission.MANAGE_PERMISSIONS)
                : self.hasPermission(Permission.MANAGE_PERMISSIONS);

        if (!hasView || !hasConnect || !hasMove || !hasManageChannel || !hasManagePerms) {
            log.warn("Permission Issue in '{}': Auto-VC failed! Missing required roles or category overrides.\n" +
                            "   [VIEW_CHANNEL: {}] [VOICE_CONNECT: {}] [VOICE_MOVE_OTHERS: {}] [MANAGE_CHANNEL: {}] [MANAGE_PERMISSIONS: {}]",
                    guild.getName(), hasView, hasConnect, hasMove, hasManageChannel, hasManagePerms);
            return;
        }

        ChannelAction<VoiceChannel> newChannel;
        if (category == null)
            newChannel = guild.createVoiceChannel(member.getEffectiveName() + "'s Channel");
        else
            newChannel = guild.createVoiceChannel(member.getEffectiveName() + "'s Channel", category);

        newChannel.setPosition(0).setBitrate(guild.getMaxBitrate()).queue(voiceChannel -> {

            guild.moveVoiceMember(member, voiceChannel).queue(
                    success -> {},
                    error -> log.error("Failed to move {} to their new channel in '{}': {}", member.getUser().getName(), guild.getName(), error.getMessage())
            );

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
            ).putPermissionOverride(
                    guild.getSelfMember(),
                    EnumSet.of(
                            Permission.VIEW_CHANNEL,
                            Permission.MANAGE_CHANNEL,
                            Permission.MANAGE_PERMISSIONS,
                            Permission.VOICE_CONNECT,
                            Permission.VOICE_MOVE_OTHERS
                    ),
                    EnumSet.noneOf(Permission.class)
            ).queue(
                    success -> ariVC.sendControlPanel(),
                    error -> log.error("Failed to set permission overrides for new channel in '{}': {}", guild.getName(), error.getMessage())
            );

        }, error -> log.error("Failed to create the voice channel in '{}': {}", guild.getName(), error.getMessage()));
    }

    public static void hideChannel(AriVoiceChannel voiceChannel) {
        Guild guild = voiceChannel.getVoiceChannel().getGuild();
        if (canManageChannel(voiceChannel.getVoiceChannel())) {
            voiceChannel.getVoiceChannel().upsertPermissionOverride(guild.getPublicRole())
                    .deny(Permission.VIEW_CHANNEL)
                    .queue(
                            success -> log.info("Hid channel {} in {}", voiceChannel.getVoiceChannel().getName(), guild.getName()),
                            error -> log.error("API Error hiding channel in '{}': {}", guild.getName(), error.getMessage())
                    );
        } else {
            log.warn("I lack MANAGE_CHANNEL permission to hide '{}' in '{}'.", voiceChannel.getVoiceChannel().getName(), guild.getName());
        }
    }

    public static void showChannel(AriVoiceChannel voiceChannel) {
        Guild guild = voiceChannel.getVoiceChannel().getGuild();
        if (canManageChannel(voiceChannel.getVoiceChannel())) {
            voiceChannel.getVoiceChannel().upsertPermissionOverride(guild.getPublicRole())
                    .grant(Permission.VIEW_CHANNEL)
                    .queue(
                            success -> log.info("Revealed channel {} in {}", voiceChannel.getVoiceChannel().getName(), guild.getName()),
                            error -> log.error("API Error revealing channel in '{}': {}", guild.getName(), error.getMessage())
                    );
        } else {
            log.warn("I lack MANAGE_CHANNEL permission to show '{}' in '{}'.", voiceChannel.getVoiceChannel().getName(), guild.getName());
        }
    }

    public static void deleteChannel(AriVoiceChannel ariVoiceChannel) {
        deleteChannel(ariVoiceChannel.getVoiceChannel());
    }

    public static void deleteChannel(VoiceChannel voiceChannel) {
        Guild guild = voiceChannel.getGuild();
        if (canManageChannel(voiceChannel)) {
            voiceChannel.delete().queue(
                    success -> log.info("Deleted empty auto-channel in {}", guild.getName()),
                    error -> log.error("API Error deleting channel in '{}': {}", guild.getName(), error.getMessage())
            );
        } else {
            log.warn("I lack MANAGE_CHANNEL permission to delete '{}' in '{}'. It will remain stuck.", voiceChannel.getName(), guild.getName());
        }
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

    public static boolean canManageChannel(VoiceChannel voiceChannel) {
        return voiceChannel.getGuild().getSelfMember().hasPermission(voiceChannel, Permission.MANAGE_CHANNEL);
    }

    public static boolean canManagePermission(VoiceChannel voiceChannel) {
        return voiceChannel.getGuild().getSelfMember().hasPermission(voiceChannel, Permission.MANAGE_PERMISSIONS);
    }

}
