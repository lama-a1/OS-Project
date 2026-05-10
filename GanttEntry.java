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
