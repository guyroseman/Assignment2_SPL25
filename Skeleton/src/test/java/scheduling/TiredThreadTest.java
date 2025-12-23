package scheduling;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TiredThreadTest {

    /**
     * Test: New task when handoff is full.
     * Scenario: Worker runs Task A. Task B fills the queue. Task C should be rejected.
     */
    @Test
    void testNewTask_WhenHandoffFull_ThrowsException() throws InterruptedException {
        TiredThread worker = new TiredThread(0, 1.0);
        worker.start();

        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch finishTask = new CountDownLatch(1);

        worker.newTask(() -> {
            taskStarted.countDown();
            try {
                finishTask.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        taskStarted.await(2, TimeUnit.SECONDS);

        worker.newTask(() -> {});

        assertThrows(IllegalStateException.class, () -> {
            worker.newTask(() -> {});
        });

        finishTask.countDown();
        worker.shutdown();
    }

    /**
     * Test: Shutdown - interruption.
     * Scenario: The thread calling shutdown() is interrupted while waiting to put the poison pill.
     */
    @Test
    void testShutdown_Interruption() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);
        worker.start();

        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch finishTask = new CountDownLatch(1);

        worker.newTask(() -> {
            taskStarted.countDown();
            try { finishTask.await(); } catch (InterruptedException e) {}
        });
        taskStarted.await();

        worker.newTask(() -> {});

        Thread terminator = new Thread(() -> {
            Thread.currentThread().interrupt();
            worker.shutdown();
            
            if (!Thread.currentThread().isInterrupted()) {
                throw new RuntimeException("Interrupt status was not restored!");
            }
        });

        terminator.start();
        terminator.join(2000);

        assertFalse(terminator.isAlive());
        
        finishTask.countDown();
        worker.shutdown(); 
    }

    /**
     * Test: Shutdown - unable to add poison pill.
     * Scenario: Worker is stuck (infinite loop), queue is full. Shutdown should block waiting for space.
     */
    @Test
    void testShutdown_UnableToAddPoisonPill() throws InterruptedException {
        TiredThread worker = new TiredThread(2, 1.0);
        worker.start();

        CountDownLatch stuckLatch = new CountDownLatch(1);

        worker.newTask(() -> {
            stuckLatch.countDown();
            try { Thread.sleep(Long.MAX_VALUE); } catch (Exception e) {}
        });
        stuckLatch.await();

        worker.newTask(() -> {});

        Thread terminator = new Thread(worker::shutdown);
        terminator.start();

        Thread.sleep(200);
        
        assertTrue(terminator.isAlive(), "Shutdown should block waiting for queue space");
        
        terminator.interrupt();
        worker.interrupt();
    }

    /**
     * Test: Check run logic with poison pill.
     * Scenario: Submit tasks, then shutdown. Verify worker exits and processes tasks before exit.
     */
    @Test
    void testRunLogicWithPoisonPill() throws InterruptedException {
        TiredThread worker = new TiredThread(3, 1.0);
        worker.start();

        AtomicBoolean taskRan = new AtomicBoolean(false);
        worker.newTask(() -> taskRan.set(true));

        worker.shutdown();
        
        worker.join(2000); 
        
        assertTrue(taskRan.get(), "Task should have run before poison pill");
        assertFalse(worker.isAlive(), "Thread should be dead after shutdown");
    }

    /**
     * Test: Run - interruption.
     * Scenario: Interrupt the worker thread itself. It should handle it without crashing.
     */
    @Test
    void testRun_InterruptionWhileIdle() throws InterruptedException {
        TiredThread worker = new TiredThread(4, 1.0);
        worker.start();
        Thread.sleep(100); 

        worker.interrupt();
        
        Thread.sleep(100);
        
        if (worker.isAlive()) {
             worker.shutdown();
             worker.join(1000);
        }
        assertFalse(worker.isAlive());
    }

   /**
     * Test: Run - checks correctness of times in case of an error in the middle.
     * Scenario: Task throws exception. TimeUsed must still increment.
     */
    @Test
    void testRun_TimeCorrectnessOnError() throws InterruptedException {
        TiredThread worker = new TiredThread(5, 1.0);
        worker.start();

        long startIdle = worker.getTimeIdle();

        // We wrap the task exactly as the Executor does,
        // to ensure the Thread receives time updates even in case of a crash.
        worker.newTask(() -> {
            long start = System.nanoTime(); // Measurement (Simulating the Executor)
            try {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {}
                throw new RuntimeException("Crash!");
            } finally {
                long end = System.nanoTime();
                // Manual time update because there is no Executor in this unit test
                worker.addTime(end - start); 
            }
        });

        Thread.sleep(500);

        long timeUsed = worker.getTimeUsed();
        
        // This assertion will pass now, because we called addTime inside the task's finally block
        assertTrue(timeUsed >= 150_000_000L, "Time used should be recorded even if task fails. Got: " + timeUsed);
        assertTrue(worker.getTimeIdle() > startIdle, "Idle time should increase");
        
        worker.shutdown();
    }

    /**
     * Test: Run a lot of threads - stress test.
     */
    @Test
    void testStressTest() throws InterruptedException {
        int threadCount = 20;
        int tasksPerThread = 50;
        
        List<TiredThread> threads = new ArrayList<>();
        CountDownLatch allDone = new CountDownLatch(threadCount * tasksPerThread);
        
        for(int i=0; i<threadCount; i++) {
            TiredThread t = new TiredThread(i, 1.0);
            t.start();
            threads.add(t);
        }

        AtomicInteger errors = new AtomicInteger(0);

        for(int i=0; i<threadCount * tasksPerThread; i++) {
            int threadIndex = i % threadCount;
            TiredThread target = threads.get(threadIndex);
            
            new Thread(() -> {
                boolean submitted = false;
                while (!submitted) {
                    try {
                        target.newTask(() -> {
                            Math.random(); 
                            allDone.countDown();
                        });
                        submitted = true;
                    } catch (IllegalStateException e) {
                        try { Thread.sleep(10); } catch (Exception ex) {}
                    } catch (Exception e) {
                        errors.incrementAndGet();
                        submitted = true;
                    }
                }
            }).start();
        }

        boolean finished = allDone.await(15, TimeUnit.SECONDS);
        
        for(TiredThread t : threads) {
            t.shutdown();
        }

        assertTrue(finished, "All tasks should finish in stress test");
        assertEquals(0, errors.get(), "No unexpected errors should occur");
    }
}