import java.util.LinkedList;

public class MemoryManager implements Runnable {

    // حجم الذاكرة الكلي حسب المشروع
    public static final int TOTAL_MEMORY = 2048;

    private LinkedList<PCB> jobQueue;
    private LinkedList<PCB> readyQueue;
    private JobLoader jobLoader;

    private int availableMemory = TOTAL_MEMORY;
    private int admittedCount = 0;   // عدد العمليات التي دخلت Ready Queue
    private int finishedCount = 0;   // عدد العمليات التي انتهت

    public MemoryManager(LinkedList<PCB> jobQueue,
                         LinkedList<PCB> readyQueue,
                         JobLoader jobLoader) {
        this.jobQueue = jobQueue;
        this.readyQueue = readyQueue;
        this.jobLoader = jobLoader;
    }

    @Override
    public void run() {
        System.out.println("[Thread 2] Started: Memory Manager");

        try {
            while (true) {
                PCB process = null;

                // انتظار وجود عملية في Job Queue
                synchronized (jobQueue) {
                    while (jobQueue.isEmpty() && !jobLoader.finished) {
                        jobQueue.wait();
                    }

                    // إذا انتهت القراءة والطابور فارغ
                    if (jobQueue.isEmpty() && jobLoader.finished) {
                        break;
                    }

                    // أخذ أول عملية دون حذفها
                    process = jobQueue.getFirst();
                }

                // التحقق من توفر الذاكرة
                synchronized (this) {
                    while (availableMemory < process.memoryRequired) {
                        System.out.println(
                                "[Thread 2] Waiting: P" + process.pid +
                                " needs " + process.memoryRequired +
                                "MB, available = " + availableMemory + "MB"
                        );
                        wait();
                    }

                    // تخصيص الذاكرة
                    availableMemory -= process.memoryRequired;
                    admittedCount++;
                }

                // إزالة العملية من Job Queue
                synchronized (jobQueue) {
                    jobQueue.removeFirst();
                }

                // إدخالها إلى Ready Queue
                process.state = "ready";

                synchronized (readyQueue) {
                    readyQueue.add(process);
                    readyQueue.notifyAll();
                }

                System.out.println(
                        "[Thread 2] P" + process.pid +
                        " admitted to Ready Queue. Available memory = " +
                        availableMemory + "MB"
                );
            }

            // الانتظار حتى تنتهي جميع العمليات التي تم قبولها
            synchronized (this) {
                while (finishedCount < admittedCount) {
                    wait();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println(
                "[Thread 2] Finished: All processes were admitted and completed."
        );
    }

    // تستدعى من Scheduler عند انتهاء عملية
    public synchronized void freeMemory(PCB process) {
        availableMemory += process.memoryRequired;
        finishedCount++;

        System.out.println(
                "[Memory] P" + process.pid +
                " completed. Freed " + process.memoryRequired +
                "MB. Available memory = " + availableMemory + "MB"
        );

        notifyAll();
    }

    public synchronized int getAvailableMemory() {
        return availableMemory;
    }

    public synchronized boolean isAllDone() {
        return jobLoader.finished && finishedCount == admittedCount;
    }
}