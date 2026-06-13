package de.thomasuebel.mc.whois.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class PlayerRecord {

    private String givenName;
    private final LinkedHashMap<String, String> akaByLowercase = new LinkedHashMap<>();

    public PlayerRecord() {
    }

    public PlayerRecord(String givenName, List<String> aka) {
        this.givenName = givenName;
        if (aka != null) {
            for (String nick : aka) {
                addNickIfNew(nick);
            }
        }
    }

    public boolean addNickIfNew(String nick) {
        Objects.requireNonNull(nick, "nick");
        String key = nick.toLowerCase(Locale.ROOT);
        if (akaByLowercase.containsKey(key)) {
            return false;
        }
        akaByLowercase.put(key, nick);
        return true;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public boolean hasGivenName() {
        return givenName != null && !givenName.isBlank();
    }

    public List<String> getAka() {
        return List.copyOf(akaByLowercase.values());
    }
}
