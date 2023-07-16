package com.epam.rd.autotasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.concurrent.atomic.AtomicInteger;

public class ThreadUnionImpl implements ThreadUnion {
    private final String FORMAT = "%s-worker-%d";
    private final List<Thread> threads = Collections.synchronizedList(new ArrayList<>());
    private final List<FinishedThreadResult> result = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger counter = new AtomicInteger(0);
    private final Object lock = new Object();
    private final String name;
    private boolean isShutdown = false;


    public ThreadUnionImpl(String name) {
        this.name = name;
    }


    @Override
    public int totalSize() {
        return threads.size();
    }

    @Override
    public int activeSize() {
        return (int) threads.stream().filter(Thread::isAlive).count();
    }

    @Override
    public void shutdown() {
        threads.forEach(Thread::interrupt);
        isShutdown = true;
    }

    @Override
    public boolean isShutdown() {
        return isShutdown;
    }

    @Override
    public void awaitTermination() {
        synchronized (lock) {
            threads.forEach(
                    thread -> {
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            thread.interrupt();
                        }
                    }
            );
        }


    }

    @Override
    public boolean isFinished() {
        return isShutdown() && activeSize() == 0;
    }

    @Override
    public List<FinishedThreadResult> results() {
        return result;
    }


    @Override
    public Thread newThread(Runnable r) {
        if (isShutdown) {
            throw new IllegalStateException();
        }

        Thread thread = createThread(r);
        thread.setUncaughtExceptionHandler((t, e) ->
                result.add(new FinishedThreadResult(t.getName(), e)));

        threads.add(thread);
        return thread;

    }

    private Thread createThread(Runnable runnable)  {
        return new Thread(runnable, String.format(FORMAT, name, counter.getAndIncrement())) {
            @Override
            public void run() {
                super.run();
                result.add(new FinishedThreadResult(this.getName()));
            }
        };

    }
}
