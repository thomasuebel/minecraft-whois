package de.thomasuebel.mc.whois.bootstrap;

import de.thomasuebel.mc.whois.model.PlayerRecord;
import de.thomasuebel.mc.whois.model.PlayerStore;
import de.thomasuebel.mc.whois.persistence.AtomicFileWriter;
import de.thomasuebel.mc.whois.persistence.StoreSerializer;
import de.thomasuebel.mc.whois.scheduler.AsyncExecutor;
import de.thomasuebel.mc.whois.scheduler.WriteScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserCacheImporterTest {

    private static final UUID UUID_A = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
    private static final UUID UUID_B = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private final Logger logger = Logger.getLogger(UserCacheImporterTest.class.getName());

    private PlayerStore buildStore(Path dir) {
        AsyncExecutor inline = new AsyncExecutor() {
            @Override public void runAsync(Runnable r) { r.run(); }
            @Override public void runSync(Runnable r) { r.run(); }
        };
        WriteScheduler<Map<UUID, PlayerRecord>> scheduler =
                new WriteScheduler<>(inline, snap -> {}, logger);
        return new PlayerStore(new AtomicFileWriter(dir.resolve("data.yml")),
                new StoreSerializer(), scheduler, logger);
    }

    @Test
    void missingFileReturnsZero(@TempDir Path dir) {
        PlayerStore store = buildStore(dir);
        UserCacheImporter importer = new UserCacheImporter(dir.resolve("usercache.json"), store, logger);

        assertEquals(0, importer.importIfPresent());
        assertTrue(store.knownUuids().isEmpty());
    }

    @Test
    void importsValidEntries(@TempDir Path dir) throws Exception {
        Path usercache = dir.resolve("usercache.json");
        Files.writeString(usercache, """
                [
                  {"name":"Steve","uuid":"069a79f4-44e9-4726-a5be-fca90e38aaf5","expiresOn":"2026-09-01 12:00:00 +0200"},
                  {"name":"Bob","uuid":"11111111-2222-3333-4444-555555555555","expiresOn":"2026-09-01 12:00:00 +0200"}
                ]
                """);

        PlayerStore store = buildStore(dir);
        UserCacheImporter importer = new UserCacheImporter(usercache, store, logger);

        assertEquals(2, importer.importIfPresent());
        assertEquals(List.of("Steve"), store.get(UUID_A).get().getAka());
        assertEquals(List.of("Bob"), store.get(UUID_B).get().getAka());
    }

    @Test
    void duplicateNicksAreNotDoubleCounted(@TempDir Path dir) throws Exception {
        Path usercache = dir.resolve("usercache.json");
        Files.writeString(usercache, """
                [
                  {"name":"Steve","uuid":"069a79f4-44e9-4726-a5be-fca90e38aaf5","expiresOn":"x"}
                ]
                """);

        PlayerStore store = buildStore(dir);
        store.recordNick(UUID_A, "Steve");

        UserCacheImporter importer = new UserCacheImporter(usercache, store, logger);

        assertEquals(0, importer.importIfPresent(),
                "existing nick should not count as a new import");
    }

    @Test
    void malformedEntriesAreSkipped(@TempDir Path dir) throws Exception {
        Path usercache = dir.resolve("usercache.json");
        Files.writeString(usercache, """
                [
                  {"name":"Steve","uuid":"not-a-uuid","expiresOn":"x"},
                  {"name":"Bob","uuid":"11111111-2222-3333-4444-555555555555","expiresOn":"x"}
                ]
                """);

        PlayerStore store = buildStore(dir);
        UserCacheImporter importer = new UserCacheImporter(usercache, store, logger);

        assertEquals(1, importer.importIfPresent());
        assertTrue(store.get(UUID_A).isEmpty());
        assertEquals(List.of("Bob"), store.get(UUID_B).get().getAka());
    }

    @Test
    void emptyArrayImportsNothing(@TempDir Path dir) throws Exception {
        Path usercache = dir.resolve("usercache.json");
        Files.writeString(usercache, "[]\n");

        PlayerStore store = buildStore(dir);
        UserCacheImporter importer = new UserCacheImporter(usercache, store, logger);

        assertEquals(0, importer.importIfPresent());
    }

    @Test
    void ioFailureReturnsZero(@TempDir Path dir) throws Exception {
        Path usercache = dir.resolve("usercache.json");
        // Create the file as a directory: reading as string then fails with IOException
        Files.createDirectory(usercache);

        PlayerStore store = buildStore(dir);
        UserCacheImporter importer = new UserCacheImporter(usercache, store, logger);

        assertEquals(0, importer.importIfPresent());
    }

    @Test
    void whitespaceVariationsParsed(@TempDir Path dir) throws Exception {
        Path usercache = dir.resolve("usercache.json");
        Files.writeString(usercache,
                "[ { \"name\" : \"Steve\" , \"uuid\" : \"069a79f4-44e9-4726-a5be-fca90e38aaf5\" , \"expiresOn\" : \"x\" } ]");

        PlayerStore store = buildStore(dir);
        UserCacheImporter importer = new UserCacheImporter(usercache, store, logger);

        assertEquals(1, importer.importIfPresent());
    }

    @Test
    void constructorRejectsNullArgs(@TempDir Path dir) {
        PlayerStore store = buildStore(dir);
        Path usercache = dir.resolve("usercache.json");

        assertThrows(NullPointerException.class,
                () -> new UserCacheImporter(null, store, logger));
        assertThrows(NullPointerException.class,
                () -> new UserCacheImporter(usercache, null, logger));
        assertThrows(NullPointerException.class,
                () -> new UserCacheImporter(usercache, store, null));
    }
}
