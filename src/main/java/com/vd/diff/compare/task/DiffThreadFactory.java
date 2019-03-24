package com.vd.diff.compare.task;

import java.util.concurrent.ThreadFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiffThreadFactory implements ThreadFactory {

    private final ThreadGroup group;
    private final boolean daemonThreads;

    private DiffThreadFactory(String group, boolean daemonThreads) {
        this.group = new ThreadGroup(group);
        this.daemonThreads = daemonThreads;
    }

    public static ThreadFactory create(String group) {
        return new DiffThreadFactory(group, false);
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread t = new Thread(group, runnable, group.getName() + "-" + group.activeCount());
        t.setDaemon(daemonThreads);
        t.setUncaughtExceptionHandler((thread, e) ->
                log.error(String.format("Uncaught exception in thread '%s'", thread.getName()), e));
        return t;
    }
}
