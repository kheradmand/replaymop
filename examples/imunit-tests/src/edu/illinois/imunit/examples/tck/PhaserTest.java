package edu.illinois.imunit.examples.tck;
import org.junit.runner.RunWith;
import edu.illinois.imunit.IMUnitRunner;
import static edu.illinois.imunit.IMUnit.fireEvent;
/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 * Other contributors include John Vint
 */

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import org.junit.Test;

import edu.illinois.imunit.Schedule;

@RunWith(IMUnitRunner.class)
public class PhaserTest extends JSR166TestCase {

//    public static void main(String[] args) {
//        junit.textui.TestRunner.run(suite());
//    }
//
//    public static Test suite() {
//        return new TestSuite(PhaserTest.class);
//    }

    /**
     * arriveAndDeregister does not wait for others to arrive at barrier
     */
    @Test
    @Schedule(name = "arrive2", value = "afterCheck@main->beforeArrive@arriveThread")
    public void testArrive2() throws InterruptedException {
        final Phaser phaser = new Phaser(1);
        phaser.register();
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < 10; i++)
            phaser.register();
            threads.add(newStartedThread(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    //Thread.sleep(SMALL_DELAY_MS);
                    fireEvent("beforeArrive");
                    phaser.arriveAndDeregister();
                }}, "arriveThread"));
            
        phaser.arrive();
        assertTrue("arrive2", threads.get(0).isAlive());
        assertFalse("arrive2", phaser.isTerminated());
        fireEvent("afterCheck");
        for (Thread thread : threads)
            thread.join();
    }

    /**
     * arriveAndDeregister returns the phase in which it leaves the
     * phaser in after deregistration
     */
    @Test
    @Schedule(name = "arriveAndDeregister", value = "[beforeWait:afterWait]@main->beforeArrive@arriveThread")
    public void testArriveAndDeregister6() throws InterruptedException {
        final Phaser phaser = new Phaser(2);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                sleepTillInterrupted(SHORT_DELAY_MS);
                fireEvent("beforeArrive");
                phaser.arrive();
            }}, "arriveThread");
        fireEvent("beforeWait");
        phaser.arriveAndAwaitAdvance();
        fireEvent("afterWait");
        int phase = phaser.arriveAndDeregister();
        assertEquals("arriveAndDeregister", phase, phaser.getPhase());
        t.join();
    }

    /**
     * awaitAdvance while waiting does not abort on interrupt.
     */
    //WAIT@Test
    //WAIT@Schedule(name = "awaitAdvance3", value = "[beforeArrive:afterArrive]@arriveThread->beforeInterrupt@main")
    public void testAwaitAdvance3() throws InterruptedException {
        final Phaser phaser = new Phaser();
        phaser.register();
        final CountDownLatch threadStarted = new CountDownLatch(1);

        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                phaser.register();
                //OPWAIT threadStarted.countDown();
                fireEvent("beforeArrive");
                phaser.awaitAdvance(phaser.arrive());
                fireEvent("afterArrive");
                assertTrue("awaitAdvance3", Thread.currentThread().isInterrupted());
            }}, "arriveThread");
        //OPWAIT assertTrue(threadStarted.await(SMALL_DELAY_MS, MILLISECONDS));
        fireEvent("beforeInterrupt");
        t.interrupt();
        phaser.arrive();
        awaitTermination(t, SMALL_DELAY_MS);
    }

    /**
     * awaitAdvance atomically waits for all parties within the same phase to
     * complete before continuing
     */
    public void testAwaitAdvance4() throws InterruptedException {
        final Phaser phaser = new Phaser(4);
        final AtomicInteger phaseCount = new AtomicInteger(0);
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < 4; i++) {
            threads.add(newStartedThread(new CheckedRunnable() {
                public void realRun() {
                    int phase = phaser.arrive();
                    phaseCount.incrementAndGet();
                    sleepTillInterrupted(SMALL_DELAY_MS);
                    phaser.awaitAdvance(phase);
                    assertEquals(phaseCount.get(), 4);
                }}));
        }
        for (Thread thread : threads)
            thread.join();
    }

    /**
     * awaitAdvance returns the current phase
     */
    public void testAwaitAdvance5() throws InterruptedException {
        final Phaser phaser = new Phaser(1);
        int phase = phaser.awaitAdvance(phaser.arrive());
        assertEquals(phase, phaser.getPhase());
        phaser.register();
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < 8; i++) {
            final CountDownLatch latch = new CountDownLatch(1);
            final boolean goesFirst = ((i & 1) == 0);
            threads.add(newStartedThread(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    if (goesFirst)
                        latch.countDown();
                    else
                        assertTrue(latch.await(SMALL_DELAY_MS, MILLISECONDS));
                    phaser.arrive();
                }}));
            if (goesFirst)
                assertTrue(latch.await(SMALL_DELAY_MS, MILLISECONDS));
            else
                latch.countDown();
            phase = phaser.awaitAdvance(phaser.arrive());
            assertEquals(phase, phaser.getPhase());
        }
        for (Thread thread : threads)
            awaitTermination(thread, SMALL_DELAY_MS);
    }

    /**
     * awaitAdvance returns when the phaser is externally terminated
     */
    public void testAwaitAdvance6() throws InterruptedException {
        final Phaser phaser = new Phaser(3);
        final CountDownLatch threadsStarted = new CountDownLatch(2);
        final List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < 2; i++) {
            Runnable r = new CheckedRunnable() {
                public void realRun() {
                    int p1 = phaser.arrive();
                    assertTrue(p1 >= 0);
                    threadsStarted.countDown();
                    int phase = phaser.awaitAdvance(p1);
                    assertTrue(phase < 0);
                    assertTrue(phaser.isTerminated());
                }};
            threads.add(newStartedThread(r));
        }
        threadsStarted.await();
        phaser.forceTermination();
        for (Thread thread : threads)
            awaitTermination(thread, SMALL_DELAY_MS);
    }

    /**
     * arriveAndAwaitAdvance throws IllegalStateException with no
     * unarrived parties
     */
    public void testArriveAndAwaitAdvance1() {
        try {
            Phaser phaser = new Phaser();
            phaser.arriveAndAwaitAdvance();
            shouldThrow();
        } catch (IllegalStateException success) {}
    }

    /**
     * Interrupted arriveAndAwaitAdvance does not throw InterruptedException
     */
    public void testArriveAndAwaitAdvance2() throws InterruptedException {
        final Phaser phaser = new Phaser(2);
        final CountDownLatch threadStarted = new CountDownLatch(1);
        final AtomicBoolean advanced = new AtomicBoolean(false);
        final AtomicBoolean checkedInterruptStatus = new AtomicBoolean(false);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                threadStarted.countDown();
                phaser.arriveAndAwaitAdvance();
                advanced.set(true);
                assertTrue(Thread.currentThread().isInterrupted());
                while (!checkedInterruptStatus.get())
                    Thread.yield();
            }});

        assertTrue(threadStarted.await(SMALL_DELAY_MS, MILLISECONDS));
        t.interrupt();
        phaser.arrive();
        while (!advanced.get())
            Thread.yield();
        assertTrue(t.isInterrupted());
        checkedInterruptStatus.set(true);
        awaitTermination(t, SMALL_DELAY_MS);
    }

    /**
     * arriveAndAwaitAdvance waits for all threads to arrive, the
     * number of arrived parties is the same number that is accounted
     * for when the main thread awaitsAdvance
     */
    public void testArriveAndAwaitAdvance3() throws InterruptedException {
        final Phaser phaser = new Phaser(1);
        final List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < 3; i++) {
            threads.add(newStartedThread(new CheckedRunnable() {
                    public void realRun() throws InterruptedException {
                        phaser.register();
                        phaser.arriveAndAwaitAdvance();
                    }}, "thread"+i));
        }
        Thread.sleep(MEDIUM_DELAY_MS);
        assertEquals(phaser.getArrivedParties(), 3);
        phaser.arriveAndAwaitAdvance();
        for (Thread thread : threads)
            thread.join();
    }

}
