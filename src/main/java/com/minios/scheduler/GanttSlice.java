package com.minios.scheduler;

/** One contiguous interval [start, end) during which {@code pid} held the CPU, or the CPU was idle. */
public record GanttSlice(int pid, int start, int end) {

    /** Pid used for intervals where no process was running. */
    public static final int IDLE = -1;

    public GanttSlice {
        if (end <= start) {
            throw new IllegalArgumentException("empty slice [" + start + ", " + end + ")");
        }
    }

    public int length() {
        return end - start;
    }

    public boolean isIdle() {
        return pid == IDLE;
    }
}
