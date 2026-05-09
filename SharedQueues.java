import java.util.LinkedList;

/**
 * SharedQueues.java - Holds the two shared queues and one shared lock.
 *
 * Instead of BlockingQueue, we use plain LinkedList + synchronized blocks.
 * We put everything in ONE place so all threads use the SAME lock object.
 * This removes the risk of deadlock from using multiple different locks.
 *
 * HOW IT WORKS:
 * - Every method that touches jobQueue or readyQueue is synchronized on
 *   the SAME object (this).
 * - Threads call wait() when the queue they need is empty.
 * - Threads call notifyAll() after adding something, to wake up waiters.
 *
 * CSC 227 - Operating Systems Project
 */
public class SharedQueues {

    // The two shared queues
    private final LinkedList<PCB> jobQueue   = new LinkedList<>();
    private final LinkedList<PCB> readyQueue = new LinkedList<>();

    // Flag: Thread 1 sets this to true when it finishes reading the file
    private boolean jobLoadingDone = false;

    // ── Job Queue operations ──────────────────────────────────────────────

    /**
     * Thread 1 calls this to add a process to the job queue.
     * Wakes up Thread 2 which may be waiting for a new job.
     */
    public synchronized void addToJobQueue(PCB process) {
        jobQueue.add(process);
        notifyAll(); // wake Thread 2
    }

    /**
     * Thread 2 calls this to take the next job from the job queue.
     * Blocks (waits) if the queue is empty and Thread 1 is still running.
     * Returns null when Thread 1 is done AND the queue is empty.
     */
    public synchronized PCB takeFromJobQueue() throws InterruptedException {
        // Wait until there is something in the queue OR Thread 1 is done
        while (jobQueue.isEmpty() && !jobLoadingDone) {
            wait();
        }

        // If done and empty → no more jobs
        if (jobQueue.isEmpty()) {
            return null;
        }

        return jobQueue.removeFirst();
    }

    /**
     * Thread 1 calls this when it finishes reading job.txt.
     * Wakes Thread 2 so it can check if the queue is empty and exit.
     */
    public synchronized void markJobLoadingDone() {
        jobLoadingDone = true;
        notifyAll(); // wake Thread 2
    }

    // ── Ready Queue operations ────────────────────────────────────────────

    /**
     * Thread 2 calls this to add an admitted process to the ready queue.
     * Wakes up the Main Thread (Scheduler) which may be waiting.
     */
    public synchronized void addToReadyQueue(PCB process) {
        readyQueue.add(process);
        notifyAll(); // wake Main Thread
    }

    /**
     * Main Thread calls this to get a process from the ready queue.
     * Blocks (waits) if the queue is empty.
     * Uses a timeout so the scheduler doesn't wait forever.
     *
     * @param timeoutMs how long to wait before returning null
     */
    public synchronized PCB pollFromReadyQueue(long timeoutMs)
            throws InterruptedException {

        if (readyQueue.isEmpty()) {
            wait(timeoutMs); // wait briefly for new arrivals
        }

        if (readyQueue.isEmpty()) {
            return null; // still empty after timeout
        }

        return readyQueue.removeFirst();
    }

    /**
     * Main Thread calls this to check and drain everything currently
     * in the ready queue without blocking.
     */
    public synchronized LinkedList<PCB> drainReadyQueue() {
        LinkedList<PCB> drained = new LinkedList<>(readyQueue);
        readyQueue.clear();
        return drained;
    }

    /**
     * Check if ready queue is empty (non-blocking).
     */
    public synchronized boolean isReadyQueueEmpty() {
        return readyQueue.isEmpty();
    }

    /**
     * Check if job loading is finished.
     */
    public synchronized boolean isJobLoadingDone() {
        return jobLoadingDone;
    }
}
