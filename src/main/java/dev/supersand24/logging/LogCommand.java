package dev.supersand24.logging;

import dev.supersand24.DataStore;
import dev.supersand24.GuildSettings;
import dev.supersand24.ICommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class LogCommand implements ICommand {

    @Override
    public String getName() { return "log"; }

    @Override
    public CommandData getCommandData() {
        return Commands.slash(getName(), "Manage Server Logging settings.")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
                .addSubcommands(
                        new SubcommandData("set-channel", "Sets the channel where bot logs will be sent.")
                                .addOptions(
                                        new OptionData(OptionType.CHANNEL, "channel", "The Text channel to send logs to.", true)
                                                .setChannelTypes(ChannelType.TEXT)
                                ),
                        new SubcommandData("disable", "Disables server logging.")
                );
    }

    @Override
    public void handleSlashCommand(SlashCommandInteractionEvent e) {
        if (e.getGuild() == null) return;
        String guildId = e.getGuild().getId();

        GuildSettings settings = DataStore.get(guildId, "settings");
        if (!settings.enabledModules.contains("logs")) {
            e.reply("The Logging module is currently disabled in this server.").setEphemeral(true).queue();
            return;
        }

        // Admin verification
        boolean isOwner = e.getMember().isOwner();
        boolean isAdmin = e.getMember().hasPermission(Permission.MANAGE_SERVER);

        if (!isOwner && !isAdmin) {
            e.reply("❌ Only Server Admins can use this command.").setEphemeral(true).queue();
            return;
        }

        if (e.getSubcommandName().equals("set-channel")) {
            Channel channel = e.getOption("channel").getAsChannel();

            settings.logChannelId = channel.getIdLong();
            DataStore.markDirty(guildId, "settings");

            e.reply("✅ Server logs will now be sent to " + channel.getAsMention()).queue();

            // Send a test log right away
            ServerLogger.logEvent(e.getGuild(), "Logging Enabled", "This channel is now receiving server logs.", java.awt.Color.GREEN);

        } else if (e.getSubcommandName().equals("disable")) {
            settings.logChannelId = null;
            DataStore.markDirty(guildId, "settings");
            e.reply("✅ Server logging has been disabled.").queue();
        }
    }

}