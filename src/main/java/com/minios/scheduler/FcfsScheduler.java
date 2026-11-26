package com.minios.scheduler;

import com.minios.process.SimProcess;

import java.util.List;

/** First-Come, First-Served: non-preemptive, in arrival order. O(1) per scheduling decision. */
public final class FcfsScheduler implements Scheduler {

    @Override
    public String name() {
        return "FCFS";
    }

    @Override
    public ScheduleResult schedule(List<SimProcess> workload) {
        List<SimProcess> processes = SchedulerSupport.sortedCopies(workload);
        Timeline timeline = new Timeline();
        int time = 0;
        for (SimProcess p : processes) {
            if (time < p.arrivalTime()) {
                timeline.record(GanttSlice.IDLE, time, p.arrivalTime());
                time = p.arrivalTime();
            }
            p.markStarted(time);
            int burst = p.remainingTime();
            timeline.record(p.pid(), time, time + burst);
            time += burst;
            p.run(burst);
            p.markCompleted(time);
        }
        return new ScheduleResult(name(), processes, timeline);
    }
}
