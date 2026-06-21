package de.thomasuebel.mc.whois.command;

import de.thomasuebel.mc.whois.model.PlayerRecord;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageRendererTest {

    private static final UUID UUID_A = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");

    private final MessageRenderer renderer = new MessageRenderer();

    private static String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    @Test
    void lookupWithGivenNameRendersIsLine() {
        PlayerRecord record = new PlayerRecord("Max Mustermann", List.of("Steve", "Bob"));
        String text = plain(renderer.lookup("Steve", UUID_A, record));

        assertEquals("Steve is Max Mustermann\nSteve is also known as Bob", text);
    }

    @Test
    void lookupWithoutGivenNameUsesUuidWithPeriod() {
        PlayerRecord record = new PlayerRecord(null, List.of("Steve"));
        String text = plain(renderer.lookup("Steve", UUID_A, record));

        assertEquals("Steve is 069a79f4-44e9-4726-a5be-fca90e38aaf5.", text);
    }

    @Test
    void lookupWithoutAkasRendersOnlyIsLine() {
        PlayerRecord record = new PlayerRecord("Max", List.of());
        String text = plain(renderer.lookup("Steve", UUID_A, record));

        assertEquals("Steve is Max", text);
    }

    @Test
    void lookupChunksAkasIntoLinesOfThree() {
        PlayerRecord record = new PlayerRecord("Max",
                List.of("Bob", "Joe", "Alex", "Jane", "Mike", "Sam", "Lee"));
        String text = plain(renderer.lookup("Steve", UUID_A, record));

        assertEquals("Steve is Max\n"
                + "Steve is also known as Bob, Joe, Alex\n"
                + "Steve is also known as Jane, Mike, Sam\n"
                + "Steve is also known as Lee", text);
    }

    @Test
    void lookupExcludesSearchedNameFromAkasCaseInsensitive() {
        PlayerRecord record = new PlayerRecord("Max", List.of("Steve", "Bob"));
        String text = plain(renderer.lookup("steve", UUID_A, record));

        assertEquals("steve is Max\nsteve is also known as Bob", text);
    }

    @Test
    void lookupByUuidWithoutGivenNameRepeatsUuid() {
        PlayerRecord record = new PlayerRecord(null, List.of("Steve"));
        String text = plain(renderer.lookup(UUID_A.toString(), UUID_A, record));

        assertEquals(UUID_A + " is " + UUID_A + ".\n"
                + UUID_A + " is also known as Steve", text);
    }

    @Test
    void notFoundReferencesArgument() {
        String text = plain(renderer.notFound("Steve"));
        assertTrue(text.contains("Steve"));
        assertTrue(text.toLowerCase().contains("kein"));
    }

    @Test
    void setSuccessMentionsUuidAndName() {
        String text = plain(renderer.setSuccess(UUID_A, "Max Mustermann"));
        assertTrue(text.contains(UUID_A.toString()));
        assertTrue(text.contains("Max Mustermann"));
    }

    @Test
    void usageMentionsBothCommandForms() {
        String text = plain(renderer.usage());
        assertTrue(text.contains("/whois <name|uuid>"));
        assertTrue(text.contains("/whois set"));
    }

    @Test
    void permissionDeniedIsLocalized() {
        assertEquals("Keine Berechtigung.", plain(renderer.permissionDenied()));
    }
}
