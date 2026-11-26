package com.minios.bench;

import com.minios.memory.PageReplacer;
import com.minios.memory.PageReplacers;
import com.minios.memory.PagingResult;
import com.minios.process.SimProcess;
import com.minios.scheduler.ScheduleResult;
import com.minios.scheduler.Scheduler;
import com.minios.scheduler.Schedulers;
import com.minios.util.AsciiTable;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Compares every scheduler and page-replacement policy on large synthetic workloads. */
public final class Benchmark {

    public record SchedulerRow(String algorithm, int processes, double avgWaiting, double avgTurnaround,
                               double avgResponse, int maxWaiting, int contextSwitches,
                               double cpuUtilization, double millis) {
    }

    public record PagingRow(String algorithm, int frames, int references, int faults, double faultRate,
                            double millis) {
    }

    private Benchmark() {
    }

    public static List<SchedulerRow> runSchedulers(List<SimProcess> workload, List<Scheduler> schedulers) {
        List<SchedulerRow> rows = new ArrayList<>();
        List<SimProcess> warmUp = workload.subList(0, Math.min(workload.size(), 500));
        for (Scheduler s : schedulers) {
            s.schedule(warmUp); // let the JIT compile the hot path before timing
            long t0 = System.nanoTime();
            ScheduleResult r = s.schedule(workload);
            double millis = (System.nanoTime() - t0) / 1_000_000.0;
            rows.add(new SchedulerRow(r.algorithm(), r.processes().size(), r.averageWaitingTime(),
                    r.averageTurnaroundTime(), r.averageResponseTime(), r.maxWaitingTime(),
                    r.contextSwitches(), r.cpuUtilization(), millis));
        }
        return rows;
    }

    public static List<PagingRow> runPaging(int[] references, int[] frameCounts) {
        List<PagingRow> rows = new ArrayList<>();
        for (int frames : frameCounts) {
            for (PageReplacer policy : PageReplacers.standardSet()) {
                long t0 = System.nanoTime();
                PagingResult r = policy.run(references, frames, false);
                double millis = (System.nanoTime() - t0) / 1_000_000.0;
                rows.add(new PagingRow(r.algorithm(), frames, r.references(), r.faults(), r.faultRate(), millis));
            }
        }
        return rows;
    }

    /** Runs the full benchmark, prints tables and optionally writes CSV files into {@code outDir}. */
    public static void run(PrintStream out, int processCount, long seed, double load, Path outDir)
            throws IOException {
        List<SimProcess> workload = WorkloadGenerator.processes(processCount, seed, load);
        out.printf(Locale.ROOT, "CPU scheduling: %,d synthetic processes, seed %d, target load %.2f%n",
                processCount, seed, load);
        List<SchedulerRow> schedulerRows = runSchedulers(workload, Schedulers.standardSet(4));
        AsciiTable st = new AsciiTable("Algorithm", "Avg wait", "Avg turnaround", "Avg response",
                "Max wait", "Ctx switches", "CPU %", "Time ms");
        for (SchedulerRow r : schedulerRows) {
            st.addRow(r.algorithm(), r.avgWaiting(), r.avgTurnaround(), r.avgResponse(), r.maxWaiting(),
                    r.contextSwitches(), r.cpuUtilization(), r.millis());
        }
        out.print(st.render());

        int[] refs = WorkloadGenerator.references(100_000, 256, 12, 0.9, seed);
        out.printf(Locale.ROOT, "%nPage replacement: %,d references over 256 pages (working set 12, 90%% locality)%n",
                refs.length);
        List<PagingRow> pagingRows = runPaging(refs, new int[] {4, 8, 16, 32});
        AsciiTable pt = new AsciiTable("Algorithm", "Frames", "Faults", "Fault rate %", "Time ms");
        for (PagingRow r : pagingRows) {
            pt.addRow(r.algorithm(), r.frames(), r.faults(), 100 * r.faultRate(), r.millis());
        }
        out.print(pt.render());
        out.println("Timings are single wall-clock runs after a warm-up; use them to compare, not as exact figures.");

        if (outDir != null) {
            Files.createDirectories(outDir);
            Path schedulingCsv = outDir.resolve("bench-scheduling.csv");
            Path pagingCsv = outDir.resolve("bench-paging.csv");
            Files.write(schedulingCsv, schedulerCsv(schedulerRows));
            Files.write(pagingCsv, pagingCsv(pagingRows));
            out.println("Wrote " + schedulingCsv + " and " + pagingCsv);
        }
    }

    private static List<String> schedulerCsv(List<SchedulerRow> rows) {
        List<String> lines = new ArrayList<>();
        lines.add("algorithm,processes,avg_waiting,avg_turnaround,avg_response,max_waiting,context_switches,cpu_utilization,millis");
        for (SchedulerRow r : rows) {
            lines.add(String.format(Locale.ROOT, "\"%s\",%d,%.3f,%.3f,%.3f,%d,%d,%.2f,%.3f",
                    r.algorithm(), r.processes(), r.avgWaiting(), r.avgTurnaround(), r.avgResponse(),
                    r.maxWaiting(), r.contextSwitches(), r.cpuUtilization(), r.millis()));
        }
        return lines;
    }

    private static List<String> pagingCsv(List<PagingRow> rows) {
        List<String> lines = new ArrayList<>();
        lines.add("algorithm,frames,references,faults,fault_rate,millis");
        for (PagingRow r : rows) {
            lines.add(String.format(Locale.ROOT, "\"%s\",%d,%d,%d,%.5f,%.3f",
                    r.algorithm(), r.frames(), r.references(), r.faults(), r.faultRate(), r.millis()));
        }
        return lines;
    }
}
