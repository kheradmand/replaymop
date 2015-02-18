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

import java.util.*;
import java.util.concurrent.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import java.io.*;
import org.junit.Test;
import edu.illinois.imunit.Schedule;


@RunWith(IMUnitRunner.class)
public class SynchronousQueueTest extends JSR166TestCase {

    public static class Fair extends BlockingQueueTest {
        protected BlockingQueue emptyCollection() {
            return new SynchronousQueue(true);
        }
    }

    public static class NonFair extends BlockingQueueTest {
        protected BlockingQueue emptyCollection() {
            return new SynchronousQueue(false);
        }
    }

//    public static void main(String[] args) {
//        junit.textui.TestRunner.run(suite());
//    }
//
//    public static Test suite() {
//        return newTestSuite(SynchronousQueueTest.class,
//                            new Fair().testSuite(),
//                            new NonFair().testSuite());
//    }

    /**
     * put blocks interruptibly if no active taker
     */
    @Test
    @Schedule(name = "blockingPut", value = "[beforePut:afterPut]@putThread->beforeInterrupt@main")
    public void testBlockingPut() throws InterruptedException {
        Thread t = new Thread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                SynchronousQueue q = new SynchronousQueue();
                fireEvent("beforePut");
                q.put(zero);
                fireEvent("afterPut");
            }}, "putThread");

        t.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeInterrupt");
        t.interrupt();
        t.join();
    }

    /**
     * put blocks waiting for take
     * 
     * events in a loop Qingzhou
     */
    //@NSchedule(name="PutWithTake1",value="[beforeAdd1:afterAdd1]@t->beforeTake@main,[beforeAdd2:afterAdd2]@t->beforeInterrupt@main, interrupted@main->afterInterrupt@main")
    public void testPutWithTake1() throws InterruptedException {
        final SynchronousQueue q = new SynchronousQueue();
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                int added = 0;
                try {
                    while (true) {
                        /* @NEvent("beforeAdd")*/
                        q.put(added);
                        /* @NEvent("afterAdd")*/
                        ++added;
                    }
                } catch (InterruptedException success) {
                    /* @NEvent("interrupted")*/
                    assertEquals("PutWithTake", 1, added);
                }
            }}, "putThread");
        t.setName("t");
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        /* @NEvent("beforeTake")*/
        assertEquals("PutWithTake",0, q.take());
        Thread.sleep(SHORT_DELAY_MS);
        /* @NEvent("beforeInterrupt")*/
        t.interrupt();
        /* @NEvent("afterInterrupt")*/
        t.join();
    }

    /**
     * put blocks waiting for take
     * 
     * events in a loop Qingzhou
     */
    //@NSchedule(name="PutWithTake2",value="[beforeAdd:afterAdd]@t->beforeTake@main,[beforeAdd:afterAdd]@t->beforeInterrupt@main, interrupted@main->afterInterrupt@main")
    public void testPutWithTake2() throws InterruptedException {
        final SynchronousQueue q = new SynchronousQueue();
        Thread t = new Thread(new CheckedRunnable() {
             public void realRun() throws InterruptedException {
                int added = 0;
                try {
                    while (true) {
                        /* @NEvent("beforeAdd")*/
                        q.put(added);
                        /* @NEvent("afterAdd")*/
                        ++added;
                    }
                } catch (InterruptedException success) {
                    /* @NEvent("interrupted")*/
                    assertEquals("PutWithTake", 1, added);
                }
            }}, "putThread");
        t.setName("t");
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        /* @NEvent("beforeTake")*/
        assertEquals("PutWithTake",0, q.take());
        Thread.sleep(SHORT_DELAY_MS);
        /* @NEvent("beforeInterrupt")*/
        t.interrupt();
        /* @NEvent("afterInterrupt")*/
        t.join();
     }

    /**
     * timed offer times out if elements not taken
     */
    public void testTimedOffer() throws InterruptedException {
        final SynchronousQueue q = new SynchronousQueue();
        Thread t = new Thread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                assertFalse(q.offer(new Object(), SHORT_DELAY_MS, MILLISECONDS));
                q.offer(new Object(), LONG_DELAY_MS, MILLISECONDS);
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
        final SynchronousQueue q = new SynchronousQueue();
        Thread t = new Thread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                fireEvent("beforeTake");              
                q.take();
                fireEvent("afterTake");
            }}, "takeThread");

        t.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeInterrupt");
        t.interrupt();
        t.join();
    }


    /**
     * put blocks interruptibly if no active taker
     */
    @Test
    @Schedule(name = "fairBlockingPut", value = "[beforePut:afterPut]@putThread->beforeInterrupt@main")
    public void testFairBlockingPut() throws InterruptedException {
        Thread t = new Thread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                SynchronousQueue q = new SynchronousQueue(true);
                fireEvent("beforePut");
                q.put(zero);
                fireEvent("afterPut");
            }}, "putThread");
  
        t.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeInterrupt");
        t.interrupt();
        t.join();
  }

    /**
     * put blocks waiting for take
     * 
     * events in a loop Qingzhou
     */
    //@NSchedule(name="fairPutWithTake1",value="[beforeAdd1:afterAdd1]@t->beforeTake@main,[beforeAdd2:afterAdd2]@t->beforeInterrupt@main, interrupted@main->afterInterrupt@main")
    public void testFairPutWithTake1() throws InterruptedException {
        final SynchronousQueue q = new SynchronousQueue(true);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                int added = 0;
                try {
                    while (true) {
                        /* @NEvent("beforeAdd")*/
                        q.put(added);
                        /* @NEvent("afterAdd")*/
                        ++added;
                    }
                } catch (InterruptedException success) {
                    /* @NEvent("interrupted")*/
                    assertEquals("fairPutWithTake", 1, added);
                }
            }}, "putThread");
        t.setName("t");
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        /* @NEvent("beforeTake")*/
        assertEquals("fairPutWithTake",0, q.take());
        Thread.sleep(SHORT_DELAY_MS);
        /* @NEvent("beforeInterrupt")*/
        t.interrupt();
        /* @NEvent("afterInterrupt")*/
        t.join();
    }

    /**
     * put blocks waiting for take
     * 
     * events in a loop Qingzhou
     */
    //@NSchedule(name="fairPutWithTake2",value="[beforeAdd:afterAdd]@t->beforeTake@main,[beforeAdd:afterAdd]@t->beforeInterrupt@main, interrupted@main->afterInterrupt@main")
    public void testFairPutWithTake2() throws InterruptedException {
        final SynchronousQueue q = new SynchronousQueue(true);
        Thread t = new Thread(new CheckedRunnable() {
             public void realRun() throws InterruptedException {
                int added = 0;
                try {
                    while (true) {
                        /* @NEvent("beforeAdd")*/
                        q.put(added);
                        /* @NEvent("afterAdd")*/
                        ++added;
                    }
                } catch (InterruptedException success) {
                    /* @NEvent("interrupted")*/
                    assertEquals("fairPutWithTake", 1, added);
                }
            }}, "putThread");
        t.setName("t");
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        /* @NEvent("beforeTake")*/
        assertEquals("fairPutWithTake",0, q.take());
        Thread.sleep(SHORT_DELAY_MS);
        /* @NEvent("beforeInterrupt")*/
        t.interrupt();
        /* @NEvent("afterInterrupt")*/
        t.join();
     }

    /**
     * timed offer times out if elements not taken
     */
    public void testFairTimedOffer() throws InterruptedException {
        final SynchronousQueue q = new SynchronousQueue(true);
        Thread t = new Thread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                assertFalse(q.offer(new Object(), SHORT_DELAY_MS, MILLISECONDS));
                q.offer(new Object(), LONG_DELAY_MS, MILLISECONDS);
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
    @Schedule(name = "fairTakeFromEmpty", value = "[beforeTake:afterTake]@takeThread->beforeInterrupt@main")
    public void testFairTakeFromEmpty() throws InterruptedException {
        final SynchronousQueue q = new SynchronousQueue(true);
        Thread t = new Thread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                fireEvent("beforeTake");              
                q.take();
                fireEvent("afterTake");
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
        final SynchronousQueue q = new SynchronousQueue();
        Thread t = new Thread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                q.poll(SMALL_DELAY_MS, MILLISECONDS);
            }});

        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        t.join();
    }

    /**
     * Interrupted timed poll throws InterruptedException instead of
     * returning timeout status
     */
    public void testFairInterruptedTimedPoll() throws InterruptedException {
        Thread t = new Thread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                SynchronousQueue q = new SynchronousQueue(true);
                q.poll(SMALL_DELAY_MS, MILLISECONDS);
            }});

        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        t.join();
    }

    /**
     * timed poll before a delayed offer fails; after offer succeeds;
     * on interruption throws
     */
    public void testFairTimedPollWithOffer() throws InterruptedException {
        final SynchronousQueue q = new SynchronousQueue(true);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                try {
                    assertNull(q.poll(SHORT_DELAY_MS, MILLISECONDS));
                    assertSame(zero, q.poll(LONG_DELAY_MS, MILLISECONDS));
                    q.poll(LONG_DELAY_MS, MILLISECONDS);
                    threadShouldThrow();
                } catch (InterruptedException success) {}
            }});

        t.start();
        Thread.sleep(SMALL_DELAY_MS);
        assertTrue(q.offer(zero, SHORT_DELAY_MS, MILLISECONDS));
        t.interrupt();
        t.join();
    }


    /**
     * offer transfers elements across Executor tasks
     */
    public void testOfferInExecutor() {
        final SynchronousQueue q = new SynchronousQueue();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.execute(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                assertFalse(q.offer(one));
                assertTrue(q.offer(one, MEDIUM_DELAY_MS, MILLISECONDS));
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
        final SynchronousQueue q = new SynchronousQueue();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.execute(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                assertNull(q.poll());
                assertSame(one, q.poll(MEDIUM_DELAY_MS, MILLISECONDS));
                assertTrue(q.isEmpty());
            }});

        executor.execute(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                Thread.sleep(SHORT_DELAY_MS);
                q.put(one);
            }});

        joinPool(executor);
    }

    /**
     * drainTo empties queue, unblocking a waiting put.
     */
    @Test
    @Schedule(name = "drainToWithActivePut", value = "[beforePut:afterPut]@drainThread->beforeDrain@main")
    public void testDrainToWithActivePut() throws InterruptedException {
        final SynchronousQueue q = new SynchronousQueue();
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                fireEvent("beforePut");
                q.put(new Integer(1));
                fireEvent("afterPut");
            }}, "drainThread");

        t.start();
        ArrayList l = new ArrayList();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeDrain");
        q.drainTo(l);
        assertTrue("drainToWithActivePut", l.size() <= 1);
        if (l.size() > 0)
            assertEquals("drainToWithActivePut", l.get(0), new Integer(1));
        t.join();
        assertTrue("drainToWithActivePut", l.size() <= 1);
    }

    /**
     * drainTo(c, n) empties up to n elements of queue into c
     */
    @Test
    @Schedule(name = "drainToN", value = "[beforePut:afterPut]@t1->beforeDrain@main, " +
        "[beforePut:afterPut]@t2->beforeDrain@main")
    public void testDrainToN() throws InterruptedException {
        final SynchronousQueue q = new SynchronousQueue();
        Thread t1 = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                fireEvent("beforePut");
                q.put(one);
                fireEvent("afterPut");
            }}, "t1");

        Thread t2 = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                fireEvent("beforePut");
                q.put(two);
                fireEvent("afterPut");
            }}, "t2");

        t1.start();
        t2.start();
        ArrayList l = new ArrayList();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeDrain");
        q.drainTo(l, 1);
        assertEquals("drainToN", 1, l.size());
        q.drainTo(l, 1);
        assertEquals("drainToN", 2, l.size());
        assertTrue("drainToN", l.contains(one));
        assertTrue("drainToN", l.contains(two));
        t1.join();
        t2.join();
    }

}
