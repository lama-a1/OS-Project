public class PCB {

    // Basic information
    int pid;
    String state;

    int burstTime;
    int remainingBurst;

    int priority;
    int originalPriority;

    int memoryRequired;

    // Arrival information
    int arrivalOrder;
    int arrivalTime;

    // Execution times
    int startTime;
    int terminationTime;
    int waitingTime;
    int turnaroundTime;

    // Helper variables
    boolean started;
    boolean starved;

    int waitingInReady;
    int agingCounter;

    // Constructor
    public PCB(int pid, int burstTime, int priority,
               int memoryRequired, int arrivalOrder) {

        this.pid = pid;
        this.burstTime = burstTime;
        this.remainingBurst = burstTime;

        this.priority = priority;
        this.originalPriority = priority;

        this.memoryRequired = memoryRequired;

        this.arrivalOrder = arrivalOrder;
        this.arrivalTime = 0;   // All processes arrive at time 0

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
        return "P" + pid +
               " [burst=" + burstTime +
               ", remaining=" + remainingBurst +
               ", priority=" + priority +
               ", memory=" + memoryRequired +
               "MB, state=" + state + "]";
    }
}