package com.minios.memory;

import java.util.ArrayDeque;

/** Evicts the page that has been resident longest. O(1) per reference. Suffers from Belady's anomaly. */
public final class FifoPageReplacer extends PageReplacer {

    private final ArrayDeque<Integer> loadOrder = new ArrayDeque<>();

    @Override
    public String name() {
        return "FIFO";
    }

    @Override
    protected void reset(int frameCount, int[] references) {
        loadOrder.clear();
    }

    @Override
    protected void onHit(int frame, int index) {
        // FIFO ignores re-references.
    }

    @Override
    protected void onLoad(int frame, int index) {
        loadOrder.addLast(frame);
    }

    @Override
    protected int chooseVictim(int index) {
        return loadOrder.pollFirst();
    }
}
