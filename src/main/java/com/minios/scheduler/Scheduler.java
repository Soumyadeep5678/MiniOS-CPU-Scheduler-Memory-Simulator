package com.minios.scheduler;

import com.minios.process.SimProcess;

import java.util.List;

/**
 * A CPU scheduling algorithm. Implementations never modify the workload passed in: they
 * schedule copies, so one workload can be replayed under every algorithm.
 */
public interface Scheduler {

    String name();

    ScheduleResult schedule(List<SimProcess> workload);
}
