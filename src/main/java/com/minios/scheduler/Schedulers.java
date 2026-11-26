package com.minios.scheduler;

import java.util.List;

/** Factory for the standard set of schedulers used by the demos and the benchmark. */
public final class Schedulers {

    private Schedulers() {
    }

    public static List<Scheduler> standardSet(int roundRobinQuantum) {
        return List.of(
                new FcfsScheduler(),
                new SjfScheduler(false),
                new SjfScheduler(true),
                new PriorityScheduler(false),
                new PriorityScheduler(true),
                new RoundRobinScheduler(roundRobinQuantum),
                new MlfqScheduler());
    }
}
