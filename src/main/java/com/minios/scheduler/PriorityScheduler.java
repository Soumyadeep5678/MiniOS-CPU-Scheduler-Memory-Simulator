package com.minios.scheduler;

import com.minios.process.SimProcess;

import java.util.Comparator;

/** Static-priority scheduling; a lower number is a higher priority. */
public final class PriorityScheduler extends HeapScheduler {

    public PriorityScheduler(boolean preemptive) {
        super(preemptive);
    }

    @Override
    public String name() {
        return isPreemptive() ? "Priority (preemptive)" : "Priority";
    }

    @Override
    protected Comparator<SimProcess> order() {
        return Comparator.comparingInt(SimProcess::priority);
    }
}
