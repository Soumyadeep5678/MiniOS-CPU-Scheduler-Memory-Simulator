package com.minios.kernel;

import com.minios.cli.Reports;
import com.minios.memory.ContiguousAllocator;
import com.minios.process.SimProcess;
import com.minios.scheduler.FcfsScheduler;
import com.minios.scheduler.MlfqScheduler;
import com.minios.scheduler.PriorityScheduler;
import com.minios.scheduler.RoundRobinScheduler;
import com.minios.scheduler.Scheduler;
import com.minios.scheduler.SjfScheduler;
import com.minios.util.AsciiTable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Scanner;
import java.util.TreeMap;

/**
 * A tiny interactive "kernel": a process table, a contiguous memory allocator and a choice
 * of CPU schedulers behind a shell. {@code run} executes every process to completion with
 * the chosen scheduler and then reclaims their memory.
 */
public final class MiniKernel {

    private record Pcb(int pid, String name, int memoryKb, int address, int arrival, int burst, int priority) {
    }

    private final ContiguousAllocator memory;
    private final Map<Integer, Pcb> processTable = new TreeMap<>();
    private final PrintStream out;
    private int nextPid = 1;
    private boolean running = true;

    public MiniKernel(int memoryKb, ContiguousAllocator.Fit fit, PrintStream out) {
        this.memory = new ContiguousAllocator(memoryKb, fit);
        this.out = out;
    }

    public void boot() {
        out.println("[boot] MiniOS kernel starting");
        out.printf(Locale.ROOT, "[boot] memory manager: %d KB, %s fit%n",
                memory.capacity(), memory.fit().name().toLowerCase(Locale.ROOT));
        out.println("[boot] schedulers: fcfs sjf srtf priority rr mlfq");
        out.println("[boot] ready. Type 'help' for commands.");
    }

    public void repl(Scanner in) {
        boot();
        while (running) {
            out.print("minios> ");
            out.flush();
            if (!in.hasNextLine()) {
                break;
            }
            execute(in.nextLine());
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void execute(String line) {
        String[] args = line.trim().split("\\s+");
        if (args[0].isEmpty()) {
            return;
        }
        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "help" -> help();
                case "ps" -> ps();
                case "create", "spawn" -> create(args);
                case "kill" -> kill(args);
                case "mem", "meminfo" -> mem();
                case "fit" -> fit(args);
                case "run" -> run(args);
                case "exit", "quit", "shutdown" -> {
                    out.println("MiniOS shutting down.");
                    running = false;
                }
                default -> out.println("Unknown command '" + args[0] + "'. Type 'help'.");
            }
        } catch (NumberFormatException e) {
            out.println("Expected a number (" + e.getMessage() + ")");
        } catch (IllegalArgumentException e) {
            out.println("Error: " + e.getMessage());
        }
    }

    private void help() {
        out.println("""
                Commands:
                  create <name> <memKB> <burst> [priority] [arrival]   start a process
                  kill <pid>                                         end a process, free its memory
                  ps                                                 list processes
                  mem                                                memory map and fragmentation
                  fit <first|best|worst>                             change allocation strategy
                  run [fcfs|sjf|srtf|priority|rr|mlfq] [quantum]     schedule all processes to completion
                  exit                                               shut down""");
    }

    private void ps() {
        if (processTable.isEmpty()) {
            out.println("No processes.");
            return;
        }
        AsciiTable table = new AsciiTable("PID", "Name", "Memory KB", "Address", "Arrival", "Burst", "Priority");
        for (Pcb p : processTable.values()) {
            table.addRow(p.pid(), p.name(), p.memoryKb(), p.address(), p.arrival(), p.burst(), p.priority());
        }
        out.print(table.render());
    }

    private void create(String[] args) {
        if (args.length < 4) {
            out.println("Usage: create <name> <memKB> <burst> [priority] [arrival]");
            return;
        }
        String name = args[1];
        int memKb = Integer.parseInt(args[2]);
        int burst = Integer.parseInt(args[3]);
        int priority = args.length > 4 ? Integer.parseInt(args[4]) : 0;
        int arrival = args.length > 5 ? Integer.parseInt(args[5]) : 0;
        if (burst <= 0 || arrival < 0) {
            throw new IllegalArgumentException("burst must be > 0 and arrival >= 0");
        }
        int pid = nextPid;
        OptionalInt address = memory.allocate(pid, memKb);
        if (address.isEmpty()) {
            out.printf(Locale.ROOT,
                    "Cannot allocate %d KB: %d KB free in total, but the largest free block is %d KB%s.%n",
                    memKb, memory.freeMemory(), memory.largestFreeBlock(),
                    memory.freeMemory() >= memKb ? " (external fragmentation)" : "");
            return;
        }
        nextPid++;
        processTable.put(pid, new Pcb(pid, name, memKb, address.getAsInt(), arrival, burst, priority));
        out.printf(Locale.ROOT, "Created PID %d '%s': %d KB at address %d%n", pid, name, memKb, address.getAsInt());
    }

    private void kill(String[] args) {
        if (args.length < 2) {
            out.println("Usage: kill <pid>");
            return;
        }
        int pid = Integer.parseInt(args[1]);
        Pcb pcb = processTable.remove(pid);
        if (pcb == null) {
            out.println("No process with PID " + pid);
            return;
        }
        int freed = memory.free(pid);
        out.printf(Locale.ROOT, "Killed PID %d '%s', freed %d KB%n", pid, pcb.name(), freed);
    }

    private void mem() {
        out.printf(Locale.ROOT, "Total %d KB | used %d KB | free %d KB | largest free block %d KB | %d hole(s)%n",
                memory.capacity(), memory.usedMemory(), memory.freeMemory(), memory.largestFreeBlock(),
                memory.freeBlockCount());
        out.printf(Locale.ROOT, "External fragmentation: %.1f%% | strategy: %s fit%n",
                100 * memory.externalFragmentation(), memory.fit().name().toLowerCase(Locale.ROOT));
        out.println(memory.renderMap(64));
        AsciiTable table = new AsciiTable("Start", "End", "Size KB", "Owner");
        for (ContiguousAllocator.Block b : memory.layout()) {
            table.addRow(b.start(), b.end(), b.size(), b.isFree() ? "free" : "PID " + b.owner());
        }
        out.print(table.render());
    }

    private void fit(String[] args) {
        if (args.length < 2) {
            out.println("Usage: fit <first|best|worst>");
            return;
        }
        memory.setFit(ContiguousAllocator.Fit.valueOf(args[1].toUpperCase(Locale.ROOT)));
        out.println("Allocation strategy: " + args[1].toLowerCase(Locale.ROOT) + " fit");
    }

    private void run(String[] args) {
        if (processTable.isEmpty()) {
            out.println("No processes to run. Use 'create' first.");
            return;
        }
        String algorithm = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "rr";
        int quantum = args.length > 2 ? Integer.parseInt(args[2]) : 4;
        Scheduler scheduler = switch (algorithm) {
            case "fcfs" -> new FcfsScheduler();
            case "sjf" -> new SjfScheduler(false);
            case "srtf" -> new SjfScheduler(true);
            case "priority" -> new PriorityScheduler(false);
            case "rr" -> new RoundRobinScheduler(quantum);
            case "mlfq" -> new MlfqScheduler();
            default -> throw new IllegalArgumentException("unknown scheduler '" + algorithm + "'");
        };
        List<SimProcess> workload = new ArrayList<>();
        for (Pcb p : processTable.values()) {
            workload.add(new SimProcess(p.pid(), p.arrival(), p.burst(), p.priority()));
        }
        Reports.schedule(out, scheduler.schedule(workload));
        for (int pid : processTable.keySet()) {
            memory.free(pid);
        }
        processTable.clear();
        out.println("All processes exited; memory reclaimed.");
    }
}
