package com.minios.concurrency;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Dining philosophers with real threads and locks, under three strategies:
 *
 * <ul>
 *   <li>{@link Strategy#NAIVE}: take the left fork, then the right. All four Coffman
 *       conditions hold. To make the deadlock reproducible instead of rare, every
 *       philosopher waits at a barrier while holding the left fork on the first meal, so the
 *       circular wait always forms.</li>
 *   <li>{@link Strategy#RESOURCE_ORDERING}: always take the lower-numbered fork first.
 *       This breaks circular wait.</li>
 *   <li>{@link Strategy#WAITER}: a counting semaphore lets at most n-1 philosophers sit
 *       down. This also breaks circular wait.</li>
 * </ul>
 *
 * <p>Deadlock is detected by the JVM ({@link ThreadMXBean#findDeadlockedThreads()}), which
 * builds the wait-for graph over lock owners. On detection the philosophers are interrupted
 * so the run always ends.
 */
public final class DiningPhilosophers {

    public enum Strategy { NAIVE, RESOURCE_ORDERING, WAITER }

    public record Report(Strategy strategy, int philosophers, boolean deadlocked, int[] meals, long millis) {
        public int totalMeals() {
            int sum = 0;
            for (int m : meals) {
                sum += m;
            }
            return sum;
        }
    }

    private DiningPhilosophers() {
    }

    public static Report run(int n, int mealsEach, Strategy strategy, long timeoutMillis)
            throws InterruptedException {
        if (n < 2) {
            throw new IllegalArgumentException("need at least 2 philosophers");
        }
        ReentrantLock[] forks = new ReentrantLock[n];
        for (int i = 0; i < n; i++) {
            forks[i] = new ReentrantLock();
        }
        Semaphore waiter = new Semaphore(n - 1);
        CyclicBarrier allHoldLeftFork = new CyclicBarrier(n);
        AtomicIntegerArray meals = new AtomicIntegerArray(n);

        Thread[] threads = new Thread[n];
        for (int i = 0; i < n; i++) {
            int id = i;
            threads[i] = new Thread(
                    () -> dine(id, n, mealsEach, strategy, forks, waiter, allHoldLeftFork, meals),
                    "philosopher-" + i);
            threads[i].setDaemon(true);
        }

        long start = System.nanoTime();
        for (Thread t : threads) {
            t.start();
        }

        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        long deadline = System.currentTimeMillis() + timeoutMillis;
        boolean deadlocked = false;
        while (anyAlive(threads)) {
            if (isDeadlocked(mx, threads)) {
                deadlocked = true;
                break;
            }
            if (System.currentTimeMillis() > deadline) {
                break;
            }
            Thread.sleep(20);
        }
        // Snapshot the meal counts BEFORE interrupting. Interrupting makes the philosophers
        // put their forks down, which lets a neighbour finish one last meal; that would
        // misreport a deadlocked table as having eaten.
        int[] eaten = new int[n];
        for (int i = 0; i < n; i++) {
            eaten[i] = meals.get(i);
        }
        for (Thread t : threads) {
            t.interrupt();
        }
        for (Thread t : threads) {
            t.join(1000);
        }
        long millis = (System.nanoTime() - start) / 1_000_000;
        return new Report(strategy, n, deadlocked, eaten, millis);
    }

    private static void dine(int id, int n, int mealsEach, Strategy strategy, ReentrantLock[] forks,
                             Semaphore waiter, CyclicBarrier allHoldLeftFork, AtomicIntegerArray meals) {
        int left = id;
        int right = (id + 1) % n;
        int first = left;
        int second = right;
        if (strategy == Strategy.RESOURCE_ORDERING) {
            first = Math.min(left, right);
            second = Math.max(left, right);
        }
        try {
            for (int meal = 0; meal < mealsEach; meal++) {
                if (strategy == Strategy.WAITER) {
                    waiter.acquire();
                }
                try {
                    forks[first].lockInterruptibly();
                    try {
                        if (strategy == Strategy.NAIVE && meal == 0) {
                            allHoldLeftFork.await();
                        }
                        forks[second].lockInterruptibly();
                        try {
                            meals.incrementAndGet(id);
                        } finally {
                            forks[second].unlock();
                        }
                    } finally {
                        forks[first].unlock();
                    }
                } finally {
                    if (strategy == Strategy.WAITER) {
                        waiter.release();
                    }
                }
            }
        } catch (InterruptedException | BrokenBarrierException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean anyAlive(Thread[] threads) {
        for (Thread t : threads) {
            if (t.isAlive()) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation") // Thread.getId() keeps Java 17 compatibility
    private static boolean isDeadlocked(ThreadMXBean mx, Thread[] threads) {
        long[] ids = mx.findDeadlockedThreads();
        if (ids == null) {
            return false;
        }
        Set<Long> ours = new HashSet<>();
        for (Thread t : threads) {
            ours.add(t.getId());
        }
        for (long id : ids) {
            if (ours.contains(id)) {
                return true;
            }
        }
        return false;
    }
}
