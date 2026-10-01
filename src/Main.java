import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Демонстраційне навантаження JVM для дослідження у VisualVM.
 */
public final class Main {
    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static final List<byte[]> retainedBlocks = new ArrayList<>();

    private Main() {
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("VisualVM demo is running. PID: " + ProcessHandle.current().pid());
        System.out.println("Stop with Ctrl+C. Use VisualVM Monitor, Threads and Sampler tabs.");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running.set(false);
            System.out.println("Stopping demo...");
        }, "shutdown-hook"));

        Thread cpuWorker = new Thread(Main::runCpuWork, "cpu-worker");
        Thread allocationWorker = new Thread(Main::runAllocationWork, "allocation-worker");
        Thread reportingWorker = new Thread(Main::printStatus, "status-reporter");
        Thread waitingWorker = new Thread(Main::waitForSignal, "waiting-worker");

        // Запускаємо потоки з різними типами навантаження для спостереження у VisualVM.
        cpuWorker.start();
        allocationWorker.start();
        reportingWorker.start();
        waitingWorker.start();

        while (running.get()) {
            Thread.sleep(1_000);
        }

        cpuWorker.join();
        allocationWorker.join();
        reportingWorker.join();
        waitingWorker.interrupt();
        waitingWorker.join();
    }

    private static void runCpuWork() {
        double value = 0.5;
        while (running.get()) {
            // Обчислення створюють стабільне CPU-навантаження для вкладки Sampler -> CPU.
            for (int i = 1; i <= 250_000; i++) {
                value = Math.sin(value + i) * Math.cos(value - i);
            }
            if (value == Double.POSITIVE_INFINITY) {
                System.out.println("Unexpected value: " + value);
            }
        }
    }

    private static void runAllocationWork() {
        int cycle = 0;
        while (running.get()) {
            // Тимчасові масиви швидко створюються та звільняються, імітуючи активність GC.
            List<byte[]> temporaryBlocks = new ArrayList<>();
            for (int i = 0; i < 2_000; i++) {
                temporaryBlocks.add(new byte[4_096]);
            }

            if (cycle++ % 10 == 0) {
                synchronized (retainedBlocks) {
                    // Обмежений список показує утримувану пам'ять без нескінченного витоку.
                    retainedBlocks.add(new byte[256 * 1_024]);
                    if (retainedBlocks.size() > 20) {
                        retainedBlocks.remove(0);
                    }
                }
            }

            temporaryBlocks.clear();
            sleepQuietly(100);
        }
    }

    private static void printStatus() {
        Runtime runtime = Runtime.getRuntime();
        while (running.get()) {
            long usedHeap = runtime.totalMemory() - runtime.freeMemory();
            System.out.printf("Heap used: %.1f MB, retained blocks: %d%n",
                    usedHeap / 1024.0 / 1024.0, retainedBlocks.size());
            sleepQuietly(5_000);
        }
    }

    private static void waitForSignal() {
        while (running.get()) {
            try {
                Thread.sleep(60_000);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static void sleepQuietly(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            running.set(false);
        }
    }
}
