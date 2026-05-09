import java.io.*;
import java.util.Scanner;

/**
 * Main.java - Program Entry Point
 *
 * Creates SharedQueues, starts Thread 1 and Thread 2,
 * then runs the chosen scheduling algorithm on the Main Thread.
 *
 * CSC 227 - Operating Systems Project
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("============================================================");
        System.out.println("CSC 227: Multithreaded CPU Scheduling Simulator");
        System.out.println("============================================================\n");

        // Count processes before starting
        int totalProcesses = countProcesses("job.txt");
        if (totalProcesses == 0) {
            System.err.println("ERROR: No valid processes found in job.txt.");
            return;
        }

        // Get algorithm choice from user
        Scanner scanner = new Scanner(System.in);
        int choice = readChoice(scanner);

        // ── Shared objects ────────────────────────────────────────────────
        SharedQueues queues     = new SharedQueues();   // replaces BlockingQueue
        Object       memoryLock = new Object();         // for memory synchronization

        // ── Create and start threads ──────────────────────────────────────
        MemoryManager memoryManager = new MemoryManager(queues, memoryLock);
        Thread memoryThread = new Thread(memoryManager, "Thread-2-MemoryManager");
        Thread loaderThread = new Thread(new JobLoader(queues, "job.txt"), "Thread-1-JobLoader");

        memoryThread.start();
        loaderThread.start();

        // ── Main Thread runs the scheduler ───────────────────────────────
        Scheduler scheduler = new Scheduler(queues, memoryManager, totalProcesses);

        switch (choice) {
            case 1: scheduler.sjf();                break;
            case 2: scheduler.roundRobin();         break;
            case 3: scheduler.priorityScheduling(); break;
        }

        // Wait for all threads to finish cleanly
        loaderThread.join();
        memoryThread.join();

        Thread.sleep(50); // let remaining thread messages flush

        System.out.println("\n[Main] Simulation completed. All threads terminated.");
        scanner.close();
    }

    private static int readChoice(Scanner scanner) {
        int choice = 0;
        while (choice < 1 || choice > 3) {
            System.out.println("Select a CPU Scheduling Algorithm:");
            System.out.println("1. Shortest Job First (SJF)");
            System.out.println("2. Round Robin (RR, q = 5ms)");
            System.out.println("3. Priority Scheduling (Non-Preemptive) with Aging");
            System.out.print("Enter your choice (1-3): ");

            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();
                if (choice < 1 || choice > 3)
                    System.out.println("Please enter 1, 2, or 3.\n");
            } else {
                scanner.next();
                System.out.println("Invalid input. Please enter a number.\n");
            }
        }
        return choice;
    }

    private static int countProcesses(String filename) {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && line.contains(":") && line.contains(";"))
                    count++;
            }
        } catch (IOException e) {
            System.err.println("ERROR: Cannot read " + filename + ": " + e.getMessage());
        }
        return count;
    }
}
