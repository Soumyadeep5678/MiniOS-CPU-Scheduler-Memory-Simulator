package com.minios.memory;

/**
 * One memory reference during a paging simulation.
 *
 * @param index   position in the reference string
 * @param page    page that was referenced
 * @param fault   true if the page was not resident
 * @param evicted page that was replaced, or -1 if none
 * @param frames  contents of each frame after the reference (-1 = empty)
 */
public record PagingStep(int index, int page, boolean fault, int evicted, int[] frames) {
}
