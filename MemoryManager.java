//Thread 2

public class MemoryManager implements Runnable {

    public static final int TOTAL_MEMORY = 2048;

    private final SharedQueues queues;
    private final Object memoryLock; //shared lock for memory access

    private int availableMemory = TOTAL_MEMORY;
    private int admittedCount   = 0; //processes entered ready queue
    private int finishedCount   = 0; //processes completed

    public MemoryManager(SharedQueues queues, Object memoryLock) {
        this.queues     = queues;
        this.memoryLock = memoryLock;
    }

    @Override
    public void run() {
        System.out.println("[Thread 2] Started: Memory Manager. Total memory = " + TOTAL_MEMORY + "MB");

        try {
            while (true) {

                PCB job = queues.takeFromJobQueue();

                if (job == null) {
                    System.out.println("[Thread 2] No more jobs from Thread 1.");
                    break;
                }

                //Wait until there is enough memory for this job
                synchronized (memoryLock) {
                    while (availableMemory < job.memoryRequired) {
                        System.out.printf(
                            "[Thread 2] Waiting: P%d needs %dMB, available = %dMB%n",
                            job.pid, job.memoryRequired, availableMemory);
                        memoryLock.wait(); //releases lock and sleeps
                    }

                    //Allocate memory
                    availableMemory -= job.memoryRequired;
                    admittedCount++;

                    System.out.printf(
                        "[Thread 2] P%d admitted to Ready Queue. Available = %dMB%n",
                        job.pid, availableMemory);
                }

                job.state = "ready";
                queues.addToReadyQueue(job);
            }

            //Wait until all admitted processes finish before Thread 2 exits
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

    //Called by the Scheduler when a process finishes to free its memory
    public void freeMemory(PCB process) {
        synchronized (memoryLock) {
            availableMemory += process.memoryRequired;
            finishedCount++;
            System.out.printf(
                "[MEM-FREE] P%d completed. Freed %dMB. Available = %dMB%n",
                process.pid, process.memoryRequired, availableMemory);
            memoryLock.notifyAll(); //wake Thread 2
        }
    }

    public int getAvailableMemory() {
    synchronized (memoryLock) {
        return availableMemory;
    }
}

}
