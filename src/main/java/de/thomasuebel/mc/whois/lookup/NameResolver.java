package de.thomasuebel.mc.whois.lookup;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public final class NameResolver {

    private static final Pattern UNDASHED_UUID = Pattern.compile("[0-9a-fA-F]{32}");

    private final OnlinePlayerLookup onlineLookup;

    public NameResolver(OnlinePlayerLookup onlineLookup) {
        this.onlineLookup = Objects.requireNonNull(onlineLookup, "onlineLookup");
    }

    public Optional<UUID> resolve(String arg) {
        Objects.requireNonNull(arg, "arg");
        Optional<UUID> uuid = tryParseUuid(arg);
        if (uuid.isPresent()) {
            return uuid;
        }
        return onlineLookup.findByExactName(arg);
    }

    private static Optional<UUID> tryParseUuid(String s) {
        if (UNDASHED_UUID.matcher(s).matches()) {
            String dashed = s.substring(0, 8) + "-"
                    + s.substring(8, 12) + "-"
                    + s.substring(12, 16) + "-"
                    + s.substring(16, 20) + "-"
                    + s.substring(20);
            return Optional.of(UUID.fromString(dashed));
        }
        try {
            return Optional.of(UUID.fromString(s));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
