package com.minios.scheduler;

import com.minios.process.SimProcess;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.ToIntFunction;

/** The outcome of running one scheduler on one workload: per-process metrics plus the Gantt timeline. */
public final class ScheduleResult {

    private final String algorithm;
    private final List<SimProcess> processes;
    private final Map<Integer, SimProcess> byPid = new HashMap<>();
    private final List<GanttSlice> timeline;
    private final int contextSwitches;

    ScheduleResult(String algorithm, List<SimProcess> processes, Timeline timeline) {
        this.algorithm = algorithm;
        List<SimProcess> sorted = new ArrayList<>(processes);
        sorted.sort(Comparator.comparingInt(SimProcess::pid));
        this.processes = List.copyOf(sorted);
        for (SimProcess p : sorted) {
            byPid.put(p.pid(), p);
        }
        this.timeline = List.copyOf(timeline.slices());
        this.contextSwitches = timeline.contextSwitches();
    }

    public String algorithm() {
        return algorithm;
    }

    /** Completed processes, ordered by pid. */
    public List<SimProcess> processes() {
        return processes;
    }

    public SimProcess process(int pid) {
        SimProcess p = byPid.get(pid);
        if (p == null) {
            throw new NoSuchElementException("no process with pid " + pid);
        }
        return p;
    }

    public List<GanttSlice> timeline() {
        return timeline;
    }

    public int contextSwitches() {
        return contextSwitches;
    }

    public double averageWaitingTime() {
        return average(SimProcess::waitingTime);
    }

    public double averageTurnaroundTime() {
        return average(SimProcess::turnaroundTime);
    }

    public double averageResponseTime() {
        return average(SimProcess::responseTime);
    }

    /** Longest any single process waited: a quick starvation indicator. */
    public int maxWaitingTime() {
        return processes.stream().mapToInt(SimProcess::waitingTime).max().orElse(0);
    }

    /** Time at which the last process finished. */
    public int makespan() {
        return timeline.isEmpty() ? 0 : timeline.get(timeline.size() - 1).end();
    }

    /** Total time the CPU spent running processes. */
    public int busyTime() {
        return timeline.stream().filter(s -> !s.isIdle()).mapToInt(GanttSlice::length).sum();
    }

    /** Busy time as a percentage of the interval from the first arrival to the last completion. */
    public double cpuUtilization() {
        int span = makespan() - firstArrival();
        return span <= 0 ? 0.0 : 100.0 * busyTime() / span;
    }

    /** Completed processes per time unit over the same interval as {@link #cpuUtilization()}. */
    public double throughput() {
        int span = makespan() - firstArrival();
        return span <= 0 ? 0.0 : (double) processes.size() / span;
    }

    private int firstArrival() {
        return processes.stream().mapToInt(SimProcess::arrivalTime).min().orElse(0);
    }

    private double average(ToIntFunction<SimProcess> metric) {
        return processes.stream().mapToInt(metric).average().orElse(0.0);
    }
}
