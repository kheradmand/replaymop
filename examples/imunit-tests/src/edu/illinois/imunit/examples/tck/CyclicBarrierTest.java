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
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import edu.illinois.imunit.Schedule;
import edu.illinois.imunit.Schedules;


@RunWith(IMUnitRunner.class)
public class CyclicBarrierTest extends JSR166TestCase {
//    public static void main(String[] args) {
//        junit.textui.TestRunner.run(suite());
//    }
//    public static Test suite() {
//        return new TestSuite(CyclicBarrierTest.class);
//    }

    private volatile int countAction;
    private class MyAction implements Runnable {
        public void run() { ++countAction; }
    }

    /**
     * Creating with negative parties throws IAE
     */
    public void testConstructor1() {
        try {
            new CyclicBarrier(-1, (Runnable)null);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Creating with negative parties and no action throws IAE
     */
    public void testConstructor2() {
        try {
            new CyclicBarrier(-1);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * getParties returns the number of parties given in constructor
     */
    public void testGetParties() {
        CyclicBarrier b = new CyclicBarrier(2);
        assertEquals(2, b.getParties());
        assertEquals(0, b.getNumberWaiting());
    }

    /**
     * A 1-party barrier triggers after single await
     */
    public void testSingleParty() throws Exception {
        CyclicBarrier b = new CyclicBarrier(1);
        assertEquals(1, b.getParties());
        assertEquals(0, b.getNumberWaiting());
        b.await();
        b.await();
        assertEquals(0, b.getNumberWaiting());
    }

    /**
     * The supplied barrier action is run at barrier
     */
    public void testBarrierAction() throws Exception {
        countAction = 0;
        CyclicBarrier b = new CyclicBarrier(1, new MyAction());
        assertEquals(1, b.getParties());
        assertEquals(0, b.getNumberWaiting());
        b.await();
        b.await();
        assertEquals(0, b.getNumberWaiting());
        assertEquals(countAction, 2);
    }

    /**
     * A 2-party/thread barrier triggers after both threads invoke await
     */
    public void testTwoParties() throws Exception {
        final CyclicBarrier b = new CyclicBarrier(2);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws Exception {
                b.await();
                b.await();
                b.await();
                b.await();
            }});

        t.start();
        b.await();
        b.await();
        b.await();
        b.await();
        t.join();
    }


    /**
     * An interruption in one party causes others waiting in await to
     * throw BrokenBarrierException
     */
    @Test
    @Schedule(name = "await1InterruptedBrokenBarrier", value = "[beforeAwait:afterAwait]@t1->beforeInterrupt@main" + 
        ",[beforeAwait:afterAwait]@t2->beforeInterrupt@main")
    public void testAwait1InterruptedBrokenBarrier() throws Exception {
        final CyclicBarrier c = new CyclicBarrier(3);
        Thread t1 = new ThreadShouldThrow(InterruptedException.class) {
            public void realRun() throws Exception {
                fireEvent("beforeAwait");
                c.await();
                fireEvent("afterAwait");
            }};
        Thread t2 = new ThreadShouldThrow(BrokenBarrierException.class) {
            public void realRun() throws Exception {
                fireEvent("beforeAwait");
                c.await();
                fireEvent("afterAwait");
            }};
        t1.setName("t1");
        t2.setName("t2");
        t1.start();
        t2.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeInterrupt");
        t1.interrupt();
        t1.join();
        t2.join();
    }

    /**
     * An interruption in one party causes others waiting in timed await to
     * throw BrokenBarrierException
     */
    public void testAwait2_Interrupted_BrokenBarrier() throws Exception {
        final CyclicBarrier c = new CyclicBarrier(3);
        Thread t1 = new ThreadShouldThrow(InterruptedException.class) {
            public void realRun() throws Exception {
                c.await(LONG_DELAY_MS, MILLISECONDS);
            }};
        Thread t2 = new ThreadShouldThrow(BrokenBarrierException.class) {
            public void realRun() throws Exception {
                c.await(LONG_DELAY_MS, MILLISECONDS);
            }};

        t1.start();
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        t1.interrupt();
        t1.join();
        t2.join();
    }

    /**
     * A timeout in timed await throws TimeoutException
     */
    public void testAwait3_TimeOutException() throws InterruptedException {
        final CyclicBarrier c = new CyclicBarrier(2);
        Thread t = new ThreadShouldThrow(TimeoutException.class) {
            public void realRun() throws Exception {
                c.await(SHORT_DELAY_MS, MILLISECONDS);
            }};

        t.start();
        t.join();
    }

    /**
     * A timeout in one party causes others waiting in timed await to
     * throw BrokenBarrierException
     */
    public void testAwait4_Timeout_BrokenBarrier() throws InterruptedException {
        final CyclicBarrier c = new CyclicBarrier(3);
        Thread t1 = new ThreadShouldThrow(TimeoutException.class) {
            public void realRun() throws Exception {
                c.await(SHORT_DELAY_MS, MILLISECONDS);
            }};
        Thread t2 = new ThreadShouldThrow(BrokenBarrierException.class) {
            public void realRun() throws Exception {
                c.await(MEDIUM_DELAY_MS, MILLISECONDS);
            }};

        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }

    /**
     * A timeout in one party causes others waiting in await to
     * throw BrokenBarrierException
     */
    public void testAwait5_Timeout_BrokenBarrier() throws InterruptedException {
        final CyclicBarrier c = new CyclicBarrier(3);
        Thread t1 = new ThreadShouldThrow(TimeoutException.class) {
            public void realRun() throws Exception {
                c.await(SHORT_DELAY_MS, MILLISECONDS);
            }};
        Thread t2 = new ThreadShouldThrow(BrokenBarrierException.class) {
            public void realRun() throws Exception {
                c.await();
            }};

        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }

    /**
     * A reset of an active barrier causes waiting threads to throw
     * BrokenBarrierException
     */
    @Test
    @Schedule(name = "resetBrokenBarrier", value = "[beforeAwait:afterAwait]@t1->beforeReset@main" + 
        ",[beforeAwait:afterAwait]@t2->beforeReset@main")
    public void testResetBrokenBarrier() throws InterruptedException {
        final CyclicBarrier c = new CyclicBarrier(3);
        Thread t1 = new ThreadShouldThrow(BrokenBarrierException.class) {
            public void realRun() throws Exception {
              fireEvent("beforeAwait");
              c.await();
              fireEvent("afterAwait");
            }};
        Thread t2 = new ThreadShouldThrow(BrokenBarrierException.class) {
            public void realRun() throws Exception {
              fireEvent("beforeAwait");
              c.await();
              fireEvent("afterAwait");
            }};
        t1.setName("t1");
        t2.setName("t2");
        t1.start();
        t2.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeReset");
        c.reset();
        t1.join();
        t2.join();
    }

    /**
     * All threads block while a barrier is broken.
     */
    //Events in a loop_Qingzhou 
    //How do we count/encode events in a while loop?
    //If the number of iterations is large, can we write a loop in our schedule?
    //@NSchedule(name="resetLeakage1", value="[beforeWait0:afterWait0]@t->beforeInterrupt0@main, interrupted0@t->afterInterrupt0@main"
    // +"[beforeWait1:afterWait1]@t->beforeInterrupt1@main, interrupted1@t->afterInterrupt1@main"+
    // +"[beforeWait2:afterWait2]@t->beforeInterrupt2@main, interrupted2@t->afterInterrupt2@main"+
    //  "[beforeWait3:afterWait3]@t->beforeInterrupt3@main, interrupted3@t->afterInterrupt3@main")
    public void testReset_Leakage1() throws InterruptedException {
        final CyclicBarrier c = new CyclicBarrier(2);
        final AtomicBoolean done = new AtomicBoolean();
        Thread t = new Thread() {
                public void run() {
                    while (!done.get()) {
                        try {
                            while (c.isBroken())
                                c.reset();
                            /* @NEvent("beforeWait")*/
                            c.await();
                            /* @NEvent("afterWait")*/
                            threadFail("await should not return");
                        }
                        catch (BrokenBarrierException e) {
                        }
                        catch (InterruptedException ie) {
                           /* @NEvent("interrupted")*/
                        }
                    }
                }
            };
        t.setName("t");
        t.start();
        for (int i = 0; i < 4; i++) {
            Thread.sleep(SHORT_DELAY_MS);
            /* @NEvent("beforeInterrupt" + i)*/
            t.interrupt();
            /* @NEvent("afterInterrupt" + i)*/
        }
        done.set(true);
        t.interrupt();
        t.join();
    }

    //Events in a loop_Qingzhou
    //@NSchedule(name="resetLeakage1", value="[beforeWait:afterWait]@t->beforeInterrupt@main, interrupted@t->afterInterrupt@main"
    // +"[beforeWait:afterWait]@t->beforeInterrupt@main, interrupted@t->afterInterrupt@main"+
    // +"[beforeWait:afterWait]@t->beforeInterrupt@main, interrupted@t->afterInterrupt@main"+
    //  "[beforeWait:afterWait]@t->beforeInterrupt@main, interrupted@t->afterInterrupt@main")
    public void testReset_Leakage2() throws InterruptedException {
        final CyclicBarrier c = new CyclicBarrier(2);
        final AtomicBoolean done = new AtomicBoolean();
        Thread t = new Thread() {
                public void run() {
                    while (!done.get()) {
                        try {
                            while (c.isBroken())
                                c.reset();

                            /* @NEvent("beforeWait")*/
                            c.await();
                            /* @NEvent("afterWait")*/
                            threadFail("await should not return");
                        }
                        catch (BrokenBarrierException e) {
                        }
                        catch (InterruptedException ie) {
                            /* @NEvent("interrupted")*/
                        }
                    }
                }
            };
        t.setName("t");
        t.start();
        for (int i = 0; i < 4; i++) {
            Thread.sleep(SHORT_DELAY_MS);
            /* @NEvent("beforeInterrupt")*/
            t.interrupt();
            /* @NEvent("afterInterrupt")*/
        }
        done.set(true);
        t.interrupt();
        t.join();
    }

    /**
     * Reset of a non-broken barrier does not break barrier
     */
    public void testResetWithoutBreakage() throws Exception {
        final CyclicBarrier start = new CyclicBarrier(3);
        final CyclicBarrier barrier = new CyclicBarrier(3);
        for (int i = 0; i < 3; i++) {
            Thread t1 = new Thread(new CheckedRunnable() {
                public void realRun() throws Exception {
                    start.await();
                    barrier.await();
                }});

            Thread t2 = new Thread(new CheckedRunnable() {
                public void realRun() throws Exception {
                    start.await();
                    barrier.await();
                }});

            t1.start();
            t2.start();
            start.await();
            barrier.await();
            t1.join();
            t2.join();
            assertFalse(barrier.isBroken());
            assertEquals(0, barrier.getNumberWaiting());
            if (i == 1) barrier.reset();
            assertFalse(barrier.isBroken());
            assertEquals(0, barrier.getNumberWaiting());
        }
    }

    /**
     * Reset of a barrier after interruption reinitializes it.
     */
    public void testResetAfterInterrupt() throws Exception {
        final CyclicBarrier start = new CyclicBarrier(3);
        final CyclicBarrier barrier = new CyclicBarrier(3);
        for (int i = 0; i < 2; i++) {
            Thread t1 = new ThreadShouldThrow(InterruptedException.class) {
                public void realRun() throws Exception {
                    start.await();
                    barrier.await();
                }};

            Thread t2 = new ThreadShouldThrow(BrokenBarrierException.class) {
                public void realRun() throws Exception {
                    start.await();
                    barrier.await();
                }};
                
            t1.start();
            t2.start();
            start.await();
            t1.interrupt();
            t1.join();
            t2.join();
            assertTrue(barrier.isBroken());
            assertEquals(0, barrier.getNumberWaiting());
            barrier.reset();
            assertFalse(barrier.isBroken());
            assertEquals(0, barrier.getNumberWaiting());
        }
    }

    /**
     * Reset of a barrier after timeout reinitializes it.
     */
    public void testResetAfterTimeout() throws Exception {
        final CyclicBarrier start = new CyclicBarrier(2);
        final CyclicBarrier barrier = new CyclicBarrier(3);
        for (int i = 0; i < 2; i++) {
            Thread t1 = new ThreadShouldThrow(TimeoutException.class) {
                    public void realRun() throws Exception {
                        start.await();
                        barrier.await(SHORT_DELAY_MS, MILLISECONDS);
                    }};

            Thread t2 = new ThreadShouldThrow(BrokenBarrierException.class) {
                public void realRun() throws Exception {
                    start.await();
                    barrier.await();
                }};

            t1.start();
            t2.start();
            t1.join();
            t2.join();
            assertTrue(barrier.isBroken());
            assertEquals(0, barrier.getNumberWaiting());
            barrier.reset();
            assertFalse(barrier.isBroken());
            assertEquals(0, barrier.getNumberWaiting());
        }
    }


    /**
     * Reset of a barrier after a failed command reinitializes it.
     */
//    Event inside loop_Qingzhou    
//    @NTest
//    @NSchedule(name = "resetAfterCommandException_1", value = "[beforeAwait:afterAwait]@t10->beforeBarrierAwait0@main" + 
//        ",[beforeAwait:afterAwait]@t20->beforeBarrierAwait0@main" + ", [beforeAwait:afterAwait]@t11->beforeBarrierAwait1@main" + 
//        ",[beforeAwait:afterAwait]@t21->beforeBarrierAwait1@main")
    public void testResetAfterCommandException_1() throws Exception {
        final CyclicBarrier start = new CyclicBarrier(3);
        final CyclicBarrier barrier =
            new CyclicBarrier(3, new Runnable() {
                    public void run() {
                        throw new NullPointerException(); }});
        for (int i = 0; i < 2; i++) {
            Thread t1 = new ThreadShouldThrow(BrokenBarrierException.class) {
                public void realRun() throws Exception {
                    start.await();
                    /* @NEvent("beforeAwait") */
                    barrier.await();
                    /* @NEvent("afterAwait") */
                }};
                
            Thread t2 = new ThreadShouldThrow(BrokenBarrierException.class) {
                public void realRun() throws Exception {
                    start.await();
                    /* @NEvent("beforeAwait") */
                    barrier.await();
                    /* @NEvent("afterAwait") */
                }};
            t1.setName("t1" + i);    
            t2.setName("t2" + i);
            t1.start();
            t2.start();
            start.await();
            //OPWAIT while (barrier.getNumberWaiting() < 2) { Thread.yield(); }
            //Thread.sleep(SMALL_DELAY_MS); //INS-SLEEP
            /* @NEvent("beforeBarrierAwait" + i) */
            try {
                barrier.await();
                shouldThrow();
            } catch (NullPointerException success) {}
            t1.join();
            t2.join();
            assertTrue("resetAfterCommandException", barrier.isBroken());
            assertEquals("resetAfterCommandException", 0, barrier.getNumberWaiting());
            barrier.reset();
            assertFalse("resetAfterCommandException", barrier.isBroken());
            assertEquals("resetAfterCommandException", 0, barrier.getNumberWaiting());
        }
    }

//    Event inside loop_Qingzhou    
//    @NTest
//    @NSchedule(name = "resetAfterCommandException_2", value = "[beforeAwait:afterAwait]@t10->beforeBarrierAwait@main" + 
//        ",[beforeAwait:afterAwait]@t20->beforeBarrierAwait@main" + ", [beforeAwait:afterAwait]@t11->beforeBarrierAwait@main" + 
//        ",[beforeAwait:afterAwait]@t21->beforeBarrierAwait@main")
    public void testResetAfterCommandException_2() throws Exception {
        final CyclicBarrier start = new CyclicBarrier(3);
        final CyclicBarrier barrier =
            new CyclicBarrier(3, new Runnable() {
                    public void run() {
                        throw new NullPointerException(); }});
        for (int i = 0; i < 2; i++) {
            Thread t1 = new ThreadShouldThrow(BrokenBarrierException.class) {
                public void realRun() throws Exception {
                    start.await();
                    /* @NEvent("beforeAwait") */
                    barrier.await();
                    /* @NEvent("afterAwait") */
                }};
                
            Thread t2 = new ThreadShouldThrow(BrokenBarrierException.class) {
                public void realRun() throws Exception {
                    start.await();
                    /* @NEvent("beforeAwait") */
                    barrier.await();
                    /* @NEvent("afterAwait") */
                }};
            t1.setName("t1" + i);    
            t2.setName("t2" + i);
            t1.start();
            t2.start();
            start.await();
            //OPWAIT while (barrier.getNumberWaiting() < 2) { Thread.yield(); }
            //Thread.sleep(SMALL_DELAY_MS); //INS-SLEEP
            /* @NEvent("beforeBarrierAwait") */
            try {
                barrier.await();
                shouldThrow();
            } catch (NullPointerException success) {}
            t1.join();
            t2.join();
            assertTrue("resetAfterCommandException", barrier.isBroken());
            assertEquals("resetAfterCommandException", 0, barrier.getNumberWaiting());
            barrier.reset();
            assertFalse("resetAfterCommandException", barrier.isBroken());
            assertEquals("resetAfterCommandException", 0, barrier.getNumberWaiting());
        }
    }

}
