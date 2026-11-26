package com.minios.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.TreeMap;

/**
 * Variable-partition (contiguous) memory allocator with first-, best- and worst-fit
 * placement. Freed blocks are coalesced with free neighbours, so external fragmentation
 * can be observed and measured.
 */
public final class ContiguousAllocator {

    public enum Fit { FIRST, BEST, WORST }

    /** A block of memory; {@code owner} is {@link #FREE} for a hole. */
    public record Block(int start, int size, int owner) {
        public static final int FREE = -1;

        public boolean isFree() {
            return owner == FREE;
        }

        public int end() {
            return start + size;
        }
    }

    private static final String SYMBOLS = "123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final int capacity;
    private Fit fit;
    private final TreeMap<Integer, Block> blocks = new TreeMap<>();

    public ContiguousAllocator(int capacity, Fit fit) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        this.capacity = capacity;
        this.fit = fit;
        blocks.put(0, new Block(0, capacity, Block.FREE));
    }

    /** Allocates {@code size} units for {@code owner}; returns the start address, or empty if no hole fits. */
    public OptionalInt allocate(int owner, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0, was " + size);
        }
        if (owner < 0) {
            throw new IllegalArgumentException("owner must be >= 0, was " + owner);
        }
        Block chosen = null;
        for (Block b : blocks.values()) {
            if (!b.isFree() || b.size() < size) {
                continue;
            }
            if (chosen == null) {
                chosen = b;
                if (fit == Fit.FIRST) {
                    break;
                }
            } else if (fit == Fit.BEST && b.size() < chosen.size()) {
                chosen = b;
            } else if (fit == Fit.WORST && b.size() > chosen.size()) {
                chosen = b;
            }
        }
        if (chosen == null) {
            return OptionalInt.empty();
        }
        blocks.put(chosen.start(), new Block(chosen.start(), size, owner));
        if (chosen.size() > size) {
            int rest = chosen.start() + size;
            blocks.put(rest, new Block(rest, chosen.size() - size, Block.FREE));
        }
        return OptionalInt.of(chosen.start());
    }

    /** Frees every block owned by {@code owner}; returns the number of units released. */
    public int free(int owner) {
        List<Integer> starts = new ArrayList<>();
        for (Block b : blocks.values()) {
            if (b.owner() == owner) {
                starts.add(b.start());
            }
        }
        int freed = 0;
        for (int start : starts) {
            Block b = blocks.get(start);
            freed += b.size();
            blocks.put(start, new Block(start, b.size(), Block.FREE));
            coalesce(start);
        }
        return freed;
    }

    private void coalesce(int start) {
        Block block = blocks.get(start);
        Map.Entry<Integer, Block> higher = blocks.higherEntry(start);
        if (higher != null && higher.getValue().isFree()) {
            blocks.remove(higher.getKey());
            block = new Block(start, block.size() + higher.getValue().size(), Block.FREE);
            blocks.put(start, block);
        }
        Map.Entry<Integer, Block> lower = blocks.lowerEntry(start);
        if (lower != null && lower.getValue().isFree()) {
            blocks.remove(start);
            Block merged = new Block(lower.getKey(), lower.getValue().size() + block.size(), Block.FREE);
            blocks.put(lower.getKey(), merged);
        }
    }

    public int capacity() {
        return capacity;
    }

    public Fit fit() {
        return fit;
    }

    public void setFit(Fit fit) {
        this.fit = fit;
    }

    public int freeMemory() {
        return blocks.values().stream().filter(Block::isFree).mapToInt(Block::size).sum();
    }

    public int usedMemory() {
        return capacity - freeMemory();
    }

    public int largestFreeBlock() {
        return blocks.values().stream().filter(Block::isFree).mapToInt(Block::size).max().orElse(0);
    }

    public int freeBlockCount() {
        return (int) blocks.values().stream().filter(Block::isFree).count();
    }

    /** 1 - largest hole / total free: 0 means all free memory is one block. */
    public double externalFragmentation() {
        int free = freeMemory();
        return free == 0 ? 0.0 : 1.0 - (double) largestFreeBlock() / free;
    }

    public List<Block> layout() {
        return List.copyOf(blocks.values());
    }

    /** A proportional bar: '.' is free, other characters identify the owning process. */
    public String renderMap(int width) {
        StringBuilder sb = new StringBuilder("[");
        for (int col = 0; col < width; col++) {
            int address = (int) ((long) col * capacity / width);
            Block b = blocks.floorEntry(address).getValue();
            sb.append(b.isFree() ? '.' : SYMBOLS.charAt(Math.floorMod(b.owner() - 1, SYMBOLS.length())));
        }
        return sb.append(']').toString();
    }
}
