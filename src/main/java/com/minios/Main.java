package com.minios;

import com.minios.bench.Benchmark;
import com.minios.cli.Demos;
import com.minios.cli.InteractiveMenu;
import com.minios.kernel.MiniKernel;
import com.minios.memory.ContiguousAllocator;

import java.nio.file.Path;
import java.util.Scanner;

/** Entry point: an interactive menu by default, or a single demo chosen on the command line. */
public final class Main {

    private static final String USAGE = """
            Usage: java -jar minios.jar [command] [options]

            With no command, an interactive menu starts.

            Commands:
              schedule [--quantum N]        compare all CPU schedulers on the sample workload
              paging [--frames N]           step-by-step FIFO / LRU / Optimal / Clock on the textbook string
              belady                        show Belady's anomaly under FIFO
              realtime                      Rate Monotonic vs EDF on a periodic task set
              concurrency                   producer-consumer and dining philosophers with real threads
              bankers                       Banker's algorithm safety check and resource requests
              bench [--processes N] [--seed S] [--load L] [--out DIR]
                                            benchmark every algorithm (default 10000 processes)
              kernel [--memory KB]          interactive mini kernel shell
              help                          show this message""";

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            new InteractiveMenu(new Scanner(System.in), System.out).run();
            return;
        }
        switch (args[0]) {
            case "schedule" -> Demos.scheduling(System.out, Demos.sampleWorkload(), intOption(args, "--quantum", 4));
            case "paging" -> Demos.paging(System.out, Demos.TEXTBOOK_REFERENCES, intOption(args, "--frames", 3));
            case "belady" -> Demos.beladyAnomaly(System.out);
            case "realtime" -> Demos.realTime(System.out, Demos.sampleTasks());
            case "concurrency" -> Demos.concurrency(System.out);
            case "bankers" -> Demos.bankers(System.out);
            case "bench" -> {
                String out = option(args, "--out", null);
                Benchmark.run(System.out,
                        intOption(args, "--processes", 10_000),
                        Long.parseLong(option(args, "--seed", "42")),
                        Double.parseDouble(option(args, "--load", "0.9")),
                        out == null ? null : Path.of(out));
            }
            case "kernel" -> new MiniKernel(intOption(args, "--memory", 1024), ContiguousAllocator.Fit.FIRST, System.out)
                    .repl(new Scanner(System.in));
            case "help", "-h", "--help" -> System.out.println(USAGE);
            default -> {
                System.err.println("Unknown command: " + args[0]);
                System.err.println(USAGE);
                System.exit(2);
            }
        }
    }

    private static String option(String[] args, String name, String fallback) {
        for (int i = 1; i < args.length - 1; i++) {
            if (args[i].equals(name)) {
                return args[i + 1];
            }
        }
        return fallback;
    }

    private static int intOption(String[] args, String name, int fallback) {
        return Integer.parseInt(option(args, name, String.valueOf(fallback)));
    }
}
