package com.minios.memory;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContiguousAllocatorTest {

    /** Layout after setup: hole[0,30) P2[30,40) hole[40,55) P4[55,65) hole[65,100). */
    private static ContiguousAllocator fragmented(ContiguousAllocator.Fit fit) {
        ContiguousAllocator a = new ContiguousAllocator(100, ContiguousAllocator.Fit.FIRST);
        a.allocate(1, 30);
        a.allocate(2, 10);
        a.allocate(3, 15);
        a.allocate(4, 10);
        a.free(1);
        a.free(3);
        a.setFit(fit);
        return a;
    }

    @Test
    void firstBestAndWorstFitChooseDifferentHoles() {
        assertEquals(0, fragmented(ContiguousAllocator.Fit.FIRST).allocate(9, 12).getAsInt());
        assertEquals(40, fragmented(ContiguousAllocator.Fit.BEST).allocate(9, 12).getAsInt());
        assertEquals(65, fragmented(ContiguousAllocator.Fit.WORST).allocate(9, 12).getAsInt());
    }

    @Test
    void freeingCoalescesNeighbouringHoles() {
        ContiguousAllocator a = fragmented(ContiguousAllocator.Fit.FIRST);
        assertEquals(3, a.freeBlockCount());
        assertEquals(10, a.free(2));
        assertEquals(2, a.freeBlockCount());
        assertEquals(55, a.largestFreeBlock());
    }

    @Test
    void externalFragmentationCanBlockAnAllocation() {
        ContiguousAllocator a = fragmented(ContiguousAllocator.Fit.FIRST);
        assertEquals(80, a.freeMemory());
        OptionalInt result = a.allocate(9, 40); // 80 free in total, but no single hole of 40
        assertTrue(result.isEmpty());
        assertEquals(1.0 - 35.0 / 80.0, a.externalFragmentation(), 1e-9);
    }

    @Test
    void freeingEverythingRestoresOneHole() {
        ContiguousAllocator a = fragmented(ContiguousAllocator.Fit.BEST);
        a.free(2);
        a.free(4);
        assertEquals(1, a.freeBlockCount());
        assertEquals(100, a.largestFreeBlock());
        assertEquals(0.0, a.externalFragmentation(), 1e-9);
    }
}
