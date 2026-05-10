import java.io.*;
//Thread 1

public class JobLoader implements Runnable {

    private final SharedQueues queues;
    private final String filename;

    public JobLoader(SharedQueues queues, String filename) {
        this.queues   = queues;
        this.filename = filename;
    }

    @Override
    public void run() {
        System.out.println("[Thread 1] Started: Reading jobs from " + filename);

        int arrivalOrder = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {

            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) continue;

                try {
                    // Format: pid:burst:priority;memory
                    String[] parts = line.split(";");
                    String[] info  = parts[0].split(":");

                    int pid            = Integer.parseInt(info[0].trim());
                    int burstTime      = Integer.parseInt(info[1].trim());
                    int priority       = Integer.parseInt(info[2].trim());
                    int memoryRequired = Integer.parseInt(parts[1].trim());

                    //Validate values
                    if (priority < 1 || priority > 30 || burstTime <= 0 || memoryRequired <= 0) {
                        System.err.println("[Thread 1] Skipping invalid values: " + line);
                        continue;
                    }

                    PCB process = new PCB(pid, burstTime, priority, memoryRequired, arrivalOrder);

                    //Add to job queue and wake Thread 2 if it's waiting
                    queues.addToJobQueue(process);
                    System.out.println("[Thread 1] Added to Job Queue: " + process);

                    arrivalOrder++;

                } catch (Exception e) {
                    System.err.println("[Thread 1] Skipping bad line: " + line);
                }
            }

        } catch (FileNotFoundException e) {
            System.err.println("[Thread 1] ERROR: File not found: " + filename);
        } catch (IOException e) {
            System.err.println("[Thread 1] ERROR reading file: " + e.getMessage());
        }

        //Signal that we are done
        queues.markJobLoadingDone();

        System.out.println("[Thread 1] Finished: All jobs inserted into Job Queue.");
    }
}
