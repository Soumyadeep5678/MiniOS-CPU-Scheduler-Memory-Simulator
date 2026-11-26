package com.minios.scheduler;

import com.minios.process.SimProcess;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class SchedulerSupport {

    private SchedulerSupport() {
    }

    /** Fresh copies of the workload ordered by (arrival, pid); rejects duplicate pids. */
    static List<SimProcess> sortedCopies(List<SimProcess> workload) {
        Objects.requireNonNull(workload, "workload");
        Set<Integer> seen = new HashSet<>();
        List<SimProcess> copies = new ArrayList<>(workload.size());
        for (SimProcess p : workload) {
            if (!seen.add(p.pid())) {
                throw new IllegalArgumentException("duplicate pid " + p.pid());
            }
            copies.add(p.copy());
        }
        copies.sort(Comparator.comparingInt(SimProcess::arrivalTime).thenComparingInt(SimProcess::pid));
        return copies;
    }
}
