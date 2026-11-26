package com.minios.scheduler;

import com.minios.bench.WorkloadGenerator;
import com.minios.process.SimProcess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulerTest {

    /** Silberschatz, Operating System Concepts: P1(0,8) P2(1,4) P3(2,9) P4(3,5). */
    private static List<SimProcess> textbook() {
        return List.of(new SimProcess(1, 0, 8), new SimProcess(2, 1, 4),
                new SimProcess(3, 2, 9), new SimProcess(4, 3, 5));
    }

    private static GanttSlice s(int pid, int start, int end) {
        return new GanttSlice(pid, start, end);
    }

    @Test
    void fcfsRunsInArrivalOrder() {
        ScheduleResult r = new FcfsScheduler().schedule(textbook());
        assertEquals(List.of(s(1, 0, 8), s(2, 8, 12), s(3, 12, 21), s(4, 21, 26)), r.timeline());
        assertEquals(8.75, r.averageWaitingTime(), 1e-9);
        assertEquals(3, r.contextSwitches());
    }

    @Test
    void sjfPicksShortestReadyJob() {
        ScheduleResult r = new SjfScheduler(false).schedule(textbook());
        assertEquals(List.of(s(1, 0, 8), s(2, 8, 12), s(4, 12, 17), s(3, 17, 26)), r.timeline());
        assertEquals(7.75, r.averageWaitingTime(), 1e-9);
    }

    @Test
    void srtfPreemptsOnShorterArrival() {
        ScheduleResult r = new SjfScheduler(true).schedule(textbook());
        assertEquals(List.of(s(1, 0, 1), s(2, 1, 5), s(4, 5, 10), s(1, 10, 17), s(3, 17, 26)), r.timeline());
        assertEquals(6.5, r.averageWaitingTime(), 1e-9);
        assertEquals(4, r.contextSwitches());
    }

    @Test
    void roundRobinQueuesArrivalsBeforePreemptedProcess() {
        ScheduleResult r = new RoundRobinScheduler(4).schedule(textbook());
        assertEquals(List.of(s(1, 0, 4), s(2, 4, 8), s(3, 8, 12), s(4, 12, 16),
                s(1, 16, 20), s(3, 20, 24), s(4, 24, 25), s(3, 25, 26)), r.timeline());
        assertEquals(11.75, r.averageWaitingTime(), 1e-9);
    }

    @Test
    void nonPreemptivePriorityTextbookExample() {
        List<SimProcess> w = List.of(new SimProcess(1, 0, 10, 3), new SimProcess(2, 0, 1, 1),
                new SimProcess(3, 0, 2, 4), new SimProcess(4, 0, 1, 5), new SimProcess(5, 0, 5, 2));
        ScheduleResult r = new PriorityScheduler(false).schedule(w);
        assertEquals(List.of(s(2, 0, 1), s(5, 1, 6), s(1, 6, 16), s(3, 16, 18), s(4, 18, 19)), r.timeline());
        assertEquals(8.2, r.averageWaitingTime(), 1e-9);
    }

    @Test
    void preemptivePriorityPreemptsOnHigherPriorityArrival() {
        List<SimProcess> w = List.of(new SimProcess(1, 0, 5, 5), new SimProcess(2, 2, 2, 1));
        ScheduleResult r = new PriorityScheduler(true).schedule(w);
        assertEquals(List.of(s(1, 0, 2), s(2, 2, 4), s(1, 4, 7)), r.timeline());
    }

    @Test
    void idleGapsAppearInTimeline() {
        ScheduleResult r = new FcfsScheduler().schedule(List.of(new SimProcess(1, 0, 2), new SimProcess(2, 5, 3)));
        assertEquals(List.of(s(1, 0, 2), s(GanttSlice.IDLE, 2, 5), s(2, 5, 8)), r.timeline());
        assertEquals(1, r.contextSwitches()); // P1 -> P2 counts once; idle time does not
    }

    @Test
    void mlfqLetsShortJobOvertakeLongJob() {
        List<SimProcess> w = List.of(new SimProcess(1, 0, 50), new SimProcess(2, 1, 2));
        ScheduleResult r = new MlfqScheduler().schedule(w);
        assertEquals(4, r.process(2).completionTime());
        assertEquals(52, r.process(1).completionTime());
    }

    @Test
    void mlfqPreemptsLowerLevelOnArrival() {
        // P1 is demoted after 2 units; P2 arriving at 3 must preempt it immediately.
        List<SimProcess> w = List.of(new SimProcess(1, 0, 20), new SimProcess(2, 3, 1));
        ScheduleResult r = new MlfqScheduler(new int[] {2, 4, 8}, 0).schedule(w);
        assertEquals(List.of(s(1, 0, 3), s(2, 3, 4), s(1, 4, 21)), r.timeline());
    }

    @Test
    void inputWorkloadIsNotModified() {
        List<SimProcess> w = textbook();
        new RoundRobinScheduler(2).schedule(w);
        for (SimProcess p : w) {
            assertEquals(p.burstTime(), p.remainingTime());
            assertEquals(-1, p.completionTime());
        }
    }

    @Test
    void duplicatePidsAreRejected() {
        List<SimProcess> w = List.of(new SimProcess(1, 0, 1), new SimProcess(1, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> new FcfsScheduler().schedule(w));
    }

    /** Properties every correct scheduler must satisfy, checked on a random 2,000-process workload. */
    @ParameterizedTest
    @ValueSource(longs = {1, 7, 42})
    void allSchedulersProduceValidTimelines(long seed) {
        List<SimProcess> workload = WorkloadGenerator.processes(2_000, seed, 0.95);
        for (Scheduler scheduler : Schedulers.standardSet(4)) {
            ScheduleResult r = scheduler.schedule(workload);
            String name = scheduler.name();
            assertEquals(workload.size(), r.processes().size(), name);

            Map<Integer, Integer> cpuTime = new HashMap<>();
            Map<Integer, Integer> lastEnd = new HashMap<>();
            int previousEnd = 0;
            for (GanttSlice slice : r.timeline()) {
                assertEquals(previousEnd, slice.start(), name + ": timeline must be contiguous");
                previousEnd = slice.end();
                if (slice.isIdle()) {
                    continue;
                }
                SimProcess p = r.process(slice.pid());
                assertTrue(slice.start() >= p.arrivalTime(), name + ": ran before arrival");
                cpuTime.merge(slice.pid(), slice.length(), Integer::sum);
                lastEnd.put(slice.pid(), slice.end());
            }
            for (SimProcess p : r.processes()) {
                assertEquals(p.burstTime(), cpuTime.get(p.pid()), name + ": P" + p.pid() + " CPU time");
                assertEquals(lastEnd.get(p.pid()), p.completionTime(), name + ": P" + p.pid() + " completion");
                assertTrue(p.waitingTime() >= 0, name);
            }
        }
    }

    @Test
    void srtfHasLowestAverageWaitingTime() {
        List<SimProcess> workload = WorkloadGenerator.processes(10_000, 42, 0.9);
        double srtf = new SjfScheduler(true).schedule(workload).averageWaitingTime();
        for (Scheduler s : Schedulers.standardSet(4)) {
            assertTrue(srtf <= s.schedule(workload).averageWaitingTime() + 1e-9, s.name());
        }
    }
}
