package de.thomasuebel.mc.whois.lookup;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OnlinePlayerLookup {

    Optional<UUID> findByExactName(String name);

    List<String> onlineNames();
}
