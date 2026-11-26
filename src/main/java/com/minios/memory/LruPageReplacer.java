package com.minios.memory;

import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * Evicts the least recently used page. Frames are kept in a linked hash set ordered from
 * least to most recently used, so every operation is O(1).
 */
public final class LruPageReplacer extends PageReplacer {

    private final LinkedHashSet<Integer> recency = new LinkedHashSet<>();

    @Override
    public String name() {
        return "LRU";
    }

    @Override
    protected void reset(int frameCount, int[] references) {
        recency.clear();
    }

    @Override
    protected void onHit(int frame, int index) {
        recency.remove(frame);
        recency.add(frame);
    }

    @Override
    protected void onLoad(int frame, int index) {
        recency.add(frame);
    }

    @Override
    protected int chooseVictim(int index) {
        Iterator<Integer> it = recency.iterator();
        int victim = it.next();
        it.remove();
        return victim;
    }
}
