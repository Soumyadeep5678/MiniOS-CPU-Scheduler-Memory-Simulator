package com.minios.realtime;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealTimeSchedulerTest {

    // U = 2/5 + 4/7 = 0.971: above the RM bound for two tasks (0.828) but <= 1.
    private static final List<PeriodicTask> TASKS =
            List.of(new PeriodicTask("T1", 2, 5), new PeriodicTask("T2", 4, 7));

    @Test
    void hyperperiodIsLcmOfPeriods() {
        assertEquals(35, RealTimeScheduler.hyperperiod(TASKS));
    }

    @Test
    void rateMonotonicMissesADeadline() {
        RealTimeResult r = RealTimeScheduler.simulate(TASKS, RtPolicy.RATE_MONOTONIC);
        assertFalse(r.allDeadlinesMet());
        RealTimeResult.DeadlineMiss first = r.misses().get(0);
        assertEquals("T2", first.task());
        assertEquals(7, first.deadline());
        assertEquals(1, first.remaining());
    }

    @Test
    void edfMeetsEveryDeadlineWhenUtilizationAtMostOne() {
        RealTimeResult r = RealTimeScheduler.simulate(TASKS, RtPolicy.EARLIEST_DEADLINE_FIRST);
        assertTrue(r.allDeadlinesMet(), () -> "misses: " + r.misses());
        assertArrayEquals(new int[] {7, 5}, r.completedJobs());
    }

    @Test
    void rateMonotonicMeetsDeadlinesUnderLiuLaylandBound() {
        List<PeriodicTask> light = List.of(new PeriodicTask("A", 1, 4), new PeriodicTask("B", 2, 8));
        RealTimeResult r = RealTimeScheduler.simulate(light, RtPolicy.RATE_MONOTONIC);
        assertTrue(r.utilization() <= r.liuLaylandBound());
        assertTrue(r.allDeadlinesMet());
    }
}
