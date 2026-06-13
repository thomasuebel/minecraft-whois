package de.thomasuebel.mc.whois.model;

import de.thomasuebel.mc.whois.persistence.AtomicFileWriter;
import de.thomasuebel.mc.whois.persistence.StoreSerializer;
import de.thomasuebel.mc.whois.scheduler.WriteScheduler;
import org.bukkit.configuration.InvalidConfigurationException;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PlayerStore {

    private final AtomicFileWriter file;
    private final StoreSerializer serializer;
    private final WriteScheduler<Map<UUID, PlayerRecord>> writer;
    private final Logger logger;
    private final Map<UUID, PlayerRecord> records = new LinkedHashMap<>();

    public PlayerStore(AtomicFileWriter file,
                       StoreSerializer serializer,
                       WriteScheduler<Map<UUID, PlayerRecord>> writer,
                       Logger logger) {
        this.file = Objects.requireNonNull(file, "file");
        this.serializer = Objects.requireNonNull(serializer, "serializer");
        this.writer = Objects.requireNonNull(writer, "writer");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void load() {
        records.clear();
        if (!file.exists()) {
            return;
        }
        try (Reader reader = file.newReader()) {
            records.putAll(serializer.load(reader));
        } catch (InvalidConfigurationException ex) {
            handleCorrupt(ex);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to read " + file.getTarget() + "; starting empty", ex);
        }
    }

    private void handleCorrupt(Exception cause) {
        records.clear();
        try {
            Path backup = file.backupCorrupt();
            logger.log(Level.WARNING,
                    "data.yml was corrupt; backed up to " + backup.getFileName() + " and starting empty",
                    cause);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to back up corrupt data file", ex);
        }
    }

    public Optional<PlayerRecord> get(UUID uuid) {
        return Optional.ofNullable(records.get(uuid));
    }

    public Set<UUID> knownUuids() {
        return Set.copyOf(records.keySet());
    }

    public boolean recordNick(UUID uuid, String nick) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(nick, "nick");
        PlayerRecord record = records.computeIfAbsent(uuid, u -> new PlayerRecord());
        if (!record.addNickIfNew(nick)) {
            return false;
        }
        writer.submit(snapshot());
        return true;
    }

    public void setGivenName(UUID uuid, String givenName) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(givenName, "givenName");
        PlayerRecord record = records.computeIfAbsent(uuid, u -> new PlayerRecord());
        record.setGivenName(givenName);
        writer.submit(snapshot());
    }

    public void flushSync() {
        writer.flushSync();
    }

    private Map<UUID, PlayerRecord> snapshot() {
        Map<UUID, PlayerRecord> copy = new LinkedHashMap<>();
        for (Map.Entry<UUID, PlayerRecord> e : records.entrySet()) {
            PlayerRecord original = e.getValue();
            copy.put(e.getKey(), new PlayerRecord(original.getGivenName(), original.getAka()));
        }
        return copy;
    }
}
