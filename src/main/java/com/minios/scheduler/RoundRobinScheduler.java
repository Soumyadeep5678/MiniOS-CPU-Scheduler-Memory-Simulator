package com.minios.scheduler;

import com.minios.process.SimProcess;

import java.util.ArrayDeque;
import java.util.List;

/**
 * Round Robin with a fixed time quantum. Processes that arrive while a quantum is running
 * join the ready queue <em>before</em> the preempted process (the usual textbook convention).
 * O(1) per scheduling decision.
 */
public final class RoundRobinScheduler implements Scheduler {

    private final int quantum;

    public RoundRobinScheduler(int quantum) {
        if (quantum <= 0) {
            throw new IllegalArgumentException("quantum must be > 0, was " + quantum);
        }
        this.quantum = quantum;
    }

    public int quantum() {
        return quantum;
    }

    @Override
    public String name() {
        return "Round Robin (q=" + quantum + ")";
    }

    @Override
    public ScheduleResult schedule(List<SimProcess> workload) {
        List<SimProcess> processes = SchedulerSupport.sortedCopies(workload);
        ArrayDeque<SimProcess> ready = new ArrayDeque<>();
        Timeline timeline = new Timeline();

        int n = processes.size();
        int next = 0;
        int done = 0;
        int time = 0;
        while (done < n) {
            while (next < n && processes.get(next).arrivalTime() <= time) {
                ready.addLast(processes.get(next++));
            }
            if (ready.isEmpty()) {
                int nextArrival = processes.get(next).arrivalTime();
                timeline.record(GanttSlice.IDLE, time, nextArrival);
                time = nextArrival;
                continue;
            }

            SimProcess p = ready.pollFirst();
            p.markStarted(time);
            int slice = Math.min(quantum, p.remainingTime());
            timeline.record(p.pid(), time, time + slice);
            time += slice;
            p.run(slice);

            // Arrivals during this quantum go ahead of the preempted process.
            while (next < n && processes.get(next).arrivalTime() <= time) {
                ready.addLast(processes.get(next++));
            }
            if (p.isFinished()) {
                p.markCompleted(time);
                done++;
            } else {
                ready.addLast(p);
            }
        }
        return new ScheduleResult(name(), processes, timeline);
    }
}
