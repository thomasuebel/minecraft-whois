package de.thomasuebel.mc.whois;

import de.thomasuebel.mc.whois.model.PlayerRecord;
import de.thomasuebel.mc.whois.model.PlayerStore;
import de.thomasuebel.mc.whois.persistence.AtomicFileWriter;
import de.thomasuebel.mc.whois.persistence.StoreSerializer;
import de.thomasuebel.mc.whois.scheduler.AsyncExecutor;
import de.thomasuebel.mc.whois.scheduler.WriteScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end walk through SPEC §14 acceptance criteria. Uses a synchronous executor
 * so writes complete before assertions, but the real Bukkit wiring (events,
 * commands) is exercised by the per-class tests.
 */
class AcceptanceTest {

    private static final UUID UUID_A = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
    private static final Logger LOGGER = Logger.getLogger(AcceptanceTest.class.getName());

    private static final AsyncExecutor INLINE = new AsyncExecutor() {
        @Override public void runAsync(Runnable r) { r.run(); }
        @Override public void runSync(Runnable r) { r.run(); }
    };

    private static PlayerStore buildStore(Path dataFile, AtomicInteger writeCount) {
        AtomicFileWriter file = new AtomicFileWriter(dataFile);
        StoreSerializer serializer = new StoreSerializer();
        WriteScheduler<Map<UUID, PlayerRecord>> scheduler = new WriteScheduler<>(
                INLINE,
                snapshot -> {
                    writeCount.incrementAndGet();
                    file.writeAtomically(serializer.save(snapshot));
                },
                LOGGER);
        PlayerStore store = new PlayerStore(file, serializer, scheduler, LOGGER);
        store.load();
        return store;
    }

    @Test
    void fullWorkflowMatchesSpec(@TempDir Path dir) {
        Path dataFile = dir.resolve("data.yml");
        AtomicInteger writes = new AtomicInteger();
        PlayerStore store = buildStore(dataFile, writes);

        // §14.1: first join creates record with aka = [nick]
        store.recordNick(UUID_A, "xX_Steve_Xx");
        assertEquals(List.of("xX_Steve_Xx"), store.get(UUID_A).get().getAka());

        // §14.4: setting given-name and reading it back
        store.setGivenName(UUID_A, "Max Mustermann");
        assertEquals("Max Mustermann", store.get(UUID_A).get().getGivenName());

        // §14.2: second join with new nick appends, prior nick preserved
        store.recordNick(UUID_A, "Steve");
        assertEquals(List.of("xX_Steve_Xx", "Steve"), store.get(UUID_A).get().getAka());

        // §14.3: re-join with known nick (case-insensitive) does not duplicate
        int writesBefore = writes.get();
        store.recordNick(UUID_A, "STEVE");
        assertEquals(List.of("xX_Steve_Xx", "Steve"), store.get(UUID_A).get().getAka());
        assertEquals(writesBefore, writes.get(), "duplicate nick must not trigger a write");

        // §14.8: state survives a restart (= fresh PlayerStore.load())
        PlayerStore restarted = buildStore(dataFile, new AtomicInteger());
        Optional<PlayerRecord> record = restarted.get(UUID_A);
        assertTrue(record.isPresent(), "record should be present after restart");
        assertEquals("Max Mustermann", record.get().getGivenName());
        assertEquals(List.of("xX_Steve_Xx", "Steve"), record.get().getAka());
    }
}
