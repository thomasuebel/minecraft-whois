package de.thomasuebel.mc.whois.scheduler;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WriteSchedulerTest {

    private final Logger logger = Logger.getLogger(WriteSchedulerTest.class.getName());

    @Test
    void submitSchedulesAsyncWrite() {
        TestAsyncExecutor exec = new TestAsyncExecutor();
        List<String> written = new ArrayList<>();
        WriteScheduler<String> scheduler = new WriteScheduler<>(exec, written::add, logger);

        scheduler.submit("a");
        assertEquals(1, exec.pending());
        assertTrue(written.isEmpty());

        exec.drainOne();
        assertEquals(List.of("a"), written);
    }

    @Test
    void multipleSubmitsWhileQueuedCoalesce() {
        TestAsyncExecutor exec = new TestAsyncExecutor();
        List<String> written = new ArrayList<>();
        WriteScheduler<String> scheduler = new WriteScheduler<>(exec, written::add, logger);

        scheduler.submit("v1");
        scheduler.submit("v2");
        scheduler.submit("v3");

        assertEquals(1, exec.pending(), "should not schedule one task per submit");

        exec.drainOne();
        assertEquals(List.of("v3"), written, "only latest snapshot is written");
    }

    @Test
    void submitAfterDrainSchedulesNewTask() {
        TestAsyncExecutor exec = new TestAsyncExecutor();
        List<String> written = new ArrayList<>();
        WriteScheduler<String> scheduler = new WriteScheduler<>(exec, written::add, logger);

        scheduler.submit("a");
        exec.drainOne();
        scheduler.submit("b");

        assertEquals(1, exec.pending());
        exec.drainOne();
        assertEquals(List.of("a", "b"), written);
    }

    @Test
    void submitDuringWriteIsDrainedBySameTask() {
        TestAsyncExecutor exec = new TestAsyncExecutor();
        List<String> written = new ArrayList<>();
        AtomicReference<WriteScheduler<String>> ref = new AtomicReference<>();

        WriteScheduler<String> scheduler = new WriteScheduler<>(exec, snap -> {
            written.add(snap);
            if (written.size() == 1) {
                ref.get().submit("b");
            }
        }, logger);
        ref.set(scheduler);

        scheduler.submit("a");
        exec.drainOne();

        assertEquals(List.of("a", "b"), written, "while-loop should pick up the inline submit");
        assertEquals(0, exec.pending(), "no extra task needed when loop already drained it");
    }

    @Test
    void writerIoExceptionIsCaughtNotPropagated() {
        TestAsyncExecutor exec = new TestAsyncExecutor();
        WriteScheduler<String> scheduler = new WriteScheduler<>(exec, snap -> {
            throw new IOException("disk full");
        }, logger);

        scheduler.submit("x");
        exec.drainOne(); // would throw if exception leaked
    }

    @Test
    void flushSyncWritesPendingSnapshotInline() {
        TestAsyncExecutor exec = new TestAsyncExecutor();
        List<String> written = new ArrayList<>();
        WriteScheduler<String> scheduler = new WriteScheduler<>(exec, written::add, logger);

        scheduler.submit("a");
        scheduler.flushSync();

        assertEquals(List.of("a"), written);
        exec.drainOne(); // queued task is now a no-op since latest was consumed
        assertEquals(List.of("a"), written);
    }

    @Test
    void flushSyncWithNoPendingIsNoop() {
        TestAsyncExecutor exec = new TestAsyncExecutor();
        List<String> written = new ArrayList<>();
        WriteScheduler<String> scheduler = new WriteScheduler<>(exec, written::add, logger);

        scheduler.flushSync();

        assertTrue(written.isEmpty());
    }

    @Test
    void flushSyncSwallowsIoException() {
        TestAsyncExecutor exec = new TestAsyncExecutor();
        WriteScheduler<String> scheduler = new WriteScheduler<>(exec, snap -> {
            throw new IOException("disk full");
        }, logger);

        scheduler.submit("x");
        scheduler.flushSync();
    }

    @Test
    void submitNullThrows() {
        TestAsyncExecutor exec = new TestAsyncExecutor();
        WriteScheduler<String> scheduler = new WriteScheduler<>(exec, s -> {}, logger);
        assertThrows(NullPointerException.class, () -> scheduler.submit(null));
    }

    @Test
    void constructorRejectsNullArgs() {
        assertThrows(NullPointerException.class,
                () -> new WriteScheduler<>(null, s -> {}, logger));
        assertThrows(NullPointerException.class,
                () -> new WriteScheduler<String>(new TestAsyncExecutor(), null, logger));
        assertThrows(NullPointerException.class,
                () -> new WriteScheduler<>(new TestAsyncExecutor(), s -> {}, null));
    }
}
