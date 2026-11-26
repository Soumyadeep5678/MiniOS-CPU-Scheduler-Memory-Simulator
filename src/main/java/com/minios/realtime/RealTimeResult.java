package com.minios.realtime;

import com.minios.scheduler.GanttSlice;

import java.util.List;

/**
 * Outcome of a real-time simulation. In {@code timeline}, the pid of a slice is the index of
 * the task in {@code tasks}.
 */
public record RealTimeResult(RtPolicy policy, List<PeriodicTask> tasks, int horizon,
                             List<GanttSlice> timeline, List<DeadlineMiss> misses, int[] completedJobs) {

    public record DeadlineMiss(String task, int release, int deadline, int remaining) {
    }

    public double utilization() {
        return tasks.stream().mapToDouble(PeriodicTask::utilization).sum();
    }

    /** Liu and Layland: RM is guaranteed to meet all deadlines when U <= n(2^(1/n) - 1). */
    public double liuLaylandBound() {
        int n = tasks.size();
        return n == 0 ? 0.0 : n * (Math.pow(2.0, 1.0 / n) - 1.0);
    }

    public boolean allDeadlinesMet() {
        return misses.isEmpty();
    }
}
