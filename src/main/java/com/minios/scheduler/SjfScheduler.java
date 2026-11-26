package com.minios.scheduler;

import com.minios.process.SimProcess;

import java.util.Comparator;

/**
 * Shortest Job First. The non-preemptive form runs the shortest ready job to completion;
 * the preemptive form is Shortest Remaining Time First (SRTF), which minimises average
 * waiting time.
 */
public final class SjfScheduler extends HeapScheduler {

    public SjfScheduler(boolean preemptive) {
        super(preemptive);
    }

    @Override
    public String name() {
        return isPreemptive() ? "SRTF (preemptive SJF)" : "SJF";
    }

    @Override
    protected Comparator<SimProcess> order() {
        return Comparator.comparingInt(SimProcess::remainingTime);
    }
}
