/**
 * MemoryManager.java - Thread 2
 *
 * Takes jobs from the job queue, checks if there is enough memory,
 * and moves them to the ready queue.
 *
 * MEMORY SYNCHRONIZATION:
 * availableMemory is shared between Thread 2 (decreases it on admission)
 * and the Main Thread (increases it when a process finishes).
 * We use 'synchronized(memoryLock)' with wait/notifyAll to coordinate.
 *
 * CSC 227 - Operating Systems Project
 */
public class MemoryManager implements Runnable {

    public static final int TOTAL_MEMORY = 2048;

    private final SharedQueues queues;
    private final Object memoryLock; // shared lock for memory access

    private int availableMemory = TOTAL_MEMORY;
    private int admittedCount   = 0; // how many processes entered ready queue
    private int finishedCount   = 0; // how many processes completed

    public MemoryManager(SharedQueues queues, Object memoryLock) {
        this.queues     = queues;
        this.memoryLock = memoryLock;
    }

    @Override
    public void run() {
        System.out.println("[Thread 2] Started: Memory Manager. Total memory = " + TOTAL_MEMORY + "MB");

        try {
            while (true) {

                // Take next job — blocks if queue is empty and Thread 1 still running
                // Returns null when Thread 1 is done and queue is empty
                PCB job = queues.takeFromJobQueue();

                if (job == null) {
                    // No more jobs will ever come
                    System.out.println("[Thread 2] No more jobs from Thread 1.");
                    break;
                }

                // Wait until there is enough memory for this job
                synchronized (memoryLock) {
                    while (availableMemory < job.memoryRequired) {
                        System.out.printf(
                            "[Thread 2] Waiting: P%d needs %dMB, available = %dMB%n",
                            job.pid, job.memoryRequired, availableMemory);
                        memoryLock.wait(); // releases lock and sleeps
                    }

                    // Allocate memory
                    availableMemory -= job.memoryRequired;
                    admittedCount++;

                    System.out.printf(
                        "[Thread 2] P%d admitted to Ready Queue. Available = %dMB%n",
                        job.pid, availableMemory);
                }

                // Move to ready queue
                job.state = "ready";
                queues.addToReadyQueue(job);
            }

            // Wait until ALL admitted processes finish before Thread 2 exits
            synchronized (memoryLock) {
                while (finishedCount < admittedCount) {
                    memoryLock.wait();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[Thread 2] Interrupted.");
        }

        System.out.println("[Thread 2] Finished: All processes admitted and completed.");
    }

    /**
     * Called by Main Thread (Scheduler) when a process finishes.
     * Frees memory and wakes Thread 2 if it was waiting for memory.
     */
    public void freeMemory(PCB process) {
        synchronized (memoryLock) {
            availableMemory += process.memoryRequired;
            finishedCount++;
            System.out.printf(
                "[MEM-FREE] P%d completed. Freed %dMB. Available = %dMB%n",
                process.pid, process.memoryRequired, availableMemory);
            memoryLock.notifyAll(); // wake Thread 2
        }
    }

    public int getAvailableMemory() {
    synchronized (memoryLock) {
        return availableMemory;
    }
}

}
