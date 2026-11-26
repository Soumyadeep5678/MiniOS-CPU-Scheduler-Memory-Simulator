package com.minios.cli;

import com.minios.memory.PagingResult;
import com.minios.memory.PagingStep;
import com.minios.process.SimProcess;
import com.minios.realtime.RealTimeResult;
import com.minios.scheduler.ScheduleResult;
import com.minios.util.AsciiTable;
import com.minios.util.GanttChart;

import java.io.PrintStream;
import java.util.List;
import java.util.Locale;

/** Console formatting for simulation results. */
public final class Reports {

    private Reports() {
    }

    public static void schedule(PrintStream out, ScheduleResult r) {
        out.println("== " + r.algorithm() + " ==");
        out.print(GanttChart.render(r.timeline(), 30));
        AsciiTable table = new AsciiTable("PID", "Arrival", "Burst", "Priority", "Start", "Finish",
                "Turnaround", "Waiting", "Response");
        for (SimProcess p : r.processes()) {
            table.addRow("P" + p.pid(), p.arrivalTime(), p.burstTime(), p.priority(), p.startTime(),
                    p.completionTime(), p.turnaroundTime(), p.waitingTime(), p.responseTime());
        }
        out.print(table.render());
        out.printf(Locale.ROOT,
                "avg waiting %.2f | avg turnaround %.2f | avg response %.2f | context switches %d | CPU %.1f%%%n",
                r.averageWaitingTime(), r.averageTurnaroundTime(), r.averageResponseTime(),
                r.contextSwitches(), r.cpuUtilization());
    }

    public static void comparison(PrintStream out, List<ScheduleResult> results) {
        AsciiTable table = new AsciiTable("Algorithm", "Avg wait", "Avg turnaround", "Avg response",
                "Max wait", "Ctx switches", "CPU %");
        for (ScheduleResult r : results) {
            table.addRow(r.algorithm(), r.averageWaitingTime(), r.averageTurnaroundTime(),
                    r.averageResponseTime(), r.maxWaitingTime(), r.contextSwitches(), r.cpuUtilization());
        }
        out.print(table.render());
    }

    public static void paging(PrintStream out, PagingResult r, boolean showSteps) {
        out.printf(Locale.ROOT, "== %s, %d frames ==%n", r.algorithm(), r.frames());
        if (showSteps) {
            AsciiTable table = new AsciiTable("Step", "Ref", "Frames", "Result");
            for (PagingStep s : r.steps()) {
                String result = !s.fault() ? "hit" : s.evicted() < 0 ? "FAULT" : "FAULT (evict " + s.evicted() + ")";
                table.addRow(s.index() + 1, s.page(), frames(s.frames()), result);
            }
            out.print(table.render());
        }
        out.printf(Locale.ROOT, "faults %d / %d references | fault rate %.1f%% | hit rate %.1f%%%n",
                r.faults(), r.references(), 100 * r.faultRate(), 100 * r.hitRate());
    }

    public static void pagingSummary(PrintStream out, List<PagingResult> results) {
        AsciiTable table = new AsciiTable("Algorithm", "Frames", "Faults", "Hits", "Fault rate %");
        for (PagingResult r : results) {
            table.addRow(r.algorithm(), r.frames(), r.faults(), r.hits(), 100 * r.faultRate());
        }
        out.print(table.render());
    }

    public static void realTime(PrintStream out, RealTimeResult r) {
        out.printf(Locale.ROOT, "== %s (horizon %d) ==%n", r.policy().label(), r.horizon());
        out.print(GanttChart.render(r.timeline(), 40, i -> r.tasks().get(i).name()));
        out.printf(Locale.ROOT, "utilization %.3f | RM bound n(2^(1/n)-1) = %.3f | EDF bound = 1.000%n",
                r.utilization(), r.liuLaylandBound());
        if (r.allDeadlinesMet()) {
            out.println("all deadlines met");
        } else {
            out.println(r.misses().size() + " deadline miss(es):");
            r.misses().stream().limit(10).forEach(m -> out.printf(Locale.ROOT,
                    "  %s job released at %d missed deadline %d with %d unit(s) left%n",
                    m.task(), m.release(), m.deadline(), m.remaining()));
        }
    }

    private static String frames(int[] frames) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < frames.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(frames[i] < 0 ? "-" : String.valueOf(frames[i]));
        }
        return sb.append(']').toString();
    }
}
