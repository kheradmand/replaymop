package edu.illinois.imunit.examples.tck;
import org.junit.runner.RunWith;
import edu.illinois.imunit.IMUnitRunner;
import static edu.illinois.imunit.IMUnit.fireEvent;
/*
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 */

import org.junit.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.*;
import edu.illinois.imunit.Schedule;


@RunWith(IMUnitRunner.class)
public class LockSupportTest extends JSR166TestCase {
//    public static void main(String[] args) {
//        junit.textui.TestRunner.run(suite());
//    }
//
//    public static Test suite() {
//        return new TestSuite(LockSupportTest.class);
//    }

    /**
     * park is released by subsequent unpark
     */
    @Test
    @Schedule(name = "parkBeforeUnpark", value = "[beforePark:afterPark]@parkThread->beforeUnpark@main")
    public void testParkBeforeUnpark() throws InterruptedException {
        final CountDownLatch threadStarted = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                threadStarted.countDown();
                fireEvent("beforePark");
                LockSupport.park();
                fireEvent("afterPark");
            }}, "parkThread");
        threadStarted.await();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeUnpark");
        LockSupport.unpark(t);
        t.join();
        //awaitTermination(t, MEDIUM_DELAY_MS);
    }

    /**
     * parkUntil is released by subsequent unpark
     */
    public void testParkUntilBeforeUnpark() throws InterruptedException {
        final CountDownLatch threadStarted = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                long d = new Date().getTime() + LONG_DELAY_MS;
                long nanos = LONG_DELAY_MS * 1000L * 1000L;
                long t0 = System.nanoTime();
                threadStarted.countDown();
                LockSupport.parkUntil(d);
                assertTrue(System.nanoTime() - t0 < nanos);
            }});

        threadStarted.await();
        Thread.sleep(SHORT_DELAY_MS);
        LockSupport.unpark(t);
        awaitTermination(t, MEDIUM_DELAY_MS);
    }

    /**
     * parkNanos is released by subsequent unpark
     */
    public void testParkNanosBeforeUnpark() throws InterruptedException {
        final CountDownLatch threadStarted = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                long nanos = LONG_DELAY_MS * 1000L * 1000L;
                long t0 = System.nanoTime();
                threadStarted.countDown();
                LockSupport.parkNanos(nanos);
                assertTrue(System.nanoTime() - t0 < nanos);
            }});

        threadStarted.await();
        Thread.sleep(SHORT_DELAY_MS);
        LockSupport.unpark(t);
        awaitTermination(t, MEDIUM_DELAY_MS);
    }

    /**
     * park is released by preceding unpark
     */
    //WAIT@Test
    //WAIT@Schedule(name = "parkAfterUnpark", value = "afterUnpark@main->beforePark@parkThread")
    public void testParkAfterUnpark() throws Exception {
        final CountDownLatch threadStarted = new CountDownLatch(1);
        //OPWAIT final AtomicBoolean unparked = new AtomicBoolean(false);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws Exception {
                threadStarted.countDown();
                //OPWAIT while (!unparked.get())
                //OPWAIT    Thread.yield();
                //Thread.sleep(SHORT_DELAY_MS); //INS-SLEEP
                fireEvent("beforePark");
                LockSupport.park();
                fireEvent("afterPark");
            }}, "parkThread");
        threadStarted.await();
        LockSupport.unpark(t);
        fireEvent("afterUnpark");
        //OPWAIT unparked.set(true);
        t.join();
        //awaitTermination(t, MEDIUM_DELAY_MS);
    }

    /**
     * parkUntil is released by preceding unpark
     */
    public void testParkUntilAfterUnpark() throws Exception {
        final CountDownLatch threadStarted = new CountDownLatch(1);
        final AtomicBoolean unparked = new AtomicBoolean(false);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws Exception {
                threadStarted.countDown();
                while (!unparked.get())
                    Thread.yield();
                long d = new Date().getTime() + LONG_DELAY_MS;
                long nanos = LONG_DELAY_MS * 1000L * 1000L;
                long t0 = System.nanoTime();
                LockSupport.parkUntil(d);
                assertTrue(System.nanoTime() - t0 < nanos);
            }});

        threadStarted.await();
        LockSupport.unpark(t);
        unparked.set(true);
        t.join();
        //awaitTermination(t, MEDIUM_DELAY_MS);
    }

    /**
     * parkNanos is released by preceding unpark
     */
    public void testParkNanosAfterUnpark() throws Exception {
        final CountDownLatch threadStarted = new CountDownLatch(1);
        final AtomicBoolean unparked = new AtomicBoolean(false);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws Exception {
                threadStarted.countDown();
                while (!unparked.get())
                    Thread.yield();
                long nanos = LONG_DELAY_MS * 1000L * 1000L;
                long t0 = System.nanoTime();
                LockSupport.parkNanos(nanos);
                assertTrue(System.nanoTime() - t0 < nanos);
            }});

        threadStarted.await();
        LockSupport.unpark(t);
        unparked.set(true);
        t.join();
        //awaitTermination(t, MEDIUM_DELAY_MS);
    }

    /**
     * park is released by subsequent interrupt
     */
    @Test
    @Schedule(name = "parkBeforeInterrupt", value = "[beforePark:afterPark]@parkThread->beforeInterrupt@main")
    public void testParkBeforeInterrupt() throws InterruptedException {
        final CountDownLatch threadStarted = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertFalse("parkBeforeInterrupt", Thread.currentThread().isInterrupted());
                threadStarted.countDown();
                do {
                    fireEvent("beforePark");
                    LockSupport.park();
                    fireEvent("afterPark");
                    // park may return spuriously
                } while (! Thread.currentThread().isInterrupted());
            }}, "parkThread");
        threadStarted.await();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeInterrupt");
        t.interrupt();
        t.join();
        //awaitTermination(t, MEDIUM_DELAY_MS);
    }

    /**
     * parkUntil is released by subsequent interrupt
     */
    public void testParkUntilBeforeInterrupt() throws InterruptedException {
        final CountDownLatch threadStarted = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                long d = new Date().getTime() + LONG_DELAY_MS;
                long nanos = LONG_DELAY_MS * 1000L * 1000L;
                long t0 = System.nanoTime();
                assertFalse(Thread.currentThread().isInterrupted());
                threadStarted.countDown();
                do {
                    LockSupport.parkUntil(d);
                    // parkUntil may return spuriously
                } while (! Thread.currentThread().isInterrupted());
                assertTrue(System.nanoTime() - t0 < nanos);
            }});

        threadStarted.await();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        awaitTermination(t, MEDIUM_DELAY_MS);
    }

    /**
     * parkNanos is released by subsequent interrupt
     */
    public void testParkNanosBeforeInterrupt() throws InterruptedException {
        final CountDownLatch threadStarted = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                long nanos = LONG_DELAY_MS * 1000L * 1000L;
                long t0 = System.nanoTime();
                assertFalse(Thread.currentThread().isInterrupted());
                threadStarted.countDown();
                do {
                    LockSupport.parkNanos(nanos);
                    // parkNanos may return spuriously
                } while (! Thread.currentThread().isInterrupted());
                assertTrue(System.nanoTime() - t0 < nanos);
            }});

        threadStarted.await();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        awaitTermination(t, MEDIUM_DELAY_MS);
    }

    /**
     * park is released by preceding interrupt
     */
    //WAIT@Test
    //WAIT@Schedule(name = "parkAfterInterrupt", value = "afterInterrupt@main->beforePark@parkThread")
    public void testParkAfterInterrupt() throws Exception {
        final CountDownLatch threadStarted = new CountDownLatch(1);
        final AtomicBoolean unparked = new AtomicBoolean(false);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws Exception {
                threadStarted.countDown();
                //OPWAIT while (!unparked.get())
                //OPWAIT    Thread.yield();
                //Thread.sleep(SHORT_DELAY_MS); //INS-SLEEP
                fireEvent("beforePark");
                assertTrue("parkAfterInterrupt", Thread.currentThread().isInterrupted());
                LockSupport.park();
                fireEvent("afterPark");
                assertTrue("parkAfterInterrupt", Thread.currentThread().isInterrupted());
            }}, "parkThread");
        threadStarted.await();
        t.interrupt();
        fireEvent("afterInterrupt");
        //OPWAIT unparked.set(true);
        t.join();
        //awaitTermination(t, MEDIUM_DELAY_MS);
    }

    /**
     * parkUntil is released by preceding interrupt
     */
    public void testParkUntilAfterInterrupt() throws Exception {
        final CountDownLatch threadStarted = new CountDownLatch(1);
        final AtomicBoolean unparked = new AtomicBoolean(false);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws Exception {
                threadStarted.countDown();
                while (!unparked.get())
                    Thread.yield();
                long d = new Date().getTime() + LONG_DELAY_MS;
                long nanos = LONG_DELAY_MS * 1000L * 1000L;
                long t0 = System.nanoTime();
                assertTrue(Thread.currentThread().isInterrupted());
                LockSupport.parkUntil(d);
                assertTrue(System.nanoTime() - t0 < nanos);
                assertTrue(Thread.currentThread().isInterrupted());
            }});

        threadStarted.await();
        t.interrupt();
        unparked.set(true);
        awaitTermination(t, MEDIUM_DELAY_MS);
    }

    /**
     * parkNanos is released by preceding interrupt
     */
    public void testParkNanosAfterInterrupt() throws Exception {
        final CountDownLatch threadStarted = new CountDownLatch(1);
        final AtomicBoolean unparked = new AtomicBoolean(false);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws Exception {
                threadStarted.countDown();
                while (!unparked.get())
                    Thread.yield();
                long nanos = LONG_DELAY_MS * 1000L * 1000L;
                long t0 = System.nanoTime();
                assertTrue(Thread.currentThread().isInterrupted());
                LockSupport.parkNanos(nanos);
                assertTrue(System.nanoTime() - t0 < nanos);
                assertTrue(Thread.currentThread().isInterrupted());
            }});

        threadStarted.await();
        t.interrupt();
        unparked.set(true);
        awaitTermination(t, MEDIUM_DELAY_MS);
    }

    /**
     * parkNanos times out if not unparked
     */
    public void testParkNanosTimesOut() throws InterruptedException {
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                for (;;) {
                    long timeoutNanos = SHORT_DELAY_MS * 1000L * 1000L;
                    long t0 = System.nanoTime();
                    LockSupport.parkNanos(timeoutNanos);
                    // parkNanos may return spuriously
                    if (System.nanoTime() - t0 >= timeoutNanos)
                        return;
                }
            }});

        awaitTermination(t, MEDIUM_DELAY_MS);
    }


    /**
     * parkUntil times out if not unparked
     */
    public void testParkUntilTimesOut() throws InterruptedException {
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                for (;;) {
                    long d = new Date().getTime() + SHORT_DELAY_MS;
                    // beware of rounding
                    long timeoutNanos = (SHORT_DELAY_MS - 1) * 1000L * 1000L;
                    long t0 = System.nanoTime();
                    LockSupport.parkUntil(d);
                    // parkUntil may return spuriously
                    if (System.nanoTime() - t0 >= timeoutNanos)
                        return;
                }
            }});

        awaitTermination(t, MEDIUM_DELAY_MS);
    }

    /**
     * parkUntil(0) returns immediately
     * Requires hotspot fix for:
     * 6763959 java.util.concurrent.locks.LockSupport.parkUntil(0) blocks forever
     */
    public void XXXXtestParkUntil0Returns() throws InterruptedException {
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                LockSupport.parkUntil(0L);
            }});

        awaitTermination(t, MEDIUM_DELAY_MS);
    }
}
