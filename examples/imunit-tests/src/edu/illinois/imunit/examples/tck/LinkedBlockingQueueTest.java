package edu.illinois.imunit.examples.tck;
import org.junit.runner.RunWith;
import edu.illinois.imunit.IMUnitRunner;
import static edu.illinois.imunit.IMUnit.fireEvent;
/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 */

import org.junit.Test;
import java.util.*;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import java.io.*;
import edu.illinois.imunit.Schedule;
import edu.illinois.imunit.Schedules;
import edu.illinois.imunit.examples.tck.JSR166TestCase.CheckedRunnable;
import edu.illinois.imunit.examples.tck.JSR166TestCase.ThreadShouldThrow;


@RunWith(IMUnitRunner.class)
public class LinkedBlockingQueueTest extends JSR166TestCase {

    public static class Unbounded extends BlockingQueueTest {
        protected BlockingQueue emptyCollection() {
            return new LinkedBlockingQueue();
        }
    }

    public static class Bounded extends BlockingQueueTest {
        protected BlockingQueue emptyCollection() {
            return new LinkedBlockingQueue(20);
        }
    }

//    public static void main(String[] args) {
//        junit.textui.TestRunner.run(suite());
//    }
//
//    public static Test suite() {
//        return newTestSuite(LinkedBlockingQueueTest.class,
//                            new Unbounded().testSuite(),
//                            new Bounded().testSuite());
//    }


    /**
     * Create a queue of given size containing consecutive
     * Integers 0 ... n.
     */
    private LinkedBlockingQueue populatedQueue(int n) {
        LinkedBlockingQueue q = new LinkedBlockingQueue(n);
        assertTrue(q.isEmpty());
        for (int i = 0; i < n; i++)
            assertTrue(q.offer(new Integer(i)));
        assertFalse(q.isEmpty());
        assertEquals(0, q.remainingCapacity());
        assertEquals(n, q.size());
        return q;
    }

    /**
     * put blocks interruptibly if full
     */
    @Test
    @Schedule(name = "blockingPut", value = "[beforePut:afterPut]@putThread->beforeInterrupt@main")
    public void testBlockingPut() throws InterruptedException {
        final LinkedBlockingQueue q = new LinkedBlockingQueue(SIZE);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; ++i)
                    q.put(i);
                assertEquals("blockingPut", SIZE, q.size());
                assertEquals("blockingPut", 0, q.remainingCapacity());
                try {
                    fireEvent("beforePut");
                    q.put(99);
                    fireEvent("afterPut");
                    shouldThrow();
                } catch (InterruptedException success) {}
            }}, "putThread");
        t.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeInterrupt");
        t.interrupt();
        t.join();
        assertEquals("blockingPut", SIZE, q.size());
        assertEquals("blockingPut", 0, q.remainingCapacity());
    }

    /**
     * put blocks waiting for take when full
     */
    @Test
    @Schedule(name = "putWithTake", value = "[beforePut:afterPut]@putThread->beforeTake@main"
      + ", [beforeSecondPut:afterSecondPut]@putThread->beforeInterrupt@main")
    public void testPutWithTake() throws InterruptedException {
        final int capacity = 2;
        final LinkedBlockingQueue q = new LinkedBlockingQueue(capacity);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                fireEvent("beforePut");
                for (int i = 0; i < capacity + 1; i++)
                    q.put(i);
                fireEvent("afterPut");
                try {
                    fireEvent("beforeSecondPut");
                    q.put(99);
                    fireEvent("afterSecondPut");
                    shouldThrow();
                } catch (InterruptedException success) {}
            }}, "putThread");

        t.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeTake");
        assertEquals("putWithTake",q.remainingCapacity(), 0);
        assertEquals("putWithTake",0, q.take());
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeInterrupt");
        t.interrupt();
        t.join();
        assertEquals("putWithTake",q.remainingCapacity(), 0);
    }

    /**
     * timed offer times out if full and elements not taken
     */
    public void testTimedOffer() throws InterruptedException {
        final LinkedBlockingQueue q = new LinkedBlockingQueue(2);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                q.put(new Object());
                q.put(new Object());
                assertFalse(q.offer(new Object(), SHORT_DELAY_MS, MILLISECONDS));
                try {
                    q.offer(new Object(), LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
            }});

        t.start();
        Thread.sleep(SMALL_DELAY_MS);
        t.interrupt();
        t.join();
    }

    /**
     * take blocks interruptibly when empty
     */
    @Test
    @Schedule(name = "takeFromEmpty", value = "[beforeTake:afterTake]@takeThread->beforeInterrupt@main")
    public void testTakeFromEmpty() throws InterruptedException {
        final LinkedBlockingQueue q = new LinkedBlockingQueue(2);
        Thread t = new ThreadShouldThrow(InterruptedException.class) {
            public void realRun() throws InterruptedException {
                fireEvent("beforeTake");              
                q.take();
                fireEvent("afterTake");
            }};
        t.setName("takeThread");    
        t.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeInterrupt");
        t.interrupt();
        t.join();
    }

    /**
     * Take removes existing elements until empty, then blocks interruptibly
     */
    @Test
    @Schedule(name = "blockingTake", value = "[beforeTake:afterTake]@takeThread->beforeInterrupt@main")
    public void testBlockingTake() throws InterruptedException {
        final LinkedBlockingQueue q = populatedQueue(SIZE);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; ++i) {
                    assertEquals("blockingTake", i, q.take());
                }
                try {
                    fireEvent("beforeTake");              
                    q.take();
                    fireEvent("afterTake");
                    shouldThrow();
                } catch (InterruptedException success) {}
            }}, "takeThread");

        t.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeInterrupt");
        t.interrupt();
        t.join();
    }


    /**
     * Interrupted timed poll throws InterruptedException instead of
     * returning timeout status
     */
    public void testInterruptedTimedPoll() throws InterruptedException {
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                LinkedBlockingQueue q = populatedQueue(SIZE);
                for (int i = 0; i < SIZE; ++i) {
                    assertEquals(i, q.poll(SHORT_DELAY_MS, MILLISECONDS));
                }
                try {
                    q.poll(SMALL_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
            }});

        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        t.join();
    }

    /**
     * offer transfers elements across Executor tasks
     */
    public void testOfferInExecutor() {
        final LinkedBlockingQueue q = new LinkedBlockingQueue(2);
        q.add(one);
        q.add(two);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.execute(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                assertFalse(q.offer(three));
                assertTrue(q.offer(three, MEDIUM_DELAY_MS, MILLISECONDS));
                assertEquals(0, q.remainingCapacity());
            }});

        executor.execute(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                Thread.sleep(SMALL_DELAY_MS);
                assertSame(one, q.take());
            }});

        joinPool(executor);
    }

    /**
     * poll retrieves elements across Executor threads
     */
    public void testPollInExecutor() {
        final LinkedBlockingQueue q = new LinkedBlockingQueue(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.execute(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                assertNull(q.poll());
                assertSame(one, q.poll(MEDIUM_DELAY_MS, MILLISECONDS));
                assertTrue(q.isEmpty());
            }});

        executor.execute(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                Thread.sleep(SMALL_DELAY_MS);
                q.put(one);
            }});

        joinPool(executor);
    }

}
