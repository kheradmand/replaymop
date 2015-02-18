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




import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.Collection;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Before;
import org.junit.Test;

import edu.illinois.imunit.Schedule;
import edu.illinois.imunit.Schedules;

@RunWith(IMUnitRunner.class)
public class ReentrantLockTest extends JSR166TestCase {
  // public static void main(String[] args) {
  // junit.textui.TestRunner.run(suite());
  // }
  // public static Test suite() {
  // return new TestSuite(ReentrantLockTest.class);
  // }
    
  @Before  
  public void setUp() {
    super.setUp();
  }

  /**
   * A runnable calling lockInterruptibly
   */
  class InterruptibleLockRunnable extends CheckedRunnable {
    final ReentrantLock lock;

    InterruptibleLockRunnable(ReentrantLock l) {
      lock = l;
    }

    public void realRun() throws InterruptedException {
      fireEvent("beforeLock");
      lock.lockInterruptibly();
      fireEvent("afterLock");
    }
  }

  /**
   * A runnable calling lockInterruptibly that expects to be interrupted
   */
  class InterruptedLockRunnable extends CheckedInterruptedRunnable {
    final ReentrantLock lock;

    InterruptedLockRunnable(ReentrantLock l) {
      lock = l;
    }

    public void realRun() throws InterruptedException {
      fireEvent("beforeLock");
      lock.lockInterruptibly();
      fireEvent("afterLock");
    }
  }

  /**
   * Subclass to expose protected methods
   */
  static class PublicReentrantLock extends ReentrantLock {
    PublicReentrantLock() {
      super();
    }

    public Collection<Thread> getQueuedThreads() {
      return super.getQueuedThreads();
    }

    public Collection<Thread> getWaitingThreads(Condition c) {
      return super.getWaitingThreads(c);
    }
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
    final ReentrantLock lock = new ReentrantLock();
    Thread t1 = new Thread(new InterruptedLockRunnable(lock), "lockThread1");
    Thread t2 = new Thread(new InterruptibleLockRunnable(lock), "lockThread2");
    assertEquals("GetQueueLength", 0, lock.getQueueLength());
    lock.lock();
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
    lock.unlock();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("checkLength4");
    assertEquals("GetQueueLength", 0, lock.getQueueLength());
    t1.join();
    t2.join();
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
    final ReentrantLock lock = new ReentrantLock(true);
    Thread t1 = new Thread(new InterruptedLockRunnable(lock), "lockThread1");
    Thread t2 = new Thread(new InterruptibleLockRunnable(lock), "lockThread2");
    assertEquals("GetQueueLengthfair", 0, lock.getQueueLength());
    lock.lock();
    t1.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("checkLength1");
    assertEquals("GetQueueLengthfair", 1, lock.getQueueLength());
    t2.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("checkLength2");
    assertEquals("GetQueueLengthfair", 2, lock.getQueueLength());
    t1.interrupt();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("checkLength3");
    assertEquals("GetQueueLengthfair", 1, lock.getQueueLength());
    lock.unlock();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("checkLength4");
    assertEquals("GetQueueLengthfair", 0, lock.getQueueLength());
    t1.join();
    t2.join();
  }

  /**
   * hasQueuedThread reports whether a thread is queued.
   */
  @Test
  @Schedules( { @Schedule(name = "HasQueuedThread", value = "[beforeLock:afterLock]@lockThread1->checkHasQueued1@main,"
      + "[beforeLock:afterLock]@lockThread2->checkHasQueued2@main,"
      + "edu.illinois.imunit.examples.tck.JSR166TestCase.interruptedException@lockThread1->checkHasQueued3@main,"
      + "afterLock@lockThread2->checkHasQueued4@main ") })
  public void testHasQueuedThread() throws InterruptedException {
    final ReentrantLock sync = new ReentrantLock();
    Thread t1 = new Thread(new InterruptedLockRunnable(sync), "lockThread1");
    Thread t2 = new Thread(new InterruptibleLockRunnable(sync), "lockThread2");
    assertFalse("HasQueuedThread", sync.hasQueuedThread(t1));
    assertFalse("HasQueuedThread", sync.hasQueuedThread(t2));
    sync.lock();
    t1.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("checkHasQueued1");
    assertTrue("HasQueuedThread", sync.hasQueuedThread(t1));
    t2.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("checkHasQueued2");
    assertTrue("HasQueuedThread", sync.hasQueuedThread(t1));
    assertTrue("HasQueuedThread", sync.hasQueuedThread(t2));
    t1.interrupt();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("checkHasQueued3");
    assertFalse("HasQueuedThread", sync.hasQueuedThread(t1));
    assertTrue("HasQueuedThread", sync.hasQueuedThread(t2));
    sync.unlock();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("checkHasQueued4");
    assertFalse("HasQueuedThread", sync.hasQueuedThread(t1));
    // Thread.sleep(SHORT_DELAY_MS);
    assertFalse("HasQueuedThread", sync.hasQueuedThread(t2));
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
    final PublicReentrantLock lock = new PublicReentrantLock();
    Thread t1 = new Thread(new InterruptedLockRunnable(lock), "lockThread1");
    Thread t2 = new Thread(new InterruptibleLockRunnable(lock), "lockThread2");
    assertTrue("GetQueuedThreads", lock.getQueuedThreads().isEmpty());
    lock.lock();
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
    lock.unlock();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("chekcGetThreads4");
    assertTrue("GetQueuedThreads", lock.getQueuedThreads().isEmpty());
    t1.join();
    t2.join();
  }

  /**
   * TryLock on a locked lock fails
   */
  public void testTryLockWhenLocked() throws InterruptedException {
    final ReentrantLock lock = new ReentrantLock();
    lock.lock();
    Thread t = new Thread(new CheckedRunnable() {
      public void realRun() {
        assertFalse(lock.tryLock());
      }
    });

    t.start();
    t.join();
    lock.unlock();
  }

  /**
   * Timed tryLock on a locked lock times out
   */
  public void testTryLock_Timeout() throws InterruptedException {
    final ReentrantLock lock = new ReentrantLock();
    lock.lock();
    Thread t = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        assertFalse(lock.tryLock(1, MILLISECONDS));
      }
    });

    t.start();
    t.join();
    lock.unlock();
  }

  /**
   * isLocked is true when locked and false when not
   */
  @Test
  @Schedules( { @Schedule(name = "IsLocked", value = "afterLocked@interruptedThread->beforeChecked@main, " + 
      "afterChecked@main->beforeUnlocked@interruptedThread") })
  public void testIsLocked() throws InterruptedException {
    final ReentrantLock lock = new ReentrantLock();
    lock.lock();
    assertTrue("IsLocked", lock.isLocked());
    lock.unlock();
    assertFalse("IsLocked", lock.isLocked());
    Thread t = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        lock.lock();
        fireEvent("afterLocked");
        // Thread.sleep(SMALL_DELAY_MS);
        fireEvent("beforeUnlocked");
        lock.unlock();
      }
    }, "interruptedThread");

    t.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeChecked");
    assertTrue("IsLocked", lock.isLocked());
    fireEvent("afterChecked");
    t.join();
    assertFalse("IsLocked", lock.isLocked());
  }

  /**
   * lockInterruptibly is interruptible.
   */
  @Test
  @Schedules( { @Schedule(name = "LockInterruptibly1", value = "[beforeLock:afterLock]@interruptedThread->beforeInterrupt@main,"
      + "edu.illinois.imunit.examples.tck.JSR166TestCase.interruptedException@interruptedThread->beforeUnlock@main") })
  public void testLockInterruptibly1() throws InterruptedException {
    final ReentrantLock lock = new ReentrantLock();
    lock.lock();
    Thread t = new Thread(new InterruptedLockRunnable(lock),
        "interruptedThread");
    t.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeInterrupt");
    t.interrupt();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeUnlock");
    lock.unlock();
    t.join();
  }

  /**
   * lockInterruptibly succeeds when unlocked, else is interruptible
   */
  @Test
  @Schedules( { @Schedule(name = "LockInterruptibly2", value = "[beforeLock:afterLock]@interruptedThread->beforeInterrupt@main") })
  public void testLockInterruptibly2() throws InterruptedException {
    final ReentrantLock lock = new ReentrantLock();
    lock.lockInterruptibly();
    Thread t = new Thread(new InterruptedLockRunnable(lock),
        "interruptedThread");
    t.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeInterrupt");
    t.interrupt();
    assertTrue("LockInterruptibly2", lock.isLocked());
    assertTrue("LockInterruptibly2", lock.isHeldByCurrentThread());
    t.join();
  }

  /**
   * await returns when signalled
   */
  @Test
  @Schedules( { @Schedule(name = "Await", value = "[beforeAwait:afterAwait]@awaitThread->beforeLock@main") })
  public void testAwait() throws InterruptedException {
    final ReentrantLock lock = new ReentrantLock();
    final Condition c = lock.newCondition();
    Thread t = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        lock.lock();
        fireEvent("beforeAwait");
        c.await();
        fireEvent("afterAwait");
        lock.unlock();
      }
    }, "awaitThread");

    t.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeLock");
    lock.lock();
    c.signal();
    lock.unlock();
    t.join();
    assertFalse("Await", t.isAlive());
  }

  /**
   * hasWaiters returns true when a thread is waiting, else false
   */
  @Test
  @Schedules( { @Schedule(name = "HasWaiters", value = "[awaitBlocked:afterAwaitBlocked]@awaitThread->beforeLock@main,"
      + "afterUnlock@awaitThread->beforeSecondLock@main") })
  public void testHasWaiters() throws InterruptedException {
    final ReentrantLock lock = new ReentrantLock();
    final Condition c = lock.newCondition();
    Thread t = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        lock.lock();
        assertFalse("HasWaiters", lock.hasWaiters(c));
        assertEquals("HasWaiters", 0, lock.getWaitQueueLength(c));
        fireEvent("awaitBlocked");
        c.await();
        fireEvent("afterAwaitBlocked");
        lock.unlock();
        fireEvent("afterUnlock");
      }
    }, "awaitThread");

    t.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeLock");
    lock.lock();
    assertTrue("HasWaiters", lock.hasWaiters(c));
    assertEquals("HasWaiters", 1, lock.getWaitQueueLength(c));
    c.signal();
    lock.unlock();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeSecondLock");
    lock.lock();
    assertFalse("HasWaiters", lock.hasWaiters(c));
    assertEquals("HasWaiters", 0, lock.getWaitQueueLength(c));
    lock.unlock();
    t.join();
    assertFalse("HasWaiters", t.isAlive());
  }

  /**
   * getWaitQueueLength returns number of waiting threads
   */
  @Test
  @Schedules( { @Schedule(name = "GetWaitQueueLength", value = "[awaitBlocked:afterAwaitBlocked]@awaitThread1->beforeCheck@awaitThread2,"
      + "[awaitBlocked:afterAwaitBlocked]@awaitThread2->beforeLock@main,"
      + "afterUnlock@awaitThread1->beforeSecondLock@main,"
      + "afterUnlock@awaitThread2->beforeSecondLock@main") })
  public void testGetWaitQueueLength() throws InterruptedException {
    final ReentrantLock lock = new ReentrantLock();
    final Condition c = lock.newCondition();
    Thread t1 = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        lock.lock();
        assertFalse("GetWaitQueueLength", lock.hasWaiters(c));
        assertEquals("GetWaitQueueLength", 0, lock.getWaitQueueLength(c));
        fireEvent("awaitBlocked");
        c.await();
        fireEvent("afterAwaitBlocked");
        lock.unlock();
        fireEvent("afterUnlock");
      }
    }, "awaitThread1");

    Thread t2 = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        lock.lock();
        fireEvent("beforeCheck");
        assertTrue("GetWaitQueueLength", lock.hasWaiters(c));
        assertEquals("GetWaitQueueLength", 1, lock.getWaitQueueLength(c));
        fireEvent("awaitBlocked");
        c.await();
        fireEvent("afterAwaitBlocked");
        lock.unlock();
        fireEvent("afterUnlock");
      }
    }, "awaitThread2");

    t1.start();
    // Thread.sleep(SHORT_DELAY_MS);
    t2.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeLock");
    lock.lock();
    assertTrue("GetWaitQueueLength", lock.hasWaiters(c));
    assertEquals("GetWaitQueueLength", 2, lock.getWaitQueueLength(c));
    c.signalAll();
    lock.unlock();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeSecondLock");
    lock.lock();
    assertFalse("GetWaitQueueLength", lock.hasWaiters(c));
    assertEquals("GetWaitQueueLength", 0, lock.getWaitQueueLength(c));
    lock.unlock();
    t1.join();
    t2.join();
    assertFalse("GetWaitQueueLength", t1.isAlive());
    assertFalse("GetWaitQueueLength", t2.isAlive());
  }

  /**
   * getWaitingThreads returns only and all waiting threads
   */
  @Test
  @Schedules( { @Schedule(name = "GetWaitingThreads", value = "[awaitBlocked:afterAwaitBlocked]@awaitThread1->beforeCheck@awaitThread2,"
      + "[awaitBlocked:afterAwaitBlocked]@awaitThread2->beforeLock@main,"
      + "afterUnlock@awaitThread1->beforeSecondLock@main,"
      + "afterUnlock@awaitThread2->beforeSecondLock@main") })
  public void testGetWaitingThreads() throws InterruptedException {
    final PublicReentrantLock lock = new PublicReentrantLock();
    final Condition c = lock.newCondition();
    Thread t1 = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        lock.lock();
        assertTrue("GetWaitingThreads", lock.getWaitingThreads(c).isEmpty());
        fireEvent("awaitBlocked");
        c.await();
        fireEvent("afterAwaitBlocked");
        lock.unlock();
        fireEvent("afterUnlock");
      }
    }, "awaitThread1");

    Thread t2 = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        lock.lock();
        fireEvent("beforeCheck");
        assertFalse("GetWaitingThreads", lock.getWaitingThreads(c).isEmpty());
        fireEvent("awaitBlocked");
        c.await();
        fireEvent("afterAwaitBlocked");
        lock.unlock();
        fireEvent("afterUnlock");
      }
    }, "awaitThread2");

    lock.lock();
    assertTrue("GetWaitingThreads", lock.getWaitingThreads(c).isEmpty());
    lock.unlock();
    t1.start();
    // Thread.sleep(SHORT_DELAY_MS);
    t2.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeLock");
    lock.lock();
    assertTrue("GetWaitingThreads", lock.hasWaiters(c));
    assertTrue("GetWaitingThreads", lock.getWaitingThreads(c).contains(t1));
    assertTrue("GetWaitingThreads", lock.getWaitingThreads(c).contains(t2));
    c.signalAll();
    lock.unlock();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeSecondLock");
    lock.lock();
    assertFalse("GetWaitingThreads", lock.hasWaiters(c));
    assertTrue("GetWaitingThreads", lock.getWaitingThreads(c).isEmpty());
    lock.unlock();
    t1.join();
    t2.join();
    assertFalse("GetWaitingThreads", t1.isAlive());
    assertFalse("GetWaitingThreads", t2.isAlive());
  }

  /** A helper class for uninterruptible wait tests */
  class UninterruptibleThread extends Thread {
    private ReentrantLock lock;
    private Condition c;

    public volatile boolean canAwake = false;
    public volatile boolean interrupted = false;
    public volatile boolean lockStarted = false;

    public UninterruptibleThread(ReentrantLock lock, Condition c) {
      this.lock = lock;
      this.c = c;
    }

    public synchronized void run() {
      lock.lock();
      lockStarted = true;
      fireEvent("lockStart");
      while (!canAwake) {
        fireEvent("awaitBlocked");
        c.awaitUninterruptibly();
        fireEvent("afterAwaitBlocked");
      }

      interrupted = isInterrupted();
      lock.unlock();
    }
  }

  /**
   * await is interruptible
   */
  @Test
  @Schedules( { @Schedule(name = "AwaitInterrupt", value = "[awaitBlocked:afterAwaitBlocked]@awaitThread->beforeInterrupt@main") })
  public void testAwaitInterrupt() throws InterruptedException {
    final ReentrantLock lock = new ReentrantLock();
    final Condition c = lock.newCondition();
    Thread t = new Thread(new CheckedInterruptedRunnable() {
      public void realRun() throws InterruptedException {
        lock.lock();
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
//  @NTest
//  @NSchedules( { @NSchedule(name = "AwaitUntilInterrupt", value = "[awaitBlocked:afterAwaitBlocked]@awaitThread->beforeInterrupt@main") })
  public void testAwaitUntilInterrupt() throws InterruptedException {
    final ReentrantLock lock = new ReentrantLock();
    final Condition c = lock.newCondition();
    Thread t = new Thread(new CheckedInterruptedRunnable() {
      public void realRun() throws InterruptedException {
        lock.lock();
        java.util.Date d = new java.util.Date();
        /* @NEvent("awaitBlocked") */
        c.awaitUntil(new java.util.Date(d.getTime() + 10000));
        /* @NEvent("afterAwaitBlocked") */
      }
    }, "awaitThread");

    t.start();
    // Thread.sleep(SHORT_DELAY_MS);
    /* @NEvent("beforeInterrupt") */
    t.interrupt();
    t.join();
    assertFalse("AwaitUntilInterrupt", t.isAlive());
  }

  /**
   * signalAll wakes up all threads
   */
  @Test
  @Schedules( { @Schedule(name = "SignalAll", value = "[awaitBlocked:afterAwaitBlocked]@awaitThread1->beforeLock@main,"
      + "[awaitBlocked:afterAwaitBlocked]@awaitThread2->beforeLock@main") })
  public void testSignalAll() throws InterruptedException {
    final ReentrantLock lock = new ReentrantLock();
    final Condition c = lock.newCondition();
    Thread t1 = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        lock.lock();
        fireEvent("awaitBlocked");
        c.await();
        fireEvent("afterAwaitBlocked");
        lock.unlock();
      }
    }, "awaitThread1");

    Thread t2 = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        lock.lock();
        fireEvent("awaitBlocked");
        c.await();
        fireEvent("afterAwaitBlocked");
        lock.unlock();
      }
    }, "awaitThread2");

    t1.start();
    t2.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeLock");
    lock.lock();
    c.signalAll();
    lock.unlock();
    t1.join();
    t2.join();
    assertFalse("SignalAll", t1.isAlive());
    assertFalse("SignalAll", t2.isAlive());
  }

  /**
   * await after multiple reentrant locking preserves lock count
   */
  @Test
  @Schedules( { @Schedule(name = "AwaitLockCount", value = "[awaitBlocked:afterAwaitBlocked]@awaitThread1->beforeLock@main,"
      + "[awaitBlocked:afterAwaitBlocked]@awaitThread2->beforeLock@main") })
  public void testAwaitLockCount() throws InterruptedException {
    final ReentrantLock lock = new ReentrantLock();
    final Condition c = lock.newCondition();
    Thread t1 = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        lock.lock();
        assertEquals("AwaitLockCount", 1, lock.getHoldCount());
        fireEvent("awaitBlocked");
        c.await();
        fireEvent("afterAwaitBlocked");
        assertEquals("AwaitLockCount", 1, lock.getHoldCount());
        lock.unlock();
      }
    }, "awaitThread1");

    Thread t2 = new Thread(new CheckedRunnable() {
      public void realRun() throws InterruptedException {
        lock.lock();
        lock.lock();
        assertEquals("AwaitLockCount", 2, lock.getHoldCount());
        fireEvent("awaitBlocked");
        c.await();
        fireEvent("afterAwaitBlocked");
        assertEquals("AwaitLockCount", 2, lock.getHoldCount());
        lock.unlock();
        lock.unlock();
      }
    }, "awaitThread2");

    t1.start();
    t2.start();
    // Thread.sleep(SHORT_DELAY_MS);
    fireEvent("beforeLock");
    lock.lock();
    c.signalAll();
    lock.unlock();
    t1.join();
    t2.join();
    assertFalse("AwaitLockCount", t1.isAlive());
    assertFalse("AwaitLockCount", t2.isAlive());
  }

  /**
   * awaitNanos is interruptible
   */
  public void testAwaitNanosInterrupt() throws InterruptedException {
    final ReentrantLock lock = new ReentrantLock();
    final Condition c = lock.newCondition();
    Thread t = new Thread(new CheckedInterruptedRunnable() {
      public void realRun() throws InterruptedException {
        lock.lock();
        c.awaitNanos(MILLISECONDS.toNanos(LONG_DELAY_MS));
      }
    }, "awaitThread");

    t.start();
    Thread.sleep(SHORT_DELAY_MS);
    t.interrupt();
    t.join();
    assertFalse("AwaitInterrupt", t.isAlive());
  }

  /**
   * awaitUninterruptibly doesn't abort on interrupt
   */
  @Test
  @Schedules( { @Schedule(name = "awaitUninterruptibly", value = "[awaitBlocked:afterAwaitBlocked]@awaitThread->beforeLock@main") })
  public void testAwaitUninterruptibly() throws InterruptedException {
    final ReentrantLock lock = new ReentrantLock();
    final Condition c = lock.newCondition();
    UninterruptibleThread thread = new UninterruptibleThread(lock, c);
    thread.setName("awaitThread");
    thread.start();
    //while (!thread.lockStarted) {
    //    Thread.sleep(100);
    //}
    
    fireEvent("beforeLock");
    lock.lock();
    try {
      thread.interrupt();
      thread.canAwake = true;
      c.signal();
    } finally {
      lock.unlock();
    }
    thread.join();
    assertTrue("awaitUninterruptibly", thread.interrupted);
    assertFalse("awaitUninterruptibly", thread.isAlive());
  }

  /**
   * timed tryLock is interruptible.
   */
  public void testInterruptedException2() throws InterruptedException {
    final ReentrantLock lock = new ReentrantLock();
    lock.lock();
    Thread t = new Thread(new CheckedInterruptedRunnable() {
      public void realRun() throws InterruptedException {
        lock.tryLock(MEDIUM_DELAY_MS, MILLISECONDS);
      }
    }, "interruptedThread");

    t.start();
    Thread.sleep(SHORT_DELAY_MS);
    t.interrupt();
    t.join();
  }

}
