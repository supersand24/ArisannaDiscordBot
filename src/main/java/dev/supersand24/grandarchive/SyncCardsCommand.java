package dev.supersand24.grandarchive;

import com.google.gson.*;
import dev.supersand24.DataStore;
import dev.supersand24.GuildSettings;
import dev.supersand24.ICommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class SyncCardsCommand implements ICommand {

    private static final Logger log = LoggerFactory.getLogger(SyncCardsCommand.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public String getName() { return "sync"; }

    @Override
    public CommandData getCommandData() {
        return Commands.slash(getName(), "Download Card Data from the Grand Archive Card Index.")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));
    }

    @Override
    public void handleSlashCommand(SlashCommandInteractionEvent e) {
        if (e.getGuild() == null) return;
        String guildId = e.getGuild().getId();

        GuildSettings settings = DataStore.get(guildId, "settings");
        if (!settings.enabledModules.contains("grand-archive")) {
            e.reply("The Grand Archive module is currently disabled in this server.").setEphemeral(true).queue();
            return;
        }

        boolean isOwner = e.getMember().isOwner();
        boolean isAdmin = e.getMember().hasPermission(Permission.MANAGE_SERVER);

        if (!isOwner && !isAdmin) {
            e.reply("❌ Only Server Admins can use this command.").setEphemeral(true).queue();
            return;
        }

        e.deferReply().setEphemeral(true).queue();
        log.info("User {} initiated a Grand Archive card sync in guild {}.", e.getUser().getName(), e.getGuild().getName());

        CompletableFuture.runAsync(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                int page = 1;
                boolean hasMore = true;

                JsonArray allCards = new JsonArray();

                while (hasMore) {
                    String url = "https://api.gatcg.com/cards/search?page=" + page;
                    log.debug("Fetching Grand Archive cards from URL: {}", url);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Accept", "application/json")
                            .GET()
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200){
                        JsonObject pageObject = JsonParser.parseString(response.body()).getAsJsonObject();

                        if (!pageObject.has("data") || !pageObject.get("data").isJsonArray()) {
                            log.info("No more valid array data found at page {}. Ending sync.", page);
                            hasMore = false;
                        } else {
                            JsonArray dataArray = pageObject.getAsJsonArray("data");

                            if (dataArray.isEmpty()) {
                                log.info("Reached the end of the Grand Archive index at page {}.", page);
                                hasMore = false;
                            } else {
                                allCards.addAll(dataArray);
                                log.info("Successfully fetched Grand Archive cards page {}. Added {} cards.", page, dataArray.size());

                                page++;
                                Thread.sleep(100);
                            }
                        }

                    } else {
                        log.warn("Failed to fetch Grand Archive page {}. HTTP Status: {}", page, response.statusCode());
                        e.getHook().sendMessage("❌ Failed to fetch data: HTTP " + response.statusCode()).queue();
                        return;
                    }
                }

                Path filePath = Paths.get("data/cards.json");
                Files.writeString(filePath, gson.toJson(allCards), StandardCharsets.UTF_8);

                String url = "https://api.gatcg.com/option/search";
                log.debug("Fetching Grand Archive options from URL: {}", url);

                HttpRequest optionsRequest = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> optionsResponse = client.send(optionsRequest, HttpResponse.BodyHandlers.ofString());

                if (optionsResponse.statusCode() == 200) {
                    JsonObject optionsObject = JsonParser.parseString(optionsResponse.body()).getAsJsonObject();
                    Path optionsFilePath = Paths.get("data/options.json");

                    Files.writeString(optionsFilePath, gson.toJson(optionsObject), StandardCharsets.UTF_8);
                    log.info("Successfully fetched and saved Grand Archive options to {}", optionsFilePath.toAbsolutePath());
                } else {
                    log.warn("Failed to fetch Grand Archive options. HTTP Status: {}", optionsResponse.statusCode());
                    e.getHook().sendMessage("⚠️ Cards synced, but failed to fetch options data: HTTP " + optionsResponse.statusCode()).queue();
                    return;
                }

                CardCache.loadCache();

                log.info("Grand Archive card sync completed successfully.");
                e.getHook().sendMessage("✅ Successfully synced all cards from the Grand Archive Index!").queue();
            } catch (Exception ex) {
                log.error("An error occurred while syncing Grand Archive cards.", ex);
                e.getHook().sendMessage("❌ An error occurred while syncing cards: " + ex.getMessage()).queue();
            }
        });
    }

}
