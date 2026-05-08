/**
 * PCB.java - Process Control Block
 *
 * This class stores all information needed to represent a process
 * in the CPU scheduling simulator.
 *
 * CSC 227 - Operating Systems Project
 */
public class PCB {

    int pid;
    String state;

    int burstTime;
    int remainingBurst;

    int priority;
    int originalPriority;

    int memoryRequired;
    int arrivalOrder;
    int arrivalTime;

    int startTime;
    int terminationTime;
    int waitingTime;
    int turnaroundTime;

    boolean started;
    boolean starved;

    int waitingInReady;
    int agingCounter;

    public PCB(int pid, int burstTime, int priority, int memoryRequired, int arrivalOrder) {
        this.pid = pid;
        this.burstTime = burstTime;
        this.remainingBurst = burstTime;
        this.priority = priority;
        this.originalPriority = priority;
        this.memoryRequired = memoryRequired;
        this.arrivalOrder = arrivalOrder;
        this.arrivalTime = 0;

        this.state = "new";
        this.startTime = -1;
        this.terminationTime = -1;
        this.waitingTime = 0;
        this.turnaroundTime = 0;
        this.started = false;
        this.starved = false;
        this.waitingInReady = 0;
        this.agingCounter = 0;
    }

    @Override
    public String toString() {
        return String.format("P%d [burst=%d, remaining=%d, priority=%d, memory=%dMB, state=%s]",
                pid, burstTime, remainingBurst, priority, memoryRequired, state);
    }
}
