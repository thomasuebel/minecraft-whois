package de.thomasuebel.mc.whois.bootstrap;

import de.thomasuebel.mc.whois.model.PlayerStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * V1.1: pre-seeds the player store from the vanilla server's usercache.json
 * so existing UUID↔name pairs are present even for players who have not
 * joined since the plugin was installed. Runs only on first enable (when
 * data.yml is absent) — see SPEC §12.
 */
public final class UserCacheImporter {

    // Matches the {"name":"...","uuid":"...","expiresOn":"..."} entries the vanilla
    // server writes. Tolerant to whitespace and to entry ordering with name first.
    private static final Pattern ENTRY = Pattern.compile(
            "\\{[^}]*?\"name\"\\s*:\\s*\"([^\"]+)\"[^}]*?\"uuid\"\\s*:\\s*\"([0-9a-fA-F\\-]+)\"[^}]*?\\}");

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
            return 0;
        }
        String json;
        try {
            json = Files.readString(usercacheJson, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Could not read " + usercacheJson, ex);
            return 0;
        }
        int imported = 0;
        Matcher matcher = ENTRY.matcher(json);
        while (matcher.find()) {
            String name = matcher.group(1);
            UUID uuid = tryParseUuid(matcher.group(2));
            if (uuid == null) {
                continue;
            }
            if (store.recordNick(uuid, name)) {
                imported++;
            }
        }
        if (imported > 0) {
            logger.info("Imported " + imported + " entries from usercache.json");
        }
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
