package com.minios.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds a Gantt timeline. Adjacent slices of the same process are merged, and a context
 * switch is counted each time the CPU moves from one process to a different one.
 */
public final class Timeline {

    private final List<GanttSlice> slices = new ArrayList<>();
    private int contextSwitches;
    private int lastRunPid = Integer.MIN_VALUE;

    public void record(int pid, int start, int end) {
        if (end <= start) {
            return;
        }
        if (!slices.isEmpty()) {
            int lastIndex = slices.size() - 1;
            GanttSlice last = slices.get(lastIndex);
            if (last.pid() == pid && last.end() == start) {
                slices.set(lastIndex, new GanttSlice(pid, last.start(), end));
                return;
            }
        }
        if (pid != GanttSlice.IDLE) {
            if (lastRunPid != Integer.MIN_VALUE && lastRunPid != pid) {
                contextSwitches++;
            }
            lastRunPid = pid;
        }
        slices.add(new GanttSlice(pid, start, end));
    }

    public List<GanttSlice> slices() {
        return Collections.unmodifiableList(slices);
    }

    public int contextSwitches() {
        return contextSwitches;
    }
}
