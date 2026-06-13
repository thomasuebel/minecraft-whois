package de.thomasuebel.mc.whois.scheduler;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class WriteScheduler<T> {

    @FunctionalInterface
    public interface WriteAction<T> {
        void write(T snapshot) throws IOException;
    }

    private final AsyncExecutor executor;
    private final WriteAction<T> writer;
    private final Logger logger;
    private final AtomicReference<T> latest = new AtomicReference<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    public WriteScheduler(AsyncExecutor executor, WriteAction<T> writer, Logger logger) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.writer = Objects.requireNonNull(writer, "writer");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void submit(T snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        latest.set(snapshot);
        kick();
    }

    public void flushSync() {
        drainOnce();
    }

    private void kick() {
        if (running.compareAndSet(false, true)) {
            executor.runAsync(this::drain);
        }
    }

    private void drain() {
        try {
            drainOnce();
        } finally {
            running.set(false);
            if (latest.get() != null) {
                kick();
            }
        }
    }

    private void drainOnce() {
        T snapshot;
        while ((snapshot = latest.getAndSet(null)) != null) {
            try {
                writer.write(snapshot);
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Failed to persist Whois data", ex);
            }
        }
    }
}
