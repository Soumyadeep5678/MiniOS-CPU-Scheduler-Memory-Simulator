package com.minios.memory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Template for a page-replacement policy over a fixed number of frames.
 *
 * <p>This class owns the frame table and a page-to-frame hash map (O(1) residency check).
 * Subclasses only keep the bookkeeping their policy needs and pick a victim frame when
 * memory is full. Instances are reusable but not thread-safe.
 */
public abstract class PageReplacer {

    public abstract String name();

    /** Clears policy state before a run. */
    protected abstract void reset(int frameCount, int[] references);

    /** A resident page in {@code frame} was referenced at position {@code index}. */
    protected abstract void onHit(int frame, int index);

    /** A page was just loaded into {@code frame} at position {@code index}. */
    protected abstract void onLoad(int frame, int index);

    /** Memory is full: return the frame to evict and drop its bookkeeping. */
    protected abstract int chooseVictim(int index);

    public final PagingResult run(int[] references, int frameCount, boolean recordSteps) {
        Objects.requireNonNull(references, "references");
        if (frameCount <= 0) {
            throw new IllegalArgumentException("frameCount must be > 0, was " + frameCount);
        }
        int[] frames = new int[frameCount];
        Arrays.fill(frames, -1);
        Map<Integer, Integer> frameOf = new HashMap<>();
        List<PagingStep> steps = recordSteps ? new ArrayList<>() : List.of();
        reset(frameCount, references);

        int used = 0;
        int faults = 0;
        for (int i = 0; i < references.length; i++) {
            int page = references[i];
            if (page < 0) {
                throw new IllegalArgumentException("page numbers must be >= 0, got " + page);
            }
            Integer frame = frameOf.get(page);
            boolean fault = frame == null;
            int evicted = -1;
            if (!fault) {
                onHit(frame, i);
            } else {
                faults++;
                if (used < frameCount) {
                    frame = used++;
                } else {
                    frame = chooseVictim(i);
                    evicted = frames[frame];
                    frameOf.remove(evicted);
                }
                frames[frame] = page;
                frameOf.put(page, frame);
                onLoad(frame, i);
            }
            if (recordSteps) {
                steps.add(new PagingStep(i, page, fault, evicted, frames.clone()));
            }
        }
        return new PagingResult(name(), frameCount, references.length, faults, steps);
    }
}
