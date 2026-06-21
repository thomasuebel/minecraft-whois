package de.thomasuebel.mc.whois.command;

import de.thomasuebel.mc.whois.model.PlayerRecord;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MessageRenderer {

    private static final String IS_LINE = "%s is %s";
    private static final String AKA_LINE = "%s is also known as %s";
    private static final int AKAS_PER_LINE = 3;

    public Component lookup(String user, UUID uuid, PlayerRecord record) {
        String identity = record.hasGivenName()
                ? record.getGivenName()
                : uuid.toString() + ".";
        Component result = Component.text(String.format(IS_LINE, user, identity), NamedTextColor.WHITE);

        List<String> akas = new ArrayList<>();
        for (String nick : record.getAka()) {
            if (!nick.equalsIgnoreCase(user)) {
                akas.add(nick);
            }
        }
        for (int i = 0; i < akas.size(); i += AKAS_PER_LINE) {
            List<String> chunk = akas.subList(i, Math.min(i + AKAS_PER_LINE, akas.size()));
            String line = String.format(AKA_LINE, user, String.join(", ", chunk));
            result = result.append(Component.newline())
                    .append(Component.text(line, NamedTextColor.WHITE));
        }
        return result;
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
