package com.minios.memory;

/**
 * Second-chance (clock) replacement: a cheap LRU approximation used by real kernels. A hand
 * sweeps the frames; a frame with its reference bit set gets the bit cleared and is skipped
 * once. Amortised O(1) per reference.
 */
public final class ClockPageReplacer extends PageReplacer {

    private boolean[] referenced = new boolean[0];
    private int hand;

    @Override
    public String name() {
        return "Clock (Second Chance)";
    }

    @Override
    protected void reset(int frameCount, int[] references) {
        referenced = new boolean[frameCount];
        hand = 0;
    }

    @Override
    protected void onHit(int frame, int index) {
        referenced[frame] = true;
    }

    @Override
    protected void onLoad(int frame, int index) {
        referenced[frame] = true;
    }

    @Override
    protected int chooseVictim(int index) {
        while (referenced[hand]) {
            referenced[hand] = false;
            hand = (hand + 1) % referenced.length;
        }
        int victim = hand;
        hand = (hand + 1) % referenced.length;
        return victim;
    }
}
