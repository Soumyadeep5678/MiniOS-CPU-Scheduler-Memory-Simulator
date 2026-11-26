package com.minios.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Runs real producer and consumer threads over a {@link BoundedBuffer} and checks nothing is lost or duplicated. */
public final class ProducerConsumer {

    private static final int POISON = -1;

    private ProducerConsumer() {
    }

    public record Report(int producers, int consumers, int capacity,
                         long produced, long consumed, long producedSum, long consumedSum,
                         int maxOccupancy, long millis) {

        /** Every item was consumed exactly once and the buffer never overflowed. */
        public boolean consistent() {
            return produced == consumed && producedSum == consumedSum && maxOccupancy <= capacity;
        }
    }

    public static Report run(int producers, int consumers, int itemsPerProducer, int capacity)
            throws InterruptedException {
        if (producers <= 0 || consumers <= 0 || itemsPerProducer <= 0) {
            throw new IllegalArgumentException("producers, consumers and itemsPerProducer must be > 0");
        }
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(capacity);
        AtomicLong consumedCount = new AtomicLong();
        AtomicLong consumedSum = new AtomicLong();
        long start = System.nanoTime();

        List<Thread> producerThreads = new ArrayList<>();
        for (int p = 0; p < producers; p++) {
            int base = p * itemsPerProducer;
            Thread t = new Thread(() -> {
                try {
                    for (int k = 0; k < itemsPerProducer; k++) {
                        buffer.put(base + k); // unique, non-negative item ids
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "producer-" + p);
            producerThreads.add(t);
        }

        List<Thread> consumerThreads = new ArrayList<>();
        for (int c = 0; c < consumers; c++) {
            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        int item = buffer.take();
                        if (item == POISON) {
                            return;
                        }
                        consumedCount.incrementAndGet();
                        consumedSum.addAndGet(item);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "consumer-" + c);
            consumerThreads.add(t);
        }

        consumerThreads.forEach(Thread::start);
        producerThreads.forEach(Thread::start);
        for (Thread t : producerThreads) {
            t.join();
        }
        for (int c = 0; c < consumers; c++) {
            buffer.put(POISON); // one stop signal per consumer
        }
        for (Thread t : consumerThreads) {
            t.join();
        }

        long total = (long) producers * itemsPerProducer;
        long millis = (System.nanoTime() - start) / 1_000_000;
        return new Report(producers, consumers, capacity, total, consumedCount.get(),
                total * (total - 1) / 2, consumedSum.get(), buffer.maxOccupancy(), millis);
    }
}
