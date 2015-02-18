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

import junit.framework.*;
import java.util.*;
import java.util.concurrent.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import java.io.*;
import org.junit.Test;
import edu.illinois.imunit.Schedule;
import edu.illinois.imunit.Schedules;

@RunWith(IMUnitRunner.class)
public class SemaphoreTest extends JSR166TestCase {
//    public static void main(String[] args) {
//        junit.textui.TestRunner.run(suite());
//    }
//    public static Test suite() {
//        return new TestSuite(SemaphoreTest.class);
//    }

    /**
     * Subclass to expose protected methods
     */
    static class PublicSemaphore extends Semaphore {
        PublicSemaphore(int p, boolean f) { super(p, f); }
        public Collection<Thread> getQueuedThreads() {
            return super.getQueuedThreads();
        }
        public void reducePermits(int p) {
            super.reducePermits(p);
        }
    }

    /**
     * A runnable calling acquire
     */
    class InterruptibleLockRunnable extends CheckedRunnable {
        final Semaphore lock;
        InterruptibleLockRunnable(Semaphore l) { lock = l; }
        public void realRun() {
            try {
                fireEvent("beforeLock");
                lock.acquire();
                fireEvent("afterLock");
            }
            catch (InterruptedException ignored) {}
        }
    }


    /**
     * A runnable calling acquire that expects to be interrupted
     */
    class InterruptedLockRunnable extends CheckedInterruptedRunnable {
        final Semaphore lock;
        InterruptedLockRunnable(Semaphore l) { lock = l; }
        public void realRun() throws InterruptedException {
            fireEvent("beforeLock");
            lock.acquire();
            fireEvent("afterLock");
            
        }
    }



    /**
     * A release in one thread enables an acquire in another thread
     */
    @Test
    @Schedule(name = "acquireRelease", value = "[beforeAcquire:afterAcquire]@acquireThread->beforeRelease@main")
    public void testAcquireReleaseInDifferentThreads()
        throws InterruptedException {
        final Semaphore s = new Semaphore(0, false);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                fireEvent("beforeAcquire");
                s.acquire();
                fireEvent("afterAcquire");
                s.release();
                s.release();
                s.acquire();
            }}, "acquireThread");

        t.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeRelease");
        s.release();
        s.release();
        s.acquire();
        s.acquire();
        s.release();
        t.join();
    }

    /**
     * A release in one thread enables an uninterruptible acquire in another thread
     */
    @Test
    @Schedule(name = "uninterruptibleAcquireRelease", value = "[beforeAcquire:afterAcquire]@acquireThread->beforeRelease@main")
    public void testUninterruptibleAcquireReleaseInDifferentThreads()
        throws InterruptedException {
        final Semaphore s = new Semaphore(0, false);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                fireEvent("beforeAcquire");
                s.acquireUninterruptibly();
                fireEvent("afterAcquire");
                s.release();
                s.release();
                s.acquireUninterruptibly();
            }}, "acquireThread");

        t.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeRelease");
        s.release();
        s.release();
        s.acquireUninterruptibly();
        s.acquireUninterruptibly();
        s.release();
        t.join();
    }


    /**
     * A release in one thread enables a timed acquire in another thread
     */
    public void testTimedAcquireReleaseInDifferentThreads()
        throws InterruptedException {
        final Semaphore s = new Semaphore(1, false);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                s.release();
                assertTrue(s.tryAcquire(SHORT_DELAY_MS, MILLISECONDS));
                s.release();
                assertTrue(s.tryAcquire(SHORT_DELAY_MS, MILLISECONDS));
            }});

        t.start();
        assertTrue(s.tryAcquire(SHORT_DELAY_MS, MILLISECONDS));
        s.release();
        assertTrue(s.tryAcquire(SHORT_DELAY_MS, MILLISECONDS));
        s.release();
        s.release();
        t.join();
    }

    /**
     * A waiting acquire blocks interruptibly
     */
    @Test
    @Schedule(name = "acquireInterruptedException", value = "[beforeAcquire:afterAcquire]@acquireThread->beforeInterrupt@main")
    public void testAcquireInterruptedException()
        throws InterruptedException {
        final Semaphore s = new Semaphore(0, false);
        Thread t = new Thread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                fireEvent("beforeAcquire");
                s.acquire();
                fireEvent("afterAcquire");
            }}, "acquireThread");

        t.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeInterrupt");
        t.interrupt();
        t.join();
    }

    /**
     * A waiting timed acquire blocks interruptibly
     */
    public void testTryAcquire_InterruptedException()
        throws InterruptedException {
        final Semaphore s = new Semaphore(0, false);
        Thread t = new Thread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                s.tryAcquire(MEDIUM_DELAY_MS, MILLISECONDS);
            }});

        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        t.join();
    }

    /**
     * hasQueuedThreads reports whether there are waiting threads
     */
    @Test
    @Schedules( { @Schedule(name = "HasQueuedThreads", value = "[beforeLock:afterLock]@lockThread1->checkHasQueued1@main,"
        + "[beforeLock:afterLock]@lockThread2->checkHasQueued2@main,"
        + "edu.illinois.imunit.examples.tck.JSR166TestCase.interruptedException@lockThread1->checkHasQueued3@main,"
        + "afterLock@lockThread2->checkHasQueued4@main ") })
    public void testHasQueuedThreads() throws InterruptedException {
        final Semaphore lock = new Semaphore(1, false);
        Thread t1 = new Thread(new InterruptedLockRunnable(lock));
        Thread t2 = new Thread(new InterruptibleLockRunnable(lock));
        t1.setName("lockThread1");
        t2.setName("lockThread2");
        assertFalse("HasQueuedThreads", lock.hasQueuedThreads());
        lock.acquireUninterruptibly();
        t1.start();
        // Thread.sleep(SHORT_DELAY_MS);
        fireEvent("checkHasQueued1");
        assertTrue("HasQueuedThreads", lock.hasQueuedThreads());
        t2.start();
        // Thread.sleep(SHORT_DELAY_MS);
        fireEvent("checkHasQueued2");
        assertTrue("HasQueuedThreads", lock.hasQueuedThreads());
        t1.interrupt();
        // Thread.sleep(SHORT_DELAY_MS);
        fireEvent("checkHasQueued3");
        assertTrue("HasQueuedThreads", lock.hasQueuedThreads());
        lock.release();
        // Thread.sleep(SHORT_DELAY_MS);
        fireEvent("checkHasQueued4");
        assertFalse("HasQueuedThreads", lock.hasQueuedThreads());
        t1.join();
        t2.join();
    }

    /**
     * getQueueLength reports number of waiting threads
     */
    @Test
    @Schedules( { @Schedule(name = "GetQueueLength", value = "[beforeLock:afterLock]@lockThread1->checkLength1@main,"
        + "[beforeLock:afterLock]@lockThread2->checkLength2@main,"
        + "edu.illinois.imunit.examples.tck.JSR166TestCase.interruptedException@lockThread1->checkLength3@main,"
        + "afterLock@lockThread2->checkLength4@main ") })
    public void testGetQueueLength() throws InterruptedException {
        final Semaphore lock = new Semaphore(1, false);
        Thread t1 = new Thread(new InterruptedLockRunnable(lock));
        Thread t2 = new Thread(new InterruptibleLockRunnable(lock));
        t1.setName("lockThread1");
        t2.setName("lockThread2");
        assertEquals("GetQueueLength", 0, lock.getQueueLength());
        lock.acquireUninterruptibly();
        t1.start();
        // Thread.sleep(SHORT_DELAY_MS);
        fireEvent("checkLength1");
        assertEquals("GetQueueLength", 1, lock.getQueueLength());
        t2.start();
        // Thread.sleep(SHORT_DELAY_MS);
        fireEvent("checkLength2");
        assertEquals("GetQueueLength", 2, lock.getQueueLength());
        t1.interrupt();
        // Thread.sleep(SHORT_DELAY_MS);
        fireEvent("checkLength3");
        assertEquals("GetQueueLength", 1, lock.getQueueLength());
        lock.release();
        // Thread.sleep(SHORT_DELAY_MS);
        fireEvent("checkLength4");
        assertEquals("GetQueueLength", 0, lock.getQueueLength());
        t1.join();
        t2.join();
    }

    /**
     * getQueuedThreads includes waiting threads
     */
    @Test
    @Schedules( { @Schedule(name = "GetQueuedThreads", value = "[beforeLock:afterLock]@lockThread1->chekcGetThreads1@main,"
        + "[beforeLock:afterLock]@lockThread2->chekcGetThreads2@main,"
        + "edu.illinois.imunit.examples.tck.JSR166TestCase.interruptedException@lockThread1->chekcGetThreads3@main,"
        + "afterLock@lockThread2->chekcGetThreads4@main ") })
    public void testGetQueuedThreads() throws InterruptedException {
        final PublicSemaphore lock = new PublicSemaphore(1, false);
        Thread t1 = new Thread(new InterruptedLockRunnable(lock));
        Thread t2 = new Thread(new InterruptibleLockRunnable(lock));
        t1.setName("lockThread1");
        t2.setName("lockThread2");
        assertTrue("GetQueuedThreads", lock.getQueuedThreads().isEmpty());
        lock.acquireUninterruptibly();
        assertTrue("GetQueuedThreads", lock.getQueuedThreads().isEmpty());
        t1.start();
        // Thread.sleep(SHORT_DELAY_MS);
        fireEvent("chekcGetThreads1");
        assertTrue("GetQueuedThreads", lock.getQueuedThreads().contains(t1));
        t2.start();
        // Thread.sleep(SHORT_DELAY_MS);
        fireEvent("chekcGetThreads2");
        assertTrue("GetQueuedThreads", lock.getQueuedThreads().contains(t1));
        assertTrue("GetQueuedThreads", lock.getQueuedThreads().contains(t2));
        t1.interrupt();
        // Thread.sleep(SHORT_DELAY_MS);
        fireEvent("chekcGetThreads3");
        assertFalse("GetQueuedThreads", lock.getQueuedThreads().contains(t1));
        assertTrue("GetQueuedThreads", lock.getQueuedThreads().contains(t2));
        lock.release();
        // Thread.sleep(SHORT_DELAY_MS);
        fireEvent("chekcGetThreads4");
        assertTrue("GetQueuedThreads", lock.getQueuedThreads().isEmpty());
        t1.join();
        t2.join();
    }

    /**
     * A release in one thread enables an acquire in another thread
     */
    @Test
    @Schedule(name = "acquireReleasefair", value = "[beforeAcquire:afterAcquire]@acquireThread->beforeRelease@main")
    public void testAcquireReleaseInDifferentThreadsfair()
        throws InterruptedException {
        final Semaphore s = new Semaphore(0, true);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                fireEvent("beforeAcquire");
                s.acquire();
                fireEvent("afterAcquire");
                s.acquire();
                s.acquire();
                s.acquire();
            }}, "acquireThread");

        t.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeRelease");
        s.release();
        s.release();
        s.release();
        s.release();
        s.release();
        s.release();
        t.join();
        assertEquals(2, s.availablePermits());
    }

    /**
     * release(n) in one thread enables acquire(n) in another thread
     */
    @Test
    @Schedule(name = "acquireReleaseNfair", value = "[beforeAcquire:afterAcquire]@acquireThread->beforeRelease@main")
    public void testAcquireReleaseNInDifferentThreadsfair()
        throws InterruptedException {
        final Semaphore s = new Semaphore(0, true);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                fireEvent("beforeAcquire");
                s.acquire();
                fireEvent("afterAcquire");
                s.release(2);
                s.acquire();
            }}, "acquireThread");

        t.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeRelease");
        s.release(2);
        s.acquire(2);
        s.release(1);
        t.join();
    }

    /**
     * release(n) in one thread enables acquire(n) in another thread
     */
    @Test
    @Schedule(name = "acquireReleaseNfair2", value = "[beforeAcquire:afterAcquire]@acquireThread->beforeRelease@main")
    public void testAcquireReleaseNInDifferentThreadsfair2()
        throws InterruptedException {
        final Semaphore s = new Semaphore(0, true);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                fireEvent("beforeAcquire");
                s.acquire(2);
                fireEvent("afterAcquire");
                s.acquire(2);
                s.release(4);
            }}, "acquireThread");

        t.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeRelease");
        s.release(6);
        s.acquire(2);
        s.acquire(2);
        s.release(2);
        t.join();
    }


    /**
     * release in one thread enables timed acquire in another thread
     */
    public void testTimedAcquireReleaseInDifferentThreads_fair()
        throws InterruptedException {
        final Semaphore s = new Semaphore(1, true);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                assertTrue(s.tryAcquire(SHORT_DELAY_MS, MILLISECONDS));
                assertTrue(s.tryAcquire(SHORT_DELAY_MS, MILLISECONDS));
                assertTrue(s.tryAcquire(SHORT_DELAY_MS, MILLISECONDS));
                assertTrue(s.tryAcquire(SHORT_DELAY_MS, MILLISECONDS));
                assertTrue(s.tryAcquire(SHORT_DELAY_MS, MILLISECONDS));
            }});

        t.start();
        s.release();
        s.release();
        s.release();
        s.release();
        s.release();
        t.join();
    }

    /**
     * release(n) in one thread enables timed acquire(n) in another thread
     */
    public void testTimedAcquireReleaseNInDifferentThreads_fair()
        throws InterruptedException {
        final Semaphore s = new Semaphore(2, true);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                assertTrue(s.tryAcquire(2, SHORT_DELAY_MS, MILLISECONDS));
                s.release(2);
                assertTrue(s.tryAcquire(2, SHORT_DELAY_MS, MILLISECONDS));
                s.release(2);
            }});

        t.start();
        assertTrue(s.tryAcquire(2, SHORT_DELAY_MS, MILLISECONDS));
        s.release(2);
        assertTrue(s.tryAcquire(2, SHORT_DELAY_MS, MILLISECONDS));
        s.release(2);
        t.join();
    }

    /**
     * A waiting acquire blocks interruptibly
     */
    @Test
    @Schedule(name = "acquireInterruptedExceptionfair", value = "[beforeAcquire:afterAcquire]@acquireThread->beforeInterrupt@main")
    public void testAcquireInterruptedExceptionfair()
        throws InterruptedException {
        final Semaphore s = new Semaphore(0, true);
        Thread t = new Thread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                fireEvent("beforeAcquire");
                s.acquire();
                fireEvent("afterAcquire");
            }}, "acquireThread");

        t.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeInterrupt");
        t.interrupt();
        t.join();
    }

    /**
     * A waiting acquire(n) blocks interruptibly
     */
    @Test
    @Schedule(name = "acquireNInterruptedExceptionfair", value = "[beforeAcquire:afterAcquire]@acquireThread->beforeInterrupt@main")
    public void testAcquireNInterruptedExceptionfair()
        throws InterruptedException {
        final Semaphore s = new Semaphore(2, true);
        Thread t = new Thread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                fireEvent("beforeAcquire");
                s.acquire(3);
                fireEvent("afterAcquire");
            }}, "acquireThread");

        t.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeInterrupt");
        t.interrupt();
        t.join();
    }

    /**
     * A waiting tryAcquire blocks interruptibly
     */
    public void testTryAcquire_InterruptedException_fair()
        throws InterruptedException {
        final Semaphore s = new Semaphore(0, true);
        Thread t = new Thread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                s.tryAcquire(MEDIUM_DELAY_MS, MILLISECONDS);
            }});

        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        t.join();
    }

    /**
     * A waiting tryAcquire(n) blocks interruptibly
     */
    public void testTryAcquireN_InterruptedException_fair()
        throws InterruptedException {
        final Semaphore s = new Semaphore(1, true);
        Thread t = new Thread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                s.tryAcquire(4, MEDIUM_DELAY_MS, MILLISECONDS);
            }});

        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        t.join();
    }

    /**
     * getQueueLength reports number of waiting threads
     */
    @Test
    @Schedules( { @Schedule(name = "GetQueueLengthfair", value = "[beforeLock:afterLock]@lockThread1->checkLength1@main,"
        + "[beforeLock:afterLock]@lockThread2->checkLength2@main,"
        + "edu.illinois.imunit.examples.tck.JSR166TestCase.interruptedException@lockThread1->checkLength3@main,"
        + "afterLock@lockThread2->checkLength4@main ") })
    public void testGetQueueLengthfair() throws InterruptedException {
        final Semaphore lock = new Semaphore(1, true);
        Thread t1 = new Thread(new InterruptedLockRunnable(lock));
        Thread t2 = new Thread(new InterruptibleLockRunnable(lock));
        t1.setName("lockThread1");
        t2.setName("lockThread2");
        assertEquals("GetQueueLength", 0, lock.getQueueLength());
        lock.acquireUninterruptibly();
        t1.start();
        // Thread.sleep(SHORT_DELAY_MS);
        fireEvent("checkLength1");
        assertEquals("GetQueueLength", 1, lock.getQueueLength());
        t2.start();
        // Thread.sleep(SHORT_DELAY_MS);
        fireEvent("checkLength2");
        assertEquals("GetQueueLength", 2, lock.getQueueLength());
        t1.interrupt();
        // Thread.sleep(SHORT_DELAY_MS);
        fireEvent("checkLength3");
        assertEquals("GetQueueLength", 1, lock.getQueueLength());
        lock.release();
        // Thread.sleep(SHORT_DELAY_MS);
        fireEvent("checkLength4");
        assertEquals("GetQueueLength", 0, lock.getQueueLength());
        t1.join();
        t2.join();
    }

}
