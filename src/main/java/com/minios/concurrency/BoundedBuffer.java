package com.minios.concurrency;

import java.util.concurrent.Semaphore;

/**
 * The classic semaphore solution to the bounded-buffer (producer-consumer) problem:
 * {@code emptySlots} blocks producers when the buffer is full, {@code fullSlots} blocks
 * consumers when it is empty, and a binary semaphore guards the ring buffer itself.
 */
public final class BoundedBuffer<T> {

    private final Object[] items;
    private int head;
    private int tail;
    private int count;
    private volatile int maxOccupancy;

    private final Semaphore emptySlots;
    private final Semaphore fullSlots = new Semaphore(0);
    private final Semaphore mutex = new Semaphore(1);

    public BoundedBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        items = new Object[capacity];
        emptySlots = new Semaphore(capacity);
    }

    public void put(T item) throws InterruptedException {
        emptySlots.acquire();
        mutex.acquire();
        try {
            items[tail] = item;
            tail = (tail + 1) % items.length;
            count++;
            if (count > maxOccupancy) {
                maxOccupancy = count;
            }
        } finally {
            mutex.release();
        }
        fullSlots.release();
    }

    @SuppressWarnings("unchecked")
    public T take() throws InterruptedException {
        fullSlots.acquire();
        T item;
        mutex.acquire();
        try {
            item = (T) items[head];
            items[head] = null;
            head = (head + 1) % items.length;
            count--;
        } finally {
            mutex.release();
        }
        emptySlots.release();
        return item;
    }

    public int capacity() {
        return items.length;
    }

    /** Highest number of items ever held at once; must never exceed {@link #capacity()}. */
    public int maxOccupancy() {
        return maxOccupancy;
    }
}
