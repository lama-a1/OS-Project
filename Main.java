import java.io.*;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Main.java - Program Entry Point
 *
 * Creates the shared queues, starts Thread 1 and Thread 2,
 * and runs the selected CPU scheduling algorithm on the main thread.
 *
 * CSC 227 - Operating Systems Project
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("============================================================");
        System.out.println("CSC 227: Multithreaded CPU Scheduling Simulator");
        System.out.println("============================================================\n");

        int totalProcesses = countProcesses("job.txt");
        if (totalProcesses == 0) {
            System.err.println("ERROR: No valid processes found in job.txt.");
            return;
        }

        Scanner scanner = new Scanner(System.in);
        int choice = readChoice(scanner);

        BlockingQueue<PCB> jobQueue = new LinkedBlockingQueue<>();
        BlockingQueue<PCB> readyQueue = new LinkedBlockingQueue<>();
        Object memoryLock = new Object();

        MemoryManager memoryManager = new MemoryManager(jobQueue, readyQueue, memoryLock);
        Thread memoryThread = new Thread(memoryManager, "Thread-2-MemoryManager");
        Thread loaderThread = new Thread(new JobLoader(jobQueue, "job.txt"), "Thread-1-JobLoader");

        memoryThread.start();
        loaderThread.start();

        Scheduler scheduler = new Scheduler(readyQueue, memoryManager, totalProcesses);

        switch (choice) {
            case 1: scheduler.sjf(); break;
            case 2: scheduler.roundRobin(); break;
            case 3: scheduler.priorityScheduling(); break;
            default: System.out.println("Invalid option.");
        }

        // Wait for both threads to fully finish BEFORE printing anything else.
        // This prevents Thread 2's "Finished" message from appearing mid-output.
        loaderThread.join();
        memoryThread.join();

        // Small pause ensures all thread messages are flushed before final line
        Thread.sleep(50);

        System.out.println("\n[Main] Simulation completed. All threads terminated.");
        /*memoryThread.start();
        loaderThread.start();

        Scheduler scheduler = new Scheduler(readyQueue, memoryManager, totalProcesses);

        switch (choice) {
            case 1:
                scheduler.sjf();
                break;
            case 2:
                scheduler.roundRobin();
                break;
            case 3:
                scheduler.priorityScheduling();
                break;
            default:
                System.out.println("Invalid option.");
        }

        loaderThread.join();
        memoryThread.join();

        System.out.println("\n[Main] Simulation completed. All threads terminated.");*/
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
                if (choice < 1 || choice > 3) {
                    System.out.println("Please enter 1, 2, or 3.\n");
                }
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
                if (!line.isEmpty() && line.contains(":") && line.contains(";")) {
                    count++;
                }
            }
        } catch (IOException e) {
            System.err.println("ERROR: Cannot read " + filename + ": " + e.getMessage());
        }

        return count;
    }
}
