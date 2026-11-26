package com.minios.memory;

import com.minios.bench.WorkloadGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageReplacementTest {

    private static final int[] TEXTBOOK = {7, 0, 1, 2, 0, 3, 0, 4, 2, 3, 0, 3, 2, 1, 2, 0, 1, 7, 0, 1};
    private static final int[] BELADY = {1, 2, 3, 4, 1, 2, 5, 1, 2, 3, 4, 5};

    @Test
    void textbookFaultCounts() {
        assertEquals(15, new FifoPageReplacer().run(TEXTBOOK, 3, false).faults());
        assertEquals(12, new LruPageReplacer().run(TEXTBOOK, 3, false).faults());
        assertEquals(9, new OptimalPageReplacer().run(TEXTBOOK, 3, false).faults());
    }

    @Test
    void fifoShowsBeladysAnomaly() {
        assertEquals(9, new FifoPageReplacer().run(BELADY, 3, false).faults());
        assertEquals(10, new FifoPageReplacer().run(BELADY, 4, false).faults());
    }

    @Test
    void lruAndOptimalOnBeladyString() {
        assertEquals(10, new LruPageReplacer().run(BELADY, 3, false).faults());
        assertEquals(7, new OptimalPageReplacer().run(BELADY, 3, false).faults());
    }

    @Test
    void stepsRecordFramesAndEvictions() {
        PagingResult r = new FifoPageReplacer().run(TEXTBOOK, 3, true);
        assertEquals(TEXTBOOK.length, r.steps().size());
        assertArrayEquals(new int[] {7, -1, -1}, r.steps().get(0).frames());
        PagingStep fourth = r.steps().get(3); // reference to 2 evicts 7, the oldest page
        assertTrue(fourth.fault());
        assertEquals(7, fourth.evicted());
        assertArrayEquals(new int[] {2, 0, 1}, fourth.frames());
        assertFalse(r.steps().get(4).fault()); // 0 is still resident
    }

    @Test
    void optimalIsALowerBound() {
        int[] refs = WorkloadGenerator.references(20_000, 64, 8, 0.85, 3);
        for (int frames : new int[] {2, 4, 8, 16}) {
            int optimal = new OptimalPageReplacer().run(refs, frames, false).faults();
            for (PageReplacer p : PageReplacers.standardSet()) {
                assertTrue(optimal <= p.run(refs, frames, false).faults(), p.name() + " beat Optimal");
            }
        }
    }

    @Test
    void lruNeverGetsWorseWithMoreFrames() {
        // LRU is a stack algorithm, so it is immune to Belady's anomaly.
        int[] refs = WorkloadGenerator.references(5_000, 32, 6, 0.8, 11);
        int previous = Integer.MAX_VALUE;
        for (int frames = 1; frames <= 32; frames++) {
            int faults = new LruPageReplacer().run(refs, frames, false).faults();
            assertTrue(faults <= previous, "frames=" + frames);
            previous = faults;
        }
    }

    @Test
    void replacerInstancesAreReusable() {
        PageReplacer lru = new LruPageReplacer();
        assertEquals(lru.run(TEXTBOOK, 3, false).faults(), lru.run(TEXTBOOK, 3, false).faults());
    }
}
