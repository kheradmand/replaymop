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

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.*;
import org.junit.Test;
import edu.illinois.imunit.Schedule;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@RunWith(IMUnitRunner.class)
public class LinkedTransferQueueTest extends JSR166TestCase {

//    public static void main(String[] args) {
//        junit.textui.TestRunner.run(suite());
//    }
//
//    public static Test suite() {
//        return newTestSuite(LinkedTransferQueueTest.class,
//                            new Generic().testSuite());
//    }

    void checkEmpty(LinkedTransferQueue q, String scheduleName) throws InterruptedException {
        assertTrue(scheduleName, q.isEmpty());
        assertEquals(scheduleName, 0, q.size());
        assertNull(scheduleName, q.peek());
        assertNull(scheduleName, q.poll());
        assertNull(scheduleName, q.poll(0, MILLISECONDS));
        assertEquals(scheduleName, q.toString(), "[]");
        assertTrue(scheduleName, Arrays.equals(q.toArray(), new Object[0]));
        assertFalse(scheduleName, q.iterator().hasNext());
        try {
            q.element();
            shouldThrow();
        } catch (NoSuchElementException success) {}
        try {
            q.iterator().next();
            shouldThrow();
        } catch (NoSuchElementException success) {}
        try {
            q.remove();
            shouldThrow();
        } catch (NoSuchElementException success) {}
    }

    /**
     * take blocks interruptibly when empty
     */
    @Test
    @Schedule(name = "takeFromEmpty", value = "[beforeTake:afterTake]@takeThread->beforeInterrupt@main")
    public void testTakeFromEmpty() throws InterruptedException {
        final LinkedTransferQueue q = new LinkedTransferQueue();
        Thread t = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                fireEvent("beforeTake");              
                q.take();
                fireEvent("afterTake");
            }}, "takeThread");
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
        final LinkedTransferQueue<Integer> q = populatedQueue(SIZE);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; ++i) {
                    assertEquals(i, (int) q.take());
                }
                try {
                    fireEvent("beforeTake");              
                    q.take();
                    fireEvent("afterTake");
                    shouldThrow();
                } catch (InterruptedException success) {}
            }}, "takeThread");
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeInterrupt");
        t.interrupt();
        t.join();
        checkEmpty(q, "blockingTake");
    }

    /**
     * transfer waits until a poll occurs. The transfered element
     * is returned by this associated poll.
     */
    @Test
    @Schedule(name = "transfer2", value = "[beforeTransfer:afterTransfer]@transferThread->beforeCheck@main")
    public void testTransfer2() throws InterruptedException {
        final LinkedTransferQueue<Integer> q
            = new LinkedTransferQueue<Integer>();

        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                fireEvent("beforeTransfer");  
                q.transfer(SIZE);
                fireEvent("afterTransfer");
                assertTrue(q.isEmpty());
            }}, "transferThread");
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeCheck");
        assertEquals("transfer2", 1, q.size());
        assertEquals("transfer2", SIZE, (int) q.poll());
        assertTrue("transfer2", q.isEmpty());
        t.join();
    }

    /**
     * transfer waits until a poll occurs, and then transfers in fifo order
     */
    @Test
    @Schedule(name = "transfer3", value = "[beforeTransfer:afterTransfer]@firstThread->beforeTransfer@interruptThread"
      + ", [beforeTransfer:afterTransfer]@interruptThread->beforeCheck@main")
    public void testTransfer3() throws InterruptedException {
        final LinkedTransferQueue<Integer> q
            = new LinkedTransferQueue<Integer>();

        Thread first = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                Integer i = SIZE + 1;
                fireEvent("beforeTransfer");  
                q.transfer(i);
                fireEvent("afterTransfer");
                assertTrue(!q.contains(i));
                assertEquals(1, q.size());
            }}, "firstThread");
        Thread interruptedThread = newStartedThread(
            new CheckedInterruptedRunnable() {
                public void realRun() throws InterruptedException {
                    try {
                        //Thread.sleep(SMALL_DELAY_MS); //INS-SLEEP
                    }
                    catch (Exception ex) {
                        fail();
                    }
                    //OPWAIT while (q.size() == 0)
                    //OPWAIT    Thread.yield();
                    fireEvent("beforeTransfer"); 
                    q.transfer(SIZE);
                    fireEvent("afterTransfer");
                }}, "interruptThread");
        //Thread.sleep(MEDIUM_DELAY_MS); //INS-SLEEP
        //OPWAIT while (q.size() < 2)
        //OPWAIT     Thread.yield();
        fireEvent("beforeCheck");
        assertEquals("transfer3", 2, q.size());
        assertEquals("transfer3", SIZE + 1, (int) q.poll());
        first.join();
        assertEquals("transfer3", 1, q.size());
        interruptedThread.interrupt();
        interruptedThread.join();
        assertEquals("transfer3", 0, q.size());
        assertTrue("transfer3", q.isEmpty());
    }

    /**
     * transfer waits until a poll occurs, at which point the polling
     * thread returns the element
     */
    @Test
    @Schedule(name = "transfer4", value = "[beforeTransfer:afterTransfer]@transferThread->beforeCheck@main")
    public void testTransfer4() throws InterruptedException {
        final LinkedTransferQueue q = new LinkedTransferQueue();

        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                fireEvent("beforeTransfer"); 
                q.transfer(four);
                fireEvent("afterTransfer");
                assertFalse("transfer4", q.contains(four));
                assertSame("transfer4", three, q.poll());
            }}, "transferThread");
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeCheck");
        assertTrue("transfer4", q.offer(three));
        assertSame("transfer4", four, q.poll());
        t.join();
    }

    /**
     * transfer waits until a take occurs. The transfered element
     * is returned by this associated take.
     */
    @Test
    @Schedule(name = "transfer5", value = "[beforeTransfer:afterTransfer]@transferThread->beforeCheck@main")
    public void testTransfer5() throws InterruptedException {
        final LinkedTransferQueue<Integer> q
            = new LinkedTransferQueue<Integer>();

        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                fireEvent("beforeTransfer"); 
                q.transfer(SIZE);
                fireEvent("afterTransfer");
                checkEmpty(q, "transfer5");
            }}, "transferThread");
        //Thread.sleep(SHORT_DELAY_MS);
        fireEvent("beforeCheck");
        assertEquals("transfer5", SIZE, (int) q.take());
        checkEmpty(q, "transfer5");
        t.join();
    }

    /**
     * If there is a consumer waiting in take, tryTransfer returns
     * true while successfully transfering object.
     */
    @Test
    @Schedule(name = "tryTransfer4", value = "[beforeTake:afterTake]@main->beforeCheck@transferThread")
    public void testTryTransfer4() throws InterruptedException {
        final Object hotPotato = new Object();
        final LinkedTransferQueue q = new LinkedTransferQueue();

        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                try {
                    //Thread.sleep(SHORT_DELAY_MS); //INS-SLEEP
                }
                catch (Exception e) {
                    fail();
                }
                //OPWAIT while (! q.hasWaitingConsumer())
                //OPWAIT    Thread.yield();
                fireEvent("beforeCheck");
                assertTrue("tryTransfer4", q.hasWaitingConsumer());
                assertTrue("tryTransfer4", q.isEmpty());
                assertEquals("tryTransfer4", q.size(), 0);
                assertTrue("tryTransfer4", q.tryTransfer(hotPotato));
            }}, "transferThread");
        fireEvent("beforeTake");
        assertSame("tryTransfer4", q.take(), hotPotato);
        fireEvent("afterTake");
        checkEmpty(q, "tryTransfer4");
        t.join();
    }

    private LinkedTransferQueue<Integer> populatedQueue(int n) {
        LinkedTransferQueue<Integer> q = new LinkedTransferQueue<Integer>();
        assertTrue(q.isEmpty());
        for (int i = 0; i < n; i++) {
            assertEquals(i, q.size());
            assertTrue(q.offer(i));
            assertEquals(Integer.MAX_VALUE, q.remainingCapacity());
        }
        assertFalse(q.isEmpty());
        return q;
    }
}
