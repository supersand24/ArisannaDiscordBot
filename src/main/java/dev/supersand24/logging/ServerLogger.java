package dev.supersand24.logging;

import dev.supersand24.DataStore;
import dev.supersand24.GuildSettings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.time.Instant;

public class ServerLogger {

    private static final Logger log = LoggerFactory.getLogger(ServerLogger.class);

    public static void logEvent(Guild guild, String title, String description, Color color) {
        GuildSettings settings = DataStore.get(guild.getId(), "settings");

        if (settings.logChannelId == null) return; // Logging is disabled for this server

        TextChannel logChannel = guild.getTextChannelById(settings.logChannelId);

        // If the channel was deleted, wipe the setting
        if (logChannel == null) {
            settings.logChannelId = null;
            DataStore.markDirty(guild.getId(), "settings");
            return;
        }

        // Permission check: We need to send messages and embed links!
        if (!guild.getSelfMember().hasPermission(logChannel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
            log.warn("Cannot send logs in '{}'. Missing MESSAGE_SEND or MESSAGE_EMBED_LINKS in the log channel.", guild.getName());
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .setTimestamp(Instant.now());

        logChannel.sendMessageEmbeds(embed.build()).queue(
                success -> {},
                error -> log.error("API Error sending log in '{}': {}", guild.getName(), error.getMessage())
        );
    }
}