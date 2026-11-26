package com.minios.cli;

import com.minios.bench.Benchmark;
import com.minios.kernel.MiniKernel;
import com.minios.memory.ContiguousAllocator;
import com.minios.process.SimProcess;
import com.minios.realtime.PeriodicTask;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** The menu shown when MiniOS starts without arguments. */
public final class InteractiveMenu {

    private final Scanner in;
    private final PrintStream out;

    public InteractiveMenu(Scanner in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    public void run() throws InterruptedException, IOException {
        while (true) {
            out.println("""

                    ===== MiniOS: CPU Scheduler & Memory Simulator =====
                    1. CPU scheduling (FCFS, SJF, SRTF, Priority, RR, MLFQ)
                    2. Page replacement (FIFO, LRU, Optimal, Clock)
                    3. Belady's anomaly
                    4. Real-time scheduling (Rate Monotonic vs EDF)
                    5. Concurrency (producer-consumer, dining philosophers)
                    6. Deadlock avoidance (Banker's algorithm)
                    7. Benchmark (10,000 synthetic processes)
                    8. Mini kernel shell
                    0. Exit""");
            String choice = prompt("Choose", "0");
            switch (choice) {
                case "1" -> scheduling();
                case "2" -> paging();
                case "3" -> Demos.beladyAnomaly(out);
                case "4" -> realTime();
                case "5" -> Demos.concurrency(out);
                case "6" -> Demos.bankers(out);
                case "7" -> Benchmark.run(out, readInt("Number of processes", 10_000), 42, 0.9, null);
                case "8" -> new MiniKernel(1024, ContiguousAllocator.Fit.FIRST, out).repl(in);
                case "0", "q", "exit" -> {
                    return;
                }
                default -> out.println("Unknown option: " + choice);
            }
        }
    }

    private void scheduling() {
        List<SimProcess> workload = Demos.sampleWorkload();
        if (prompt("Use the sample workload? [Y/n]", "y").toLowerCase().startsWith("n")) {
            int n = readInt("How many processes", 4);
            workload = new ArrayList<>();
            out.println("Enter each process as: arrival burst [priority]   (lower priority number = more important)");
            for (int pid = 1; pid <= n; pid++) {
                int[] v = readInts("P" + pid, 2, 3);
                workload.add(new SimProcess(pid, v[0], v[1], v.length > 2 ? v[2] : 0));
            }
        }
        int quantum = readInt("Round Robin time quantum", 4);
        Demos.scheduling(out, workload, quantum);
    }

    private void paging() {
        int[] refs = Demos.TEXTBOOK_REFERENCES;
        String line = prompt("Reference string (space separated, blank = textbook example)", "");
        if (!line.isBlank()) {
            refs = parseInts(line);
        }
        int frames = readInt("Number of frames", 3);
        Demos.paging(out, refs, frames);
    }

    private void realTime() {
        List<PeriodicTask> tasks = Demos.sampleTasks();
        if (prompt("Use the sample task set? [Y/n]", "y").toLowerCase().startsWith("n")) {
            int n = readInt("How many tasks", 2);
            tasks = new ArrayList<>();
            out.println("Enter each task as: executionTime period");
            for (int i = 1; i <= n; i++) {
                int[] v = readInts("T" + i, 2, 2);
                tasks.add(new PeriodicTask("T" + i, v[0], v[1]));
            }
        }
        Demos.realTime(out, tasks);
    }

    private String prompt(String question, String fallback) {
        out.print(question + (fallback.isEmpty() ? "" : " [" + fallback + "]") + ": ");
        out.flush();
        if (!in.hasNextLine()) {
            return fallback;
        }
        String line = in.nextLine().trim();
        return line.isEmpty() ? fallback : line;
    }

    private int readInt(String question, int fallback) {
        while (true) {
            String s = prompt(question, String.valueOf(fallback));
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                out.println("Please enter a whole number.");
            }
        }
    }

    private int[] readInts(String question, int min, int max) {
        while (true) {
            out.print(question + ": ");
            out.flush();
            if (!in.hasNextLine()) {
                throw new IllegalStateException("input ended");
            }
            try {
                int[] values = parseInts(in.nextLine());
                if (values.length >= min && values.length <= max) {
                    return values;
                }
            } catch (NumberFormatException e) {
                // fall through to the message below
            }
            out.println("Please enter " + (min == max ? min : min + " to " + max) + " whole numbers.");
        }
    }

    private static int[] parseInts(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return new int[0];
        }
        String[] parts = trimmed.split("[\\s,]+");
        int[] values = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            values[i] = Integer.parseInt(parts[i]);
        }
        return values;
    }
}
