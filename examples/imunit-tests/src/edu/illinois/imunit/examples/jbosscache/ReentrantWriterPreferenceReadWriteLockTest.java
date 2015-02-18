package edu.illinois.imunit.examples.jbosscache;
import org.junit.runner.RunWith;
import edu.illinois.imunit.IMUnitRunner;
import static edu.illinois.imunit.IMUnit.fireEvent;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import edu.illinois.imunit.Schedule;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Tests ReentrantWriterPreferenceReadWriteLock
 * 
 * @author Bela Ban
 * @version $Id: ReentrantWriterPreferenceReadWriteLockTest.java 7295 2008-12-12
 *          08:41:33Z mircea.markus $
 */
@RunWith(IMUnitRunner.class)
public class ReentrantWriterPreferenceReadWriteLockTest {
  ReentrantReadWriteLock lock;
  Lock rl, wl;
  Exception thread_ex = null;

  @Before
  public void setUp() throws Exception {
     //lock=new ReentrantWriterPreferenceReadWriteLock();
    lock = new ReentrantReadWriteLock();
    rl = lock.readLock();
    wl = lock.writeLock();
    thread_ex = null;
  }

  @After
  public void tearDown() throws Exception {
    lock = null;
    if (thread_ex != null)
      throw thread_ex;
  }

  public void testMultipleReadLockAcquisitions() throws InterruptedException {
    rl.lock();
    rl.lock();
  }

  public void testInterruptedLockAcquisition() {
    Thread.currentThread().interrupt();
    try {
      rl.lockInterruptibly();
      fail("thread should be in interrupted status");
    } catch (InterruptedException e) {
    } finally {
      try {
        rl.unlock();
        fail("unlock() should throw an IllegalStateException");
      } catch (IllegalMonitorStateException illegalStateEx) {
        assertTrue(true);
      }
    }
  }

  public void testMultipleWriteLockAcquisitions() throws InterruptedException {
    wl.lock();
    wl.lock();
  }

  public void testMultipleReadLockReleases() throws InterruptedException {
    rl.lock();
    rl.unlock();
    try {
      rl.unlock();
      fail("we should not get here, cannot lock RL once but unlock twice");
    } catch (IllegalMonitorStateException illegalState) {
      // this is as expected
    }
  }

  public void testMultipleWriteLockReleases() throws InterruptedException {
    wl.lock();
    wl.unlock();
    try {
      wl.unlock();
      fail("expected");
    } catch (IllegalMonitorStateException e) {
    }
  }

  public void testAcquireWriteLockAfterReadLock() throws InterruptedException {
    rl.lock();
    rl.unlock();
    wl.lock();
  }


  @Test
  @Schedule(name = "default", value = "afterLock@lockThread->beforeCheck@main")
  public void testAcquiringReadLockedLockWithRead() throws InterruptedException {
    new Thread() {
      public void run() {
        try {
          Thread.currentThread().setName("lockThread");
          rl.lockInterruptibly();
          fireEvent("afterLock");
        } catch (InterruptedException e) {
        }
      }
    }.start();

    //Thread.sleep(500);


    // now we have a RL by another thread
    fireEvent("beforeCheck");
    boolean flag = rl.tryLock(3000, TimeUnit.MILLISECONDS);
    assertTrue(flag);
    flag = wl.tryLock(3000, TimeUnit.MILLISECONDS);
    assertFalse(flag);
  }


  @Test
  @Schedule(name = "default", value = "afterLock@lockThread->beforeCheck@main")
  public void testAcquiringReadLockedLock() throws InterruptedException {
    new Thread() {
      public void run() {
        try {
          Thread.currentThread().setName("lockThread");
          rl.lockInterruptibly();
          fireEvent("afterLock");
        } catch (InterruptedException e) {
        }
      }
    }.start();

    //Thread.sleep(500);
    // now we have a RL by another thread
    fireEvent("beforeCheck");
    boolean flag = wl.tryLock(3000, TimeUnit.MILLISECONDS);
    assertFalse(flag);
  }

  //WAIT@Test
  //WAIT@Schedule(name = "default", value = "afterLock@Writer->afterWriterStart@main, " + 
  //WAIT   "[beforeLock:afterLock]@Reader->afterReaderStart@main,afterReaderStart@main->beforeUnlock@Writer," +
  //WAIT" afterUnlock@Writer->beforeRelease@main, " + 
  //WAIT"beforeRelease@main->beforeUnlock@Reader")
  public void testWriteThenReadByDifferentTx() throws InterruptedException {
    Writer writer = new Writer("Writer");
    Reader reader = new Reader("Reader");
    writer.start();
    fireEvent("afterWriterStart");
    //Thread.sleep(500);
    reader.start();
    //Thread.sleep(1000);
    fireEvent("afterReaderStart");

    //OPWAIT synchronized (writer) {
    //OPWAIT   writer.notify();
    //OPWAIT }
    //Thread.sleep(500);
    fireEvent("beforeRelease");
    //OPWAIT synchronized (reader) {
    //OPWAIT   reader.notify();
    //OPWAIT }
    writer.join();
    reader.join();
  }

  //WAIT@Test
  //WAIT@Schedule(name = "default", value = "afterLock@Reader->afterReaderStart@main, " + 
  //WAIT    "[beforeLock:afterLock]@Writer->afterWriterStart@main,afterWriterStart@main->beforeUnlock@Reader," +
  //WAIT    " afterUnlock@Reader->beforeRelease@main, " + 
  //WAIT    "beforeRelease@main->beforeUnlock@Writer")
  public void testReadThenWriteByDifferentTx() throws InterruptedException {
    Writer writer = new Writer("Writer");
    Reader reader = new Reader("Reader");

    reader.start();
    fireEvent("afterReaderStart");
    //Thread.sleep(500);
    writer.start();
    fireEvent("afterWriterStart");
    //Thread.sleep(1000);

    //OPWAIT synchronized (reader) {
    //OPWAIT   reader.notify();
    //OPWAIT }
    fireEvent("beforeRelease");
    Thread.sleep(500);
    //OPWAIT synchronized (writer) {
    //OPWAIT   writer.notify();
    //OPWAIT }
    writer.join();
    reader.join();
  }

  class Reader extends Thread {

    public Reader(String name) {
      super(name);
    }

    public void run() {
      try {
        fireEvent("beforeLock");
        rl.lock();
        fireEvent("afterLock");
        //OPWAIT synchronized (this) {
        //OPWAIT   this.wait();
        //OPWAIT }
        fireEvent("beforeUnlock");
        rl.unlock();
        fireEvent("afterUnlock");
      } catch (Exception e) {
      }
    }
  }

  class Writer extends Thread {

    public Writer(String name) {
      super(name);
    }

    public void run() {
      try {
        fireEvent("beforeLock");
        wl.lock();
        fireEvent("afterLock");
        //OPWAIT synchronized (this) {
        //OPWAIT   this.wait();
        //OPWAIT }
        fireEvent("beforeUnlock");
        wl.unlock();
        fireEvent("afterUnlock");
      } catch (Exception e) {
      }
    }
  }

  class Upgrader extends Thread {
    boolean upgradeSuccessful = false;

    public Upgrader(String name) {
      super(name);
    }

    public boolean wasUpgradeSuccessful() {
      return upgradeSuccessful;
    }

    public void run() {
      try {
        rl.lock();
        synchronized (this) {
          this.wait();
        }
        // rl.unlock();
        wl.lock();
        upgradeSuccessful = true;
        wl.unlock();
      } catch (Exception e) {
      }
    }
  }

}
