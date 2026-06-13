package de.thomasuebel.mc.whois.lookup;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class BukkitOnlinePlayerLookup implements OnlinePlayerLookup {

    @Override
    public Optional<UUID> findByExactName(String name) {
        Player player = Bukkit.getPlayerExact(name);
        return Optional.ofNullable(player).map(Player::getUniqueId);
    }

    @Override
    public List<String> onlineNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .toList();
    }
}
