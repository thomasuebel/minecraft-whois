package de.thomasuebel.mc.whois.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerRecordTest {

    @Test
    void newRecordIsEmpty() {
        PlayerRecord record = new PlayerRecord();
        assertNull(record.getGivenName());
        assertFalse(record.hasGivenName());
        assertTrue(record.getAka().isEmpty());
    }

    @Test
    void addNickIfNewReturnsTrueOnFirstInsert() {
        PlayerRecord record = new PlayerRecord();
        assertTrue(record.addNickIfNew("Steve"));
        assertEquals(List.of("Steve"), record.getAka());
    }

    @Test
    void addNickIfNewReturnsFalseOnSameCasing() {
        PlayerRecord record = new PlayerRecord();
        record.addNickIfNew("Steve");
        assertFalse(record.addNickIfNew("Steve"));
        assertEquals(List.of("Steve"), record.getAka());
    }

    @Test
    void dedupIsCaseInsensitiveButPreservesFirstCasing() {
        PlayerRecord record = new PlayerRecord();
        record.addNickIfNew("xX_Steve_Xx");
        assertFalse(record.addNickIfNew("XX_STEVE_XX"));
        assertFalse(record.addNickIfNew("xx_steve_xx"));
        assertEquals(List.of("xX_Steve_Xx"), record.getAka());
    }

    @Test
    void insertionOrderIsPreserved() {
        PlayerRecord record = new PlayerRecord();
        record.addNickIfNew("alpha");
        record.addNickIfNew("Bravo");
        record.addNickIfNew("charlie");
        assertEquals(List.of("alpha", "Bravo", "charlie"), record.getAka());
    }

    @Test
    void addNullNickThrows() {
        PlayerRecord record = new PlayerRecord();
        assertThrows(NullPointerException.class, () -> record.addNickIfNew(null));
    }

    @Test
    void givenNameSetAndGet() {
        PlayerRecord record = new PlayerRecord();
        record.setGivenName("Max Mustermann");
        assertEquals("Max Mustermann", record.getGivenName());
        assertTrue(record.hasGivenName());
    }

    @Test
    void hasGivenNameFalseForNullAndBlank() {
        PlayerRecord record = new PlayerRecord();
        assertFalse(record.hasGivenName());
        record.setGivenName("");
        assertFalse(record.hasGivenName());
        record.setGivenName("   ");
        assertFalse(record.hasGivenName());
        record.setGivenName(null);
        assertFalse(record.hasGivenName());
    }

    @Test
    void constructorWithInitialValuesPopulates() {
        PlayerRecord record = new PlayerRecord("Anna", List.of("Steve", "STEVE", "Bob"));
        assertEquals("Anna", record.getGivenName());
        assertEquals(List.of("Steve", "Bob"), record.getAka());
    }

    @Test
    void constructorAcceptsNullAka() {
        PlayerRecord record = new PlayerRecord("Anna", null);
        assertEquals("Anna", record.getGivenName());
        assertTrue(record.getAka().isEmpty());
    }

    @Test
    void getAkaReturnsImmutableCopy() {
        PlayerRecord record = new PlayerRecord();
        record.addNickIfNew("Steve");
        List<String> aka = record.getAka();
        assertThrows(UnsupportedOperationException.class, () -> aka.add("Bob"));
    }
}
