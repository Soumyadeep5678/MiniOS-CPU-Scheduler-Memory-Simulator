package com.minios.scheduler;

import com.minios.process.SimProcess;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Multilevel Feedback Queue.
 *
 * <ul>
 *   <li>New processes enter the top queue (level 0).</li>
 *   <li>Level {@code i} is round robin with quantum {@code quanta[i]}.</li>
 *   <li>A process that uses its whole quantum is demoted one level (CPU-bound jobs sink).</li>
 *   <li>A process running below level 0 is preempted when a new process arrives.</li>
 *   <li>Every {@code boostInterval} time units all processes return to level 0, so long
 *       jobs cannot starve (set it to 0 to disable).</li>
 * </ul>
 */
public final class MlfqScheduler implements Scheduler {

    private final int[] quanta;
    private final int boostInterval;

    public MlfqScheduler() {
        this(new int[] {2, 4, 8}, 100);
    }

    public MlfqScheduler(int[] quanta, int boostInterval) {
        if (quanta == null || quanta.length == 0) {
            throw new IllegalArgumentException("at least one queue level is required");
        }
        for (int q : quanta) {
            if (q <= 0) {
                throw new IllegalArgumentException("quanta must be > 0: " + Arrays.toString(quanta));
            }
        }
        if (boostInterval < 0) {
            throw new IllegalArgumentException("boostInterval must be >= 0");
        }
        this.quanta = quanta.clone();
        this.boostInterval = boostInterval;
    }

    @Override
    public String name() {
        return "MLFQ " + Arrays.toString(quanta) + (boostInterval > 0 ? " boost=" + boostInterval : "");
    }

    private static final class Job {
        final SimProcess process;
        int level;
        int usedInLevel;

        Job(SimProcess process) {
            this.process = process;
        }
    }

    @Override
    public ScheduleResult schedule(List<SimProcess> workload) {
        List<SimProcess> processes = SchedulerSupport.sortedCopies(workload);
        int levels = quanta.length;
        List<ArrayDeque<Job>> queues = new ArrayList<>(levels);
        for (int i = 0; i < levels; i++) {
            queues.add(new ArrayDeque<>());
        }
        Timeline timeline = new Timeline();

        int n = processes.size();
        int next = 0;
        int done = 0;
        int time = 0;
        int nextBoost = boostInterval > 0 ? boostInterval : Integer.MAX_VALUE;

        while (done < n) {
            while (next < n && processes.get(next).arrivalTime() <= time) {
                queues.get(0).addLast(new Job(processes.get(next++)));
            }
            if (time >= nextBoost) {
                boost(queues);
                while (nextBoost <= time) {
                    nextBoost += boostInterval;
                }
            }

            int level = highestNonEmpty(queues);
            if (level < 0) {
                int nextArrival = processes.get(next).arrivalTime();
                timeline.record(GanttSlice.IDLE, time, nextArrival);
                time = nextArrival;
                continue;
            }

            Job job = queues.get(level).pollFirst();
            SimProcess p = job.process;
            p.markStarted(time);

            int slice = Math.min(quanta[level] - job.usedInLevel, p.remainingTime());
            if (level > 0 && next < n) {
                slice = Math.min(slice, processes.get(next).arrivalTime() - time);
            }
            slice = Math.min(slice, nextBoost - time);

            timeline.record(p.pid(), time, time + slice);
            time += slice;
            p.run(slice);
            job.usedInLevel += slice;

            if (p.isFinished()) {
                p.markCompleted(time);
                done++;
            } else if (job.usedInLevel >= quanta[level]) {
                job.level = Math.min(level + 1, levels - 1);
                job.usedInLevel = 0;
                queues.get(job.level).addLast(job);
            } else {
                // Preempted before its quantum ran out: resume first at the same level.
                queues.get(level).addFirst(job);
            }
        }
        return new ScheduleResult(name(), processes, timeline);
    }

    private static void boost(List<ArrayDeque<Job>> queues) {
        ArrayDeque<Job> top = queues.get(0);
        for (int level = 1; level < queues.size(); level++) {
            ArrayDeque<Job> queue = queues.get(level);
            while (!queue.isEmpty()) {
                Job job = queue.pollFirst();
                job.level = 0;
                job.usedInLevel = 0;
                top.addLast(job);
            }
        }
    }

    private static int highestNonEmpty(List<ArrayDeque<Job>> queues) {
        for (int i = 0; i < queues.size(); i++) {
            if (!queues.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }
}
