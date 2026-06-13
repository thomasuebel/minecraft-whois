package de.thomasuebel.mc.whois.bootstrap;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

public final class UserCacheLocator {

    private static final String USERCACHE = "usercache.json";

    private UserCacheLocator() {
    }

    /**
     * Resolves the vanilla server's usercache.json from the plugin's data
     * folder. By the Bukkit layout the data folder lives at
     * {@code <server>/plugins/<plugin>/}, so the server root is two
     * directories up.
     */
    public static Path resolveFrom(File pluginDataFolder) {
        Objects.requireNonNull(pluginDataFolder, "pluginDataFolder");
        return pluginDataFolder.getAbsoluteFile().toPath()
                .getParent()
                .getParent()
                .resolve(USERCACHE);
    }
}
