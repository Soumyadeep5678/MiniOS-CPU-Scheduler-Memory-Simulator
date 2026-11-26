package com.minios.cli;

import com.minios.concurrency.DiningPhilosophers;
import com.minios.concurrency.ProducerConsumer;
import com.minios.deadlock.BankersAlgorithm;
import com.minios.memory.FifoPageReplacer;
import com.minios.memory.PageReplacer;
import com.minios.memory.PageReplacers;
import com.minios.memory.PagingResult;
import com.minios.process.SimProcess;
import com.minios.realtime.PeriodicTask;
import com.minios.realtime.RealTimeScheduler;
import com.minios.realtime.RtPolicy;
import com.minios.scheduler.ScheduleResult;
import com.minios.scheduler.Scheduler;
import com.minios.scheduler.Schedulers;
import com.minios.util.AsciiTable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Ready-made demonstrations, used by both the interactive menu and the command line. */
public final class Demos {

    /** Silberschatz's classic reference string. */
    public static final int[] TEXTBOOK_REFERENCES = {7, 0, 1, 2, 0, 3, 0, 4, 2, 3, 0, 3, 2, 1, 2, 0, 1, 7, 0, 1};

    /** The string that shows Belady's anomaly under FIFO. */
    public static final int[] BELADY_REFERENCES = {1, 2, 3, 4, 1, 2, 5, 1, 2, 3, 4, 5};

    private Demos() {
    }

    public static List<SimProcess> sampleWorkload() {
        return List.of(
                new SimProcess(1, 0, 8, 3),
                new SimProcess(2, 1, 4, 1),
                new SimProcess(3, 2, 9, 4),
                new SimProcess(4, 3, 5, 2),
                new SimProcess(5, 10, 2, 0));
    }

    public static List<PeriodicTask> sampleTasks() {
        // U = 2/5 + 4/7 = 0.971: schedulable by EDF, but above the RM bound and RM misses.
        return List.of(new PeriodicTask("T1", 2, 5), new PeriodicTask("T2", 4, 7));
    }

    public static void scheduling(PrintStream out, List<SimProcess> workload, int quantum) {
        List<ScheduleResult> results = new ArrayList<>();
        for (Scheduler s : Schedulers.standardSet(quantum)) {
            ScheduleResult r = s.schedule(workload);
            Reports.schedule(out, r);
            out.println();
            results.add(r);
        }
        out.println("== Comparison ==");
        Reports.comparison(out, results);
    }

    public static void paging(PrintStream out, int[] references, int frames) {
        out.println("Reference string: " + Arrays.toString(references));
        List<PagingResult> results = new ArrayList<>();
        for (PageReplacer policy : PageReplacers.standardSet()) {
            PagingResult r = policy.run(references, frames, true);
            Reports.paging(out, r, true);
            out.println();
            results.add(r);
        }
        out.println("== Comparison ==");
        Reports.pagingSummary(out, results);
    }

    public static void beladyAnomaly(PrintStream out) {
        out.println("Belady's anomaly: with FIFO, MORE frames can cause MORE faults.");
        out.println("Reference string: " + Arrays.toString(BELADY_REFERENCES));
        AsciiTable table = new AsciiTable("Frames", "FIFO faults");
        for (int frames = 1; frames <= 5; frames++) {
            table.addRow(frames, new FifoPageReplacer().run(BELADY_REFERENCES, frames, false).faults());
        }
        out.print(table.render());
    }

    public static void realTime(PrintStream out, List<PeriodicTask> tasks) {
        for (RtPolicy policy : RtPolicy.values()) {
            Reports.realTime(out, RealTimeScheduler.simulate(tasks, policy));
            out.println();
        }
    }

    public static void concurrency(PrintStream out) throws InterruptedException {
        out.println("== Producer-consumer (semaphores) ==");
        ProducerConsumer.Report pc = ProducerConsumer.run(3, 2, 10_000, 8);
        out.printf(Locale.ROOT,
                "%d producers, %d consumers, buffer %d: produced %d, consumed %d, max occupancy %d, %s (%d ms)%n",
                pc.producers(), pc.consumers(), pc.capacity(), pc.produced(), pc.consumed(), pc.maxOccupancy(),
                pc.consistent() ? "no items lost or duplicated" : "INCONSISTENT", pc.millis());

        out.println();
        out.println("== Dining philosophers (5 philosophers, 1000 meals each) ==");
        for (DiningPhilosophers.Strategy strategy : DiningPhilosophers.Strategy.values()) {
            DiningPhilosophers.Report r = DiningPhilosophers.run(5, 1000, strategy, 5000);
            out.printf(Locale.ROOT, "%-18s %s | meals %s (%d ms)%n", strategy,
                    r.deadlocked() ? "DEADLOCK detected (circular wait), threads interrupted" : "completed, no deadlock",
                    Arrays.toString(r.meals()), r.millis());
        }
    }

    public static void bankers(PrintStream out) {
        int[] available = {3, 3, 2};
        int[][] max = {{7, 5, 3}, {3, 2, 2}, {9, 0, 2}, {2, 2, 2}, {4, 3, 3}};
        int[][] allocation = {{0, 1, 0}, {2, 0, 0}, {3, 0, 2}, {2, 1, 1}, {0, 0, 2}};
        BankersAlgorithm banker = new BankersAlgorithm(available, max, allocation);

        out.println("Available " + Arrays.toString(banker.available()));
        AsciiTable table = new AsciiTable("Process", "Allocation", "Max", "Need");
        for (int i = 0; i < max.length; i++) {
            table.addRow("P" + i, Arrays.toString(allocation[i]), Arrays.toString(max[i]),
                    Arrays.toString(banker.need(i)));
        }
        out.print(table.render());
        out.println("Safe sequence: " + banker.safeSequence().map(Demos::sequence).orElse("none (UNSAFE)"));

        request(out, banker, 1, new int[] {1, 0, 2});
        request(out, banker, 4, new int[] {3, 3, 0});
        request(out, banker, 0, new int[] {0, 2, 0});
    }

    private static void request(PrintStream out, BankersAlgorithm banker, int pid, int[] req) {
        BankersAlgorithm.Outcome outcome = banker.request(pid, req);
        out.printf(Locale.ROOT, "P%d requests %s -> %s | available now %s%n",
                pid, Arrays.toString(req), outcome, Arrays.toString(banker.available()));
    }

    private static String sequence(List<Integer> pids) {
        StringBuilder sb = new StringBuilder("<");
        for (int i = 0; i < pids.size(); i++) {
            sb.append(i == 0 ? "" : ", ").append('P').append(pids.get(i));
        }
        return sb.append('>').toString();
    }
}
