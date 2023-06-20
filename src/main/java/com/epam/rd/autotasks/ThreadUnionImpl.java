package com.epam.rd.autotasks;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadUnionImpl implements ThreadUnion{
    private final List<Thread> threads;
    private final List<FinishedThreadResult> res;
    private final AtomicInteger counter;
    private boolean shutdown;
    String name;
    private static final String NAME_THREAD_PATTERN;

    static {
        NAME_THREAD_PATTERN = "%s-worker-%d";
    }

    public ThreadUnionImpl(String name) {
        this.name = name;
        threads = Collections.synchronizedList(new ArrayList<>());
        res = Collections.synchronizedList(new ArrayList<>());
        counter = new AtomicInteger(0);
    }

    @Override
    public int totalSize() {
        return threads.size();
    }

    @Override
    public int activeSize() {
        return (int) threads.stream()
                .filter(Thread::isAlive)
                .count();
    }

    @Override
    public void shutdown() {
        shutdown = true;
        threads.forEach(Thread::interrupt);
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public void awaitTermination() {
        synchronized (threads) {
                threads.forEach(thread -> {
                    try {
                        thread.join();
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                });
        }
    }

    @Override
    public boolean isFinished() {
        return isShutdown()&&activeSize()==0;
    }

    @Override
    public List<FinishedThreadResult> results() {
        return res;
    }

    @NotNull
    @Override
    public Thread newThread(@NotNull Runnable r) {
        if (isShutdown()) {
            throw new IllegalStateException("Thread is shutdown");
        }
        Thread thread = getThread(r);
        thread.setUncaughtExceptionHandler((t, e) ->
                res.add(new FinishedThreadResult(t.getName(), e)));
        threads.add(thread);
        return thread;
    }
    private Thread getThread(Runnable r) {
        return new Thread(r, String.format(NAME_THREAD_PATTERN, name, counter.getAndIncrement())) {
            @Override
            public void run() {
                super.run();
                res.add(new FinishedThreadResult(this.getName()));
            }
        };
    }
}
