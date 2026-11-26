package com.minios.scheduler;

import com.minios.process.SimProcess;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Base for schedulers that always pick the "best" ready process by some key (shortest job,
 * highest priority). The ready queue is a binary heap, so each scheduling decision costs
 * O(log n). Preemptive variants re-evaluate the choice whenever a new process arrives.
 *
 * <p>Ties are broken by earlier arrival, then lower pid, so results are deterministic.
 */
public abstract class HeapScheduler implements Scheduler {

    private final boolean preemptive;

    protected HeapScheduler(boolean preemptive) {
        this.preemptive = preemptive;
    }

    /** The primary ordering of the ready queue; smaller runs first. */
    protected abstract Comparator<SimProcess> order();

    public final boolean isPreemptive() {
        return preemptive;
    }

    @Override
    public final ScheduleResult schedule(List<SimProcess> workload) {
        List<SimProcess> processes = SchedulerSupport.sortedCopies(workload);
        PriorityQueue<SimProcess> ready = new PriorityQueue<>(
                order().thenComparingInt(SimProcess::arrivalTime).thenComparingInt(SimProcess::pid));
        Timeline timeline = new Timeline();

        int n = processes.size();
        int next = 0;
        int done = 0;
        int time = 0;
        while (done < n) {
            while (next < n && processes.get(next).arrivalTime() <= time) {
                ready.add(processes.get(next++));
            }
            if (ready.isEmpty()) {
                int nextArrival = processes.get(next).arrivalTime();
                timeline.record(GanttSlice.IDLE, time, nextArrival);
                time = nextArrival;
                continue;
            }

            SimProcess p = ready.poll();
            p.markStarted(time);
            int slice = p.remainingTime();
            if (preemptive && next < n) {
                // Run only until the next arrival, then let the heap re-decide.
                slice = Math.min(slice, processes.get(next).arrivalTime() - time);
            }
            timeline.record(p.pid(), time, time + slice);
            time += slice;
            p.run(slice);

            if (p.isFinished()) {
                p.markCompleted(time);
                done++;
            } else {
                ready.add(p);
            }
        }
        return new ScheduleResult(name(), processes, timeline);
    }
}
