package com.minios.bench;

import com.minios.process.SimProcess;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Seeded, reproducible synthetic workloads. */
public final class WorkloadGenerator {

    /** 70% short jobs (1-10 units, mean 5.5) and 30% long jobs (10-60 units, mean 35). */
    private static final double SHORT_FRACTION = 0.7;
    private static final double MEAN_BURST = SHORT_FRACTION * 5.5 + (1 - SHORT_FRACTION) * 35.0;

    private WorkloadGenerator() {
    }

    /**
     * Generates {@code count} processes with exponential inter-arrival times (a Poisson
     * process) scaled so the CPU is busy about {@code load} of the time. Priorities are
     * uniform in 0-9.
     */
    public static List<SimProcess> processes(int count, long seed, double load) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be > 0");
        }
        if (load <= 0) {
            throw new IllegalArgumentException("load must be > 0");
        }
        Random rnd = new Random(seed);
        double meanGap = MEAN_BURST / load;
        List<SimProcess> processes = new ArrayList<>(count);
        long time = 0;
        for (int pid = 1; pid <= count; pid++) {
            int burst = rnd.nextDouble() < SHORT_FRACTION ? 1 + rnd.nextInt(10) : 10 + rnd.nextInt(51);
            processes.add(new SimProcess(pid, Math.toIntExact(time), burst, rnd.nextInt(10)));
            time += Math.round(-Math.log(1.0 - rnd.nextDouble()) * meanGap);
        }
        return processes;
    }

    /**
     * A page reference string with locality of reference: most references fall in a small
     * working set that occasionally moves to a different part of the address space.
     */
    public static int[] references(int length, int pages, int workingSet, double locality, long seed) {
        if (length <= 0 || pages <= 0 || workingSet <= 0) {
            throw new IllegalArgumentException("length, pages and workingSet must be > 0");
        }
        Random rnd = new Random(seed);
        int[] refs = new int[length];
        int base = 0;
        for (int i = 0; i < length; i++) {
            if (rnd.nextDouble() < 0.002) {
                base = rnd.nextInt(pages); // phase change
            }
            refs[i] = rnd.nextDouble() < locality
                    ? (base + rnd.nextInt(workingSet)) % pages
                    : rnd.nextInt(pages);
        }
        return refs;
    }
}
