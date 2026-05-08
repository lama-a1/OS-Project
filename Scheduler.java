import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler.java - Main Thread CPU Scheduler
 *
 * Contains the three required scheduling algorithms:
 * 1. Shortest Job First
 * 2. Round Robin with q = 5 ms
 * 3. Non-preemptive Priority Scheduling with starvation detection and aging
 *
 * The scheduler works dynamically with the ready queue while Thread 2 is
 * loading jobs based on available memory.
 *
 * CSC 227 - Operating Systems Project
 */
public class Scheduler {

    private static final int TIME_QUANTUM = 5;
    private static final int AGING_INTERVAL = 4;
    private static final int STARVATION_MULTIPLIER = 5;

    private final BlockingQueue<PCB> readyQueue;
    private final MemoryManager memoryManager;
    private final int totalProcesses;

    private final List<PCB> allProcesses = new ArrayList<>();
    private final List<GanttEntry> gantt = new ArrayList<>();

    private int currentTime = 0;

    public Scheduler(BlockingQueue<PCB> readyQueue, MemoryManager memoryManager, int totalProcesses) {
        this.readyQueue = readyQueue;
        this.memoryManager = memoryManager;
        this.totalProcesses = totalProcesses;
    }

    public void sjf() {
    System.out.println("\n[Scheduler] Starting Shortest Job First Scheduling...");

    List<PCB> readyList = new ArrayList<>();
    int completed = 0;

    // Since all processes arrive at time 0, wait until ALL of them
    // are in the ready queue before making any scheduling decision.
    // This ensures SJF always sees the full picture before picking.
    while (readyList.size() < totalProcesses) {
        waitForProcess(readyList);
        moveArrivedProcesses(readyList);
    }

    // Now pick shortest job first, one at a time
    while (completed < totalProcesses) {

        PCB selected = Collections.min(readyList, (a, b) -> {
            if (a.burstTime != b.burstTime) return a.burstTime - b.burstTime;
            return a.arrivalOrder - b.arrivalOrder;
        });

        readyList.remove(selected);
        runNonPreemptive(selected, readyList);
        completed++;
    }

    printAllOutput(false);
}

    /*public void sjf() {
        System.out.println("\n[Scheduler] Starting Shortest Job First Scheduling...");

        List<PCB> readyList = new ArrayList<>();
        int completed = 0;

        while (completed < totalProcesses) {
            moveArrivedProcesses(readyList);

            if (readyList.isEmpty()) {
                waitForProcess(readyList);
                continue;
            }

            PCB selected = Collections.min(readyList, (a, b) -> {
                if (a.burstTime != b.burstTime) return a.burstTime - b.burstTime;
                return a.arrivalOrder - b.arrivalOrder;
            });

            readyList.remove(selected);
            runNonPreemptive(selected, readyList);
            completed++;
        }

        printAllOutput(false);
    }*/

    public void roundRobin() {
        System.out.println("\n[Scheduler] Starting Round Robin Scheduling (q = 5ms)...");

        Queue<PCB> rrQueue = new LinkedList<>();
        int completed = 0;

        while (completed < totalProcesses) {
            moveArrivedProcesses(rrQueue);

            if (rrQueue.isEmpty()) {
                waitForProcess(rrQueue);
                continue;
            }

            PCB selected = rrQueue.poll();
            selected.state = "running";

            if (!selected.started) {
                selected.startTime = currentTime;
                selected.started = true;
            }

            int startBurst = selected.remainingBurst;
            int runTime = Math.min(TIME_QUANTUM, selected.remainingBurst);

            for (int i = 0; i < runTime; i++) {
                currentTime++;
                selected.remainingBurst--;
                incrementWaitingForQueue(rrQueue, 1);
                moveArrivedProcesses(rrQueue);
            }

            gantt.add(new GanttEntry(selected.pid, currentTime - runTime, currentTime,
                    startBurst, selected.remainingBurst));

            if (selected.remainingBurst == 0) {
                finishProcess(selected);
                completed++;
            } else {
                selected.state = "ready";
                rrQueue.add(selected);
            }
        }

        printAllOutput(false);
    }

    public void priorityScheduling() {
        System.out.println("\n[Scheduler] Starting Priority Scheduling with Aging...");

        List<PCB> readyList = new ArrayList<>();
        List<Integer> starvedPIDs = new ArrayList<>();
        int completed = 0;

        while (completed < totalProcesses) {
            moveArrivedProcesses(readyList);

            if (readyList.isEmpty()) {
                waitForProcess(readyList);
                continue;
            }

            PCB selected = Collections.min(readyList, (a, b) -> {
                if (a.priority != b.priority) return a.priority - b.priority;
                return a.arrivalOrder - b.arrivalOrder;
            });

            readyList.remove(selected);
            selected.state = "running";

            if (!selected.started) {
                selected.startTime = currentTime;
                selected.started = true;
            }

            int startBurst = selected.remainingBurst;
            int runStart = currentTime;

            while (selected.remainingBurst > 0) {
                currentTime++;
                selected.remainingBurst--;

                moveArrivedProcesses(readyList);
                applyPriorityWaitingAndAging(readyList, starvedPIDs);
            }

            gantt.add(new GanttEntry(selected.pid, runStart, currentTime, startBurst, selected.remainingBurst));
            finishProcess(selected);
            completed++;
        }

        printAllOutput(true);
        printStarvationReport(starvedPIDs);
    }

    private void runNonPreemptive(PCB selected, Collection<PCB> waitingProcesses) {
        selected.state = "running";

        if (!selected.started) {
            selected.startTime = currentTime;
            selected.started = true;
        }

        int startBurst = selected.remainingBurst;
        int runStart = currentTime;

        while (selected.remainingBurst > 0) {
            currentTime++;
            selected.remainingBurst--;
            incrementWaitingForQueue(waitingProcesses, 1);
            moveArrivedProcesses(waitingProcesses);
        }

        gantt.add(new GanttEntry(selected.pid, runStart, currentTime, startBurst, selected.remainingBurst));
        finishProcess(selected);
    }

    private void finishProcess(PCB process) {
        process.state = "terminated";
        process.terminationTime = currentTime;
        process.turnaroundTime = process.terminationTime - process.arrivalTime;
        process.waitingTime = process.turnaroundTime - process.burstTime;
        memoryManager.freeMemory(process);
    }

    private void moveArrivedProcesses(Collection<PCB> destination) {
        PCB process;
        while ((process = readyQueue.poll()) != null) {
            process.state = "ready";
            destination.add(process);
            if (!allProcesses.contains(process)) {
                allProcesses.add(process);
            }
            System.out.println("[Scheduler] Received: " + process);
        }
    }

    private void waitForProcess(Collection<PCB> destination) {
        try {
            PCB process = readyQueue.poll(50, TimeUnit.MILLISECONDS);
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

    private void incrementWaitingForQueue(Collection<PCB> waitingProcesses, int amount) {
        for (PCB p : waitingProcesses) {
            p.waitingInReady += amount;
        }
    }

    private void applyPriorityWaitingAndAging(List<PCB> readyList, List<Integer> starvedPIDs) {
        int n = readyList.size();
        if (n == 0) {
            return;
        }

        int threshold = n * STARVATION_MULTIPLIER;

        for (PCB p : readyList) {
            p.waitingInReady++;

            if (p.waitingInReady > threshold) {
                if (!p.starved) {
                    p.starved = true;
                    starvedPIDs.add(p.pid);
                    System.out.printf("[Starvation] P%d waited %dms > %dms%n",
                            p.pid, p.waitingInReady, threshold);
                }

                p.agingCounter++;
                if (p.agingCounter == AGING_INTERVAL) {
                    if (p.priority > 1) {
                        p.priority--;
                        System.out.printf("[Aging] P%d priority improved to %d%n", p.pid, p.priority);
                    }
                    p.agingCounter = 0;
                }
            }
        }
    }

    private void printAllOutput(boolean showPriority) {
        printGanttChart();
        printProcessTable(showPriority);
        printAverages();
    }

    private void printGanttChart() {
        System.out.println("\n============================================================");
        System.out.println("GANTT CHART");
        System.out.println("============================================================");

        StringBuilder boxes = new StringBuilder("|");
        StringBuilder times = new StringBuilder();

        if (gantt.isEmpty()) {
            System.out.println("No execution segments.");
            return;
        }

        times.append(gantt.get(0).startTime);
        for (GanttEntry entry : gantt) {
            boxes.append(String.format(" P%-3d|", entry.pid));
            times.append(String.format("%6d", entry.endTime));
        }

        System.out.println(boxes);
        System.out.println(times);

        System.out.println("\nDetailed Execution Log:");
        for (GanttEntry entry : gantt) {
            System.out.printf("P%d: time %d -> %d ms, burst %d -> %d%n",
                    entry.pid, entry.startTime, entry.endTime, entry.startBurst, entry.endBurst);
        }
    }

    private void printProcessTable(boolean showPriority) {
        System.out.println("\n============================================================");
        System.out.println("PROCESS TABLE");
        System.out.println("============================================================");

        List<PCB> sorted = new ArrayList<>(allProcesses);
        sorted.sort(Comparator.comparingInt(p -> p.pid));

        if (showPriority) {
            System.out.printf("%-6s %-8s %-10s %-10s %-12s %-10s %-14s %-14s%n",
                    "PID", "Burst", "Priority", "Start", "Finish", "Waiting", "Turnaround", "Starved");
        } else {
            System.out.printf("%-6s %-8s %-10s %-12s %-10s %-14s%n",
                    "PID", "Burst", "Start", "Finish", "Waiting", "Turnaround");
        }

        for (PCB p : sorted) {
            if (showPriority) {
                System.out.printf("%-6d %-8d %-10d %-10d %-12d %-10d %-14d %-14s%n",
                        p.pid, p.burstTime, p.priority, p.startTime, p.terminationTime,
                        p.waitingTime, p.turnaroundTime, p.starved ? "Yes" : "No");
            } else {
                System.out.printf("%-6d %-8d %-10d %-12d %-10d %-14d%n",
                        p.pid, p.burstTime, p.startTime, p.terminationTime,
                        p.waitingTime, p.turnaroundTime);
            }
        }
    }

    private void printAverages() {
        double totalWaiting = 0;
        double totalTurnaround = 0;

        for (PCB p : allProcesses) {
            totalWaiting += p.waitingTime;
            totalTurnaround += p.turnaroundTime;
        }

        System.out.println("\n============================================================");
        System.out.println("PERFORMANCE METRICS");
        System.out.println("============================================================");
        System.out.printf("Average Waiting Time    : %.2f ms%n", totalWaiting / allProcesses.size());
        System.out.printf("Average Turnaround Time : %.2f ms%n", totalTurnaround / allProcesses.size());
    }

    private void printStarvationReport(List<Integer> starvedPIDs) {
        System.out.println("\n============================================================");
        System.out.println("STARVATION REPORT");
        System.out.println("============================================================");

        if (starvedPIDs.isEmpty()) {
            System.out.println("No process suffered from starvation.");
        } else {
            System.out.println("Processes that suffered from starvation and received aging:");
            for (int pid : starvedPIDs) {
                System.out.println("P" + pid);
            }
        }
    }
}
