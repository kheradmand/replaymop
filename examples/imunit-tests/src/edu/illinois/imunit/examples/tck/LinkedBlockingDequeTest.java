package edu.illinois.imunit.examples.tck;
import org.junit.runner.RunWith;
import edu.illinois.imunit.IMUnitRunner;
import static edu.illinois.imunit.IMUnit.fireEvent;
/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 */

import org.junit.Test;
import java.util.*;
import java.util.concurrent.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import java.io.*;
import edu.illinois.imunit.Schedule;
import edu.illinois.imunit.Schedules;


@RunWith(IMUnitRunner.class)
public class LinkedBlockingDequeTest extends JSR166TestCase {

    public static class Unbounded extends BlockingQueueTest {
        protected BlockingQueue emptyCollection() {
            return new LinkedBlockingDeque();
        }
    }

    public static class Bounded extends BlockingQueueTest {
        protected BlockingQueue emptyCollection() {
            return new LinkedBlockingDeque(20);
        }
    }

//    public static void main(String[] args) {
//        junit.textui.TestRunner.run(suite());
//    }
//
//    public static Test suite() {
//        return newTestSuite(LinkedBlockingDequeTest.class,
//                            new Unbounded().testSuite(),
//                            new Bounded().testSuite());
//    }

    /**
     * Create a deque of given size containing consecutive
     * Integers 0 ... n.
     */
    private LinkedBlockingDeque populatedDeque(int n) {
        LinkedBlockingDeque q = new LinkedBlockingDeque(n);
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
        final LinkedBlockingDeque q = new LinkedBlockingDeque(SIZE);
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
        final LinkedBlockingDeque q = new LinkedBlockingDeque(capacity);
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
        final LinkedBlockingDeque q = new LinkedBlockingDeque(2);
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
        final LinkedBlockingDeque q = new LinkedBlockingDeque(2);
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
        final LinkedBlockingDeque q = populatedDeque(SIZE);
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
                LinkedBlockingDeque q = populatedDeque(SIZE);
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
     * putFirst blocks interruptibly if full
     */
    @Test
    @Schedule(name = "blockingPutFirst", value = "[beforePut:afterPut]@putThread->beforeInterrupt@main")
    public void testBlockingPutFirst() throws InterruptedException {
        final LinkedBlockingDeque q = new LinkedBlockingDeque(SIZE);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; ++i)
                    q.putFirst(i);
                assertEquals("blockingPutFirst", SIZE, q.size());
                assertEquals("blockingPutFirst", 0, q.remainingCapacity());
                try {
                    fireEvent("beforePut");
                    q.putFirst(99);
                    fireEvent("afterPut");
                    shouldThrow();
                } catch (InterruptedException success) {}
            }}, "putThread");

        t.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeInterrupt");
        t.interrupt();
        t.join();
        assertEquals("blockingPutFirst", SIZE, q.size());
        assertEquals("blockingPutFirst", 0, q.remainingCapacity());
    }

    /**
     * putFirst blocks waiting for take when full
     */
    @Test
    @Schedule(name = "putFirstWithTake", value = "[beforePut:afterPut]@putThread->beforeTake@main" + 
        ", [beforeSecondPut:afterSecondPut]@putThread->beforeInterrupt@main" )
    public void testPutFirstWithTake() throws InterruptedException {
        final int capacity = 2;
        final LinkedBlockingDeque q = new LinkedBlockingDeque(capacity);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                fireEvent("beforePut");
                for (int i = 0; i < capacity + 1; i++)
                    q.putFirst(i);
                fireEvent("afterPut");
                try {
                    fireEvent("beforeSecondPut");
                    q.putFirst(99);
                    fireEvent("afterSecondPut"); 
                    shouldThrow();
                } catch (InterruptedException success) {}
            }}, "putThread");

        t.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeTake");
        assertEquals("putFirstWithTake", q.remainingCapacity(), 0);
        assertEquals("putFirstWithTake",capacity - 1, q.take());
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeInterrupt");
        t.interrupt();
        t.join();
        assertEquals("putFirstWithTake", q.remainingCapacity(), 0);
    }

    /**
     * timed offerFirst times out if full and elements not taken
     */
    public void testTimedOfferFirst() throws InterruptedException {
        final LinkedBlockingDeque q = new LinkedBlockingDeque(2);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                q.putFirst(new Object());
                q.putFirst(new Object());
                assertFalse(q.offerFirst(new Object(), SHORT_DELAY_MS, MILLISECONDS));
                try {
                    q.offerFirst(new Object(), LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
            }});

        t.start();
        Thread.sleep(SMALL_DELAY_MS);
        t.interrupt();
        t.join();
    }

    /**
     * takeFirst blocks interruptibly when empty
     */
    @Test
    @Schedule(name = "takeFirstFromEmpty", value = "[beforeTake:afterTake]@takeThread->beforeInterrupt@main")
    public void testTakeFirstFromEmpty() throws InterruptedException {
        final LinkedBlockingDeque q = new LinkedBlockingDeque(2);
        Thread t = new ThreadShouldThrow(InterruptedException.class) {
            public void realRun() throws InterruptedException {
                fireEvent("beforeTake");
                q.takeFirst();
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
     * TakeFirst removes existing elements until empty, then blocks interruptibly
     */
    @Test
    @Schedule(name = "blockingTakeFirst", value = "[beforeTake:afterTake]@takeThread->beforeInterrupt@main")
    public void testBlockingTakeFirst() throws InterruptedException {
        final LinkedBlockingDeque q = populatedDeque(SIZE);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; ++i)
                    assertEquals("blockingTakeFirst",i, q.takeFirst());
                try {
                    fireEvent("beforeTake");
                    q.takeFirst();
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
     * timed pollFirst with zero timeout succeeds when non-empty, else times out
     */
    public void testTimedPollFirst0() throws InterruptedException {
        LinkedBlockingDeque q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.pollFirst(0, MILLISECONDS));
        }
        assertNull(q.pollFirst(0, MILLISECONDS));
    }

    /**
     * timed pollFirst with nonzero timeout succeeds when non-empty, else times out
     */
    public void testTimedPollFirst() throws InterruptedException {
        LinkedBlockingDeque q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(i, q.pollFirst(SHORT_DELAY_MS, MILLISECONDS));
        }
        assertNull(q.pollFirst(SHORT_DELAY_MS, MILLISECONDS));
    }

    /**
     * Interrupted timed pollFirst throws InterruptedException instead of
     * returning timeout status
     */
    public void testInterruptedTimedPollFirst() throws InterruptedException {
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                LinkedBlockingDeque q = populatedDeque(SIZE);
                for (int i = 0; i < SIZE; ++i) {
                    assertEquals(i, q.pollFirst(SHORT_DELAY_MS, MILLISECONDS));
                }
                try {
                    q.pollFirst(SMALL_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
            }});

        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        t.join();
    }

    /**
     * timed pollFirst before a delayed offerFirst fails; after offerFirst succeeds;
     * on interruption throws
     */
    public void testTimedPollFirstWithOfferFirst() throws InterruptedException {
        final LinkedBlockingDeque q = new LinkedBlockingDeque(2);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                assertNull(q.pollFirst(SHORT_DELAY_MS, MILLISECONDS));
                assertSame(zero, q.pollFirst(LONG_DELAY_MS, MILLISECONDS));
                try {
                    q.pollFirst(LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
            }});

        t.start();
        Thread.sleep(SMALL_DELAY_MS);
        assertTrue(q.offerFirst(zero, SHORT_DELAY_MS, MILLISECONDS));
        t.interrupt();
        t.join();
    }

    /**
     * putLast blocks interruptibly if full
     */
    @Test
    @Schedule(name = "blockingPutLast", value = "[beforePut:afterPut]@putThread->beforeInterrupt@main")
    public void testBlockingPutLast() throws InterruptedException {
        final LinkedBlockingDeque q = new LinkedBlockingDeque(SIZE);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; ++i)
                    q.putLast(i);
                assertEquals("blockingPutLast", SIZE, q.size());
                assertEquals("blockingPutLast", 0, q.remainingCapacity());
                try {
                    fireEvent("beforePut");
                    q.putLast(99);
                    fireEvent("afterPut");
                    shouldThrow();
                } catch (InterruptedException success) {}
            }}, "putThread");

        t.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeInterrupt");
        t.interrupt();
        t.join();
        assertEquals("blockingPutLast", SIZE, q.size());
        assertEquals("blockingPutLast", 0, q.remainingCapacity());
    }

    /**
     * putLast blocks waiting for take when full
     */
    @Test
    @Schedule(name = "putLastWithTake", value = "[beforePut:afterPut]@putThread->beforeTake@main" + 
        ", [beforeSecondPut:afterSecondPut]@putThread->beforeInterrupt@main" )
    public void testPutLastWithTake() throws InterruptedException {
        final int capacity = 2;
        final LinkedBlockingDeque q = new LinkedBlockingDeque(capacity);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                fireEvent("beforePut");
                for (int i = 0; i < capacity + 1; i++)
                    q.putLast(i);
                fireEvent("afterPut");
                try {
                    fireEvent("beforeSecondPut");
                    q.putLast(99);
                    fireEvent("afterSecondPut");
                    shouldThrow();
                } catch (InterruptedException success) {}
            }}, "putThread");

        t.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeTake");
        assertEquals("putLastWithTake", q.remainingCapacity(), 0);
        assertEquals("putLastWithTake", 0, q.take());
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeInterrupt");
        t.interrupt();
        t.join();
        assertEquals("putLastWithTake", q.remainingCapacity(), 0);
    }

    /**
     * timed offerLast times out if full and elements not taken
     */
    public void testTimedOfferLast() throws InterruptedException {
        final LinkedBlockingDeque q = new LinkedBlockingDeque(2);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                q.putLast(new Object());
                q.putLast(new Object());
                assertFalse(q.offerLast(new Object(), SHORT_DELAY_MS, MILLISECONDS));
                try {
                    q.offerLast(new Object(), LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
            }});

        t.start();
        Thread.sleep(SMALL_DELAY_MS);
        t.interrupt();
        t.join();
    }

    /**
     * takeLast retrieves elements in FIFO order
     */
    public void testTakeLast() throws InterruptedException {
        LinkedBlockingDeque q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(SIZE-i-1, q.takeLast());
        }
    }

    /**
     * takeLast blocks interruptibly when empty
     */
    @Test
    @Schedule(name = "takeLastFromEmpty", value = "[beforeTake:afterTake]@takeThread->beforeInterrupt@main")
    public void testTakeLastFromEmpty() throws InterruptedException {
        final LinkedBlockingDeque q = new LinkedBlockingDeque(2);
        Thread t = new ThreadShouldThrow(InterruptedException.class) {
            public void realRun() throws InterruptedException {
                fireEvent("beforeTake");
                q.takeLast();
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
     * TakeLast removes existing elements until empty, then blocks interruptibly
     */
    @Test
    @Schedule(name = "blockingTakeLast", value = "[beforeTake:afterTake]@takeThread->beforeInterrupt@main")
    public void testBlockingTakeLast() throws InterruptedException {
        final LinkedBlockingDeque q = populatedDeque(SIZE);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; ++i)
                    assertEquals("blockingTakeLast", SIZE - 1 - i, q.takeLast());
                try {
                    fireEvent("beforeTake");
                    q.takeLast();
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
     * timed pollLast with zero timeout succeeds when non-empty, else times out
     */
    public void testTimedPollLast0() throws InterruptedException {
        LinkedBlockingDeque q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(SIZE-i-1, q.pollLast(0, MILLISECONDS));
        }
        assertNull(q.pollLast(0, MILLISECONDS));
    }

    /**
     * timed pollLast with nonzero timeout succeeds when non-empty, else times out
     */
    public void testTimedPollLast() throws InterruptedException {
        LinkedBlockingDeque q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertEquals(SIZE-i-1, q.pollLast(SHORT_DELAY_MS, MILLISECONDS));
        }
        assertNull(q.pollLast(SHORT_DELAY_MS, MILLISECONDS));
    }

    /**
     * Interrupted timed pollLast throws InterruptedException instead of
     * returning timeout status
     */
    public void testInterruptedTimedPollLast() throws InterruptedException {
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                LinkedBlockingDeque q = populatedDeque(SIZE);
                for (int i = 0; i < SIZE; ++i) {
                    assertEquals(SIZE-i-1, q.pollLast(SHORT_DELAY_MS, MILLISECONDS));
                }
                try {
                    q.pollLast(SMALL_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
            }});

        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        t.join();
    }

    /**
     * timed poll before a delayed offerLast fails; after offerLast succeeds;
     * on interruption throws
     */
    public void testTimedPollWithOfferLast() throws InterruptedException {
        final LinkedBlockingDeque q = new LinkedBlockingDeque(2);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                assertNull(q.poll(SHORT_DELAY_MS, MILLISECONDS));
                assertSame(zero, q.poll(LONG_DELAY_MS, MILLISECONDS));
                try {
                    q.poll(LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
            }});

        t.start();
        Thread.sleep(SMALL_DELAY_MS);
        assertTrue(q.offerLast(zero, SHORT_DELAY_MS, MILLISECONDS));
        t.interrupt();
        t.join();
    }


    /**
     * offer transfers elements across Executor tasks
     */
    public void testOfferInExecutor() {
        final LinkedBlockingDeque q = new LinkedBlockingDeque(2);
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
        final LinkedBlockingDeque q = new LinkedBlockingDeque(2);
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

    /**
     * drainTo empties full deque, unblocking a waiting put.
     */
    public void testDrainToWithActivePut() throws InterruptedException {
        final LinkedBlockingDeque q = populatedDeque(SIZE);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                q.put(new Integer(SIZE+1));
            }});

        t.start();
        ArrayList l = new ArrayList();
        q.drainTo(l);
        assertTrue(l.size() >= SIZE);
        for (int i = 0; i < SIZE; ++i)
            assertEquals(l.get(i), new Integer(i));
        t.join();
        assertTrue(q.size() + l.size() >= SIZE);
    }

}
