package de.thomasuebel.mc.whois.command;

import de.thomasuebel.mc.whois.lookup.NameResolver;
import de.thomasuebel.mc.whois.model.PlayerRecord;
import de.thomasuebel.mc.whois.model.PlayerStore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class WhoisCommand implements CommandExecutor {

    public static final String PERMISSION = "whois.admin";

    private final PlayerStore store;
    private final NameResolver resolver;
    private final MessageRenderer renderer;

    public WhoisCommand(PlayerStore store, NameResolver resolver, MessageRenderer renderer) {
        this.store = Objects.requireNonNull(store, "store");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(renderer.permissionDenied());
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(renderer.usage());
            return true;
        }
        return handleLookup(sender, args[0]);
    }

    private boolean handleLookup(CommandSender sender, String arg) {
        Optional<UUID> uuid = resolver.resolve(arg);
        if (uuid.isEmpty()) {
            sender.sendMessage(renderer.notFound(arg));
            return true;
        }
        Optional<PlayerRecord> record = store.get(uuid.get());
        if (record.isEmpty()) {
            sender.sendMessage(renderer.notFound(arg));
            return true;
        }
        sender.sendMessage(renderer.lookup(uuid.get(), record.get()));
        return true;
    }
}
