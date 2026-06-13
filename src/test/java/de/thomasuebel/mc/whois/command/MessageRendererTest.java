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
    void lookupShowsUuidGivenNameAndAka() {
        PlayerRecord record = new PlayerRecord("Max Mustermann", List.of("Steve", "Bob"));
        String text = plain(renderer.lookup(UUID_A, record));

        assertTrue(text.contains("UUID: 069a79f4-44e9-4726-a5be-fca90e38aaf5"));
        assertTrue(text.contains("given-name: Max Mustermann"));
        assertTrue(text.contains("aka:"));
        assertTrue(text.contains("Steve"));
        assertTrue(text.contains("Bob"));
    }

    @Test
    void lookupWithoutGivenNameShowsPlaceholder() {
        PlayerRecord record = new PlayerRecord(null, List.of("Steve"));
        String text = plain(renderer.lookup(UUID_A, record));

        assertTrue(text.contains("given-name: — nicht gesetzt"));
    }

    @Test
    void lookupWithEmptyAkaShowsDash() {
        PlayerRecord record = new PlayerRecord("Max", List.of());
        String text = plain(renderer.lookup(UUID_A, record));

        assertTrue(text.contains("aka: —"));
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
