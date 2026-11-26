package com.minios.realtime;

import java.util.Objects;

/** A periodic real-time task: needs {@code wcet} units of CPU every {@code period}; deadline = period. */
public record PeriodicTask(String name, int wcet, int period) {

    public PeriodicTask {
        Objects.requireNonNull(name, "name");
        if (wcet <= 0 || period <= 0 || wcet > period) {
            throw new IllegalArgumentException(name + ": need 0 < wcet <= period, got wcet=" + wcet + " period=" + period);
        }
    }

    public double utilization() {
        return (double) wcet / period;
    }
}
