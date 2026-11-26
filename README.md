# MiniOS: CPU Scheduler & Memory Simulator

![CI](https://github.com/Soumyadeep5678/MiniOS-CPU-Scheduler-Memory-Simulator/actions/workflows/ci.yml/badge.svg)

MiniOS is a Java 17 simulator for core operating-system concepts. Every algorithm prints its
work step by step: Gantt charts for schedulers, frame tables for paging, and live thread
results for concurrency. A benchmark compares all of them on 10,000 synthetic processes.

```
== SRTF (preemptive SJF) ==
| P1 | P2 | P4 | P1 | P3 |
0    1    5    10   17   26
```

## Features

| Area | What is implemented | Package |
| --- | --- | --- |
| CPU scheduling | FCFS, SJF, SRTF (preemptive SJF), Priority (preemptive and non-preemptive), Round Robin, Multilevel Feedback Queue with priority boost | `scheduler` |
| Metrics | Waiting, turnaround and response time, max wait (starvation), context switches, CPU utilization, throughput | `scheduler.ScheduleResult` |
| Virtual memory | FIFO, LRU, Optimal (Belady) and Clock (second chance) page replacement, with a step-by-step frame table and a Belady's-anomaly demo | `memory` |
| Physical memory | Contiguous allocation with first-, best- and worst-fit, hole coalescing and an external-fragmentation measure | `memory.ContiguousAllocator` |
| Real-time | Rate Monotonic vs Earliest Deadline First over a hyperperiod, deadline-miss detection, Liu & Layland bound | `realtime` |
| Concurrency | Producer-consumer with counting semaphores; dining philosophers showing a real deadlock (found by the JVM's wait-for graph) and two ways to prevent it | `concurrency` |
| Deadlock avoidance | Banker's algorithm: safety check and resource-request handling | `deadlock` |
| Mini kernel | A shell with `create`, `kill`, `ps`, `mem`, `fit` and `run <scheduler>` over a process table and the allocator | `kernel` |
| Benchmark | 10,000 seeded synthetic processes (Poisson arrivals, mixed short and long jobs) and 100,000 page references with locality, with CSV output | `bench` |

## Data structures and complexity

| Component | Structure | Cost per decision |
| --- | --- | --- |
| SJF / SRTF / Priority ready queue | Binary heap (`PriorityQueue`) | O(log n) |
| FCFS, Round Robin, MLFQ levels | Array deque | O(1) |
| Page lookup (all policies) | Hash map, page to frame | O(1) |
| LRU | Linked hash set in recency order | O(1) |
| Optimal | Next-use indices precomputed in one backward pass, plus a balanced tree (`TreeSet`) of resident frames by next use | O(log f) |
| Clock | Circular reference-bit array | amortised O(1) |
| Contiguous allocator | Sorted map (`TreeMap`) of blocks by address; neighbours found in O(log b) for coalescing | O(b) to search, O(log b) to free |

## Run it

You need JDK 17 or newer.

```bash
./run.sh                 # interactive menu (no Maven needed)
./run.sh schedule        # compare all schedulers on the sample workload
./run.sh paging          # FIFO / LRU / Optimal / Clock, step by step
./run.sh belady          # Belady's anomaly
./run.sh realtime        # Rate Monotonic vs EDF
./run.sh concurrency     # producer-consumer and dining philosophers
./run.sh bankers         # Banker's algorithm
./run.sh bench --processes 10000 --out bench-results
./run.sh kernel          # mini kernel shell
```

With Maven:

```bash
mvn verify                       # compile and run all tests
java -jar target/minios.jar      # same commands as above
```

### Kernel shell example

```
minios> create editor 300 6 2
minios> create compiler 500 10 1
minios> create shell 100 3 0
minios> kill 2
minios> mem          # memory map, holes, external fragmentation
minios> run mlfq     # schedule everything to completion, then reclaim memory
```

## Tests

JUnit 5 tests check the algorithms against worked examples from *Operating System Concepts*
(Silberschatz), along with general properties:

- Exact Gantt charts and average waiting times for FCFS (8.75), SJF (7.75), SRTF (6.5), RR q=4 (11.75) and Priority (8.2).
- On random 2,000-process workloads, every scheduler produces a contiguous timeline, never runs a process before it arrives, and gives each process exactly its burst time.
- SRTF has the lowest average waiting time of all schedulers on 10,000 processes.
- Page faults on the textbook string are FIFO 15, LRU 12 and Optimal 9. Belady's anomaly reproduces (FIFO: 9 faults with 3 frames, 10 with 4). Optimal is never beaten, and LRU never gets worse with more frames.
- Banker's algorithm finds the safe sequence ⟨P1, P3, P4, P0, P2⟩ and correctly refuses the unsafe request.
- RM misses a deadline at U = 0.971 while EDF meets every one.
- Producer-consumer loses no items. Naive dining philosophers always deadlocks; resource ordering and the waiter never do.

## Project layout

```
src/main/java/com/minios/
  process/      SimProcess (simulated PCB)
  scheduler/    Scheduler interface, FCFS, SJF/SRTF, Priority, RR, MLFQ, Timeline
  memory/       Page replacers, ContiguousAllocator
  realtime/     Rate Monotonic and EDF simulation
  concurrency/  BoundedBuffer, ProducerConsumer, DiningPhilosophers
  deadlock/     BankersAlgorithm
  kernel/       MiniKernel shell
  bench/        WorkloadGenerator, Benchmark
  cli/          Interactive menu, demos, report formatting
  util/         ASCII tables and Gantt charts
```

## Credits

MiniOS consolidates and rewrites two earlier projects: my Java OS coursework simulations,
and design ideas from SimuKernel by Mohamed Mostafa (mo7ammedd, MIT License). See [NOTICE](NOTICE).
