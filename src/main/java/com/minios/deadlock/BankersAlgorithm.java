package com.minios.deadlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Dijkstra's Banker's algorithm for deadlock avoidance. A resource request is granted only
 * if the state that results is still safe, meaning some order exists in which every
 * process can finish.
 */
public final class BankersAlgorithm {

    public enum Outcome {
        /** Request granted; the system stays in a safe state. */
        GRANTED,
        /** The process asked for more than its declared maximum claim. */
        DENIED_EXCEEDS_CLAIM,
        /** Not enough resources are free right now; the process must wait. */
        MUST_WAIT,
        /** Granting would leave the system in an unsafe state; the process must wait. */
        DENIED_UNSAFE
    }

    private final int processes;
    private final int resources;
    private final int[] available;
    private final int[][] max;
    private final int[][] allocation;

    public BankersAlgorithm(int[] available, int[][] max, int[][] allocation) {
        this.processes = max.length;
        this.resources = available.length;
        if (allocation.length != processes) {
            throw new IllegalArgumentException("max and allocation must have the same number of rows");
        }
        this.available = available.clone();
        this.max = new int[processes][];
        this.allocation = new int[processes][];
        for (int i = 0; i < processes; i++) {
            if (max[i].length != resources || allocation[i].length != resources) {
                throw new IllegalArgumentException("row " + i + " must have " + resources + " columns");
            }
            for (int j = 0; j < resources; j++) {
                if (allocation[i][j] > max[i][j]) {
                    throw new IllegalArgumentException("P" + i + " holds more of R" + j + " than its max claim");
                }
            }
            this.max[i] = max[i].clone();
            this.allocation[i] = allocation[i].clone();
        }
    }

    public int[] available() {
        return available.clone();
    }

    public int[] need(int pid) {
        int[] need = new int[resources];
        for (int j = 0; j < resources; j++) {
            need[j] = max[pid][j] - allocation[pid][j];
        }
        return need;
    }

    /** The safety algorithm: returns a safe sequence of process ids, or empty if the state is unsafe. */
    public Optional<List<Integer>> safeSequence() {
        int[] work = available.clone();
        boolean[] finished = new boolean[processes];
        List<Integer> sequence = new ArrayList<>();
        boolean progress = true;
        while (progress) {
            progress = false;
            for (int i = 0; i < processes; i++) {
                if (!finished[i] && fits(need(i), work)) {
                    for (int j = 0; j < resources; j++) {
                        work[j] += allocation[i][j];
                    }
                    finished[i] = true;
                    sequence.add(i);
                    progress = true;
                }
            }
        }
        return sequence.size() == processes ? Optional.of(sequence) : Optional.empty();
    }

    public boolean isSafe() {
        return safeSequence().isPresent();
    }

    /** The resource-request algorithm. State changes only when the result is {@link Outcome#GRANTED}. */
    public Outcome request(int pid, int[] request) {
        if (request.length != resources) {
            throw new IllegalArgumentException("request must have " + resources + " entries");
        }
        if (!fits(request, need(pid))) {
            return Outcome.DENIED_EXCEEDS_CLAIM;
        }
        if (!fits(request, available)) {
            return Outcome.MUST_WAIT;
        }
        apply(pid, request, 1);
        if (isSafe()) {
            return Outcome.GRANTED;
        }
        apply(pid, request, -1);
        return Outcome.DENIED_UNSAFE;
    }

    private void apply(int pid, int[] request, int sign) {
        for (int j = 0; j < resources; j++) {
            available[j] -= sign * request[j];
            allocation[pid][j] += sign * request[j];
        }
    }

    private static boolean fits(int[] amount, int[] limit) {
        for (int j = 0; j < amount.length; j++) {
            if (amount[j] > limit[j]) {
                return false;
            }
        }
        return true;
    }
}
