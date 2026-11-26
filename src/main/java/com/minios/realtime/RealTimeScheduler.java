package com.minios.realtime;

import com.minios.scheduler.GanttSlice;
import com.minios.scheduler.Timeline;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Discrete-time simulation of periodic tasks under Rate Monotonic or EDF scheduling.
 * Each task releases a job every period, with an implicit deadline at the next release.
 * A job still unfinished at its deadline is recorded as a miss and dropped.
 */
public final class RealTimeScheduler {

    /** Simulations longer than this are cut short even if the hyperperiod is larger. */
    public static final int MAX_HORIZON = 100_000;

    private RealTimeScheduler() {
    }

    private static final class Job {
        final int task;
        final int release;
        final int deadline;
        int remaining;

        Job(int task, int release, int deadline, int remaining) {
            this.task = task;
            this.release = release;
            this.deadline = deadline;
            this.remaining = remaining;
        }
    }

    /** Least common multiple of all periods (capped at {@link #MAX_HORIZON}). */
    public static int hyperperiod(List<PeriodicTask> tasks) {
        long lcm = 1;
        for (PeriodicTask t : tasks) {
            lcm = lcm / gcd(lcm, t.period()) * t.period();
            if (lcm > MAX_HORIZON) {
                return MAX_HORIZON;
            }
        }
        return (int) lcm;
    }

    /** Simulates exactly one hyperperiod, after which the schedule repeats. */
    public static RealTimeResult simulate(List<PeriodicTask> tasks, RtPolicy policy) {
        return simulate(tasks, policy, hyperperiod(tasks));
    }

    public static RealTimeResult simulate(List<PeriodicTask> tasks, RtPolicy policy, int horizon) {
        if (tasks.isEmpty()) {
            throw new IllegalArgumentException("no tasks");
        }
        if (horizon <= 0) {
            throw new IllegalArgumentException("horizon must be > 0");
        }
        List<Job> active = new ArrayList<>();
        List<RealTimeResult.DeadlineMiss> misses = new ArrayList<>();
        int[] completed = new int[tasks.size()];
        Timeline timeline = new Timeline();

        for (int t = 0; t < horizon; t++) {
            collectMisses(active, t, tasks, misses);
            for (int i = 0; i < tasks.size(); i++) {
                PeriodicTask task = tasks.get(i);
                if (t % task.period() == 0) {
                    active.add(new Job(i, t, t + task.period(), task.wcet()));
                }
            }

            Job pick = null;
            for (Job job : active) {
                if (pick == null || runsBefore(job, pick, policy, tasks)) {
                    pick = job;
                }
            }
            if (pick == null) {
                timeline.record(GanttSlice.IDLE, t, t + 1);
            } else {
                timeline.record(pick.task, t, t + 1);
                pick.remaining--;
                if (pick.remaining == 0) {
                    active.remove(pick);
                    completed[pick.task]++;
                }
            }
        }
        collectMisses(active, horizon, tasks, misses);
        return new RealTimeResult(policy, List.copyOf(tasks), horizon, timeline.slices(), misses, completed);
    }

    private static boolean runsBefore(Job a, Job b, RtPolicy policy, List<PeriodicTask> tasks) {
        if (policy == RtPolicy.RATE_MONOTONIC) {
            int pa = tasks.get(a.task).period();
            int pb = tasks.get(b.task).period();
            if (pa != pb) {
                return pa < pb;
            }
        } else if (a.deadline != b.deadline) {
            return a.deadline < b.deadline;
        }
        if (a.task != b.task) {
            return a.task < b.task;
        }
        return a.release < b.release;
    }

    private static void collectMisses(List<Job> active, int now, List<PeriodicTask> tasks,
                                      List<RealTimeResult.DeadlineMiss> misses) {
        Iterator<Job> it = active.iterator();
        while (it.hasNext()) {
            Job job = it.next();
            if (job.deadline <= now) {
                misses.add(new RealTimeResult.DeadlineMiss(
                        tasks.get(job.task).name(), job.release, job.deadline, job.remaining));
                it.remove();
            }
        }
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long r = a % b;
            a = b;
            b = r;
        }
        return a;
    }
}
