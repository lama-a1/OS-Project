import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("============================================================");
        System.out.println("CSC 227: Multithreaded CPU Scheduling Simulator");
        System.out.println("============================================================");

        // حساب عدد العمليات في الملف
        int totalProcesses = countProcesses("src/job.txt");

        if (totalProcesses == 0) {
            System.out.println("No valid processes found in job.txt.");
            return;
        }

        // اختيار الخوارزمية
        Scanner scanner = new Scanner(System.in);
        int choice = readChoice(scanner);

        // الطوابير المشتركة
        LinkedList<PCB> jobQueue = new LinkedList<>();
        LinkedList<PCB> readyQueue = new LinkedList<>();

        // إنشاء Thread 1
        JobLoader jobLoader = new JobLoader(jobQueue, "src/job.txt");
        Thread loaderThread = new Thread(jobLoader);

        // إنشاء Thread 2
        MemoryManager memoryManager =
                new MemoryManager(jobQueue, readyQueue, jobLoader);
        Thread memoryThread = new Thread(memoryManager);

        // تشغيل الـThreads
        memoryThread.start();
        loaderThread.start();

        // إنشاء Scheduler (Main Thread)
        Scheduler scheduler =
                new Scheduler(readyQueue, memoryManager, totalProcesses);

        // تنفيذ الخوارزمية المختارة
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
        }

        // الانتظار حتى تنتهي الـThreads
        loaderThread.join();
        memoryThread.join();

        System.out.println("\n[Main] Simulation completed. All threads terminated.");

        scanner.close();
    }

    // =========================================================
    // قراءة اختيار المستخدم
    // =========================================================
    private static int readChoice(Scanner scanner) {
        int choice = 0;

        while (choice < 1 || choice > 3) {
            System.out.println();
            System.out.println("Select a CPU Scheduling Algorithm:");
            System.out.println("1. Shortest Job First (SJF)");
            System.out.println("2. Round Robin (RR, q = 5ms)");
            System.out.println("3. Priority Scheduling (Non-Preemptive) with Aging");
            System.out.print("Enter your choice (1-3): ");

            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();

                if (choice < 1 || choice > 3) {
                    System.out.println("Please enter 1, 2, or 3.");
                }
            } else {
                scanner.next(); // تجاهل الإدخال غير الصحيح
                System.out.println("Invalid input. Please enter a number.");
            }
        }

        return choice;
    }

    // =========================================================
    // حساب عدد العمليات الموجودة في الملف
    // =========================================================
    private static int countProcesses(String fileName) {
        int count = 0;

        try {
            BufferedReader reader =
                    new BufferedReader(new FileReader(fileName));

            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (!line.isEmpty()
                        && line.contains(":")
                        && line.contains(";")) {
                    count++;
                }
            }

            reader.close();

        } catch (IOException e) {
            System.out.println("Error reading " + fileName + ": "
                    + e.getMessage());
        }

        return count;
    }
}