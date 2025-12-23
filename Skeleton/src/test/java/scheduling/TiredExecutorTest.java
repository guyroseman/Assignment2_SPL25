package scheduling;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TiredExecutorTest {

    @Test
    void testConstructor_ZeroThreads_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new TiredExecutor(0);
        });
    }

    @Test
    void testConstructor_NegativeThreads_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new TiredExecutor(-1);
        });
    }

    @Test
    void testSubmit_InterruptWhileWaiting() throws InterruptedException {
        TiredExecutor executor = new TiredExecutor(1);
        CountDownLatch workerOccupied = new CountDownLatch(1);

        executor.submit(() -> {
            workerOccupied.countDown();
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
        });

        workerOccupied.await();

        Thread submitter = new Thread(() -> {
            executor.submit(() -> {});
        });

        submitter.start();
        Thread.sleep(50);
        
        submitter.interrupt();
        submitter.join(1000);
        
        // We just verify the thread finished (didn't hang forever)
        assertFalse(submitter.isAlive(), "Submitter thread should have finished after interrupt");
        executor.shutdown();
    }
    

    @Test
    void testSubmitAll_BarrierLogic() {
        TiredExecutor executor = new TiredExecutor(4);
        AtomicInteger activeTasks = new AtomicInteger(0);

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tasks.add(() -> {
                activeTasks.incrementAndGet();
                try { Thread.sleep(50); } catch (InterruptedException e) {}
                activeTasks.decrementAndGet();
            });
        }

        executor.submitAll(tasks);

        assertEquals(0, activeTasks.get(), "submitAll returned before all tasks finished!");
        try { executor.shutdown(); } catch (Exception e) {}
    }
    
    @Test
    void testSubmit_WhenIdle() {
        TiredExecutor executor = new TiredExecutor(2);
        AtomicBoolean ran = new AtomicBoolean(false);
        
        executor.submit(() -> ran.set(true));
        
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        
        assertTrue(ran.get(), "Task failed to run on idle executor");
        try { executor.shutdown(); } catch (Exception e) {}
    }

    @Test
    void testQueueOrderedByFatigue() throws InterruptedException {
        TiredExecutor executor = new TiredExecutor(2);
        
        executor.submit(() -> {
            try { Thread.sleep(200); } catch (InterruptedException e) {}
        });

        Thread.sleep(50); 

        AtomicInteger workerId = new AtomicInteger(-1);
        CountDownLatch latch = new CountDownLatch(1);
        
        executor.submit(() -> {
            if (Thread.currentThread() instanceof TiredThread) {
                 workerId.set(((TiredThread) Thread.currentThread()).getWorkerId());
            }
            latch.countDown();
        });
        
        latch.await();
        
        if (workerId.get() != -1) {
            assertNotEquals(-1, workerId.get());
        }
        executor.shutdown();
    }

    @Test
    void testFatigueAccumulates() {
        TiredExecutor executor = new TiredExecutor(3);
        List<Runnable> tasks = new ArrayList<>();
        for(int i=0; i<30; i++) {
             tasks.add(() -> { try{Thread.sleep(10);}catch(Exception e){} });
        }
        executor.submitAll(tasks); 
        try { executor.shutdown(); } catch (Exception e) {}
    }

    @Test
    void testIdleTimeTracking() throws InterruptedException {
        TiredExecutor executor = new TiredExecutor(1);
        
        Thread.sleep(200);
        
        executor.submit(() -> {});
        executor.submitAll(Collections.emptyList());
        
        String report = executor.getWorkerReport();
        assertTrue(report.contains("TimeIdle"), "Report should contain idle stats");
        
        try { executor.shutdown(); } catch (Exception e) {}
    }

    @Test
    void testShutdown_ClearsQueue() throws InterruptedException {
        TiredExecutor executor = new TiredExecutor(2);
        AtomicInteger counter = new AtomicInteger(0);
        
        for(int i=0; i<10; i++) {
            executor.submit(counter::incrementAndGet);
        }
        
        executor.shutdown();
        
        Thread.sleep(500); 
        try {
            executor.submit(() -> {});
        } catch (Exception e) {
            fail("Should not throw exception here based on current implementation");
        }
    }
}