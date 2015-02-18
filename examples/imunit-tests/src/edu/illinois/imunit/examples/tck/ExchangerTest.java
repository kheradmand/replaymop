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
public class ExchangerTest extends JSR166TestCase {

//    public static void main(String[] args) {
//        junit.textui.TestRunner.run(suite());
//    }
//    public static Test suite() {
//        return new TestSuite(ExchangerTest.class);
//    }

    /**
     * exchange exchanges objects across two threads
     */
    public void testExchange() throws InterruptedException {
        final Exchanger e = new Exchanger();
        Thread t1 = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                assertSame(one, e.exchange(two));
                assertSame(two, e.exchange(one));
            }});
        Thread t2 = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                assertSame(two, e.exchange(one));
                assertSame(one, e.exchange(two));
            }});

        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }

    /**
     * timed exchange exchanges objects across two threads
     */
    public void testTimedExchange() throws InterruptedException {
        final Exchanger e = new Exchanger();
        Thread t1 = new Thread(new CheckedRunnable() {
            public void realRun() throws Exception {
                assertSame(one, e.exchange(two, SHORT_DELAY_MS, MILLISECONDS));
                assertSame(two, e.exchange(one, SHORT_DELAY_MS, MILLISECONDS));
            }});
        Thread t2 = new Thread(new CheckedRunnable() {
            public void realRun() throws Exception {
                assertSame(two, e.exchange(one, SHORT_DELAY_MS, MILLISECONDS));
                assertSame(one, e.exchange(two, SHORT_DELAY_MS, MILLISECONDS));
            }});

        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }

    /**
     * interrupt during wait for exchange throws IE
     */
    @Test
    @Schedule(name = "exchangeInterruptedException", value = "[beforeExchange:afterExchange]@exchangeThread->beforeInterrupt@main")
    public void testExchangeInterruptedException() throws InterruptedException {
        final Exchanger e = new Exchanger();
        Thread t = new Thread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
              fireEvent("beforeExchange");
                e.exchange(one);
              fireEvent("afterExchange");  
            }}, "exchangeThread");

        t.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeInterrupt"); 
        t.interrupt();
        t.join();
    }

    /**
     * interrupt during wait for timed exchange throws IE
     */
    public void testTimedExchange_InterruptedException() throws InterruptedException {
        final Exchanger e = new Exchanger();
        Thread t = new Thread(new CheckedInterruptedRunnable() {
            public void realRun() throws Exception {
                e.exchange(null, SMALL_DELAY_MS, MILLISECONDS);
            }});

        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        t.join();
    }

    /**
     * timeout during wait for timed exchange throws TOE
     */
    public void testExchange_TimeOutException() throws InterruptedException {
        final Exchanger e = new Exchanger();
        Thread t = new ThreadShouldThrow(TimeoutException.class) {
            public void realRun() throws Exception {
                e.exchange(null, SHORT_DELAY_MS, MILLISECONDS);
            }};

        t.start();
        t.join();
    }

    /**
     * If one exchanging thread is interrupted, another succeeds.
     */
    @Test
    @Schedule(name = "replacementAfterExchange", value = "[beforeSecondExchange:afterSecondExchange]@t1->beforeInterrupt@main"
      + ", edu.illinois.imunit.examples.tck.JSR166TestCase.interruptedException@t1->beforeFirstExchange@t3, edu.illinois.imunit.examples.tck.JSR166TestCase.interruptedException@t1->beforeSecondExchange@t2")
    public void testReplacementAfterExchange() throws InterruptedException {
        final Exchanger e = new Exchanger();
        Thread t1 = new Thread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                assertSame("replacementAfterExchange", two, e.exchange(one));
                fireEvent("beforeSecondExchange");
                e.exchange(two);
                fireEvent("afterSecondExchange"); 
            }}, "t1");
        Thread t2 = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
              assertSame("replacementAfterExchange",one, e.exchange(two));
                //Thread.sleep(SMALL_DELAY_MS);  
                fireEvent("beforeSecondExchange");
              assertSame("replacementAfterExchange",three, e.exchange(one));
            }}, "t2");
        Thread t3 = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                //Thread.sleep(SMALL_DELAY_MS);
                fireEvent("beforeFirstExchange");
              assertSame("replacementAfterExchange",one, e.exchange(three));
            }}, "t3");

        t1.start();
        t2.start();
        t3.start();
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeInterrupt"); 
        t1.interrupt();
        fireEvent("afterInterrupt"); 
        t1.join();
        t2.join();
        t3.join();
    }

}
