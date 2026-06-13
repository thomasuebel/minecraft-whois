package de.thomasuebel.mc.whois.lookup;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameResolverTest {

    private static final UUID UUID_A = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");

    private static final class FakeLookup implements OnlinePlayerLookup {
        String expectedName;
        UUID result;
        int calls;

        @Override
        public Optional<UUID> findByExactName(String name) {
            calls++;
            return expectedName != null && expectedName.equals(name)
                    ? Optional.of(result)
                    : Optional.empty();
        }

        @Override
        public List<String> onlineNames() {
            return List.of();
        }
    }

    @Test
    void dashedUuidParsesDirectly() {
        FakeLookup lookup = new FakeLookup();
        NameResolver resolver = new NameResolver(lookup);
        assertEquals(Optional.of(UUID_A), resolver.resolve("069a79f4-44e9-4726-a5be-fca90e38aaf5"));
        assertEquals(0, lookup.calls);
    }

    @Test
    void undashed32HexParsesAsUuid() {
        FakeLookup lookup = new FakeLookup();
        NameResolver resolver = new NameResolver(lookup);
        assertEquals(Optional.of(UUID_A), resolver.resolve("069a79f444e94726a5befca90e38aaf5"));
        assertEquals(0, lookup.calls);
    }

    @Test
    void undashedUuidIsCaseInsensitive() {
        FakeLookup lookup = new FakeLookup();
        NameResolver resolver = new NameResolver(lookup);
        assertEquals(Optional.of(UUID_A), resolver.resolve("069A79F444E94726A5BEFCA90E38AAF5"));
    }

    @Test
    void nameMatchingOnlinePlayerReturnsUuid() {
        FakeLookup lookup = new FakeLookup();
        lookup.expectedName = "Steve";
        lookup.result = UUID_A;
        NameResolver resolver = new NameResolver(lookup);

        assertEquals(Optional.of(UUID_A), resolver.resolve("Steve"));
        assertEquals(1, lookup.calls);
    }

    @Test
    void unknownNameReturnsEmpty() {
        FakeLookup lookup = new FakeLookup();
        NameResolver resolver = new NameResolver(lookup);
        assertTrue(resolver.resolve("UnknownPlayer").isEmpty());
    }

    @Test
    void invalidLengthStringFallsThroughToLookup() {
        FakeLookup lookup = new FakeLookup();
        NameResolver resolver = new NameResolver(lookup);
        assertTrue(resolver.resolve("not-a-uuid").isEmpty());
        assertEquals(1, lookup.calls);
    }

    @Test
    void malformedDashedUuidIsTreatedAsName() {
        FakeLookup lookup = new FakeLookup();
        NameResolver resolver = new NameResolver(lookup);
        assertTrue(resolver.resolve("069a79f4-XXXX-4726-a5be-fca90e38aaf5").isEmpty());
        assertEquals(1, lookup.calls);
    }

    @Test
    void thirtyTwoCharNonHexIsTreatedAsName() {
        FakeLookup lookup = new FakeLookup();
        NameResolver resolver = new NameResolver(lookup);
        String thirtyTwoNonHex = "abcdefghijklmnopqrstuvwxyz123456";
        assertEquals(32, thirtyTwoNonHex.length());
        assertTrue(resolver.resolve(thirtyTwoNonHex).isEmpty());
        assertEquals(1, lookup.calls);
    }

    @Test
    void nullArgThrows() {
        NameResolver resolver = new NameResolver(new FakeLookup());
        assertThrows(NullPointerException.class, () -> resolver.resolve(null));
    }

    @Test
    void constructorRejectsNull() {
        assertThrows(NullPointerException.class, () -> new NameResolver(null));
    }
}
