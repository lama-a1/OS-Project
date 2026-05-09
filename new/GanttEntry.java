public class GanttEntry {

    int pid;          // Process ID
    int startTime;    // Time when execution started
    int endTime;      // Time when execution ended

    int startBurst;   // Remaining burst before execution
    int endBurst;     // Remaining burst after execution

    public GanttEntry(int pid,
                      int startTime,
                      int endTime,
                      int startBurst,
                      int endBurst) {

        this.pid = pid;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startBurst = startBurst;
        this.endBurst = endBurst;
    }
}