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
import java.util.concurrent.locks.*;
import java.io.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.illinois.imunit.Schedule;
import edu.illinois.imunit.Schedules;

@RunWith(IMUnitRunner.class)
public class AbstractQueuedLongSynchronizerTest extends JSR166TestCase {
  // public static void main(String[] args) {
  // junit.textui.TestRunner.run(suite());
  // }
  // public static Test suite() {
  // return new TestSuite(AbstractQueuedLongSynchronizerTest.class);
  // }

  /**
   * A simple mutex class, adapted from the AbstractQueuedLongSynchronizer
   * javadoc. Exclusive acquire tests exercise this as a sample user extension.
   * Other methods/features of AbstractQueuedLongSynchronizerTest are tested via
   * other test classes, including those for ReentrantLock,
   * ReentrantReadWriteLock, and Semaphore
   */
  static class Mutex extends AbstractQueuedLongSynchronizer {
    // Use value > 32 bits for locked state
    static final long LOCKED = 1 << 48;

    public boolean isHeldExclusively() {
      return getState() == LOCKED;
    }

    public boolean tryAcquire(long acquires) {
      return compareAndSetState(0, LOCKED);
    }

    public boolean tryRelease(long releases) {
      if (getState() == 0)
        throw new IllegalMonitorStateException();
      setState(0);
      return true;
    }

    public AbstractQueuedLongSynchronizer.ConditionObject newCondition() {
      return new AbstractQueuedLongSynchronizer.ConditionObject();
    }

  }

  /**
   * A simple latch class, to test shared mode.
   */
  static class BooleanLatch extends AbstractQueuedLongSynchronizer {
    public boolean isSignalled() {
      return getState() != 0;
    }

    public long tryAcquireShared(long ignore) {
      return isSignalled() ? 1 : -1;
    }

    public boolean tryReleaseShared(long ignore) {
      setState(1 << 62);
      return true;
    }
  }

  /**
   * A runnable calling acquireInterruptibly that does not expect to be
   * interrupted.
   */
  class InterruptibleSyncRunnable extends CheckedRunnable {
    final Mutex sync;

    InterruptibleSyncRunnable(Mutex l) {
      sync = l;
    }

    public void realRun() throws InterruptedException {
      fireEvent("acqBlocked");
      sync.acquireInterruptibly(1);
      fireEvent("afterAcq");
    }
  }

  /**
   * A runnable calling acquireInterruptibly that expects to be interrupted.
   */
  class InterruptedSyncRunnable extends CheckedInterruptedRunnable {
    final Mutex sync;

    InterruptedSyncRunnable(Mutex l) {
      sync = l;
    }

    public void realRun() throws InterruptedException {
      fireEvent("acqBlocked");
      sync.acquireInterruptibly(1);
      fireEvent("afterAcq");
    }
  }

  /**
   * hasQueuedThreads reports whether there are waiting threads
   */
  @Test
  @Schedules( { @Schedule(name = "HasQueuedThreads", value = "[acqBlocked:afterAcqInterrupted]@hasqueuedThread1->checkHasQueued1@main,"
      + "[acqBlocked:afterAcq]@hasqueuedThread2->checkHasQueued2@main,"
      + "edu.illinois.imunit.examples.tck.JSR166TestCase.interruptedException@hasqueuedThread1->checkHasQueued3@main,"
      + "afterAcq@hasqueuedThread2->checkHasQueued4@main ") })
  public void testhasQueuedThreads() throws InterruptedException {
    final Mutex sync = new Mutex();
    Thread t1 = new Thread(new InterruptedSyncRunnable(sync),
        "hasqueuedThread1");
    Thread t2 = new Thread(new InterruptibleSyncRunnable(sync),
        "hasqueuedThread2");
    assertFalse(sync.hasQueuedThreads());
    sync.acquire(1);
    t1.start();
    // Thread.sleep(SHORT_DELAY_MS);    
    fireEvent("checkHasQueued1");
    assertTrue(sync.hasQueuedThreads());
    t2.start();
    // Thread.sleep(SHORT_DELAY_MS);   
    fireEvent("checkHasQueued2");
    assertTrue(sync.hasQueuedThreads());
    t1.interrupt();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("checkHasQueued3");
    assertTrue(sync.hasQueuedThreads());
    sync.release(1);
    // Thread.sleep(SHORT_DELAY_MS);     
    fireEvent("checkHasQueued4");
    assertFalse(sync.hasQueuedThreads());
    t1.join();
    t2.join();
  }

  /**
   * isQueued reports whether a thread is queued.
   */
  @Test
  @Schedules( { @Schedule(name = "IsQueued", value = "[acqBlocked:afterAcq]@isqueuedThread1->checkIsQueued1@main,"
      + "[acqBlocked:afterAcq]@isqueuedThread2->checkIsQueued2@main, "
      + "edu.illinois.imunit.examples.tck.JSR166TestCase.interruptedException@isqueuedThread1->checkIsQueued3@main,"
      + "afterAcq@isqueuedThread2->checkIsQueued4@main  ") })
  public void testIsQueued() throws InterruptedException {
    final Mutex sync = new Mutex();
    Thread t1 = new Thread(new InterruptedSyncRunnable(sync), "isqueuedThread1");
    Thread t2 = new Thread(new InterruptibleSyncRunnable(sync),
        "isqueuedThread2");
    assertFalse(sync.isQueued(t1));
    assertFalse(sync.isQueued(t2));
    sync.acquire(1);
    t1.start();
    // Thread.sleep(SHORT_DELAY_MS);    
    fireEvent("checkIsQueued1");
    assertTrue(sync.isQueued(t1));
    t2.start();
    // Thread.sleep(SHORT_DELAY_MS);    
    fireEvent("checkIsQueued2");
    assertTrue(sync.isQueued(t1));
    assertTrue(sync.isQueued(t2));
    t1.interrupt();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("checkIsQueued3");
    assertFalse(sync.isQueued(t1));
    assertTrue(sync.isQueued(t2));
    sync.release(1);
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("checkIsQueued4");
    assertFalse(sync.isQueued(t1));
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("checkIsQueued5");
    assertFalse(sync.isQueued(t2));
    t1.join();
    t2.join();
  }

  /**
   * getFirstQueuedThread returns first waiting thread or null if none
   */
  @Test
  @Schedules( { @Schedule(name = "GetFirstQueued", value = "[acqBlocked:afterAcq]@firstqueuedThread1->firstQueued1@main,"
      + "[acqBlocked:afterAcq]@firstqueuedThread2->firstQueued2@main, "
      + "edu.illinois.imunit.examples.tck.JSR166TestCase.interruptedException@firstqueuedThread1->firstQueued3@main,"
      + "afterAcq@firstqueuedThread2->firstQueued4@main  ") })
  public void testGetFirstQueuedThread() throws InterruptedException {
    final Mutex sync = new Mutex();
    Thread t1 = new Thread(new InterruptedSyncRunnable(sync),
        "firstqueuedThread1");
    Thread t2 = new Thread(new InterruptibleSyncRunnable(sync),
        "firstqueuedThread2");
    assertNull(sync.getFirstQueuedThread());
    sync.acquire(1);
    t1.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("firstQueued1");
    assertEquals(t1, sync.getFirstQueuedThread());
    t2.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("firstQueued2");
    assertEquals(t1, sync.getFirstQueuedThread());
    t1.interrupt();
    // Thread.sleep(SHORT_DELAY_MS);
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("firstQueued3");
    assertEquals(t2, sync.getFirstQueuedThread());
    sync.release(1);
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("firstQueued4");
    assertNull(sync.getFirstQueuedThread());
    t1.join();
    t2.join();
  }

  /**
   * hasContended reports false if no thread has ever blocked, else true
   */
  @Test
  @Schedules( { @Schedule(name = "HasContended", value = "[acqBlocked:afterAcq]@hascontendedThread1->hasContended1@main,"
      + "[acqBlocked:afterAcq]@hascontendedThread2->hasContended2@main, "
      + "edu.illinois.imunit.examples.tck.JSR166TestCase.interruptedException@hascontendedThread1->hasContended3@main,"
      + "afterAcq@hascontendedThread2->hasContended4@main  ") })
  public void testHasContended() throws InterruptedException {
    final Mutex sync = new Mutex();
    Thread t1 = new Thread(new InterruptedSyncRunnable(sync),
        "hascontendedThread1");
    Thread t2 = new Thread(new InterruptibleSyncRunnable(sync),
        "hascontendedThread2");
    assertFalse(sync.hasContended());
    sync.acquire(1);
    t1.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("hasContended1");
    assertTrue(sync.hasContended());
    t2.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("hasContended2");
    assertTrue(sync.hasContended());
    t1.interrupt();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("hasContended3");
    assertTrue(sync.hasContended());
    sync.release(1);
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("hasContended4");
    assertTrue(sync.hasContended());
    t1.join();
    t2.join();
  }

  /**
   * getQueuedThreads includes waiting threads
   */
  @Test
  @Schedules( { @Schedule(name = "GetQueuedThreads", value = "[acqBlocked:afterAcq]@getqueuedThread1->getQueued1@main,"
      + "[acqBlocked:afterAcq]@getqueuedThread2->getQueued2@main, "
      + "edu.illinois.imunit.examples.tck.JSR166TestCase.interruptedException@getqueuedThread1->getQueued3@main,"
      + "afterAcq@getqueuedThread2->getQueued4@main  ") })
  public void testGetQueuedThreads() throws InterruptedException {
    final Mutex sync = new Mutex();
    Thread t1 = new Thread(new InterruptedSyncRunnable(sync),
        "getqueuedThread1");
    Thread t2 = new Thread(new InterruptibleSyncRunnable(sync),
        "getqueuedThread2");
    assertTrue(sync.getQueuedThreads().isEmpty());
    sync.acquire(1);
    assertTrue(sync.getQueuedThreads().isEmpty());
    t1.start();
    // Thread.sleep(SHORT_DELAY_MS); 
    fireEvent("getQueued1");
    assertTrue(sync.getQueuedThreads().contains(t1));
    t2.start();
    // Thread.sleep(SHORT_DELAY_MS);     
    fireEvent("getQueued2");
    assertTrue(sync.getQueuedThreads().contains(t1)); 
    assertTrue(sync.getQueuedThreads().contains(t2));    
    t1.interrupt();
    // Thread.sleep(SHORT_DELAY_MS);       
    fireEvent("getQueued3");
    assertFalse(sync.getQueuedThreads().contains(t1)); 
    assertTrue(sync.getQueuedThreads().contains(t2));
    sync.release(1);
    // Thread.sleep(SHORT_DELAY_MS); 
    fireEvent("getQueued4");
    assertTrue(sync.getQueuedThreads().isEmpty());
    t1.join();
    t2.join();
  }

  /**
   * getExclusiveQueuedThreads includes waiting threads
   */
  @Test
  @Schedules( { @Schedule(name = "GetExclusiveQueuedThreads", value = "[acqBlocked:afterAcq]@getexclusivequeuedThread1->getExclusiveQueued1@main,"
      + "[acqBlocked:afterAcq]@getexclusivequeuedThread2->getExclusiveQueued2@main, "
      + "edu.illinois.imunit.examples.tck.JSR166TestCase.interruptedException@getexclusivequeuedThread1->getExclusiveQueued3@main,"
      + "afterAcq@getexclusivequeuedThread2->getExclusiveQueued4@main  ") })
  public void testGetExclusiveQueuedThreads() throws InterruptedException {
    final Mutex sync = new Mutex();
    Thread t1 = new Thread(new InterruptedSyncRunnable(sync),
        "getexclusivequeuedThread1");
    Thread t2 = new Thread(new InterruptibleSyncRunnable(sync),
        "getexclusivequeuedThread2");
    assertTrue(sync.getExclusiveQueuedThreads().isEmpty());
    sync.acquire(1);
    assertTrue(sync.getExclusiveQueuedThreads().isEmpty());
    t1.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("getExclusiveQueued1");
    assertTrue(sync.getExclusiveQueuedThreads().contains(t1));
    t2.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("getExclusiveQueued2");
    assertTrue(sync.getExclusiveQueuedThreads().contains(t1));
    assertTrue(sync.getExclusiveQueuedThreads().contains(t2));
    t1.interrupt();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("getExclusiveQueued3");
    assertFalse(sync.getExclusiveQueuedThreads().contains(t1));
    assertTrue(sync.getExclusiveQueuedThreads().contains(t2));
    sync.release(1);
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("getExclusiveQueued4");
    assertTrue(sync.getExclusiveQueuedThreads().isEmpty());
    t1.join();
    t2.join();
  }

  /**
   * getSharedQueuedThreads does not include exclusively waiting threads
   */
  @Test
  @Schedules( { @Schedule(name = "GetSharedQueuedThreads", value = "[acqBlocked:afterAcq]@getsharedqueuedThread1->getSharedQueued1@main,"
      + "[acqBlocked:afterAcq]@getsharedqueuedThread2->getSharedQueued2@main, "
      + "edu.illinois.imunit.examples.tck.JSR166TestCase.interruptedException@getsharedqueuedThread1->getSharedQueued3@main,"
      + "afterAcq@getsharedqueuedThread2->getSharedQueued4@main  ") })
  public void testGetSharedQueuedThreads() throws InterruptedException {
    final Mutex sync = new Mutex();
    Thread t1 = new Thread(new InterruptedSyncRunnable(sync),
        "getsharedqueuedThread1");
    Thread t2 = new Thread(new InterruptibleSyncRunnable(sync),
        "getsharedqueuedThread2");
    assertTrue(sync.getSharedQueuedThreads().isEmpty());
    sync.acquire(1);
    assertTrue(sync.getSharedQueuedThreads().isEmpty());
    t1.start();
    // Thread.sleep(SHORT_DELAY_MS);   
    fireEvent("getSharedQueued1");
    assertTrue(sync.getSharedQueuedThreads().isEmpty());
    t2.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("getSharedQueued2");
    assertTrue(sync.getSharedQueuedThreads().isEmpty());
    t1.interrupt();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("getSharedQueued3");
    assertTrue(sync.getSharedQueuedThreads().isEmpty());
    sync.release(1);
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("getSharedQueued4");
    assertTrue(sync.getSharedQueuedThreads().isEmpty());
    t1.join();
    t2.join();
  }

  /**
   * getState is true when acquired and false when not
   */
  @Test
  @Schedules( { @Schedule(name = "GetState", value = "afterAcquire@acqthread->beforeCheck@main,"
      + "afterCheck@main->beforeRelease@acqthread") })
  public void testGetState() throws InterruptedException {
    final Mutex sync = new Mutex();
    sync.acquire(1);
    assertTrue("GetState", sync.isHeldExclusively());
    sync.release(1);
    assertFalse("GetState", sync.isHeldExclusively());
    Thread t = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        sync.acquire(1);
        fireEvent("afterAcquire");
        // Thread.sleep(SMALL_DELAY_MS);
        fireEvent("beforeRelease");
        sync.release(1);
      }
    }, "acqthread");

    t.start();
    // Thread.sleep(SHORT_DELAY_MS);     
    fireEvent("beforeCheck");
    assertTrue(sync.isHeldExclusively());
    fireEvent("afterCheck");
    t.join();
    assertFalse(sync.isHeldExclusively());
  }

  /**
   * acquireInterruptibly is interruptible.
   */
  @Test
  @Schedules( { @Schedule(name = "AcquireInterruptibly1", value = "[acqBlocked:afterAcq]@acqthread->beforeInterrupt@main,"
      + "edu.illinois.imunit.examples.tck.JSR166TestCase.interruptedException@acqthread->beforeRelease@main") })
  public void testAcquireInterruptibly1() throws InterruptedException {
    final Mutex sync = new Mutex();
    sync.acquire(1);
    Thread t = new Thread(new InterruptedSyncRunnable(sync), "acqthread");
    t.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeInterrupt");
    t.interrupt();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeRelease");
    sync.release(1);
    t.join();
  }

  /**
   * acquireInterruptibly succeeds when released, else is interruptible
   */
  @Test
  @Schedules( { @Schedule(name = "AcquireInterruptibly2", value = "[acqBlocked:afterAcq]@acqthread->beforeInterrupt@main") })
  public void testAcquireInterruptibly2() throws InterruptedException {
    final Mutex sync = new Mutex();
    sync.acquireInterruptibly(1);
    Thread t = new Thread(new InterruptedSyncRunnable(sync), "acqthread");
    t.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeInterrupt");
    t.interrupt();
    fireEvent("beforeTestHeld");
    assertTrue(sync.isHeldExclusively());
    t.join();
  }

  /**
   * await returns when signalled
   */
  @Test
  @Schedules( { @Schedule(name = "Await", value = "[awaitBlocked:afterAwaitBlocked]@awaitThread->beforeAcquire@main") })
  public void testAwait() throws InterruptedException {
    final Mutex sync = new Mutex();
    final AbstractQueuedLongSynchronizer.ConditionObject c = sync.newCondition();
    Thread t = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        sync.acquire(1);
        fireEvent("awaitBlocked");
        c.await();
        fireEvent("afterAwaitBlocked");
        sync.release(1);
      }
    }, "awaitThread");

    t.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeAcquire");
    sync.acquire(1);
    c.signal();
    sync.release(1);
    t.join();
    assertFalse("Await", t.isAlive());
  }

  /**
   * hasWaiters returns true when a thread is waiting, else false
   */
  @Test
  @Schedules( { @Schedule(name = "HasWaiters", value = "[awaitBlocked:afterAwaitBlocked]@awaitThread->beforeAcquire@main,"
      + "afterRelease@awaitThread->beforeSecondAcquire@main") })
  public void testHasWaiters() throws InterruptedException {
    final Mutex sync = new Mutex();
    final AbstractQueuedLongSynchronizer.ConditionObject c = sync.newCondition();
    Thread t = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        sync.acquire(1);
        assertFalse("HasWaiters", sync.hasWaiters(c));
        assertEquals("HasWaiters", 0, sync.getWaitQueueLength(c));
        fireEvent("awaitBlocked");
        c.await();
        fireEvent("afterAwaitBlocked");
        sync.release(1);
        fireEvent("afterRelease");
      }
    }, "awaitThread");

    t.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeAcquire");
    sync.acquire(1);
    assertTrue("HasWaiters", sync.hasWaiters(c));
    assertEquals("HasWaiters", 1, sync.getWaitQueueLength(c));
    c.signal();
    sync.release(1);
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeSecondAcquire");
    sync.acquire(1);
    assertFalse("HasWaiters", sync.hasWaiters(c));
    assertEquals("HasWaiters", 0, sync.getWaitQueueLength(c));
    sync.release(1);
    t.join();
    assertFalse("HasWaiters", t.isAlive());
  }

  /**
   * getWaitQueueLength returns number of waiting threads
   */
  @Test
  @Schedules( { @Schedule(name = "GetWaitQueueLength", value = "[awaitBlocked:afterAwaitBlocked]@awaitThread1->beforeAcquire@awaitThread2,"
      + "[awaitBlocked:afterAwaitBlocked]@awaitThread2->beforeAcquire@main,"
      + "afterRelease@awaitThread1->beforeSecondAcquire@main,"
      + "afterRelease@awaitThread2->beforeSecondAcquire@main") })
  public void testGetWaitQueueLength() throws InterruptedException {
    final Mutex sync = new Mutex();
    final AbstractQueuedLongSynchronizer.ConditionObject c = sync.newCondition();
    Thread t1 = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        sync.acquire(1);
        assertFalse("GetWaitQueueLength", sync.hasWaiters(c));
        assertEquals("GetWaitQueueLength", 0, sync.getWaitQueueLength(c));
        fireEvent("awaitBlocked");
        c.await();
        fireEvent("afterAwaitBlocked");
        sync.release(1);
        fireEvent("afterRelease");
      }
    }, "awaitThread1");

    Thread t2 = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        fireEvent("beforeAcquire");
        sync.acquire(1);
        assertTrue("GetWaitQueueLength", sync.hasWaiters(c));
        assertEquals("GetWaitQueueLength", 1, sync.getWaitQueueLength(c));
        fireEvent("awaitBlocked");
        c.await();
        fireEvent("afterAwaitBlocked");
        sync.release(1);
        fireEvent("afterRelease");
      }
    }, "awaitThread2");

    t1.start();
    // Thread.sleep(SHORT_DELAY_MS);
    t2.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeAcquire");
    sync.acquire(1);
    assertTrue("GetWaitQueueLength", sync.hasWaiters(c));
    assertEquals("GetWaitQueueLength", 2, sync.getWaitQueueLength(c));
    c.signalAll();
    sync.release(1);
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeSecondAcquire");
    sync.acquire(1);
    assertFalse("GetWaitQueueLength", sync.hasWaiters(c));
    assertEquals("GetWaitQueueLength", 0, sync.getWaitQueueLength(c));
    sync.release(1);
    //t1.join(SHORT_DELAY_MS);
    //t2.join(SHORT_DELAY_MS);
    t1.join();
    t2.join();
    assertFalse("GetWaitQueueLength", t1.isAlive());
    assertFalse("GetWaitQueueLength", t2.isAlive());
  }

  /**
   * getWaitingThreads returns only and all waiting threads
   */
  @Test
  @Schedules( { @Schedule(name = "GetWaitingThreads", value = "[awaitBlocked:afterAwaitBlocked]@awaitThread1->beforeAcquire@awaitThread2,"
      + "[awaitBlocked:afterAwaitBlocked]@awaitThread2->beforeAcquire@main,"
      + "afterRelease@awaitThread1->beforeSecondAcquire@main,"
      + "afterRelease@awaitThread2->beforeSecondAcquire@main") })
  public void testGetWaitingThreads() throws InterruptedException {
    final Mutex sync = new Mutex();
    final AbstractQueuedLongSynchronizer.ConditionObject c = sync.newCondition();
    Thread t1 = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        sync.acquire(1);
        assertTrue("GetWaitingThreads", sync.getWaitingThreads(c).isEmpty());
        fireEvent("awaitBlocked");
        c.await();
        fireEvent("afterAwaitBlocked");
        sync.release(1);
        fireEvent("afterRelease");
      }
    }, "awaitThread1");

    Thread t2 = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        fireEvent("beforeAcquire");
        sync.acquire(1);
        assertFalse("GetWaitingThreads", sync.getWaitingThreads(c).isEmpty());
        fireEvent("awaitBlocked");
        c.await();
        fireEvent("afterAwaitBlocked");
        sync.release(1);
        fireEvent("afterRelease");
      }
    }, "awaitThread2");

    sync.acquire(1);
    assertTrue(sync.getWaitingThreads(c).isEmpty());
    sync.release(1);
    t1.start();
    // Thread.sleep(SHORT_DELAY_MS);
    t2.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeAcquire");
    sync.acquire(1);
    assertTrue("GetWaitingThreads", sync.hasWaiters(c));
    assertTrue("GetWaitingThreads", sync.getWaitingThreads(c).contains(t1));
    assertTrue("GetWaitingThreads", sync.getWaitingThreads(c).contains(t2));
    c.signalAll();
    sync.release(1);
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeSecondAcquire");
    sync.acquire(1);
    assertFalse("GetWaitingThreads", sync.hasWaiters(c));
    assertTrue("GetWaitingThreads", sync.getWaitingThreads(c).isEmpty());
    sync.release(1);
    //t1.join(SHORT_DELAY_MS);
    //t2.join(SHORT_DELAY_MS);
    t1.join();
    t2.join();
    assertFalse("GetWaitingThreads", t1.isAlive());
    assertFalse("GetWaitingThreads", t2.isAlive());
  }

  /**
   * awaitUninterruptibly doesn't abort on interrupt
   */
  @Test
  @Schedules( { @Schedule(name = "AwaitUninterruptibly", value = "[awaitBlocked:afterAwaitBlocked]@awaitThread->beforeInterrupt@main") })
  public void testAwaitUninterruptibly() throws InterruptedException {
    final Mutex sync = new Mutex();
    final AbstractQueuedLongSynchronizer.ConditionObject c = sync.newCondition();
    Thread t = new Thread(new CheckedRunnable() {
      public void realRun() {
        sync.acquire(1);
        fireEvent("awaitBlocked");
        c.awaitUninterruptibly();
        fireEvent("afterAwaitBlocked");
        sync.release(1);
        fireEvent("afterRelease");
      }
    }, "awaitThread");

    t.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeInterrupt");
    t.interrupt();
    sync.acquire(1);
    c.signal();
    sync.release(1);
    t.join();
    assertFalse("AwaitUninterruptibly", t.isAlive());
  }

  /**
   * await is interruptible
   */
  @Test
  @Schedules( { @Schedule(name = "AwaitInterrupt", value = "[awaitBlocked:afterAwaitBlocked]@awaitThread->beforeInterrupt@main") })
  public void testAwaitInterrupt() throws InterruptedException {
    final Mutex sync = new Mutex();
    final AbstractQueuedLongSynchronizer.ConditionObject c = sync.newCondition();
    Thread t = new Thread(new CheckedInterruptedRunnable() {
      public void realRun() throws InterruptedException {
        sync.acquire(1);
        fireEvent("awaitBlocked");
        c.await();
        fireEvent("afterAwaitBlocked");
      }
    }, "awaitThread");

    t.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeInterrupt");
    t.interrupt();
    t.join();
    assertFalse("AwaitInterrupt", t.isAlive());
  }

  /**
   * awaitUntil is interruptible
   */
  public void testAwaitUntilInterrupt() throws InterruptedException {
    final Mutex sync = new Mutex();
    final AbstractQueuedLongSynchronizer.ConditionObject c = sync.newCondition();
    Thread t = new Thread(new CheckedInterruptedRunnable() {
      public void realRun() throws InterruptedException {
        sync.acquire(1);
        java.util.Date d = new java.util.Date();
        fireEvent("awaitBlocked");
        c.awaitUntil(new java.util.Date(d.getTime() + 10000));
        fireEvent("afterAwaitBlocked");
      }
    }, "awaitThread");

    t.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeInterrupt");
    t.interrupt();
    t.join();
    assertFalse("AwaitUntilInterrupt", t.isAlive());
  }

  /**
   * signalAll wakes up all threads
   */
  @Test
  @Schedules( { @Schedule(name = "SignalAll", value = "[awaitBlocked:afterAwaitBlocked]@awaitThread1->beforeAcquire@main,"
      + "[awaitBlocked:afterAwaitBlocked]@awaitThread2->beforeAcquire@main") })
  public void testSignalAll() throws InterruptedException {
    final Mutex sync = new Mutex();
    final AbstractQueuedLongSynchronizer.ConditionObject c = sync.newCondition();
    Thread t1 = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        sync.acquire(1);
        fireEvent("awaitBlocked");
        c.await();
        fireEvent("afterAwaitBlocked");
        sync.release(1);
      }
    }, "awaitThread1");

    Thread t2 = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        sync.acquire(1);
        fireEvent("awaitBlocked");
        c.await();
        fireEvent("afterAwaitBlocked");
        sync.release(1);
      }
    }, "awaitThread2");

    t1.start();
    t2.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeAcquire");
    sync.acquire(1);
    c.signalAll();
    sync.release(1);
    //t1.join(SHORT_DELAY_MS);
    //t2.join(SHORT_DELAY_MS);
    t1.join();
    t2.join();
    assertFalse("SignalAll", t1.isAlive());
    assertFalse("SignalAll", t2.isAlive());
  }

  /**
   * acquireSharedInterruptibly returns after release, but not before
   */
  @Test
  @Schedules( { @Schedule(name = "AcquireSharedInterruptibly", value = "[acqBlocked:afteracqBlocked]@acqBlockedThread->beforeRelease@main") })
  public void testAcquireSharedInterruptibly() throws InterruptedException {
    final BooleanLatch l = new BooleanLatch();

    Thread t = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        assertFalse(l.isSignalled());
        fireEvent("acqBlocked");
        l.acquireSharedInterruptibly(0);
        fireEvent("afteracqBlocked");
        assertTrue("AcquireSharedInterruptibly", l.isSignalled());
      }
    }, "acqBlockedThread");

    t.start();
    assertFalse("AcquireSharedInterruptibly", l.isSignalled());
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeRelease");
    l.releaseShared(0);
    assertTrue("AcquireSharedInterruptibly", l.isSignalled());
    t.join();
  }

  /**
   * acquireSharedInterruptibly throws IE if interrupted before released
   */
  @Test
  @Schedules( { @Schedule(name = "default", value = "[acqBlocked:afteracqBlocked]@acqBlockedThread->beforeInterrupt@main") })
  public void testAcquireSharedInterruptiblyInterruptedException()
      throws InterruptedException {
    final BooleanLatch l = new BooleanLatch();
    Thread t = new Thread(new CheckedInterruptedRunnable() {
      public void realRun() throws InterruptedException {
        assertFalse("AcquireSharedInterruptiblyInterruptedException",
            l.isSignalled());
        fireEvent("acqBlocked");
        l.acquireSharedInterruptibly(0);
        fireEvent("afteracqBlocked");
      }
    }, "acqBlockedThread");

    t.start();
    //Thread.sleep(SHORT_DELAY_MS);
    assertFalse("AcquireSharedInterruptiblyInterruptedException",
        l.isSignalled());
    fireEvent("beforeInterrupt");
    t.interrupt();
    t.join();
  }

  /**
   * acquireSharedTimed returns after release
   */
  public void testAcquireSharedTimed() throws InterruptedException {
    final BooleanLatch l = new BooleanLatch();

    Thread t = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        assertFalse(l.isSignalled());
        long nanos = MILLISECONDS.toNanos(MEDIUM_DELAY_MS);
        assertTrue(l.tryAcquireSharedNanos(0, nanos));
        assertTrue(l.isSignalled());
      }
    }, "acqBlockedThread");

    t.start();
    assertFalse(l.isSignalled());
    Thread.sleep(SHORT_DELAY_MS);
    l.releaseShared(0);
    assertTrue("AcquireSharedTimed", l.isSignalled());
    t.join();
  }

  /**
   * acquireSharedTimed throws IE if interrupted before released
   */
  public void testAcquireSharedNanos_InterruptedException()
      throws InterruptedException {
    final BooleanLatch l = new BooleanLatch();
    Thread t = new Thread(new CheckedInterruptedRunnable() {
      public void realRun() throws InterruptedException {
        assertFalse(l.isSignalled());
        long nanos = MILLISECONDS.toNanos(SMALL_DELAY_MS);
        l.tryAcquireSharedNanos(0, nanos);
      }
    });

    t.start();
    Thread.sleep(SHORT_DELAY_MS);
    assertFalse("AcquireSharedNanos_InterruptedException", l.isSignalled());
    t.interrupt();
    t.join();
  }

  /**
   * acquireSharedTimed times out if not released before timeout
   */
  public void testAcquireSharedNanos_Timeout() throws InterruptedException {
    final BooleanLatch l = new BooleanLatch();
    Thread t = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        assertFalse(l.isSignalled());
        long nanos = MILLISECONDS.toNanos(SMALL_DELAY_MS);
        assertFalse(l.tryAcquireSharedNanos(0, nanos));
      }
    });

    t.start();
    Thread.sleep(SHORT_DELAY_MS);
    assertFalse(l.isSignalled());
    t.join();
  }
}
