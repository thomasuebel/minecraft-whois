package de.thomasuebel.mc.whois.scheduler;

public interface AsyncExecutor {

    void runAsync(Runnable task);

    void runSync(Runnable task);
}
