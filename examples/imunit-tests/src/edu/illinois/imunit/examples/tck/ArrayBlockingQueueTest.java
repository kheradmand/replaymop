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


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.illinois.imunit.Schedule;
import edu.illinois.imunit.Schedules;

@RunWith(IMUnitRunner.class)
public class ArrayBlockingQueueTest extends JSR166TestCase {

  public static class Fair extends BlockingQueueTest {
    protected BlockingQueue emptyCollection() {
      return new ArrayBlockingQueue(20, true);
    }
  }

  public static class NonFair extends BlockingQueueTest {
    protected BlockingQueue emptyCollection() {
      return new ArrayBlockingQueue(20, false);
    }
  }

  // public static void main(String[] args) {
  // junit.textui.TestRunner.run(suite());
  // }
  //
  // public static Test suite() {
  // return newTestSuite(ArrayBlockingQueueTest.class,
  // new Fair().testSuite(),
  // new NonFair().testSuite());
  // }

  /**
   * Create a queue of given size containing consecutive Integers 0 ... n.
   */
  private ArrayBlockingQueue populatedQueue(int n) {
    ArrayBlockingQueue q = new ArrayBlockingQueue(n);
    assertTrue(q.isEmpty());
    for (int i = 0; i < n; i++)
      assertTrue(q.offer(new Integer(i)));
    assertFalse(q.isEmpty());
    assertEquals(0, q.remainingCapacity());
    assertEquals(n, q.size());
    return q;
  }

  @Before  
  public void setUp() {
    super.setUp();
  }
  
  @After
  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  
  /**
   * put blocks interruptibly if full
   */
  @Test
  @Schedules( { @Schedule(name = "BlockingPut", value = "[putBlocked:afterPutBlocked]@putThread->beforeInterrupt@main") })
  public void testBlockingPut() throws InterruptedException {
    final ArrayBlockingQueue q = new ArrayBlockingQueue(SIZE);
    Thread t = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        for (int i = 0; i < SIZE; ++i)
          q.put(i);
        assertEquals("BlockingPut", SIZE, q.size());
        assertEquals("BlockingPut", 0, q.remainingCapacity());
        try {
          fireEvent("putBlocked");
          q.put(99);
          fireEvent("afterPutBlocked");
          shouldThrow();
        } catch (InterruptedException success) {
        }
      }
    }, "putThread");

    t.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeInterrupt");
    t.interrupt();
    t.join();
    assertEquals("BlockingPut", SIZE, q.size());
    assertEquals("BlockingPut", 0, q.remainingCapacity());
  }

  /**
   * Take removes existing elements until empty, then blocks interruptibly
   */
  @Test
  @Schedules( { @Schedule(name = "BlockingTake", value = "[takeBlocked:afterTakeBlocked]@takeThread->beforeInterrupt@main") })
  public void testBlockingTake() throws InterruptedException {
    final ArrayBlockingQueue q = populatedQueue(SIZE);
    Thread t = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        for (int i = 0; i < SIZE; ++i) {
          assertEquals(i, q.take());
        }
        try {
          fireEvent("takeBlocked");
          q.take();
          fireEvent("afterTakeBlocked");
          shouldThrow();
        } catch (InterruptedException success) {
        }
      }
    }, "takeThread");

    t.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeInterrupt");
    t.interrupt();
    t.join();
  }

  /**
   * Interrupted timed poll throws InterruptedException instead of returning
   * timeout status
   */
  public void testInterruptedTimedPoll() throws InterruptedException {
    Thread t = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        ArrayBlockingQueue q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
          assertEquals(i, q.poll(SHORT_DELAY_MS, MILLISECONDS));
          ;
        }
        try {
          q.poll(SMALL_DELAY_MS, MILLISECONDS);
          shouldThrow();
        } catch (InterruptedException success) {
        }
      }
    });

    t.start();
    Thread.sleep(SHORT_DELAY_MS);
    t.interrupt();
    t.join();
  }

  /**
   * offer transfers elements across Executor tasks
   */
  public void testOfferInExecutor() {
    final ArrayBlockingQueue q = new ArrayBlockingQueue(2);
    q.add(one);
    q.add(two);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    executor.execute(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        assertFalse(q.offer(three));
        assertTrue(q.offer(three, MEDIUM_DELAY_MS, MILLISECONDS));
        assertEquals(0, q.remainingCapacity());
      }
    });

    executor.execute(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        Thread.sleep(SMALL_DELAY_MS);
        assertSame(one, q.take());
      }
    });

    joinPool(executor);
  }

  /**
   * poll retrieves elements across Executor threads
   */
  public void testPollInExecutor() {
    final ArrayBlockingQueue q = new ArrayBlockingQueue(2);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    executor.execute(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        assertNull(q.poll());
        assertSame(one, q.poll(MEDIUM_DELAY_MS, MILLISECONDS));
        assertTrue(q.isEmpty());
      }
    });

    executor.execute(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        Thread.sleep(SMALL_DELAY_MS);
        q.put(one);
      }
    });

    joinPool(executor);
  }

  /**
   * drainTo empties full queue, unblocking a waiting put.
   */
  public void testDrainToWithActivePut() throws InterruptedException {
    final ArrayBlockingQueue q = populatedQueue(SIZE);
    Thread t = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        q.put(new Integer(SIZE + 1));
      }
    });

    t.start();
    ArrayList l = new ArrayList();
    q.drainTo(l);
    assertTrue(l.size() >= SIZE);
    for (int i = 0; i < SIZE; ++i)
      assertEquals(l.get(i), new Integer(i));
    t.join();
    assertTrue(q.size() + l.size() >= SIZE);
  }

  /**
   * timed offer times out if full and elements not taken
   */

  public void testTimedOffer() throws InterruptedException {
    final ArrayBlockingQueue q = new ArrayBlockingQueue(2);
    Thread t = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        q.put(new Object());
        q.put(new Object());
        assertFalse(q.offer(new Object(), SHORT_DELAY_MS / 2, MILLISECONDS));
        try {
          q.offer(new Object(), LONG_DELAY_MS, MILLISECONDS);
          shouldThrow();
        } catch (InterruptedException success) {
        }
      }
    });

    t.start();
    Thread.sleep(SHORT_DELAY_MS);
    t.interrupt();
    t.join();
  }

  /**
   * take blocks interruptibly when empty
   */
  @Test
  @Schedules( { @Schedule(name = "TakeFromEmpty", value = "[takeBlocked:afterTakeBlocked]@takeThread->beforeInterrupt@main") })
  public void testTakeFromEmpty() throws InterruptedException {
    final ArrayBlockingQueue q = new ArrayBlockingQueue(2);
    Thread t = new ThreadShouldThrow(InterruptedException.class) {
      public void realRun() throws InterruptedException {
        fireEvent("takeBlocked");
        q.take();
        fireEvent("afterTakeBlocked");
      }
    };

    t.setName("takeThread");
    t.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeInterrupt");
    t.interrupt();
    t.join();
  }

  /**
   * put blocks waiting for take when full
   * 
   * can't be transformed as there is a schedule inside a loop
   */
  @Test
  @Schedule(name = "putWithTake", value = "[beforePut:afterPut]@putThread->beforeTake@main"
    + ", [beforeSecondPut:afterSecondPut]@putThread->beforeInterrupt@main")
  public void testPutWithTake() throws InterruptedException {
    final int capacity = 2;
    final ArrayBlockingQueue q = new ArrayBlockingQueue(capacity);
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
        } catch (InterruptedException success) {
        }
      }
    }, "putThread");

    t.start();
    //Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeTake");
    assertEquals("putWithTake", q.remainingCapacity(), 0);
    assertEquals("putWithTake", 0, q.take());
    //Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeInterrupt");
    t.interrupt();
    t.join();
    assertEquals("putWithTake", q.remainingCapacity(), 0);
  }

    //@NTest
    //@NSchedule(value = "afterAdd1@addThread->beforeTake1@main,"
    //                   + "[beforeTake2:afterTake2]@main->beforeAdd2@addThread")
    public void testAddAndTakeInterplay() throws InterruptedException {
        final ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<Integer>(1);
        Thread add = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                queue.add(1);
                /* @NEvent("afterAdd1") */
                //Thread.sleep(150);
                /* @NEvent("beforeAdd2") */
                queue.add(2);
            }
        }, "addThread");
        add.start();
        //Thread.sleep(50);
        /* @NEvent("beforeTake1") */
        Integer taken = queue.take();
        assertTrue(taken == 1 && queue.isEmpty());        
        /* @NEvent("beforeTake2") */
        taken = queue.take();
        /* @NEvent("afterTake2") */
        assertTrue(taken == 2 && queue.isEmpty());
        add.join();
    }
}
