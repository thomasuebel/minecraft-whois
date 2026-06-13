package de.thomasuebel.mc.whois.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtomicFileWriterTest {

    @Test
    void writeCreatesFileWithContent(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("data.yml");
        AtomicFileWriter writer = new AtomicFileWriter(target);

        writer.writeAtomically("hello\nworld\n");

        assertTrue(Files.exists(target));
        assertEquals("hello\nworld\n", Files.readString(target, StandardCharsets.UTF_8));
    }

    @Test
    void writeOverwritesExistingFile(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("data.yml");
        Files.writeString(target, "old");
        AtomicFileWriter writer = new AtomicFileWriter(target);

        writer.writeAtomically("new");

        assertEquals("new", Files.readString(target, StandardCharsets.UTF_8));
    }

    @Test
    void writeCreatesParentDirectory(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("nested/sub/data.yml");
        AtomicFileWriter writer = new AtomicFileWriter(target);

        writer.writeAtomically("content");

        assertTrue(Files.exists(target));
        assertEquals("content", Files.readString(target));
    }

    @Test
    void writeLeavesNoTempFileBehind(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("data.yml");
        AtomicFileWriter writer = new AtomicFileWriter(target);

        writer.writeAtomically("content");

        assertFalse(Files.exists(dir.resolve("data.yml.tmp")));
    }

    @Test
    void existsReturnsTrueWhenFilePresent(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("data.yml");
        Files.writeString(target, "x");
        assertTrue(new AtomicFileWriter(target).exists());
    }

    @Test
    void existsReturnsFalseWhenFileAbsent(@TempDir Path dir) {
        Path target = dir.resolve("missing.yml");
        assertFalse(new AtomicFileWriter(target).exists());
    }

    @Test
    void newReaderReturnsFileContent(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("data.yml");
        Files.writeString(target, "yaml-content", StandardCharsets.UTF_8);
        AtomicFileWriter writer = new AtomicFileWriter(target);

        try (Reader reader = writer.newReader()) {
            char[] buf = new char[64];
            int n = reader.read(buf);
            assertEquals("yaml-content", new String(buf, 0, n));
        }
    }

    @Test
    void newReaderOnMissingFileThrows(@TempDir Path dir) {
        Path target = dir.resolve("missing.yml");
        AtomicFileWriter writer = new AtomicFileWriter(target);
        assertThrows(NoSuchFileException.class, writer::newReader);
    }

    @Test
    void backupCorruptRenamesWithBrokenSuffix(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("data.yml");
        Files.writeString(target, "broken");
        AtomicFileWriter writer = new AtomicFileWriter(target);

        Path backup = writer.backupCorrupt();

        assertFalse(Files.exists(target));
        assertTrue(Files.exists(backup));
        assertTrue(backup.getFileName().toString().startsWith("data.yml.broken-"));
        assertEquals("broken", Files.readString(backup));
    }

    @Test
    void writeAtomicallyFallsBackWhenAtomicMoveUnsupported(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("data.yml");
        AtomicFileWriter writer = new AtomicFileWriter(target);

        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class, Mockito.CALLS_REAL_METHODS)) {
            filesMock.when(() -> Files.move(
                    Mockito.any(Path.class),
                    Mockito.any(Path.class),
                    Mockito.eq(StandardCopyOption.ATOMIC_MOVE),
                    Mockito.eq(StandardCopyOption.REPLACE_EXISTING)
            )).thenThrow(new AtomicMoveNotSupportedException("src", "dst", "no atomic"));

            writer.writeAtomically("content");

            assertEquals("content", Files.readString(target));
        }
    }

    @Test
    void writePropagatesIOExceptionFromWrite(@TempDir Path dir) {
        Path target = dir.resolve("data.yml");
        AtomicFileWriter writer = new AtomicFileWriter(target);

        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class, Mockito.CALLS_REAL_METHODS)) {
            filesMock.when(() -> Files.newBufferedWriter(Mockito.any(Path.class), Mockito.eq(StandardCharsets.UTF_8)))
                    .thenThrow(new IOException("disk full"));

            assertThrows(IOException.class, () -> writer.writeAtomically("x"));
        }
    }

    @Test
    void constructorRejectsNull() {
        assertThrows(NullPointerException.class, () -> new AtomicFileWriter(null));
    }

    @Test
    void getTargetReturnsConfiguredPath(@TempDir Path dir) {
        Path target = dir.resolve("data.yml");
        assertEquals(target, new AtomicFileWriter(target).getTarget());
    }
}
