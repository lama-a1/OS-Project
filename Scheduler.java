import java.util.*;

/**
 * Scheduler.java - Main Thread CPU Scheduler
 *
 * Implements three scheduling algorithms:
 * 1. Shortest Job First (SJF)
 * 2. Round Robin (q = 5ms)
 * 3. Priority Scheduling with Starvation Detection and Aging
 *
 * Uses SharedQueues instead of BlockingQueue.
 *
 * CSC 227 - Operating Systems Project
 */
public class Scheduler {

    private static final int TIME_QUANTUM          = 5;
    private static final int AGING_INTERVAL        = 4;
    private static final int STARVATION_MULTIPLIER = 5;

    private final SharedQueues   queues;
    private final MemoryManager  memoryManager;
    private final int            totalProcesses;

    private final List<PCB>        allProcesses = new ArrayList<>();
    private final List<GanttEntry> gantt        = new ArrayList<>();

    private int currentTime = 0;

    public Scheduler(SharedQueues queues, MemoryManager memoryManager, int totalProcesses) {
        this.queues         = queues;
        this.memoryManager  = memoryManager;
        this.totalProcesses = totalProcesses;
    }

    // =========================================================
    // ALGORITHM 1: Shortest Job First
    // =========================================================
    public void sjf() {
        System.out.println("\n[Scheduler] Starting Shortest Job First Scheduling...");

        List<PCB> readyList = new ArrayList<>();

        // All processes arrive at time 0 — wait for ALL before picking
        while (readyList.size() < totalProcesses) {
            collectFromReadyQueue(readyList);
        }

        int completed = 0;

        while (completed < totalProcesses) {

            // Pick the process with shortest burst time
            // Tie → earlier arrival order wins
            PCB selected = Collections.min(readyList, (a, b) -> {
                if (a.burstTime != b.burstTime) return a.burstTime - b.burstTime;
                return a.arrivalOrder - b.arrivalOrder;
            });

            readyList.remove(selected);
            runToCompletion(selected, readyList);
            completed++;
        }

        printAllOutput(false);
    }

    // =========================================================
    // ALGORITHM 2: Round Robin (q = 5ms)
    // =========================================================
    public void roundRobin() {
    System.out.println("\n[Scheduler] Starting Round Robin Scheduling (q = 5ms)...");

    // Wait for ALL processes first (same as SJF)
    // Since all arrive at time 0, collect everyone before starting
    LinkedList<PCB> rrQueue = new LinkedList<>();
    while (rrQueue.size() < totalProcesses) {
        collectFromReadyQueue(rrQueue);
        if (rrQueue.size() < totalProcesses) {
            waitForOneProcess(rrQueue);
        }
    }

    int completed = 0;

    while (completed < totalProcesses) {

        PCB selected = rrQueue.poll(); // take from front
        selected.state = "running";

        if (!selected.started) {
            selected.startTime = currentTime;
            selected.started   = true;
        }

        int startBurst = selected.remainingBurst;
        int runTime    = Math.min(TIME_QUANTUM, selected.remainingBurst);

        for (int i = 0; i < runTime; i++) {
            currentTime++;
            selected.remainingBurst--;
            for (PCB p : rrQueue) p.waitingInReady++;
        }

        gantt.add(new GanttEntry(
            selected.pid,
            currentTime - runTime, currentTime,
            startBurst, selected.remainingBurst));

        if (selected.remainingBurst == 0) {
            finishProcess(selected);
            completed++;
        } else {
            selected.state = "ready";
            rrQueue.add(selected); // back of queue
        }
    }

    printAllOutput(false);
}
    /*public void roundRobin() {
        System.out.println("\n[Scheduler] Starting Round Robin Scheduling (q = 5ms)...");

        // LinkedList used as a circular queue (add to back, take from front)
        LinkedList<PCB> rrQueue = new LinkedList<>();
        int completed = 0;

        while (completed < totalProcesses) {

            // Move any newly arrived processes into the RR queue
            collectFromReadyQueue(rrQueue);

            if (rrQueue.isEmpty()) {
                waitForOneProcess(rrQueue); // wait briefly
                continue;
            }

            PCB selected = rrQueue.poll(); // take from front
            selected.state = "running";

            if (!selected.started) {
                selected.startTime = currentTime;
                selected.started   = true;
            }

            int startBurst = selected.remainingBurst;
            int runTime    = Math.min(TIME_QUANTUM, selected.remainingBurst);

            // Run one ms at a time so we can track waiting for other processes
            for (int i = 0; i < runTime; i++) {
                currentTime++;
                selected.remainingBurst--;

                // Every ms, increment waiting time for all waiting processes
                for (PCB p : rrQueue) p.waitingInReady++;

                // Check if new processes have arrived
                collectFromReadyQueue(rrQueue);
            }

            gantt.add(new GanttEntry(
                selected.pid,
                currentTime - runTime, currentTime,
                startBurst, selected.remainingBurst));

            if (selected.remainingBurst == 0) {
                finishProcess(selected);
                completed++;
            } else {
                selected.state = "ready";
                rrQueue.add(selected); // goes to back of queue
            }
        }

        printAllOutput(false);
    }*/

    // =========================================================
    // ALGORITHM 3: Priority Scheduling with Aging
    // =========================================================
    public void priorityScheduling() {
        System.out.println("\n[Scheduler] Starting Priority Scheduling with Aging...");

        List<PCB>     readyList  = new ArrayList<>();
        List<Integer> starvedIDs = new ArrayList<>();
        int completed = 0;

        while (completed < totalProcesses) {

            collectFromReadyQueue(readyList);

            if (readyList.isEmpty()) {
                waitForOneProcess(readyList);
                continue;
            }

            // Pick process with lowest priority number (= highest priority)
            // Tie → earlier arrival order wins
            PCB selected = Collections.min(readyList, (a, b) -> {
                if (a.priority != b.priority) return a.priority - b.priority;
                return a.arrivalOrder - b.arrivalOrder;
            });

            readyList.remove(selected);
            selected.state = "running";

            if (!selected.started) {
                selected.startTime = currentTime;
                selected.started   = true;
            }

            int startBurst = selected.remainingBurst;
            int runStart   = currentTime;

            // Run to completion (non-preemptive), applying aging each ms
            while (selected.remainingBurst > 0) {
                currentTime++;
                selected.remainingBurst--;

                collectFromReadyQueue(readyList);
                applyAging(readyList, starvedIDs);
            }

            gantt.add(new GanttEntry(
                selected.pid, runStart, currentTime,
                startBurst, selected.remainingBurst));

            finishProcess(selected);
            completed++;
        }

        printAllOutput(true);
        printStarvationReport(starvedIDs);
    }

    // =========================================================
    // HELPER: Run a process to full completion (SJF)
    // =========================================================
    private void runToCompletion(PCB selected, Collection<PCB> waiting) {
        selected.state = "running";

        if (!selected.started) {
            selected.startTime = currentTime;
            selected.started   = true;
        }

        int startBurst = selected.remainingBurst;
        int runStart   = currentTime;

        while (selected.remainingBurst > 0) {
            currentTime++;
            selected.remainingBurst--;
            for (PCB p : waiting) p.waitingInReady++;
            collectFromReadyQueue(waiting);
        }

        gantt.add(new GanttEntry(
            selected.pid, runStart, currentTime,
            startBurst, selected.remainingBurst));

        finishProcess(selected);
    }

    // =========================================================
    // HELPER: Mark process as finished, calculate metrics
    // =========================================================
    private void finishProcess(PCB process) {
        process.state           = "terminated";
        process.terminationTime = currentTime;
        process.turnaroundTime  = process.terminationTime - process.arrivalTime;
        process.waitingTime     = process.turnaroundTime - process.burstTime;
        memoryManager.freeMemory(process);
    }

    // =========================================================
    // HELPER: Move all available processes from ready queue
    // =========================================================
    private void collectFromReadyQueue(Collection<PCB> destination) {
        // Drain everything currently waiting in the shared ready queue
        LinkedList<PCB> arrived = queues.drainReadyQueue();
        for (PCB p : arrived) {
            p.state = "ready";
            destination.add(p);
            if (!allProcesses.contains(p)) {
                allProcesses.add(p);
            }
            System.out.println("[Scheduler] Received: " + p);
        }
    }

    // =========================================================
    // HELPER: Wait briefly for at least one process
    // =========================================================
    private void waitForOneProcess(Collection<PCB> destination) {
        try {
            PCB process = queues.pollFromReadyQueue(50); // wait up to 50ms
            if (process != null) {
                process.state = "ready";
                destination.add(process);
                if (!allProcesses.contains(process)) {
                    allProcesses.add(process);
                }
                System.out.println("[Scheduler] Received: " + process);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // =========================================================
    // HELPER: Aging for Priority Scheduling
    // =========================================================
    private void applyAging(List<PCB> readyList, List<Integer> starvedIDs) {
        int n = readyList.size();
        if (n == 0) return;

        int threshold = n * STARVATION_MULTIPLIER; // N * 5 ms

        for (PCB p : readyList) {
            p.waitingInReady++;

            if (p.waitingInReady > threshold) {

                // Mark as starved (only once)
                if (!p.starved) {
                    p.starved = true;
                    starvedIDs.add(p.pid);
                    System.out.printf("[Starvation] P%d waited %dms > threshold %dms%n",
                            p.pid, p.waitingInReady, threshold);
                }

                // Every 4ms, boost priority (decrease priority number by 1)
                p.agingCounter++;
                if (p.agingCounter == AGING_INTERVAL) {
                    if (p.priority > 1) {
                        p.priority--;
                        System.out.printf("[Aging] P%d priority improved to %d%n",
                                p.pid, p.priority);
                    }
                    p.agingCounter = 0;
                }
            }
        }
    }

    // =========================================================
    // OUTPUT
    // =========================================================

    private void printAllOutput(boolean showPriority) {
        printGanttChart();
        printProcessTable(showPriority);
        printAverages();
    }

    private void printGanttChart() {
        System.out.println("\n============================================================");
        System.out.println("GANTT CHART");
        System.out.println("============================================================");

        if (gantt.isEmpty()) {
            System.out.println("No execution segments.");
            return;
        }

        StringBuilder boxes = new StringBuilder("|");
        StringBuilder times = new StringBuilder();

        times.append(gantt.get(0).startTime);
        for (GanttEntry e : gantt) {
            boxes.append(String.format(" P%-3d|", e.pid));
            times.append(String.format("%6d", e.endTime));
        }

        System.out.println(boxes);
        System.out.println(times);

        System.out.println("\nDetailed Execution Log:");
        for (GanttEntry e : gantt) {
            System.out.printf("P%d: time %d -> %d ms, burst %d -> %d%n",
                    e.pid, e.startTime, e.endTime, e.startBurst, e.endBurst);
        }
    }

    private void printProcessTable(boolean showPriority) {
        System.out.println("\n============================================================");
        System.out.println("PROCESS TABLE");
        System.out.println("============================================================");

        List<PCB> sorted = new ArrayList<>(allProcesses);
        sorted.sort(Comparator.comparingInt(p -> p.pid));

        if (showPriority) {
            System.out.printf("%-6s %-8s %-10s %-10s %-12s %-10s %-14s %-10s%n",
                    "PID", "Burst", "Priority", "Start", "Finish", "Waiting", "Turnaround", "Starved");
        } else {
            System.out.printf("%-6s %-8s %-10s %-12s %-10s %-14s%n",
                    "PID", "Burst", "Start", "Finish", "Waiting", "Turnaround");
        }

        for (PCB p : sorted) {
            if (showPriority) {
                System.out.printf("%-6d %-8d %-10d %-10d %-12d %-10d %-14d %-10s%n",
                        p.pid, p.burstTime, p.priority, p.startTime,
                        p.terminationTime, p.waitingTime, p.turnaroundTime,
                        p.starved ? "Yes" : "No");
            } else {
                System.out.printf("%-6d %-8d %-10d %-12d %-10d %-14d%n",
                        p.pid, p.burstTime, p.startTime, p.terminationTime,
                        p.waitingTime, p.turnaroundTime);
            }
        }
    }

    private void printAverages() {
        double totalWait = 0, totalTA = 0;
        for (PCB p : allProcesses) {
            totalWait += p.waitingTime;
            totalTA   += p.turnaroundTime;
        }
        int n = allProcesses.size();

        System.out.println("\n============================================================");
        System.out.println("PERFORMANCE METRICS");
        System.out.println("============================================================");
        System.out.printf("Average Waiting Time    : %.2f ms%n", totalWait / n);
        System.out.printf("Average Turnaround Time : %.2f ms%n", totalTA   / n);
    }

    private void printStarvationReport(List<Integer> starvedIDs) {
        System.out.println("\n============================================================");
        System.out.println("STARVATION REPORT");
        System.out.println("============================================================");

        if (starvedIDs.isEmpty()) {
            System.out.println("No process suffered from starvation.");
        } else {
            System.out.println("Processes that suffered starvation and received aging:");
            for (int pid : starvedIDs) {
                System.out.println("P" + pid);
            }
        }
    }
}
