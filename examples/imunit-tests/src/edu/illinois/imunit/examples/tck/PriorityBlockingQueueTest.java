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

import edu.illinois.imunit.Schedule;
import java.util.*;
import java.util.concurrent.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import java.io.*;

@RunWith(IMUnitRunner.class)
public class PriorityBlockingQueueTest extends JSR166TestCase {

    public static class Generic extends BlockingQueueTest {
        protected BlockingQueue emptyCollection() {
            return new PriorityBlockingQueue();
        }
    }

    public static class InitialCapacity extends BlockingQueueTest {
        protected BlockingQueue emptyCollection() {
            return new PriorityBlockingQueue(20);
        }
    }

//    public static void main(String[] args) {
//        junit.textui.TestRunner.run(suite());
//    }
//
//    public static Test suite() {
//        return newTestSuite(PriorityBlockingQueueTest.class,
//                            new Generic().testSuite(),
//                            new InitialCapacity().testSuite());
//    }

    private static final int NOCAP = Integer.MAX_VALUE;

    /** Sample Comparator */
    static class MyReverseComparator implements Comparator {
        public int compare(Object x, Object y) {
            return ((Comparable)y).compareTo(x);
        }
    }

    /**
     * Create a queue of given size containing consecutive
     * Integers 0 ... n.
     */
    private PriorityBlockingQueue populatedQueue(int n) {
        PriorityBlockingQueue q = new PriorityBlockingQueue(n);
        assertTrue(q.isEmpty());
        for (int i = n-1; i >= 0; i-=2)
            assertTrue(q.offer(new Integer(i)));
        for (int i = (n & 1); i < n; i+=2)
            assertTrue(q.offer(new Integer(i)));
        assertFalse(q.isEmpty());
        assertEquals(NOCAP, q.remainingCapacity());
        assertEquals(n, q.size());
        return q;
    }

    /**
     * put doesn't block waiting for take
     */
    @Test
    @Schedule(name = "putWithTake", value = "afterPut@putThread->beforeTake@main")
    public void testPutWithTake() throws InterruptedException {
        final PriorityBlockingQueue q = new PriorityBlockingQueue(2);
        final int size = 4;
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() {
                for (int i = 0; i < size; i++)
                    q.put(new Integer(0));
                fireEvent("afterPut");
            }}, "putThread");

        t.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeTake");
        assertEquals("putWithTake", q.size(), size);
        q.take();
        t.interrupt();
        t.join();
    }

    /**
     * timed offer does not time out
     */
    public void testTimedOffer() throws InterruptedException {
        final PriorityBlockingQueue q = new PriorityBlockingQueue(2);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() {
                q.put(new Integer(0));
                q.put(new Integer(0));
                assertTrue(q.offer(new Integer(0), SHORT_DELAY_MS, MILLISECONDS));
                assertTrue(q.offer(new Integer(0), LONG_DELAY_MS, MILLISECONDS));
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
        final PriorityBlockingQueue q = new PriorityBlockingQueue(2);
        Thread t = new Thread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                fireEvent("beforeTake");              
                q.take();
                fireEvent("afterTake");
            }});
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
        final PriorityBlockingQueue q = populatedQueue(SIZE);
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
     * timed pool with zero timeout succeeds when non-empty, else times out
     */
    public void testTimedPoll0() throws InterruptedException {
        PriorityBlockingQueue q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.poll(0, MILLISECONDS));
        }
        assertNull(q.poll(0, MILLISECONDS));
    }

    /**
     * timed pool with nonzero timeout succeeds when non-empty, else times out
     */
    public void testTimedPoll() throws InterruptedException {
        PriorityBlockingQueue q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.poll(SHORT_DELAY_MS, MILLISECONDS));
        }
        assertNull(q.poll(SHORT_DELAY_MS, MILLISECONDS));
    }

    /**
     * Interrupted timed poll throws InterruptedException instead of
     * returning timeout status
     */
    public void testInterruptedTimedPoll() throws InterruptedException {
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                PriorityBlockingQueue q = populatedQueue(SIZE);
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
    public void testPollInExecutor() {
        final PriorityBlockingQueue q = new PriorityBlockingQueue(2);
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
