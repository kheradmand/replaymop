/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package edu.illinois.imunit.examples.jbosscache;
import org.junit.runner.RunWith;
import edu.illinois.imunit.IMUnitRunner;
import static edu.illinois.imunit.IMUnit.fireEvent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.jboss.cache.NodeSPI;
import org.jboss.cache.lock.IdentityLock;
import org.jboss.cache.lock.IsolationLevel;
import org.jboss.cache.lock.LockMap;
import org.jboss.cache.lock.LockStrategyFactory;
import org.jboss.cache.lock.LockingException;
import org.jboss.cache.lock.NodeLock;
import org.jboss.cache.lock.TimeoutException;
import org.junit.Test;

import edu.illinois.imunit.Schedule;

/**
 * Testing of different locking semantics.
 * 
 * @author Bela Ban
 * @author Ben Wang
 * @version $Revision: 7567 $
 */
@RunWith(IMUnitRunner.class)
public class IdentityLockTest {
  NodeLock lock_;
  Object other_ = new Object();
  static Throwable thread_ex = null;
  final NodeSPI NODE = null;

  public void setUp() throws Exception {
    lock_ = new IdentityLock(new LockStrategyFactory(), NODE);
  }

  public void tearDown() throws Exception {
    lock_.releaseAll();
    lock_ = null;
    thread_ex = null;
  }

  private void setLevelRW() {
    log("set lock level to RWUpgrade ...");
    LockStrategyFactory lsf = new LockStrategyFactory();
    lsf.setIsolationLevel(IsolationLevel.REPEATABLE_READ);
    lock_ = new IdentityLock(lsf, NODE);
  }

  private void setLevelSerial() {
    log("set lock level to SimpleLock ...");
    LockStrategyFactory lsf = new LockStrategyFactory();
    lsf.setIsolationLevel(IsolationLevel.SERIALIZABLE);
    lock_ = new IdentityLock(lsf, NODE);
  }

  public void testAcquireAndRelease_RWLock() throws InterruptedException {
    setLevelRW();
    acquireAndRelease();
  }

  public void testAcquireAndRelease_SimpleLock() throws InterruptedException {
    setLevelSerial();
    acquireAndRelease();
  }

  private void acquireAndRelease() throws InterruptedException {
    log("testAcquireAndRelease ...");
    try {
      lock_.acquireReadLock(this, 0);
      assertTrue("Is the lock owner", lock_.isOwner(this));
      assertTrue(lock_.getReaderOwners().contains(this));

      lock_.acquireReadLock(this, 0);// this should succeed
      assertTrue("Is the lock owner", lock_.isOwner(this));
      assertTrue(lock_.getReaderOwners().contains(this));

      lock_.acquireWriteLock(this, 0);// this should succeed
      assertTrue("Is the lock owner", lock_.isOwner(this));
      assertTrue(!lock_.getReaderOwners().contains(this));
      assertTrue(lock_.getWriterOwner().equals(this));

      lock_.release(this);
      assertTrue(!lock_.isOwner(this));
    } catch (LockingException e) {
      fail(e.toString());
    } catch (TimeoutException e2) {
      fail(e2.toString());
    }
  }

  // public void testThreadedAccess_RWLock() throws Throwable
  // {
  // setLevelRW();
  // log("testThreadedAccess_RWLock ...");
  // final Object o1 = new Object();
  // final Object o2 = new Object();
  //
  // // 1. o1 acquires the lock -- succeeds
  // Thread t1 = new Thread()
  // {
  // public void run()
  // {
  // try
  // {
  // log("o1 acquiring lock");
  // lock_.acquireReadLock(o1, 0);
  // log("o1: OK");
  // }
  // catch (Throwable e)
  // {
  // log("o1: FAIL");
  // thread_ex = e;
  // }
  // }
  // };
  //
  // // 2. o2 wants to acquire the lock -- this will fail and o2 will block for
  // 2 secs
  // Thread t2 = new Thread()
  // {
  // public void run()
  // {
  // try
  // {
  // log("o2 acquiring lock");
  // lock_.acquireWriteLock(o2, 2000);
  // log("o2: OK");
  // }
  // catch (Throwable e)
  // {
  // log("o2: FAIL");
  // thread_ex = e;
  // }
  // }
  // };
  //
  // // 3. o1 acquires the lock a second time -- succeeds
  // Thread t3 = new Thread()
  // {
  // public void run()
  // {
  // try
  // {
  // log("o1 acquiring lock");
  // lock_.acquireWriteLock(o1, 10);
  // log("o1: OK");
  // }
  // catch (Throwable e)
  // {
  // log("o1: FAIL");
  // thread_ex = e;
  // }
  // }
  // };
  //
  // t1.start();
  // TestingUtil.sleepThread(100);
  // t2.start();
  // TestingUtil.sleepThread(1000);
  //
  // // o1 must be the owner of the lock
  // assertTrue(lock_.isOwner(o1));
  // TestingUtil.sleepThread(100);
  // // o1 must still be the owner of the lock
  // assertTrue(lock_.isOwner(o1));
  //
  // t3.start();
  // TestingUtil.sleepThread(100);
  // // o1 must still be the owner of the lock
  // assertTrue(lock_.isOwner(o1));
  //
  // // 4. o1 releases the lock; now o2 will succeed in acquiring the lock
  // log("o1 releasing lock");
  // lock_.release(o1);
  // log("o1: OK");
  //
  // TestingUtil.sleepThread(200);
  // //log("o2: " + o2.hashCode() + ", lock_.getOwner()=" + lock_.getOwner());
  // // assertTrue(lock_.isOwner(o2));
  // // lock_.release(o2);
  //
  // t1.join(20000);
  // t2.join(20000);
  // t3.join(20000);
  // if (thread_ex != null)
  // {
  // throw thread_ex;
  // }
  // }

  // public void testThreadedAccess_SimpleLock() throws Throwable
  // {
  // setLevelSerial();
  // log("testThreadedAccess_SimpleLock() ...");
  // final Object o1 = new Object();
  // final Object o2 = new Object();
  //
  // // 1. o1 acquires the lock -- succeeds
  // Thread t1 = new Thread()
  // {
  // public void run()
  // {
  // try
  // {
  // log("o1 acquiring lock");
  // lock_.acquireReadLock(o1, 0);
  // log("o1: OK");
  // }
  // catch (Throwable e)
  // {
  // log("o1: FAIL");
  // thread_ex = e;
  // }
  // }
  // };
  //
  // // 2. o2 wants to acquire the lock -- this will fail and o2 will block for
  // 2 secs
  // Thread t2 = new Thread()
  // {
  // public void run()
  // {
  // try
  // {
  // log("o2 acquiring lock");
  // lock_.acquireWriteLock(o2, 2000);
  // log("o2: OK");
  // }
  // catch (Throwable e)
  // {
  // log("o2: FAIL");
  // thread_ex = e;
  // }
  // }
  // };
  //
  // // 3. o1 acquires the lock a second time -- succeeds
  // Thread t3 = new Thread()
  // {
  // public void run()
  // {
  // try
  // {
  // log("o1 acquiring lock");
  // lock_.acquireWriteLock(o1, 10);
  // log("o1: OK");
  // }
  // catch (Throwable e)
  // {
  // log("o1: FAIL");
  // thread_ex = e;
  // }
  // }
  // };
  //
  // t1.start();
  // // make sure t1 has the WL!
  // t1.join();
  //
  // t2.start();
  //
  //
  // // o1 must be the owner of the lock
  // assertTrue(lock_.isOwner(o1));
  //
  // t3.start();
  // assertTrue(lock_.isOwner(o1));
  //
  // log("o1 releasing lock");
  // lock_.release(o1);
  //
  // t2.join(20000);
  // t3.join(20000);
  // if (thread_ex != null)
  // {
  // throw thread_ex;
  // }
  // }

  @Test
  @Schedule(name = "readAndReleaseAll", value = "afterRelease@t1->beforeCheck@main")
  public void testReadAndReleaseAll() {
    try {
      this.setUp();
    } catch (Exception e1) {
      e1.printStackTrace();
    }
    setLevelRW();
    log("testReadAndReleaseAll() ...");
    final Object o1 = new Object();
    final Object o2 = new Object();

    // 1. o1 acquires the lock -- succeeds
    try {
      log("o1: acquiring");
      lock_.acquireReadLock(o1, 0);
      log("o1: OK");
      log("o2: acquiring");
      lock_.acquireReadLock(o2, 0);
      log("o2: OK");
    } catch (Throwable t) {
        t.printStackTrace();  
      log("read lock: FAIL");
      fail(t.getMessage());
    }

    Thread t1 = new Thread() {
      public void run() {
        try {
          log("calling releaseAll()");
          lock_.releaseAll();
          log("releaseAll(): OK");
          fireEvent("afterRelease");
        } catch (Throwable e) {
          log("releaseAll(): FAIL");
          thread_ex = e;
        }
      }
    };

    try {
      t1.setDaemon(true);
      t1.setName("t1");
      t1.start();
      try {
        // Thread.sleep(1000);
      } catch (Exception e) {
        e.printStackTrace();
      }

      fireEvent("beforeCheck");
      assertFalse("Lock map cleared", lock_.isReadLocked());
    } finally {
      // Manually release the locks so tearDown() will not fail
      // if there is a problem with releaseAll()
      lock_.release(o1);
      lock_.release(o2);
    }
    try {
      this.tearDown();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  @Schedule(name = "writeAndReleaseAll", value = "afterRelease@t1->beforeCheck@main")
  public void testWriteAndReleaseAll() {
    try {
      this.setUp();
    } catch (Exception e1) {
      e1.printStackTrace();
    }
    setLevelSerial();
    log("testWriteAndReleaseAll() ...");
    final Object o1 = new Object();

    // 1. o1 acquires the lock -- succeeds
    try {
      log("o1: acquiring");
      lock_.acquireWriteLock(o1, 0);
      log("o1: OK");
    } catch (Throwable t) {
      log("write lock: FAIL");
      fail(t.getMessage());
    }

    Thread t1 = new Thread() {
      public void run() {
        try {
          log("calling releaseAll()");
          lock_.releaseAll();
          log("releaseAll(): OK");
          fireEvent("afterRelease");
        } catch (Throwable e) {
          log("releaseAll(): FAIL");
          thread_ex = e;
        }
      }
    };

    try {
      t1.setDaemon(true);
      t1.setName("t1");
      t1.start();
      try {
        // Thread.sleep(1000);
      } catch (Exception e) {
        e.printStackTrace();
      }

      fireEvent("beforeCheck");
      assertFalse("Lock map cleared", lock_.isReadLocked());
    } finally {
      // Manually release the lock so tearDown() will not fail
      // if there is a problem with releaseAll()
      lock_.release(o1);
    }
    try {
      this.tearDown();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  void log(String msg) {
  }

  /**
   * When IdentityLock.toString is called and readLockOwners are modified an
   * ConcurrentModificationException exception might be thrown.
   */
  public void testConcurrentModificationOfReadLocksAndToString()
      throws Exception {
    final IdentityLock iLock = (IdentityLock) lock_;
    final LockMap lockMap = iLock.getLockMap();
    final int opCount = 100000;
    final Thread readLockChanger = new Thread() {
      public void run() {
        for (int i = 0; i < opCount; i++) {
          lockMap.addReader(new Object());
        }
      }
    };

    final boolean[] flags = new boolean[] { false, false }; // {testFailure,
    // stopTheThread}
    Thread toStringProcessor = new Thread() {
      public void run() {
        for (int i = 0; i < opCount; i++) {
          try {
            iLock.toString(new StringBuilder(), false);
          } catch (Exception e) {
            e.printStackTrace();
            flags[0] = true;
            break;
          }
          if (flags[1])
            break;
        }
      }
    };
    toStringProcessor.start();
    readLockChanger.start();

    readLockChanger.join();
    flags[1] = true;// stopping the toStringProcessor

    toStringProcessor.join();

    assertFalse(flags[0]);
  }
}
