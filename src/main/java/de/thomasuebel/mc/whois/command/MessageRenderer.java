package de.thomasuebel.mc.whois.command;

import de.thomasuebel.mc.whois.model.PlayerRecord;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

public final class MessageRenderer {

    private static final String UNSET = "— nicht gesetzt";

    public Component lookup(UUID uuid, PlayerRecord record) {
        Component result = Component.text("UUID: ", NamedTextColor.GRAY)
                .append(Component.text(uuid.toString(), NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.text("given-name: ", NamedTextColor.GRAY))
                .append(record.hasGivenName()
                        ? Component.text(record.getGivenName(), NamedTextColor.WHITE)
                        : Component.text(UNSET, NamedTextColor.DARK_GRAY))
                .append(Component.newline());

        if (record.getAka().isEmpty()) {
            return result.append(Component.text("aka: —", NamedTextColor.GRAY));
        }

        Component akaSection = Component.text("aka:", NamedTextColor.GRAY);
        for (String nick : record.getAka()) {
            akaSection = akaSection
                    .append(Component.newline())
                    .append(Component.text("  - " + nick, NamedTextColor.WHITE));
        }
        return result.append(akaSection);
    }

    public Component notFound(String arg) {
        return Component.text("Kein Datensatz für \"" + arg + "\". Online-Namen oder UUID verwenden.",
                NamedTextColor.RED);
    }

    public Component setSuccess(UUID uuid, String givenName) {
        return Component.text("given-name für ", NamedTextColor.GREEN)
                .append(Component.text(uuid.toString(), NamedTextColor.WHITE))
                .append(Component.text(" gesetzt: ", NamedTextColor.GREEN))
                .append(Component.text(givenName, NamedTextColor.WHITE));
    }

    public Component usage() {
        return Component.text(
                "Benutzung: /whois <name|uuid>  oder  /whois set <name|uuid> <given-name...>",
                NamedTextColor.YELLOW);
    }

    public Component permissionDenied() {
        return Component.text("Keine Berechtigung.", NamedTextColor.RED);
    }
}
