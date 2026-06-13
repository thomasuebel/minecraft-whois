package de.thomasuebel.mc.whois;

import de.thomasuebel.mc.whois.command.MessageRenderer;
import de.thomasuebel.mc.whois.command.WhoisCommand;
import de.thomasuebel.mc.whois.listener.JoinListener;
import de.thomasuebel.mc.whois.lookup.BukkitOnlinePlayerLookup;
import de.thomasuebel.mc.whois.lookup.NameResolver;
import de.thomasuebel.mc.whois.lookup.OnlinePlayerLookup;
import de.thomasuebel.mc.whois.model.PlayerRecord;
import de.thomasuebel.mc.whois.model.PlayerStore;
import de.thomasuebel.mc.whois.persistence.AtomicFileWriter;
import de.thomasuebel.mc.whois.persistence.StoreSerializer;
import de.thomasuebel.mc.whois.scheduler.BukkitAsyncExecutor;
import de.thomasuebel.mc.whois.scheduler.WriteScheduler;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;

public final class WhoisPlugin extends JavaPlugin {

    private PlayerStore store;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        AtomicFileWriter file = new AtomicFileWriter(getDataFolder().toPath().resolve("data.yml"));
        StoreSerializer serializer = new StoreSerializer();

        WriteScheduler<Map<UUID, PlayerRecord>> scheduler = new WriteScheduler<>(
                new BukkitAsyncExecutor(this),
                snapshot -> file.writeAtomically(serializer.save(snapshot)),
                getLogger());

        store = new PlayerStore(file, serializer, scheduler, getLogger());
        store.load();

        OnlinePlayerLookup onlineLookup = new BukkitOnlinePlayerLookup();
        NameResolver resolver = new NameResolver(onlineLookup);
        MessageRenderer renderer = new MessageRenderer();

        getServer().getPluginManager().registerEvents(new JoinListener(store), this);

        PluginCommand cmd = getCommand("whois");
        if (cmd == null) {
            getLogger().severe("Command 'whois' is not declared in plugin.yml");
            return;
        }
        WhoisCommand whois = new WhoisCommand(store, resolver, onlineLookup, renderer);
        cmd.setExecutor(whois);
        cmd.setTabCompleter(whois);
    }

    @Override
    public void onDisable() {
        if (store != null) {
            store.flushSync();
        }
    }
}
