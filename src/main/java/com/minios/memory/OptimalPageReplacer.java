package com.minios.memory;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * Belady's optimal policy: evict the page whose next use is furthest in the future.
 *
 * <p>Not implementable in a real OS (it needs the future), but it is the lower bound other
 * policies are measured against. The next use of every reference is precomputed in one
 * backward pass, and resident frames are kept in a balanced tree keyed by next use, so each
 * reference costs O(log f) for f frames instead of scanning the rest of the string.
 */
public final class OptimalPageReplacer extends PageReplacer {

    private static final int NEVER = Integer.MAX_VALUE;

    private int[] nextUse = new int[0];
    private long[] keyOfFrame = new long[0];
    private final TreeSet<Long> byNextUse = new TreeSet<>();

    @Override
    public String name() {
        return "Optimal";
    }

    @Override
    protected void reset(int frameCount, int[] references) {
        nextUse = new int[references.length];
        Map<Integer, Integer> seenAt = new HashMap<>();
        for (int i = references.length - 1; i >= 0; i--) {
            nextUse[i] = seenAt.getOrDefault(references[i], NEVER);
            seenAt.put(references[i], i);
        }
        keyOfFrame = new long[frameCount];
        byNextUse.clear();
    }

    @Override
    protected void onHit(int frame, int index) {
        byNextUse.remove(keyOfFrame[frame]);
        track(frame, index);
    }

    @Override
    protected void onLoad(int frame, int index) {
        track(frame, index);
    }

    @Override
    protected int chooseVictim(int index) {
        long key = byNextUse.pollLast();
        return (int) (key & 0xFFFF_FFFFL);
    }

    /** Key = (next use << 32) | frame: ordered by next use, unique per frame. */
    private void track(int frame, int index) {
        long key = ((long) nextUse[index] << 32) | frame;
        keyOfFrame[frame] = key;
        byNextUse.add(key);
    }
}
