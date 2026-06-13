package de.thomasuebel.mc.whois.listener;

import de.thomasuebel.mc.whois.model.PlayerRecord;
import de.thomasuebel.mc.whois.model.PlayerStore;
import de.thomasuebel.mc.whois.persistence.AtomicFileWriter;
import de.thomasuebel.mc.whois.persistence.StoreSerializer;
import de.thomasuebel.mc.whois.scheduler.AsyncExecutor;
import de.thomasuebel.mc.whois.scheduler.WriteScheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JoinListenerTest {

    private static final UUID UUID_A = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");

    private final Logger logger = Logger.getLogger(JoinListenerTest.class.getName());

    private PlayerStore buildStore(Path dir, List<Map<UUID, PlayerRecord>> captured) {
        AsyncExecutor inline = new AsyncExecutor() {
            @Override public void runAsync(Runnable r) { r.run(); }
            @Override public void runSync(Runnable r) { r.run(); }
        };
        WriteScheduler<Map<UUID, PlayerRecord>> scheduler =
                new WriteScheduler<>(inline, captured::add, logger);
        return new PlayerStore(new AtomicFileWriter(dir.resolve("data.yml")),
                new StoreSerializer(), scheduler, logger);
    }

    @Test
    void onJoinRecordsNickIntoStore(@TempDir Path dir) {
        List<Map<UUID, PlayerRecord>> captured = new ArrayList<>();
        PlayerStore store = buildStore(dir, captured);
        JoinListener listener = new JoinListener(store);

        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID_A);
        when(player.getName()).thenReturn("Steve");
        PlayerJoinEvent event = mock(PlayerJoinEvent.class);
        when(event.getPlayer()).thenReturn(player);

        listener.onJoin(event);

        assertTrue(store.get(UUID_A).isPresent());
        assertEquals(List.of("Steve"), store.get(UUID_A).get().getAka());
        assertEquals(1, captured.size());
    }

    @Test
    void duplicateJoinDoesNotDoubleSubmit(@TempDir Path dir) {
        List<Map<UUID, PlayerRecord>> captured = new ArrayList<>();
        PlayerStore store = buildStore(dir, captured);
        JoinListener listener = new JoinListener(store);

        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID_A);
        when(player.getName()).thenReturn("Steve");
        PlayerJoinEvent event = mock(PlayerJoinEvent.class);
        when(event.getPlayer()).thenReturn(player);

        listener.onJoin(event);
        listener.onJoin(event);

        assertEquals(1, captured.size(), "second join with same nick should not submit again");
    }

    @Test
    void constructorRejectsNull() {
        assertThrows(NullPointerException.class, () -> new JoinListener(null));
    }
}
