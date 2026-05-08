/**
 * GanttEntry.java - Represents one execution segment in the Gantt chart.
 *
 * The project requires showing which process was selected, when it ran,
 * and the starting/stopping burst values.
 *
 * CSC 227 - Operating Systems Project
 */
public class GanttEntry {

    int pid;
    int startTime;
    int endTime;
    int startBurst;
    int endBurst;

    public GanttEntry(int pid, int startTime, int endTime, int startBurst, int endBurst) {
        this.pid = pid;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startBurst = startBurst;
        this.endBurst = endBurst;
    }
}
