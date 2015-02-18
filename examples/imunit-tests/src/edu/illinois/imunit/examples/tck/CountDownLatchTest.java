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
import edu.illinois.imunit.Schedule;
import edu.illinois.imunit.Schedules;



@RunWith(IMUnitRunner.class)
public class CountDownLatchTest extends JSR166TestCase {
    //public static void main(String[] args) {
    //    junit.textui.TestRunner.run(suite());
    //}
    //public static Test suite() {
    //    return new TestSuite(CountDownLatchTest.class);
    //}

    /**
     * negative constructor argument throws IAE
     */
    public void testConstructor() {
        try {
            new CountDownLatch(-1);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * await returns after countDown to zero, but not before
     */
    @Test
    @Schedule(name = "awaitThreads", value = "[beforeAwait:afterAwait]@waitThread->beforeCountDown@main")
    public void testAwait() throws InterruptedException {
        final CountDownLatch l = new CountDownLatch(2);

        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                assertTrue(l.getCount() > 0);
                fireEvent("beforeAwait");
                l.await();
                fireEvent("afterAwait");
                assertEquals(0, l.getCount());
            }}, "waitThread");

        t.start();
        assertEquals("awaitThreads",l.getCount(), 2);
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeCountDown");
        l.countDown();
        assertEquals("awaitThreads",l.getCount(), 1);
        l.countDown();
        assertEquals("awaitThreads",l.getCount(), 0);
        t.join();
    }


    /**
     * timed await returns after countDown to zero
     */
    public void testTimedAwait() throws InterruptedException {
        final CountDownLatch l = new CountDownLatch(2);

        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                assertTrue(l.getCount() > 0);
                assertTrue(l.await(SMALL_DELAY_MS, MILLISECONDS));
            }});

        t.start();
        assertEquals(l.getCount(), 2);
        Thread.sleep(SHORT_DELAY_MS);
        l.countDown();
        assertEquals(l.getCount(), 1);
        l.countDown();
        assertEquals(l.getCount(), 0);
        t.join();
    }

    /**
     * await throws IE if interrupted before counted down
     */
    @Test
    @Schedule(name = "awaitInterruptThreads", value = "[beforeAwait:afterAwait]@awaitThread->beforeInterrupt@main")
    public void testAwaitInterruptedException() throws InterruptedException {
        final CountDownLatch l = new CountDownLatch(1);
        Thread t = new Thread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                assertTrue(l.getCount() > 0);
                fireEvent("beforeAwait");
                l.await();
                fireEvent("afterAwait");
            }},"awaitThread");

        t.start();
        //Thread.sleep(SHORT_DELAY_MS);
        assertEquals(l.getCount(), 1);
        fireEvent("beforeInterrupt");
        t.interrupt();
        t.join();
    }

    /**
     * timed await throws IE if interrupted before counted down
     */
    public void testTimedAwait_InterruptedException() throws InterruptedException {
        final CountDownLatch l = new CountDownLatch(1);
        Thread t = new Thread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                assertTrue(l.getCount() > 0);
                l.await(MEDIUM_DELAY_MS, MILLISECONDS);
            }});

        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        assertEquals(l.getCount(), 1);
        t.interrupt();
        t.join();
    }

    /**
     * timed await times out if not counted down before timeout
     */
    public void testAwaitTimeout() throws InterruptedException {
        final CountDownLatch l = new CountDownLatch(1);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                assertTrue(l.getCount() > 0);
                assertFalse(l.await(SHORT_DELAY_MS, MILLISECONDS));
                assertTrue(l.getCount() > 0);
            }});

        t.start();
        assertEquals(l.getCount(), 1);
        t.join();
    }

    /**
     * toString indicates current count
     */
    public void testToString() {
        CountDownLatch s = new CountDownLatch(2);
        String us = s.toString();
        assertTrue(us.indexOf("Count = 2") >= 0);
        s.countDown();
        String s1 = s.toString();
        assertTrue(s1.indexOf("Count = 1") >= 0);
        s.countDown();
        String s2 = s.toString();
        assertTrue(s2.indexOf("Count = 0") >= 0);
    }

}
