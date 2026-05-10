import java.util.LinkedList;
//Contains the two shared queues and one shared lock for synchronization.

public class SharedQueues {

    private final LinkedList<PCB> jobQueue   = new LinkedList<>();
    private final LinkedList<PCB> readyQueue = new LinkedList<>();

    private boolean jobLoadingDone = false;

    //Job Queue operations

    public synchronized void addToJobQueue(PCB process) {
        jobQueue.add(process);
        notifyAll();
    }

    public synchronized PCB takeFromJobQueue() throws InterruptedException {
        while (jobQueue.isEmpty() && !jobLoadingDone) {
            wait();
        }

        if (jobQueue.isEmpty()) {
            return null;
        }

        return jobQueue.removeFirst();
    }

    public synchronized void markJobLoadingDone() {
        jobLoadingDone = true;
        notifyAll();
    }

    //Ready Queue operations

    public synchronized void addToReadyQueue(PCB process) {
        readyQueue.add(process);
        notifyAll(); //wake Main Thread
    }

    //timeoutMs: show how long to wait before returning null
    public synchronized PCB pollFromReadyQueue(long timeoutMs)
            throws InterruptedException {

        if (readyQueue.isEmpty()) {
            wait(timeoutMs); //wait for new arrivals
        }

        if (readyQueue.isEmpty()) {
            return null; //still empty after timeout
        }

        return readyQueue.removeFirst();
    }

    public synchronized LinkedList<PCB> drainReadyQueue() {
        LinkedList<PCB> drained = new LinkedList<>(readyQueue);
        readyQueue.clear();
        return drained;
    }

    public synchronized boolean isReadyQueueEmpty() {
        return readyQueue.isEmpty();
    }

    public synchronized boolean isJobLoadingDone() {
        return jobLoadingDone;
    }
}
