package de.thomasuebel.mc.whois.command;

import de.thomasuebel.mc.whois.lookup.NameResolver;
import de.thomasuebel.mc.whois.lookup.OnlinePlayerLookup;
import de.thomasuebel.mc.whois.model.PlayerRecord;
import de.thomasuebel.mc.whois.model.PlayerStore;
import de.thomasuebel.mc.whois.persistence.AtomicFileWriter;
import de.thomasuebel.mc.whois.persistence.StoreSerializer;
import de.thomasuebel.mc.whois.scheduler.AsyncExecutor;
import de.thomasuebel.mc.whois.scheduler.WriteScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WhoisCommandTest {

    private static final UUID UUID_A = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");

    private final Logger logger = Logger.getLogger(WhoisCommandTest.class.getName());
    private final MessageRenderer renderer = new MessageRenderer();

    private PlayerStore store;
    private FakeLookup lookup;
    private NameResolver resolver;
    private WhoisCommand command;
    private CommandSender sender;
    private Command bukkitCommand;

    private static final class FakeLookup implements OnlinePlayerLookup {
        String name;
        UUID uuid;
        List<String> online = List.of();
        @Override public Optional<UUID> findByExactName(String n) {
            return name != null && name.equals(n) ? Optional.of(uuid) : Optional.empty();
        }
        @Override public List<String> onlineNames() { return online; }
    }

    @BeforeEach
    void setUp(@TempDir Path dir) {
        AsyncExecutor inline = new AsyncExecutor() {
            @Override public void runAsync(Runnable r) { r.run(); }
            @Override public void runSync(Runnable r) { r.run(); }
        };
        WriteScheduler<Map<UUID, PlayerRecord>> scheduler =
                new WriteScheduler<>(inline, snap -> {}, logger);
        store = new PlayerStore(new AtomicFileWriter(dir.resolve("data.yml")),
                new StoreSerializer(), scheduler, logger);
        lookup = new FakeLookup();
        resolver = new NameResolver(lookup);
        command = new WhoisCommand(store, resolver, lookup, renderer);

        sender = mock(CommandSender.class);
        when(sender.hasPermission(WhoisCommand.PERMISSION)).thenReturn(true);
        bukkitCommand = mock(Command.class);
    }

    private static String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    private String capturedMessage() {
        ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(sender).sendMessage(captor.capture());
        return plain(captor.getValue());
    }

    @Test
    void senderWithoutPermissionGetsDeniedMessage() {
        when(sender.hasPermission(WhoisCommand.PERMISSION)).thenReturn(false);

        boolean handled = command.onCommand(sender, bukkitCommand, "whois", new String[]{"Steve"});

        assertTrue(handled);
        assertEquals("Keine Berechtigung.", capturedMessage());
    }

    @Test
    void emptyArgsShowsUsage() {
        boolean handled = command.onCommand(sender, bukkitCommand, "whois", new String[]{});

        assertTrue(handled);
        assertTrue(capturedMessage().contains("/whois <name|uuid>"));
    }

    @Test
    void lookupByOnlineNameRendersRecord() {
        store.recordNick(UUID_A, "Steve");
        store.setGivenName(UUID_A, "Max");
        lookup.name = "Steve";
        lookup.uuid = UUID_A;

        command.onCommand(sender, bukkitCommand, "whois", new String[]{"Steve"});

        String msg = capturedMessage();
        assertTrue(msg.contains(UUID_A.toString()));
        assertTrue(msg.contains("Max"));
        assertTrue(msg.contains("Steve"));
    }

    @Test
    void lookupByDashedUuidRendersRecord() {
        store.recordNick(UUID_A, "Steve");

        command.onCommand(sender, bukkitCommand, "whois", new String[]{UUID_A.toString()});

        String msg = capturedMessage();
        assertTrue(msg.contains(UUID_A.toString()));
        assertTrue(msg.contains("Steve"));
        assertTrue(msg.contains("— nicht gesetzt"));
    }

    @Test
    void lookupByUndashedUuidRendersRecord() {
        store.recordNick(UUID_A, "Steve");
        String undashed = UUID_A.toString().replace("-", "");

        command.onCommand(sender, bukkitCommand, "whois", new String[]{undashed});

        assertTrue(capturedMessage().contains("Steve"));
    }

    @Test
    void unknownNameShowsNotFound() {
        command.onCommand(sender, bukkitCommand, "whois", new String[]{"Unknown"});

        String msg = capturedMessage();
        assertTrue(msg.contains("Kein Datensatz"));
        assertTrue(msg.contains("Unknown"));
    }

    @Test
    void knownUuidWithoutRecordShowsNotFound() {
        // resolver succeeds (it's a valid UUID), but the store has nothing for it
        command.onCommand(sender, bukkitCommand, "whois",
                new String[]{"11111111-2222-3333-4444-555555555555"});

        assertTrue(capturedMessage().contains("Kein Datensatz"));
    }

    @Test
    void setWithUuidUpdatesGivenName() {
        store.recordNick(UUID_A, "Steve");

        command.onCommand(sender, bukkitCommand, "whois",
                new String[]{"set", UUID_A.toString(), "Max", "Mustermann"});

        assertEquals("Max Mustermann", store.get(UUID_A).get().getGivenName());
        String msg = capturedMessage();
        assertTrue(msg.contains("Max Mustermann"));
        assertTrue(msg.contains(UUID_A.toString()));
    }

    @Test
    void setWithOnlineNameUpdatesGivenName() {
        lookup.name = "Steve";
        lookup.uuid = UUID_A;

        command.onCommand(sender, bukkitCommand, "whois",
                new String[]{"set", "Steve", "Anna"});

        assertEquals("Anna", store.get(UUID_A).get().getGivenName());
    }

    @Test
    void setWithSingleWordGivenNameWorks() {
        command.onCommand(sender, bukkitCommand, "whois",
                new String[]{"set", UUID_A.toString(), "Max"});

        assertEquals("Max", store.get(UUID_A).get().getGivenName());
    }

    @Test
    void setWithUnknownTargetShowsNotFound() {
        command.onCommand(sender, bukkitCommand, "whois",
                new String[]{"set", "Unknown", "Max"});

        assertTrue(capturedMessage().contains("Kein Datensatz"));
    }

    @Test
    void setWithoutGivenNameShowsUsage() {
        command.onCommand(sender, bukkitCommand, "whois",
                new String[]{"set", UUID_A.toString()});

        assertTrue(capturedMessage().contains("/whois set"));
    }

    @Test
    void setWithOnlyKeywordShowsUsage() {
        command.onCommand(sender, bukkitCommand, "whois", new String[]{"set"});

        assertTrue(capturedMessage().contains("/whois set"));
    }

    @Test
    void setSubcommandIsCaseInsensitive() {
        command.onCommand(sender, bukkitCommand, "whois",
                new String[]{"SET", UUID_A.toString(), "Max"});

        assertEquals("Max", store.get(UUID_A).get().getGivenName());
    }

    @Test
    void constructorRejectsNullArgs() {
        assertThrows(NullPointerException.class,
                () -> new WhoisCommand(null, resolver, lookup, renderer));
        assertThrows(NullPointerException.class,
                () -> new WhoisCommand(store, null, lookup, renderer));
        assertThrows(NullPointerException.class,
                () -> new WhoisCommand(store, resolver, null, renderer));
        assertThrows(NullPointerException.class,
                () -> new WhoisCommand(store, resolver, lookup, null));
    }

    @Test
    void tabCompletionWithoutPermissionReturnsEmpty() {
        when(sender.hasPermission(WhoisCommand.PERMISSION)).thenReturn(false);
        lookup.online = List.of("Steve", "Bob");

        List<String> completions = command.onTabComplete(sender, bukkitCommand, "whois", new String[]{""});

        assertEquals(List.of(), completions);
    }

    @Test
    void tabCompletionOfFirstArgIncludesSetAndOnlineNames() {
        lookup.online = List.of("Steve", "Bob");

        List<String> completions = command.onTabComplete(sender, bukkitCommand, "whois", new String[]{""});

        assertTrue(completions.contains("set"));
        assertTrue(completions.contains("Steve"));
        assertTrue(completions.contains("Bob"));
    }

    @Test
    void tabCompletionFiltersByPrefix() {
        lookup.online = List.of("Steve", "Bob", "Steven");

        List<String> completions = command.onTabComplete(sender, bukkitCommand, "whois", new String[]{"Ste"});

        assertEquals(List.of("Steve", "Steven"), completions);
    }

    @Test
    void tabCompletionPrefixIsCaseInsensitive() {
        lookup.online = List.of("Steve", "Bob");

        List<String> completions = command.onTabComplete(sender, bukkitCommand, "whois", new String[]{"ste"});

        assertEquals(List.of("Steve"), completions);
    }

    @Test
    void tabCompletionOfSecondArgAfterSetReturnsOnlineNames() {
        lookup.online = List.of("Steve", "Bob");

        List<String> completions = command.onTabComplete(sender, bukkitCommand, "whois",
                new String[]{"set", "Bo"});

        assertEquals(List.of("Bob"), completions);
    }

    @Test
    void tabCompletionOfSecondArgWithoutSetReturnsEmpty() {
        lookup.online = List.of("Steve", "Bob");

        List<String> completions = command.onTabComplete(sender, bukkitCommand, "whois",
                new String[]{"Steve", "Bo"});

        assertEquals(List.of(), completions);
    }

    @Test
    void tabCompletionBeyondTwoArgsReturnsEmpty() {
        lookup.online = List.of("Steve");

        List<String> completions = command.onTabComplete(sender, bukkitCommand, "whois",
                new String[]{"set", "Steve", "Max"});

        assertEquals(List.of(), completions);
    }
}
