package de.thomasuebel.mc.whois.listener;

import de.thomasuebel.mc.whois.model.PlayerStore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Objects;

public final class JoinListener implements Listener {

    private final PlayerStore store;

    public JoinListener(PlayerStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        store.recordNick(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }
}
