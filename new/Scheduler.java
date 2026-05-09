import java.util.*;

public class Scheduler {

    // ثوابت المشروع
    private final int TIME_QUANTUM = 5;      // Round Robin quantum
    private final int AGING_INTERVAL = 4;    // Aging every 4 ms
    private final int STARVATION_FACTOR = 5; // N × 5 ms

    private LinkedList<PCB> readyQueue;
    private MemoryManager memoryManager;
    private int totalProcesses;

    // جميع العمليات المنفذة
    private ArrayList<PCB> allProcesses = new ArrayList<>();

    // بيانات الـ Gantt Chart
    private ArrayList<GanttEntry> gantt = new ArrayList<>();

    // الوقت الحالي في المحاكاة
    private int currentTime = 0;

    public Scheduler(LinkedList<PCB> readyQueue,
                     MemoryManager memoryManager,
                     int totalProcesses) {
        this.readyQueue = readyQueue;
        this.memoryManager = memoryManager;
        this.totalProcesses = totalProcesses;
    }

    // =========================================================
    // Shortest Job First (Non-Preemptive)
    // =========================================================
    public void sjf() {
        System.out.println("\n[Scheduler] Starting Shortest Job First...");

        ArrayList<PCB> readyList = new ArrayList<>();
        int completed = 0;

        // انتظار وصول جميع العمليات
        while (readyList.size() < totalProcesses) {
            moveProcesses(readyList);
            if (readyList.size() < totalProcesses) {
                waitForProcess();
            }
        }

        // تنفيذ العمليات حسب أقصر Burst
        while (completed < totalProcesses) {

            PCB selected = readyList.get(0);

            for (PCB p : readyList) {
                if (p.burstTime < selected.burstTime) {
                    selected = p;
                } else if (p.burstTime == selected.burstTime &&
                           p.arrivalOrder < selected.arrivalOrder) {
                    selected = p;
                }
            }

            readyList.remove(selected);
            runNonPreemptive(selected, readyList);
            completed++;
        }

        printAll(false);
    }

    // =========================================================
    // Round Robin (q = 5 ms)
    // =========================================================
    public void roundRobin() {
        System.out.println("\n[Scheduler] Starting Round Robin...");

        LinkedList<PCB> rrQueue = new LinkedList<>();
        int completed = 0;

        while (completed < totalProcesses) {

            moveProcesses(rrQueue);

            if (rrQueue.isEmpty()) {
                waitForProcess();
                continue;
            }

            PCB selected = rrQueue.removeFirst();
            selected.state = "running";

            if (!selected.started) {
                selected.startTime = currentTime;
                selected.started = true;
            }

            int startTime = currentTime;
            int startBurst = selected.remainingBurst;

            int runTime = Math.min(TIME_QUANTUM, selected.remainingBurst);

            for (int i = 0; i < runTime; i++) {
                currentTime++;
                selected.remainingBurst--;

                incrementWaiting(rrQueue);
                moveProcesses(rrQueue);
            }

            gantt.add(new GanttEntry(
                    selected.pid,
                    startTime,
                    currentTime,
                    startBurst,
                    selected.remainingBurst
            ));

            if (selected.remainingBurst == 0) {
                finishProcess(selected);
                completed++;
            } else {
                selected.state = "ready";
                rrQueue.add(selected);
            }
        }

        printAll(false);
    }

    // =========================================================
    // Priority Scheduling (Non-Preemptive)
    // =========================================================
    public void priorityScheduling() {
        System.out.println("\n[Scheduler] Starting Priority Scheduling...");

        ArrayList<PCB> readyList = new ArrayList<>();
        ArrayList<Integer> starvedProcesses = new ArrayList<>();
        int completed = 0;

        while (completed < totalProcesses) {

            moveProcesses(readyList);

            if (readyList.isEmpty()) {
                waitForProcess();
                continue;
            }

            // اختيار أعلى أولوية (أصغر رقم)
            PCB selected = readyList.get(0);

            for (PCB p : readyList) {
                if (p.priority < selected.priority) {
                    selected = p;
                } else if (p.priority == selected.priority &&
                           p.arrivalOrder < selected.arrivalOrder) {
                    selected = p;
                }
            }

            readyList.remove(selected);
            selected.state = "running";

            if (!selected.started) {
                selected.startTime = currentTime;
                selected.started = true;
            }

            int startTime = currentTime;
            int startBurst = selected.remainingBurst;
                        // تنفيذ العملية بالكامل (Non-Preemptive)
            while (selected.remainingBurst > 0) {
                currentTime++;
                selected.remainingBurst--;

                // أثناء التنفيذ تستمر العمليات الأخرى بالانتظار
                incrementWaiting(readyList);

                // قد تصل عمليات جديدة من Ready Queue
                moveProcesses(readyList);

                // تطبيق Starvation و Aging
                applyAging(readyList, starvedProcesses);
            }

            // حفظ هذا الجزء في Gantt Chart
            gantt.add(new GanttEntry(
                    selected.pid,
                    startTime,
                    currentTime,
                    startBurst,
                    selected.remainingBurst
            ));

            finishProcess(selected);
            completed++;
        }

        printAll(true);
        printStarvationReport(starvedProcesses);
    }

    // =========================================================
    // تنفيذ عملية كاملة (يستخدم في SJF)
    // =========================================================
    private void runNonPreemptive(PCB selected,
                                  Collection<PCB> waitingProcesses) {

        selected.state = "running";

        if (!selected.started) {
            selected.startTime = currentTime;
            selected.started = true;
        }

        int startTime = currentTime;
        int startBurst = selected.remainingBurst;

        while (selected.remainingBurst > 0) {
            currentTime++;
            selected.remainingBurst--;

            // زيادة وقت انتظار العمليات الأخرى
            incrementWaiting(waitingProcesses);

            // إدخال أي عمليات جديدة وصلت
            moveProcesses(waitingProcesses);
        }

        gantt.add(new GanttEntry(
                selected.pid,
                startTime,
                currentTime,
                startBurst,
                selected.remainingBurst
        ));

        finishProcess(selected);
    }

    // =========================================================
    // إنهاء العملية وحساب القيم النهائية
    // =========================================================
    private void finishProcess(PCB process) {
        process.state = "terminated";
        process.terminationTime = currentTime;
        process.turnaroundTime =
                process.terminationTime - process.arrivalTime;
        process.waitingTime =
                process.turnaroundTime - process.burstTime;

        // تحرير الذاكرة
        memoryManager.freeMemory(process);

        // حفظ العملية في القائمة النهائية
        allProcesses.add(process);
    }

    // =========================================================
    // نقل العمليات من Ready Queue المشتركة إلى قائمة محلية
    // =========================================================
    private void moveProcesses(Collection<PCB> destination) {
        synchronized (readyQueue) {
            while (!readyQueue.isEmpty()) {
                PCB process = readyQueue.removeFirst();
                process.state = "ready";
                destination.add(process);

                System.out.println(
                        "[Scheduler] Received: " + process
                );
            }
        }
    }

    // =========================================================
    // الانتظار قليلًا حتى تصل عملية جديدة
    // =========================================================
    private void waitForProcess() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // =========================================================
    // زيادة waitingInReady لكل العمليات المنتظرة
    // =========================================================
    private void incrementWaiting(Collection<PCB> waitingProcesses) {
        for (PCB p : waitingProcesses) {
            p.waitingInReady++;
        }
    }

    // =========================================================
    // Starvation Detection + Aging
    // =========================================================
    private void applyAging(ArrayList<PCB> readyList,
                            ArrayList<Integer> starvedProcesses) {

        int n = readyList.size();
        if (n == 0) {
            return;
        }

        int threshold = n * STARVATION_FACTOR;   // N × 5

        for (PCB p : readyList) {

            // إذا تجاوزت العملية حد الـStarvation
            if (p.waitingInReady > threshold) {

                if (!p.starved) {
                    p.starved = true;
                    starvedProcesses.add(p.pid);

                    System.out.println(
                            "[Starvation] P" + p.pid +
                            " waited " + p.waitingInReady +
                            " ms (threshold = " + threshold + " ms)"
                    );
                }

                // تطبيق Aging كل 4 ms
                p.agingCounter++;

                if (p.agingCounter >= AGING_INTERVAL) {
                    if (p.priority > 1) {
                        p.priority--;

                        System.out.println(
                                "[Aging] P" + p.pid +
                                " priority improved to " +
                                p.priority
                        );
                    }

                    p.agingCounter = 0;
                }
            }
        }
    }

    // =========================================================
    // طباعة جميع النتائج
    // =========================================================
    private void printAll(boolean showPriority) {
        printGanttChart();
        printProcessTable(showPriority);
        printAverages();
    }

    // =========================================================
    // Gantt Chart
    // =========================================================
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

        for (GanttEntry entry : gantt) {
            boxes.append(String.format(" P%-3d|", entry.pid));
            times.append(String.format("%6d", entry.endTime));
        }

        System.out.println(boxes);
        System.out.println(times);

        System.out.println("\nDetailed Execution Log:");

        for (GanttEntry entry : gantt) {
            System.out.printf(
                    "P%d: time %d -> %d ms, burst %d -> %d%n",
                    entry.pid,
                    entry.startTime,
                    entry.endTime,
                    entry.startBurst,
                    entry.endBurst
            );
        }
    }

    // =========================================================
    // Process Table
    // =========================================================
    private void printProcessTable(boolean showPriority) {
        System.out.println("\n============================================================");
        System.out.println("PROCESS TABLE");
        System.out.println("============================================================");

        allProcesses.sort((a, b) -> a.pid - b.pid);

        if (showPriority) {
            System.out.printf(
                    "%-6s %-8s %-10s %-10s %-12s %-10s %-14s %-14s%n",
                    "PID", "Burst", "Priority", "Start",
                    "Finish", "Waiting", "Turnaround", "Starved"
            );
        } else {
            System.out.printf(
                    "%-6s %-8s %-10s %-12s %-10s %-14s%n",
                    "PID", "Burst", "Start",
                    "Finish", "Waiting", "Turnaround"
            );
        }

        for (PCB p : allProcesses) {
            if (showPriority) {
                System.out.printf(
                        "%-6d %-8d %-10d %-10d %-12d %-10d %-14d %-14s%n",
                        p.pid,
                        p.burstTime,
                        p.originalPriority,
                        p.startTime,
                        p.terminationTime,
                        p.waitingTime,
                        p.turnaroundTime,
                        p.starved ? "Yes" : "No"
                );
            } else {
                System.out.printf(
                        "%-6d %-8d %-10d %-12d %-10d %-14d%n",
                        p.pid,
                        p.burstTime,
                        p.startTime,
                        p.terminationTime,
                        p.waitingTime,
                        p.turnaroundTime
                );
            }
        }
    }

    // =========================================================
    // Average Waiting and Turnaround
    // =========================================================
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

        System.out.printf(
                "Average Waiting Time    : %.2f ms%n",
                totalWaiting / allProcesses.size()
        );

        System.out.printf(
                "Average Turnaround Time : %.2f ms%n",
                totalTurnaround / allProcesses.size()
        );
    }

    // =========================================================
    // Starvation Report
    // =========================================================
    private void printStarvationReport(ArrayList<Integer> starvedProcesses) {
        System.out.println("\n============================================================");
        System.out.println("STARVATION REPORT");
        System.out.println("============================================================");

        if (starvedProcesses.isEmpty()) {
            System.out.println("No process suffered from starvation.");
        } else {
            System.out.println(
                    "Processes that suffered from starvation and received aging:"
            );

            for (int pid : starvedProcesses) {
                System.out.println("P" + pid);
            }
        }
    }
}