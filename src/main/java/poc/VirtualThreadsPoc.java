package poc;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class VirtualThreadsPoc {

    public static void main(String[] args) throws Exception {
        int tasks = argInt(args, 0, 20_000);
        int sleepMs = argInt(args, 1, 50);

        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("Tasks: " + tasks + ", per-task sleep: " + sleepMs + "ms");
        System.out.println();

        run("platform threads", tasks, sleepMs, false);
        System.out.println();
        run("virtual threads", tasks, sleepMs, true);

        System.out.println();
        System.out.println("Tip: try higher task counts, e.g. 100000 (virtual should still work fine on most machines).");
    }

    private static void run(String label, int tasks, int sleepMs, boolean virtual) throws InterruptedException {
        Instant start = Instant.now();

        CountDownLatch latch = new CountDownLatch(tasks);
        List<Thread> threads = new ArrayList<>(Math.min(tasks, 100_000));

        for (int i = 0; i < tasks; i++) {
            Runnable r = () -> {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            };

            Thread t = virtual ? Thread.ofVirtual().unstarted(r) : Thread.ofPlatform().unstarted(r);
            threads.add(t);
            t.start();
        }

        latch.await();
        Duration d = Duration.between(start, Instant.now());

        long usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);

        System.out.println(label + " -> elapsed: " + d.toMillis() + "ms, approx used heap: " + usedMb + "MB");

        // Also show an idiomatic executor approach for virtual threads.
        if (virtual) {
            Instant start2 = Instant.now();
            try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
                CountDownLatch latch2 = new CountDownLatch(tasks);
                for (int i = 0; i < tasks; i++) {
                    exec.submit(() -> {
                        try {
                            Thread.sleep(sleepMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            latch2.countDown();
                        }
                    });
                }
                latch2.await();
            }
            Duration d2 = Duration.between(start2, Instant.now());
            System.out.println(label + " (executor) -> elapsed: " + d2.toMillis() + "ms");
        }
    }

    private static int argInt(String[] args, int idx, int def) {
        if (args == null || args.length <= idx) return def;
        try {
            return Integer.parseInt(args[idx]);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
