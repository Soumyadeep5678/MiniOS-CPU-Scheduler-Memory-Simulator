package com.minios.memory;

import java.util.List;

/** Factory for the standard set of page-replacement policies (new instances on each call). */
public final class PageReplacers {

    private PageReplacers() {
    }

    public static List<PageReplacer> standardSet() {
        return List.of(
                new FifoPageReplacer(),
                new LruPageReplacer(),
                new OptimalPageReplacer(),
                new ClockPageReplacer());
    }
}
