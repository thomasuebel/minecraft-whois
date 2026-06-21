package de.thomasuebel.mc.whois.command;

import de.thomasuebel.mc.whois.lookup.NameResolver;
import de.thomasuebel.mc.whois.lookup.OnlinePlayerLookup;
import de.thomasuebel.mc.whois.model.PlayerRecord;
import de.thomasuebel.mc.whois.model.PlayerStore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class WhoisCommand implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "whois.admin";
    private static final String SET = "set";

    private final PlayerStore store;
    private final NameResolver resolver;
    private final OnlinePlayerLookup onlineLookup;
    private final MessageRenderer renderer;

    public WhoisCommand(PlayerStore store,
                        NameResolver resolver,
                        OnlinePlayerLookup onlineLookup,
                        MessageRenderer renderer) {
        this.store = Objects.requireNonNull(store, "store");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.onlineLookup = Objects.requireNonNull(onlineLookup, "onlineLookup");
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
        if (args[0].equalsIgnoreCase(SET)) {
            return handleSet(sender, args);
        }
        return handleLookup(sender, args[0]);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> candidates = new ArrayList<>();
            candidates.add(SET);
            candidates.addAll(onlineLookup.onlineNames());
            return filterPrefix(candidates, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase(SET)) {
            return filterPrefix(onlineLookup.onlineNames(), args[1]);
        }
        return List.of();
    }

    private static List<String> filterPrefix(List<String> all, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return all.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lower))
                .toList();
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(renderer.usage());
            return true;
        }
        String target = args[1];
        String givenName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        Optional<UUID> uuid = resolver.resolve(target);
        if (uuid.isEmpty()) {
            sender.sendMessage(renderer.notFound(target));
            return true;
        }
        store.setGivenName(uuid.get(), givenName);
        sender.sendMessage(renderer.setSuccess(uuid.get(), givenName));
        return true;
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
        sender.sendMessage(renderer.lookup(arg, uuid.get(), record.get()));
        return true;
    }
}
