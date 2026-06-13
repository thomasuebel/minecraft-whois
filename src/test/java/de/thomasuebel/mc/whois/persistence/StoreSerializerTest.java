package de.thomasuebel.mc.whois.persistence;

import de.thomasuebel.mc.whois.model.PlayerRecord;
import org.bukkit.configuration.InvalidConfigurationException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoreSerializerTest {

    private static final UUID UUID_A = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
    private static final UUID UUID_B = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private final StoreSerializer serializer = new StoreSerializer();

    @Test
    void emptyMapRoundTrip() throws Exception {
        String yaml = serializer.save(Map.of());
        Map<UUID, PlayerRecord> loaded = serializer.load(new StringReader(yaml));
        assertTrue(loaded.isEmpty());
    }

    @Test
    void singleRecordRoundTrip() throws Exception {
        PlayerRecord record = new PlayerRecord("Max Mustermann", List.of("Steve", "Bob"));
        Map<UUID, PlayerRecord> input = new LinkedHashMap<>();
        input.put(UUID_A, record);

        String yaml = serializer.save(input);
        Map<UUID, PlayerRecord> loaded = serializer.load(new StringReader(yaml));

        assertEquals(1, loaded.size());
        PlayerRecord loadedRecord = loaded.get(UUID_A);
        assertEquals("Max Mustermann", loadedRecord.getGivenName());
        assertEquals(List.of("Steve", "Bob"), loadedRecord.getAka());
    }

    @Test
    void recordWithoutGivenNameOmitsKey() throws Exception {
        PlayerRecord record = new PlayerRecord();
        record.addNickIfNew("Steve");
        Map<UUID, PlayerRecord> input = new LinkedHashMap<>();
        input.put(UUID_A, record);

        String yaml = serializer.save(input);
        Map<UUID, PlayerRecord> loaded = serializer.load(new StringReader(yaml));

        assertNull(loaded.get(UUID_A).getGivenName());
        assertEquals(List.of("Steve"), loaded.get(UUID_A).getAka());
    }

    @Test
    void multipleRecordsRoundTrip() throws Exception {
        Map<UUID, PlayerRecord> input = new LinkedHashMap<>();
        input.put(UUID_A, new PlayerRecord("Anna", List.of("a1", "a2")));
        input.put(UUID_B, new PlayerRecord(null, List.of("b1")));

        String yaml = serializer.save(input);
        Map<UUID, PlayerRecord> loaded = serializer.load(new StringReader(yaml));

        assertEquals(2, loaded.size());
        assertEquals("Anna", loaded.get(UUID_A).getGivenName());
        assertEquals(List.of("a1", "a2"), loaded.get(UUID_A).getAka());
        assertNull(loaded.get(UUID_B).getGivenName());
        assertEquals(List.of("b1"), loaded.get(UUID_B).getAka());
    }

    @Test
    void loadEmptyYamlReturnsEmptyMap() throws Exception {
        Map<UUID, PlayerRecord> loaded = serializer.load(new StringReader(""));
        assertTrue(loaded.isEmpty());
    }

    @Test
    void loadYamlWithoutPlayersSectionReturnsEmptyMap() throws Exception {
        Map<UUID, PlayerRecord> loaded = serializer.load(new StringReader("other: value\n"));
        assertTrue(loaded.isEmpty());
    }

    @Test
    void loadSkipsInvalidUuidKey() throws Exception {
        String yaml = """
                players:
                  not-a-uuid:
                    aka:
                      - foo
                  069a79f4-44e9-4726-a5be-fca90e38aaf5:
                    aka:
                      - Steve
                """;
        Map<UUID, PlayerRecord> loaded = serializer.load(new StringReader(yaml));
        assertEquals(1, loaded.size());
        assertEquals(List.of("Steve"), loaded.get(UUID_A).getAka());
    }

    @Test
    void corruptYamlThrowsInvalidConfigurationException() {
        String corrupt = "players:\n  : invalid: : yaml\n   - broken\n";
        assertThrows(InvalidConfigurationException.class,
                () -> serializer.load(new StringReader(corrupt)));
    }

    @Test
    void loadPropagatesIOException() {
        java.io.Reader broken = new java.io.Reader() {
            @Override public int read(char[] cbuf, int off, int len) throws IOException {
                throw new IOException("boom");
            }
            @Override public void close() {}
        };
        assertThrows(IOException.class, () -> serializer.load(broken));
    }
}
