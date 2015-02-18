package edu.illinois.imunit.examples.jbosscache;
import org.junit.runner.RunWith;
import edu.illinois.imunit.IMUnitRunner;
import static edu.illinois.imunit.IMUnit.fireEvent;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.jboss.cache.lock.LockStrategy;
import org.jboss.cache.lock.LockStrategyReadCommitted;
import org.jboss.cache.lock.LockStrategyReadUncommitted;
import org.jboss.cache.lock.LockStrategyRepeatableRead;
import org.jboss.cache.lock.LockStrategySerializable;
import org.junit.Test;
import org.junit.Before;

import edu.illinois.imunit.Schedule;

/**
 * Various tests that test isolation level semantics provided by locks
 * 
 * @author Bela Ban
 * @version $Id: LockTest.java 7552 2009-01-20 21:56:37Z mircea.markus $
 */
@RunWith(IMUnitRunner.class)
public class LockTest {
  int value = 10;
  Throwable t1_ex, t2_ex;
  long start = 0;
  final long TIMEOUT = 5000;
  final long SLEEP = 500;

  volatile boolean committed;

  @Before
  public void setUp() throws Exception {
    value = 10;
    t1_ex = t2_ex = null;
    committed = false;
  }

  static class MyFIFOSemaphore extends Semaphore {
    private static final long serialVersionUID = 3247961778517846603L;

    public MyFIFOSemaphore(int permits) {
      super(permits);
    }

    public void acquire() throws InterruptedException {
      super.acquire();
    }

    public void release() {
      super.release();
    }
  }

  /**
   * Thread1 reads data, thread2 changes and - before thread2 commits - t1
   * should see t2's changes. Timeline:
   * <ol>
   * T1 reads data - 10 T2 writes data - 20 T1 reads data - 20 (see's T2's
   * uncommitted modfication) T2 commits (releases its lock) T1 reads data - 20
   * </ol>
   */
  @Test
  @Schedule(name = "readUncommitted", value = "afterAcquire@t1->beforeAcquire@t2,afterWrite@t2->beforeSecondRead@t1," + 
      "afterSecondRead@t1->beforeCommit@t2, afterCommit@t2->beforeThirdRead@t1")
  public void testReadUncommitted() throws Throwable {
    final LockStrategy s = new LockStrategyReadUncommitted();
    final Semaphore sem = new MyFIFOSemaphore(1);
    final CyclicBarrier barrier = new CyclicBarrier(2);

    Thread t1 = new Thread("t1") {
      Lock lock = null;

      public void run() {
        try {
          sem.acquire(); // we're first to the semaphore
          fireEvent("afterAcquire");
          
          // log("waiting on barrier");
          barrier.await(); // wait until t2 joins us
          // log("passed barrier");
          lock = s.readLock();
          lock.tryLock(TIMEOUT, TimeUnit.MILLISECONDS);
          log("1st read: value is " + value);
          assertEquals(10, value);
          sem.release(); // give t2 time to make the modification
          //Thread.sleep(100);
          fireEvent("beforeSecondRead");
          sem.acquire(); // to read the uncommitted modification by t2
          log("2nd read: value is " + value
              + "; we should see t2's uncommitted change (20)");
          assertEquals(20, value); // we're seeing the modification by t2 before
                                   // t2 committed (a.k.a. released the lock)
          sem.release();
          fireEvent("afterSecondRead");
          //Thread.sleep(100);
          fireEvent("beforeThirdRead");
          sem.acquire(); // to read the committed change by t2
          log("3rd read: value is still " + value
              + "; we should see t2's committed change");
          assertEquals(20, value);
        } catch (Throwable ex) {
          t1_ex = ex;
        } finally {
          if (lock != null)
            lock.unlock();
          sem.release();
        }
      }
    };

    Thread t2 = new Thread("t2") {
      Lock lock = null;

      public void run() {
        try {
          //Thread.sleep(100);
          fireEvent("beforeAcquire");
          barrier.await();
          sem.acquire();
          lock = s.writeLock();
          lock.tryLock(TIMEOUT, TimeUnit.MILLISECONDS);
          log("changing value from " + value + " to 20");
          value = 20;
          sem.release(); 
          fireEvent("afterWrite");
          // now t1 can read the uncommitted modification
          //Thread.sleep(100);
          fireEvent("beforeCommit");
          sem.acquire(); // to unlock the lock
          log("committing the TX");
          lock.unlock();
        } catch (Throwable ex) {
          t2_ex = ex;
        } finally {
          if (lock != null)
            lock.unlock();
          sem.release();
          fireEvent("afterCommit");
        }
      }
    };

    t1.start();
    t2.start();
    t1.join();
    t2.join();
    if (t1_ex != null)
      throw t1_ex;
    if (t2_ex != null)
      throw t2_ex;
    
  }

  /**
   * Thread1 reads data, thread2 changes and - before thread2 commits - t1
   * should *not* see t2's changes. Timeline:
   * <ol>
   * T1 reads data - 10 T2 writes data - 20 (*not* visible to T1) T1 reads data
   * - 10 (should *not* see T2's uncommitted modfication) T2 commits (releases
   * its lock) T1 sees T2's committed modification - 20
   * </ol>
   * <em>Commented for now, until we get the right semantics</em> See
   * http://www-128.ibm.com/developerworks/java/library/j-jtp0514.html for a
   * discussion of isolation levels
   */

  // removed in favor of
  // o.j.c.transaction.IsolationLevelReadCommittedTest.testReadCommitted()
  // The lock interceptor makes one request a read lock before *each* read.

  /*
   * public void testReadCommitted() throws Throwable { final IdentityLock
   * identity_lock=new IdentityLock(IsolationLevel.READ_COMMITTED);
   * 
   * Thread t1=new Thread("t1") {
   * 
   * public void run() { try { identity_lock.acquireReadLock(this, TIMEOUT);
   * log("1st read: value is " + value); assertEquals(10, value);
   * Thread.sleep(SLEEP);
   * 
   * log("2nd read: value is " + value +
   * "; we should *not* see t2's uncommitted change (20)"); // we're seeing the
   * modification by t2 before t2 committed (a.k.a. released the lock)
   * assertEquals("This is due to incorrect impl of READ_COMMITTED", 10, value);
   * 
   * Thread.sleep(SLEEP);
   * 
   * log("3rd read: value is still " + value +
   * "; we should see t2's committed change"); assertEquals(20, value); }
   * catch(Throwable ex) { t1_ex=ex; } finally { identity_lock.unlock(this); } }
   * };
   * 
   * 
   * Thread t2=new Thread("t2") {
   * 
   * public void run() { try { Thread.sleep(100);
   * identity_lock.acquireWriteLock(this, TIMEOUT); log("changing value from " +
   * value + " to 20"); value=20; Thread.sleep(SLEEP * 2);
   * 
   * log("committing the TX"); identity_lock.unlock(this); } catch(Throwable ex)
   * { t2_ex=ex; } finally { identity_lock.unlock(this); } } };
   * 
   * t1.start(); t2.start(); t1.join(); t2.join(); if(t1_ex != null) throw
   * t1_ex; if(t2_ex != null) throw t2_ex; }
   */

  public void testWriteThanRead() throws Throwable {
    final LockStrategy s = new LockStrategyReadCommitted();

    Thread t1 = new Thread("t1") {
      Lock lock = null;

      public void run() {
        try {
          Thread.sleep(100);
          lock = s.readLock();
          lock.tryLock(TIMEOUT, TimeUnit.MILLISECONDS);
          log("1st read: value is " + value);
          assertEquals(20, value);
          Thread.sleep(SLEEP);

          log("2nd read: value is " + value
              + "; we should see t2's uncommitted change (20)");
          assertEquals(20, value); // we're seeing the modification by t2 before
                                   // t2 committed (a.k.a. released the lock)
          Thread.sleep(SLEEP);
        } catch (Throwable ex) {
          t1_ex = ex;
        } finally {
          lock.unlock();
        }
      }
    };

    Thread t2 = new Thread("t2") {
      Lock lock = null;

      public void run() {
        try {
          lock = s.writeLock();
          lock.tryLock(TIMEOUT, TimeUnit.MILLISECONDS);
          log("changing value from " + value + " to 20");
          value = 20;
          Thread.sleep(SLEEP);

          log("committing the TX");
          lock.unlock();
        } catch (Throwable ex) {
          t2_ex = ex;
        } finally {
          lock.unlock();
        }
      }
    };

    t2.start();
    t1.start();
    t2.join();
    t1.join();
    if (t1_ex != null)
      throw t1_ex;
    if (t2_ex != null)
      throw t2_ex;
  }

  /**
   * Thread1 reads data, thread2 changes and - before thread2 commits - t1
   * should *not* see t2's changes. In addition, Thread1 should *not* see
   * thread2's changes even after thread2 commits, until thread1 commits.
   * Timeline:
   * <ol>
   * T1 reads data - 10 T2 writes data - 20 (*not* visible to T1) T1 reads data
   * - 10 (should *not* see T2's uncommitted modfication) T2 commits (releases
   * its lock) T1 reads data, should *not* see T2's committed modification - 10
   * T1 commits T1 starts a new TX - should see 20
   * </ol>
   * Note: because we use pessimistic locking, the above sequence will
   * effectively be serialized into sequential execution: thread1 will acquire
   * the read lock on the data and hold on to it until TX commit, only then will
   * thread2 be able to access the data with a write lock.
   */
  public void testRepeatableRead() throws Throwable {
    final LockStrategy s = new LockStrategyRepeatableRead();

    Thread t1 = new Thread("t1") {
      Lock lock = null;

      public void run() {
        try {
          lock = s.readLock();
          lock.tryLock(TIMEOUT, TimeUnit.MILLISECONDS);
          log("1st read: value is " + value);
          assertEquals(10, value);
          //Thread.sleep(SLEEP);

          log("2nd read: value is " + value
              + "; we should *not* see t2's uncommitted change (20)");
          assertEquals(10, value);
          //Thread.sleep(SLEEP);

          log("3rd read: value is still " + value
              + "; we should not see t2's committed change");
          assertEquals(10, value);
          lock.unlock();
          fireEvent("afterUnlock");
          
          //Thread.sleep(SLEEP);
          fireEvent("beforeFourthRead");
          lock.tryLock(TIMEOUT, TimeUnit.MILLISECONDS);
          log("4th read: value is now " + value
              + "; we should see t2's committed change in our new TX");
          assertEquals(20, value);
        } catch (Throwable ex) {
          System.out.println(ex);
          t1_ex = ex;
        } finally {
          lock.unlock();
        }
      }
    };

    Thread t2 = new Thread("t2") {
      Lock lock = null;

      public void run() {
        try {
          //Thread.sleep(100);
          fireEvent("beforeWrite");
          lock = s.writeLock();
          lock.tryLock(TIMEOUT, TimeUnit.MILLISECONDS);
          log("changing value from " + value + " to 20");
          value = 20;
          //Thread.sleep(SLEEP);

          fireEvent("beforeCommit");
          log("committing the TX");
          lock.unlock();
          fireEvent("afterCommit");
        } catch (Throwable ex) {
          t2_ex = ex;
        } finally {
          lock.unlock();
        }
      }
    };

    t1.start();
    t2.start();
    t1.join();
    t2.join();
    if (t1_ex != null)
      throw t1_ex;
    if (t2_ex != null)
      throw t2_ex;
  }

  /**
   * Because only 1 reader or writer can hold the lock at any given time, since
   * thread1 is the first to get the lock, it will hold on to it until it
   * commits. The the writer thread (thread2) will have a chance to change the
   * value. Timeline:
   * <ol>
   * T1 reads data - 10 T1 commits T2 writes data - 20 T2 commits T1 starts a
   * new TX and reads data - 20 T2 commits (releases its lock)
   * </ol>
   */
  @Test
  @Schedule(name = "serializable", value = "afterFirstRead@t1->beforeWrite@t2,afterWrite@t2->beforeSecondRead@t1")
  public void testSerializable() throws Throwable {
    final LockStrategy s = new LockStrategySerializable();

    Thread t1 = new Thread("t1") {
      Lock lock = null;

      public void run() {
        try {
          lock = s.readLock();
          lock.tryLock(TIMEOUT, TimeUnit.MILLISECONDS);
          log("1st read: value is " + value);
          assertEquals(10, value);
          lock.unlock();
          fireEvent("afterFirstRead");
          //Thread.sleep(SLEEP);

          fireEvent("beforeSecondRead");
          lock.tryLock(TIMEOUT, TimeUnit.MILLISECONDS);
          log("2nd read: value is " + value
              + "; we should see t2's committed change (20)");
          assertEquals(20, value);
        } catch (Throwable ex) {
          t1_ex = ex;
        } finally {
          lock.unlock();
        }
      }
    };

    Thread t2 = new Thread("t2") {
      Lock lock = null;

      public void run() {
        try {
          //Thread.sleep(100);
          fireEvent("beforeWrite");
          lock = s.writeLock();
          lock.tryLock(TIMEOUT, TimeUnit.MILLISECONDS);
          log("changing value from " + value + " to 20");
          value = 20;
          log("committing the TX");
          lock.unlock();
        } catch (Throwable ex) {
          t2_ex = ex;
        } finally {
          lock.unlock();
	  fireEvent("afterWrite");
        }
      }
    };

    t1.start();
    t2.start();
    t1.join();
    t2.join();
    if (t1_ex != null)
      throw t1_ex;
    if (t2_ex != null)
      throw t2_ex;
  }

  void log(String s) {
    // long now;
    // if (start == 0)
    //   start = System.currentTimeMillis();
    // now = System.currentTimeMillis();
    // System.out.println(s);
  }

}
