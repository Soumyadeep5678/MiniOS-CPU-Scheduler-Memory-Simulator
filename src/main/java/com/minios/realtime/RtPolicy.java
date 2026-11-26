package com.minios.realtime;

public enum RtPolicy {
    /** Static priority: the shorter the period, the higher the priority. */
    RATE_MONOTONIC("Rate Monotonic"),
    /** Dynamic priority: the job with the nearest absolute deadline runs. */
    EARLIEST_DEADLINE_FIRST("Earliest Deadline First");

    private final String label;

    RtPolicy(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
