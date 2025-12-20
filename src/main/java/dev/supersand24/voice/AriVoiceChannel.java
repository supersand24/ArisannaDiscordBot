package dev.supersand24.voice;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AriVoiceChannel {

    final VoiceChannel voiceChannel;

    Member creator;

    Message controlPanel;

    final List<Member> channelAdmins = new ArrayList<>();

    public AriVoiceChannel(VoiceChannel voiceChannel) {
        this.voiceChannel = voiceChannel;
    }

    public VoiceChannel getVoiceChannel() { return voiceChannel; }

    public void addChannelAdmin(Member member) {
        channelAdmins.add(member);
        updateControlPanel();
    }
    public void removeChannelAdmin(Member member) {
        channelAdmins.remove(member);
        updateControlPanel();
    }
    public List<Member> getChannelAdmins() {
        return channelAdmins;
    }
    public List<Member> getMembers() {
        return getVoiceChannel().getMembers();
    }

    public void sendControlPanel() {
        voiceChannel.sendMessageComponents(createControlPanel())
                .setSuppressedNotifications(true)
                .mentionUsers("0")
                .useComponentsV2().queue(this::setControlPanel);
    }

    private void setControlPanel(Message message) { this.controlPanel = message; }

    public void updateControlPanel() {
        if (controlPanel == null) return;
        controlPanel.editMessageComponents(createControlPanel()).useComponentsV2().queue();
    }

    private Container createControlPanel() {
        PermissionOverride override = voiceChannel.getPermissionOverride(voiceChannel.getGuild().getPublicRole());
        boolean isCurrentlyHidden = (override != null && override.getDenied().contains(Permission.VIEW_CHANNEL));
        Button hideChannelButton = Button.primary("vc:hideChannel:" + voiceChannel.getId(), "Hide")
                .withDisabled(isCurrentlyHidden);
        Button showChannelButton = Button.danger("vc:showChannel:" + voiceChannel.getId(), "Show")
                .withDisabled(!isCurrentlyHidden);
        Button sendVisibilitySetting = Button.secondary("vc:sendVisibility:" + voiceChannel.getId(), "Settings")
                .withDisabled(!isCurrentlyHidden);

        String visibleToText = isCurrentlyHidden
                ? getFormattedVisibilityDetails()
                : "-# Channel is currently visible to everyone.";

        return Container.of(
                TextDisplay.of("## Voice Controls"),
                Separator.createDivider(Separator.Spacing.SMALL),
                TextDisplay.of("### Channel Visibility"),
                ActionRow.of( hideChannelButton, showChannelButton, sendVisibilitySetting ),
                TextDisplay.of(visibleToText)
        );
    }

    private String getFormattedVisibilityDetails() {
        List<Role> topVisibleRoles = voiceChannel.getPermissionOverrides().stream()
                .filter(PermissionOverride::isRoleOverride)
                .filter(po -> po.getAllowed().contains(Permission.VIEW_CHANNEL))
                .map(PermissionOverride::getRole)
                .filter(role -> role != null && !role.isPublicRole())
                .filter(role -> !role.getTags().isBot())
                .limit(3)
                .toList();

        List<Member> allVisibleMembers = voiceChannel.getGuild().getMembers().stream()
                .filter(member -> !member.getUser().isBot())
                .filter(member -> member.hasPermission(voiceChannel, Permission.VIEW_CHANNEL))
                .toList();

        List<Member> membersVisibleForOtherReasons = allVisibleMembers.stream()
                .filter(member -> Collections.disjoint(member.getRoles(), topVisibleRoles))
                .toList();

        StringBuilder details = new StringBuilder();

        if (!topVisibleRoles.isEmpty()) {
            details.append("**Visible to Roles:**\n");
            String roleList = topVisibleRoles.stream()
                    .map(Role::getAsMention)
                    .collect(Collectors.joining("\n- ", "- ", ""));
            details.append(roleList);
        }

        if (!membersVisibleForOtherReasons.isEmpty()) {
            if (!details.isEmpty()) {
                details.append("\n\n");
            }
            details.append("**Visible to Members:**\n");

            final int displayLimit = 10;
            String memberList = membersVisibleForOtherReasons.stream()
                    .limit(displayLimit)
                    .map(Member::getAsMention)
                    .collect(Collectors.joining("\n- ", "- ", ""));
            details.append(memberList);

            if (membersVisibleForOtherReasons.size() > displayLimit) {
                details.append("\n... and ").append(membersVisibleForOtherReasons.size() - displayLimit).append(" more.");
            }
        }

        if (details.isEmpty()) {
            return "This channel is hidden and no specific roles or members have view access.";
        }

        return details.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("--------------------------------------\n");
        sb.append(voiceChannel.getName()).append(" | ").append(voiceChannel.getIdLong()).append("\n");
        sb.append("--------------------------------------\n");
        sb.append("Creator : ");
        sb.append(creator == null ? "None" : creator.getEffectiveName());
        sb.append(" | Admins:").append(channelAdmins.stream().map(Member::getEffectiveName).toList()).append("\n");
        sb.append("Voice Channel Members");
        for (Member member : voiceChannel.getMembers()) {
            sb.append("\n   ").append(member.getEffectiveName()).append(" -> ").append(member.getActivities().stream().map(Activity::getName).collect(Collectors.toList()));
        }
        return sb.toString();
    }

}
