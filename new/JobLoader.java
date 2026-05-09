import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

public class JobLoader implements Runnable {

    private LinkedList<PCB> jobQueue;
    private String fileName;

    // لمعرفة أن القراءة انتهت
    boolean finished = false;

    public JobLoader(LinkedList<PCB> jobQueue, String fileName) {
        this.jobQueue = jobQueue;
        this.fileName = fileName;
    }

    @Override
    public void run() {
        System.out.println("[Thread 1] Started: Reading jobs from " + fileName);

        int arrivalOrder = 0;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // تجاهل السطور الفارغة
                if (line.isEmpty()) {
                    continue;
                }

                // Format: pid:burst:priority;memory
                String[] parts = line.split(";");
                String[] info = parts[0].split(":");

                int pid = Integer.parseInt(info[0]);
                int burstTime = Integer.parseInt(info[1]);
                int priority = Integer.parseInt(info[2]);
                int memoryRequired = Integer.parseInt(parts[1]);

                PCB process = new PCB(
                        pid,
                        burstTime,
                        priority,
                        memoryRequired,
                        arrivalOrder
                );

                arrivalOrder++;

                // إضافة العملية إلى Job Queue
                synchronized (jobQueue) {
                    jobQueue.add(process);
                    System.out.println("[Thread 1] Added to Job Queue: " + process);

                    // إشعار Thread 2 بوجود عملية جديدة
                    jobQueue.notifyAll();
                }
            }

            reader.close();

        } catch (IOException e) {
            System.out.println("[Thread 1] Error reading file: " + e.getMessage());
        }

        // انتهت القراءة
        finished = true;

        // إشعار Thread 2 بأن القراءة انتهت
        synchronized (jobQueue) {
            jobQueue.notifyAll();
        }

        System.out.println("[Thread 1] Finished: All jobs were inserted into the Job Queue.");
    }
}