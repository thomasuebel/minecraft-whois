package de.thomasuebel.mc.whois.persistence;

import de.thomasuebel.mc.whois.model.PlayerRecord;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class StoreSerializer {

    private static final String PLAYERS = "players";
    private static final String GIVEN_NAME = "given-name";
    private static final String AKA = "aka";

    public Map<UUID, PlayerRecord> load(Reader reader) throws IOException, InvalidConfigurationException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = reader.read(buf)) != -1) {
            sb.append(buf, 0, n);
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(sb.toString());
        return parse(yaml);
    }

    public String save(Map<UUID, PlayerRecord> records) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerRecord> entry : records.entrySet()) {
            String base = PLAYERS + "." + entry.getKey();
            PlayerRecord record = entry.getValue();
            if (record.hasGivenName()) {
                yaml.set(base + "." + GIVEN_NAME, record.getGivenName());
            }
            yaml.set(base + "." + AKA, record.getAka());
        }
        return yaml.saveToString();
    }

    private Map<UUID, PlayerRecord> parse(YamlConfiguration yaml) {
        Map<UUID, PlayerRecord> result = new LinkedHashMap<>();
        ConfigurationSection players = yaml.getConfigurationSection(PLAYERS);
        if (players == null) {
            return result;
        }
        for (String key : players.getKeys(false)) {
            UUID uuid = tryParseUuid(key);
            if (uuid == null) {
                continue;
            }
            ConfigurationSection section = players.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            String givenName = section.getString(GIVEN_NAME);
            List<String> aka = section.getStringList(AKA);
            result.put(uuid, new PlayerRecord(givenName, aka));
        }
        return result;
    }

    private static UUID tryParseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
