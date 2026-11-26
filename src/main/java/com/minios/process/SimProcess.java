package com.minios.process;

/**
 * A simulated process (a minimal process control block).
 *
 * <p>Holds the static attributes a scheduler needs (arrival, burst, priority) plus the
 * run-time accounting it updates (remaining time, first start, completion). Schedulers
 * always work on {@link #copy() copies}, so a workload can be replayed under many
 * algorithms.
 *
 * <p>Priority convention: a <em>lower</em> number means a <em>higher</em> priority.
 */
public final class SimProcess {

    private final int pid;
    private final int arrivalTime;
    private final int burstTime;
    private final int priority;

    private int remainingTime;
    private int startTime = -1;
    private int completionTime = -1;

    public SimProcess(int pid, int arrivalTime, int burstTime, int priority) {
        if (arrivalTime < 0) {
            throw new IllegalArgumentException("arrivalTime must be >= 0, was " + arrivalTime);
        }
        if (burstTime <= 0) {
            throw new IllegalArgumentException("burstTime must be > 0, was " + burstTime);
        }
        this.pid = pid;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.priority = priority;
        this.remainingTime = burstTime;
    }

    public SimProcess(int pid, int arrivalTime, int burstTime) {
        this(pid, arrivalTime, burstTime, 0);
    }

    /** A fresh copy with the same attributes and no run-time state. */
    public SimProcess copy() {
        return new SimProcess(pid, arrivalTime, burstTime, priority);
    }

    /** Records the first time this process gets the CPU (later calls are ignored). */
    public void markStarted(int time) {
        if (startTime < 0) {
            startTime = time;
        }
    }

    /** Consumes {@code units} of CPU time. */
    public void run(int units) {
        if (units <= 0 || units > remainingTime) {
            throw new IllegalArgumentException(
                    "P" + pid + ": cannot run " + units + " units with " + remainingTime + " remaining");
        }
        remainingTime -= units;
    }

    public void markCompleted(int time) {
        if (remainingTime != 0) {
            throw new IllegalStateException("P" + pid + " still has " + remainingTime + " units left");
        }
        completionTime = time;
    }

    public boolean isFinished() {
        return remainingTime == 0;
    }

    public boolean isCompleted() {
        return completionTime >= 0;
    }

    public int pid() {
        return pid;
    }

    public int arrivalTime() {
        return arrivalTime;
    }

    public int burstTime() {
        return burstTime;
    }

    public int priority() {
        return priority;
    }

    public int remainingTime() {
        return remainingTime;
    }

    public int startTime() {
        return startTime;
    }

    public int completionTime() {
        return completionTime;
    }

    /** Completion minus arrival. */
    public int turnaroundTime() {
        requireCompleted();
        return completionTime - arrivalTime;
    }

    /** Time spent ready but not running: turnaround minus burst. */
    public int waitingTime() {
        return turnaroundTime() - burstTime;
    }

    /** Time from arrival until the process first ran. */
    public int responseTime() {
        requireCompleted();
        return startTime - arrivalTime;
    }

    private void requireCompleted() {
        if (!isCompleted()) {
            throw new IllegalStateException("P" + pid + " has not completed");
        }
    }

    @Override
    public String toString() {
        return "P" + pid + "[arrival=" + arrivalTime + ", burst=" + burstTime + ", priority=" + priority + "]";
    }
}
