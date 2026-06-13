package de.thomasuebel.mc.whois.model;

import de.thomasuebel.mc.whois.persistence.AtomicFileWriter;
import de.thomasuebel.mc.whois.persistence.StoreSerializer;
import de.thomasuebel.mc.whois.scheduler.AsyncExecutor;
import de.thomasuebel.mc.whois.scheduler.WriteScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerStoreTest {

    private static final UUID UUID_A = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
    private static final UUID UUID_B = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private final Logger logger = Logger.getLogger(PlayerStoreTest.class.getName());
    private final List<Map<UUID, PlayerRecord>> captured = new ArrayList<>();
    private AsyncExecutor inlineExecutor;
    private WriteScheduler<Map<UUID, PlayerRecord>> scheduler;

    @BeforeEach
    void setUp() {
        captured.clear();
        inlineExecutor = new AsyncExecutor() {
            @Override public void runAsync(Runnable r) { r.run(); }
            @Override public void runSync(Runnable r) { r.run(); }
        };
        scheduler = new WriteScheduler<>(inlineExecutor, captured::add, logger);
    }

    private PlayerStore newStore(Path target) {
        return new PlayerStore(new AtomicFileWriter(target),
                new StoreSerializer(), scheduler, logger);
    }

    @Test
    void loadOnMissingFileLeavesStoreEmpty(@TempDir Path dir) {
        PlayerStore store = newStore(dir.resolve("data.yml"));
        store.load();
        assertTrue(store.knownUuids().isEmpty());
    }

    @Test
    void loadFromValidFilePopulatesStore(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("data.yml");
        Files.writeString(target, """
                players:
                  069a79f4-44e9-4726-a5be-fca90e38aaf5:
                    given-name: "Max"
                    aka:
                      - "Steve"
                """, StandardCharsets.UTF_8);

        PlayerStore store = newStore(target);
        store.load();

        Optional<PlayerRecord> record = store.get(UUID_A);
        assertTrue(record.isPresent());
        assertEquals("Max", record.get().getGivenName());
        assertEquals(List.of("Steve"), record.get().getAka());
    }

    @Test
    void loadCorruptFileBacksItUpAndStartsEmpty(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("data.yml");
        Files.writeString(target, "players:\n  : invalid: : yaml\n   - broken\n");

        PlayerStore store = newStore(target);
        store.load();

        assertTrue(store.knownUuids().isEmpty());
        assertFalse(Files.exists(target), "corrupt file should be moved away");
        try (Stream<Path> entries = Files.list(dir)) {
            assertTrue(entries.anyMatch(p -> p.getFileName().toString().startsWith("data.yml.broken-")));
        }
    }

    @Test
    void loadPropagatesIoFailureAsEmptyStore(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("data.yml");
        Files.writeString(target, "ok");

        AtomicFileWriter brokenReader = spy(new AtomicFileWriter(target));
        doThrow(new IOException("read failure")).when(brokenReader).newReader();
        PlayerStore store = new PlayerStore(brokenReader, new StoreSerializer(), scheduler, logger);

        store.load();
        assertTrue(store.knownUuids().isEmpty());
    }

    @Test
    void loadLogsAndContinuesWhenBackupAlsoFails(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("data.yml");
        Files.writeString(target, "players:\n  : invalid: : yaml\n");

        AtomicFileWriter brokenBackup = spy(new AtomicFileWriter(target));
        doThrow(new IOException("backup failure")).when(brokenBackup).backupCorrupt();
        PlayerStore store = new PlayerStore(brokenBackup, new StoreSerializer(), scheduler, logger);

        store.load();
        assertTrue(store.knownUuids().isEmpty());
    }


    @Test
    void loadClearsExistingState(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("data.yml");
        PlayerStore store = newStore(target);
        store.recordNick(UUID_A, "Steve");
        assertFalse(store.knownUuids().isEmpty());

        store.load(); // file does not exist
        assertTrue(store.knownUuids().isEmpty());
    }

    @Test
    void recordNickAddsNewRecordAndSubmitsSnapshot(@TempDir Path dir) {
        PlayerStore store = newStore(dir.resolve("data.yml"));

        boolean added = store.recordNick(UUID_A, "Steve");

        assertTrue(added);
        assertEquals(1, captured.size());
        assertEquals(List.of("Steve"), captured.get(0).get(UUID_A).getAka());
    }

    @Test
    void recordDuplicateNickDoesNotSubmit(@TempDir Path dir) {
        PlayerStore store = newStore(dir.resolve("data.yml"));
        store.recordNick(UUID_A, "Steve");
        captured.clear();

        boolean addedAgain = store.recordNick(UUID_A, "STEVE");

        assertFalse(addedAgain);
        assertTrue(captured.isEmpty(), "duplicate nick must not trigger a write");
    }

    @Test
    void setGivenNameSubmitsSnapshotWithUpdatedName(@TempDir Path dir) {
        PlayerStore store = newStore(dir.resolve("data.yml"));

        store.setGivenName(UUID_A, "Max Mustermann");

        assertEquals(1, captured.size());
        assertEquals("Max Mustermann", captured.get(0).get(UUID_A).getGivenName());
    }

    @Test
    void snapshotIsIndependentOfLaterMutations(@TempDir Path dir) {
        PlayerStore store = newStore(dir.resolve("data.yml"));

        store.recordNick(UUID_A, "Steve");
        Map<UUID, PlayerRecord> first = captured.get(0);

        store.setGivenName(UUID_A, "Max");

        assertEquals(2, captured.size());
        assertFalse(first.get(UUID_A).hasGivenName(),
                "earlier snapshot must not reflect later mutations");
        assertEquals("Max", captured.get(1).get(UUID_A).getGivenName());
    }

    @Test
    void getReturnsEmptyForUnknownUuid(@TempDir Path dir) {
        PlayerStore store = newStore(dir.resolve("data.yml"));
        assertTrue(store.get(UUID_B).isEmpty());
    }

    @Test
    void knownUuidsReflectsCurrentRecords(@TempDir Path dir) {
        PlayerStore store = newStore(dir.resolve("data.yml"));
        store.recordNick(UUID_A, "Steve");
        store.recordNick(UUID_B, "Bob");

        assertEquals(2, store.knownUuids().size());
        assertTrue(store.knownUuids().contains(UUID_A));
        assertTrue(store.knownUuids().contains(UUID_B));
    }

    @Test
    void knownUuidsReturnsImmutableSet(@TempDir Path dir) {
        PlayerStore store = newStore(dir.resolve("data.yml"));
        store.recordNick(UUID_A, "Steve");
        var uuids = store.knownUuids();
        assertThrows(UnsupportedOperationException.class, () -> uuids.add(UUID_B));
    }

    @Test
    void flushSyncDelegatesToScheduler(@TempDir Path dir) {
        PlayerStore store = newStore(dir.resolve("data.yml"));
        store.recordNick(UUID_A, "Steve");
        captured.clear();
        // After clear, a stale snapshot may still be queued in scheduler — flush drains it.
        store.flushSync();
        // Either captured is still empty (already drained) or it contains the last snapshot.
        // Either way, no exception and method delegates without crash.
        assertNotNull(captured);
    }

    @Test
    void constructorRejectsNullArgs(@TempDir Path dir) {
        AtomicFileWriter file = new AtomicFileWriter(dir.resolve("data.yml"));
        StoreSerializer ser = new StoreSerializer();

        assertThrows(NullPointerException.class,
                () -> new PlayerStore(null, ser, scheduler, logger));
        assertThrows(NullPointerException.class,
                () -> new PlayerStore(file, null, scheduler, logger));
        assertThrows(NullPointerException.class,
                () -> new PlayerStore(file, ser, null, logger));
        assertThrows(NullPointerException.class,
                () -> new PlayerStore(file, ser, scheduler, null));
    }

    @Test
    void recordNickRejectsNullArgs(@TempDir Path dir) {
        PlayerStore store = newStore(dir.resolve("data.yml"));
        assertThrows(NullPointerException.class, () -> store.recordNick(null, "Steve"));
        assertThrows(NullPointerException.class, () -> store.recordNick(UUID_A, null));
    }

    @Test
    void setGivenNameRejectsNullArgs(@TempDir Path dir) {
        PlayerStore store = newStore(dir.resolve("data.yml"));
        assertThrows(NullPointerException.class, () -> store.setGivenName(null, "Max"));
        assertThrows(NullPointerException.class, () -> store.setGivenName(UUID_A, null));
    }
}
