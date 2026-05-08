import java.util.concurrent.BlockingQueue;

/**
 * MemoryManager.java - Thread 2
 *
 * Loads jobs from the job queue to the ready queue only when enough
 * memory is available. It waits until all admitted processes have also
 * completed before terminating.
 *
 * CSC 227 - Operating Systems Project
 */
public class MemoryManager implements Runnable {

    public static final int TOTAL_MEMORY = 2048;

    private final BlockingQueue<PCB> jobQueue;
    private final BlockingQueue<PCB> readyQueue;
    private final Object memoryLock;

    private int availableMemory = TOTAL_MEMORY;
    private int admittedCount = 0;
    private int finishedCount = 0;
    private boolean loaderFinished = false;

    public MemoryManager(BlockingQueue<PCB> jobQueue, BlockingQueue<PCB> readyQueue, Object memoryLock) {
        this.jobQueue = jobQueue;
        this.readyQueue = readyQueue;
        this.memoryLock = memoryLock;
    }

    @Override
    public void run() {
        System.out.println("[Thread 2] Started: Memory Manager. Total memory = " + TOTAL_MEMORY + "MB");

        try {
            while (true) {
                PCB job = jobQueue.take();

                if (job.pid == -1) {
                    synchronized (memoryLock) {
                        loaderFinished = true;
                        memoryLock.notifyAll();
                    }
                    System.out.println("[Thread 2] No more jobs will arrive from Thread 1.");
                    break;
                }

                synchronized (memoryLock) {
                    while (availableMemory < job.memoryRequired) {
                        System.out.printf("[Thread 2] Waiting: P%d needs %dMB, available memory = %dMB%n",
                                job.pid, job.memoryRequired, availableMemory);
                        memoryLock.wait();
                    }

                    availableMemory -= job.memoryRequired;
                    admittedCount++;
                    job.state = "ready";
                    System.out.printf("[Thread 2] P%d admitted to Ready Queue. Used %dMB, available = %dMB%n",
                            job.pid, job.memoryRequired, availableMemory);
                }

                readyQueue.put(job);
            }

            synchronized (memoryLock) {
                while (finishedCount < admittedCount) {
                    memoryLock.wait();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[Thread 2] Interrupted.");
        }

        System.out.println("[Thread 2] Finished: All processes were admitted and completed.");
    }

    public void freeMemory(PCB process) {
        synchronized (memoryLock) {
            availableMemory += process.memoryRequired;
            finishedCount++;
            System.out.printf("[MemMgr] P%d completed. Freed %dMB. Available memory = %dMB%n",
                    process.pid, process.memoryRequired, availableMemory);
            memoryLock.notifyAll();
        }
    }

    public int getAvailableMemory() {
        synchronized (memoryLock) {
            return availableMemory;
        }
    }

    public boolean isLoaderFinished() {
        synchronized (memoryLock) {
            return loaderFinished;
        }
    }

    public boolean isAllDone() {
        synchronized (memoryLock) {
            return loaderFinished && finishedCount == admittedCount;
        }
    }
}
