package com.minios.memory;

import java.util.List;

/** Summary of one page-replacement run; {@code steps} is empty unless steps were recorded. */
public record PagingResult(String algorithm, int frames, int references, int faults, List<PagingStep> steps) {

    public int hits() {
        return references - faults;
    }

    public double faultRate() {
        return references == 0 ? 0.0 : (double) faults / references;
    }

    public double hitRate() {
        return references == 0 ? 0.0 : (double) hits() / references;
    }
}
