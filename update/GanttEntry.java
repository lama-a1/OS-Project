/**
 * GanttEntry.java - One segment in the Gantt chart.
 * CSC 227 - Operating Systems Project
 */
public class GanttEntry {

    int pid;
    int startTime;
    int endTime;
    int startBurst;
    int endBurst;

    public GanttEntry(int pid, int startTime, int endTime,
                      int startBurst, int endBurst) {
        this.pid        = pid;
        this.startTime  = startTime;
        this.endTime    = endTime;
        this.startBurst = startBurst;
        this.endBurst   = endBurst;
    }
}
