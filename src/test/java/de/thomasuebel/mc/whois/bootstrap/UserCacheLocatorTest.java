package de.thomasuebel.mc.whois.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserCacheLocatorTest {

    @Test
    void resolvesRelativeToServerRoot(@TempDir Path serverRoot) {
        File dataFolder = serverRoot.resolve("plugins/Whois").toFile();

        Path result = UserCacheLocator.resolveFrom(dataFolder);

        assertEquals(serverRoot.resolve("usercache.json").toAbsolutePath(),
                result.toAbsolutePath());
    }

    @Test
    void resolvesEvenWhenDataFolderPathIsRelative() {
        // Bukkit's getDataFolder() may hand back a relative File like
        // new File("plugins/Whois"). Such a Path has only two segments —
        // a naive getParent().getParent() walks off to null and NPEs.
        File relative = new File("plugins/Whois");

        Path result = UserCacheLocator.resolveFrom(relative);

        assertTrue(result.endsWith("usercache.json"),
                "should resolve regardless of whether the data folder path is relative");
    }

    @Test
    void rejectsNullInput() {
        assertThrows(NullPointerException.class,
                () -> UserCacheLocator.resolveFrom(null));
    }
}
