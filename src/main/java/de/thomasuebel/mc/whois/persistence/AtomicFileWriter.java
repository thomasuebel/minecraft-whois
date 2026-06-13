package de.thomasuebel.mc.whois.persistence;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public final class AtomicFileWriter {

    private final Path target;
    private final Path tmp;

    public AtomicFileWriter(Path target) {
        this.target = Objects.requireNonNull(target, "target");
        this.tmp = target.resolveSibling(target.getFileName() + ".tmp");
    }

    public Path getTarget() {
        return target;
    }

    public boolean exists() {
        return Files.exists(target);
    }

    public Reader newReader() throws IOException {
        return Files.newBufferedReader(target, StandardCharsets.UTF_8);
    }

    public void writeAtomically(String content) throws IOException {
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            w.write(content);
        }
        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public Path backupCorrupt() throws IOException {
        Path backup = target.resolveSibling(target.getFileName() + ".broken-" + System.currentTimeMillis());
        Files.move(target, backup, StandardCopyOption.REPLACE_EXISTING);
        return backup;
    }
}
