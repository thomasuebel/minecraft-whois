package de.thomasuebel.mc.whois.bootstrap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import de.thomasuebel.mc.whois.model.PlayerStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * V1.1: pre-seeds the player store from the vanilla server's usercache.json
 * so existing UUID↔name pairs are present even for players who have not
 * joined since the plugin was installed. Runs only on first enable (when
 * data.yml is absent) — see SPEC §12.
 */
public final class UserCacheImporter {

    private static final String FIELD_NAME = "name";
    private static final String FIELD_UUID = "uuid";

    private final Path usercacheJson;
    private final PlayerStore store;
    private final Logger logger;

    public UserCacheImporter(Path usercacheJson, PlayerStore store, Logger logger) {
        this.usercacheJson = Objects.requireNonNull(usercacheJson, "usercacheJson");
        this.store = Objects.requireNonNull(store, "store");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public int importIfPresent() {
        if (!Files.exists(usercacheJson)) {
            logger.info("usercache.json not found at " + usercacheJson + "; skipping bootstrap import");
            return 0;
        }
        String json;
        try {
            json = Files.readString(usercacheJson, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Could not read " + usercacheJson, ex);
            return 0;
        }

        JsonArray array;
        try {
            array = JsonParser.parseString(json).getAsJsonArray();
        } catch (JsonSyntaxException | IllegalStateException ex) {
            logger.log(Level.WARNING, "usercache.json is not a JSON array: " + usercacheJson, ex);
            return 0;
        }

        int candidates = 0;
        int imported = 0;
        int skipped = 0;
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                skipped++;
                continue;
            }
            JsonObject obj = element.getAsJsonObject();
            if (!obj.has(FIELD_NAME) || !obj.has(FIELD_UUID)) {
                skipped++;
                continue;
            }
            String name = obj.get(FIELD_NAME).getAsString();
            UUID uuid = tryParseUuid(obj.get(FIELD_UUID).getAsString());
            if (uuid == null) {
                skipped++;
                continue;
            }
            candidates++;
            if (store.recordNick(uuid, name)) {
                imported++;
            }
        }
        logger.info(String.format(
                "usercache.json bootstrap: %d candidates, %d new, %d skipped (from %s)",
                candidates, imported, skipped, usercacheJson));
        return imported;
    }

    private static UUID tryParseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
