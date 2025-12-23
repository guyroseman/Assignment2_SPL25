package scheduling;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public TiredExecutor(int numThreads) {
        if (numThreads<1) {
            throw new IllegalArgumentException("numThreads must be at least 1");
        }
        workers = new TiredThread[numThreads];
        // iterate to create and start workers
        for (int i = 0; i < numThreads; i++) {
            // Fatigue factor between 0.5 and 1.5
            workers[i] = new TiredThread(i, 0.5 + Math.random() * 1.0); 
            workers[i].start();
            idleMinHeap.add(workers[i]);
        }
    }

    public void submit(Runnable task) {
        // try to get the least fatigued idle worker
        try {
            TiredThread worker = idleMinHeap.take();
            inFlight.incrementAndGet();
            // assign task to the worker
            worker.newTask(() -> {
                //Start measuring time
                long start = System.nanoTime();
                try {
                    task.run();
                } finally {
                    // Stop measuring time
                    long end = System.nanoTime();
                    // Update the worker's fatigue before returning to the queue
                    worker.addTime(end - start);
                    // after task completion, put the worker back to idle heap
                    idleMinHeap.add(worker);
                    // decrease inFlight counter and notify if all tasks are done
                    if(inFlight.decrementAndGet() == 0){
                        synchronized(this){
                            this.notifyAll();
                        }
                    }
                }
            });
        } catch (InterruptedException e) {
            // interrupt current thread if unable to get a worker
            Thread.currentThread().interrupt();
        }
    }

    public void submitAll(Iterable<Runnable> tasks) {
        
        if (tasks == null) {
            return;
        }
        for (Runnable task : tasks) {
            submit(task);
        }

        // wait until all tasks are done
        synchronized(this){
            while (inFlight.get() > 0) {
                try {
                    this.wait(); // wait to avoid busy waiting
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void shutdown() throws InterruptedException {

        if (workers == null) {
        return;
        }
        
        // request all workers to shutdown
        for (TiredThread worker : workers) {
            worker.shutdown();
        }

        // wait for all workers to finish
        for (TiredThread worker : workers) {
            worker.join();
        }

        // reset inFlight counter and idle heap
        inFlight.set(0);
    }

    public synchronized String getWorkerReport() {
        StringBuilder report = new StringBuilder();
        for (TiredThread worker : workers) {
            report.append("Worker ").append(worker.getWorkerId())
                  .append(": Fatigue=").append(worker.getFatigue())
                  .append(", TimeUsed=").append(worker.getTimeUsed())
                  .append(", TimeIdle=").append(worker.getTimeIdle())
                  .append("\n");
        }
        return report.toString();
    }
}
