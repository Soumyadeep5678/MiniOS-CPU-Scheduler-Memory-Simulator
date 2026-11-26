package com.minios.util;

import com.minios.scheduler.GanttSlice;

import java.util.List;
import java.util.function.IntFunction;

/**
 * Renders a timeline as a text Gantt chart:
 *
 * <pre>
 * | P1 | P2 | P4 | P1 | P3 |
 * 0    1    5    10   17   26
 * </pre>
 */
public final class GanttChart {

    private GanttChart() {
    }

    public static String render(List<GanttSlice> slices, int maxSlices) {
        return render(slices, maxSlices, pid -> "P" + pid);
    }

    public static String render(List<GanttSlice> slices, int maxSlices, IntFunction<String> labeler) {
        if (slices.isEmpty()) {
            return "(empty timeline)\n";
        }
        int shown = Math.min(slices.size(), Math.max(1, maxSlices));
        StringBuilder bar = new StringBuilder("|");
        StringBuilder axis = new StringBuilder();
        for (int i = 0; i < shown; i++) {
            GanttSlice s = slices.get(i);
            String label = s.isIdle() ? "idle" : labeler.apply(s.pid());
            place(axis, bar.length() - 1, String.valueOf(s.start()));
            bar.append(' ').append(label).append(' ').append('|');
        }
        place(axis, bar.length() - 1, String.valueOf(slices.get(shown - 1).end()));
        if (shown < slices.size()) {
            bar.append(" ... ").append(slices.size() - shown).append(" more slices, last ends at t=")
                    .append(slices.get(slices.size() - 1).end());
        }
        return bar.append('\n').append(axis).append('\n').toString();
    }

    private static void place(StringBuilder axis, int column, String label) {
        if (axis.length() > column) {
            axis.append(' ');
        }
        while (axis.length() < column) {
            axis.append(' ');
        }
        axis.append(label);
    }
}
