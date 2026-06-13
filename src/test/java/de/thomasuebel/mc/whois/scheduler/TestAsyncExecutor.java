package de.thomasuebel.mc.whois.scheduler;

import java.util.ArrayDeque;
import java.util.Deque;

final class TestAsyncExecutor implements AsyncExecutor {

    private final Deque<Runnable> async = new ArrayDeque<>();

    @Override
    public void runAsync(Runnable task) {
        async.add(task);
    }

    @Override
    public void runSync(Runnable task) {
        task.run();
    }

    int pending() {
        return async.size();
    }

    boolean drainOne() {
        Runnable r = async.poll();
        if (r == null) {
            return false;
        }
        r.run();
        return true;
    }
}
