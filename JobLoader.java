import java.io.*;
import java.util.concurrent.BlockingQueue;

/**
 * JobLoader.java - Thread 1
 *
 * Reads process information from job.txt, creates PCB objects,
 * and inserts them into the job queue. It terminates after all jobs
 * are inserted.
 *
 * File format:
 * pid:burstTime:priority;memoryRequired
 * Example:
 * 1:25:4;500
 *
 * CSC 227 - Operating Systems Project
 */
public class JobLoader implements Runnable {

    private final BlockingQueue<PCB> jobQueue;
    private final String filename;

    public JobLoader(BlockingQueue<PCB> jobQueue, String filename) {
        this.jobQueue = jobQueue;
        this.filename = filename;
    }

    @Override
    public void run() {
        System.out.println("[Thread 1] Started: Reading jobs from " + filename);

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int arrivalOrder = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    String[] parts = line.split(";");
                    if (parts.length != 2) {
                        System.err.println("[Thread 1] Skipping invalid line: " + line);
                        continue;
                    }

                    String[] processInfo = parts[0].split(":");
                    if (processInfo.length != 3) {
                        System.err.println("[Thread 1] Skipping invalid line: " + line);
                        continue;
                    }

                    int pid = Integer.parseInt(processInfo[0].trim());
                    int burstTime = Integer.parseInt(processInfo[1].trim());
                    int priority = Integer.parseInt(processInfo[2].trim());
                    int memoryRequired = Integer.parseInt(parts[1].trim());

                    if (priority < 1 || priority > 30 || burstTime <= 0 || memoryRequired <= 0) {
                        System.err.println("[Thread 1] Skipping invalid values: " + line);
                        continue;
                    }

                    PCB process = new PCB(pid, burstTime, priority, memoryRequired, arrivalOrder);
                    jobQueue.put(process);
                    System.out.println("[Thread 1] Added to Job Queue: " + process);
                    arrivalOrder++;

                } catch (NumberFormatException e) {
                    System.err.println("[Thread 1] Skipping invalid numeric line: " + line);
                }
            }

        } catch (FileNotFoundException e) {
            System.err.println("[Thread 1] ERROR: " + filename + " was not found.");
        } catch (IOException e) {
            System.err.println("[Thread 1] ERROR while reading file: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[Thread 1] Interrupted while adding jobs.");
        }

        try {
            jobQueue.put(new PCB(-1, 0, 1, 0, -1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("[Thread 1] Finished: All jobs were inserted into the Job Queue.");
    }
}
