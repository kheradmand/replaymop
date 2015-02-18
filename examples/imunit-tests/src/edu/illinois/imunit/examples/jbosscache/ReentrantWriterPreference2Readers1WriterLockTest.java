/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package edu.illinois.imunit.examples.jbosscache;
import org.junit.runner.RunWith;
import edu.illinois.imunit.IMUnitRunner;
import static edu.illinois.imunit.IMUnit.fireEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.Test;

import edu.illinois.imunit.Schedule;

/**
 * Tests ReentrantWriterPreferenceReadWriteLock
 * 
 * @author Bela Ban
 * @version $Id: ReentrantWriterPreference2Readers1WriterLockTest.java 7295
 *          2008-12-12 08:41:33Z mircea.markus $
 */
// historical disabled - and wont fix. See JBCACHE-461
@RunWith(IMUnitRunner.class)
public class ReentrantWriterPreference2Readers1WriterLockTest {
  ReentrantReadWriteLock lock;
  ReentrantReadWriteLock.ReadLock rl;
  ReentrantReadWriteLock.WriteLock wl;

  public void setUp() throws Exception {
    lock = new ReentrantReadWriteLock();
    rl = lock.readLock();
    wl = lock.writeLock();
  }

  public void tearDown() throws Exception {
    lock = null;
  }

  public void testSimpleUpgradeFromReadLockToWriteLock() {
    int readers, writers;
    try {
      rl.lock();
      readers = lock.getReadLockCount();
      assertEquals(1, readers);
      boolean wl_acquired = wl.tryLock(500, TimeUnit.MILLISECONDS);
      if (!wl_acquired) {
        fail("write lock could not be acquired");
        return;
      }
      readers = lock.getReadLockCount();
      assertEquals(1, readers);
      writers = lock.getWriteHoldCount();
      assertEquals(1, writers);
    } catch (InterruptedException e) {

    } finally {
      rl.unlock();
      if (lock.getWriteHoldCount() > 0)
        wl.unlock();
    }
  }

  //  @NTest
  //  @NSchedule(name = "readersAnd1Writer", value = "afterLock@Reader->check1@main,"
  //      + "afterLock@Upgrader->check2@main, [beforeSecondLock:afterSecondLock]@Upgrader->check3@main,"
  //      + "afterCheck3@main->finish@Reader, afterSecondLock@Upgrader->check4@main,beforeCheck5@main->beforeUnlock@Upgrader"
  //      + ", afterUnlock@Upgrader->check5@main")
  public void test2ReadersAnd1Writer() throws InterruptedException {
    try {
      this.setUp();
    } catch (Exception e) {
      e.printStackTrace();
    }
    int readers, writers;
    Upgrader upgrader = new Upgrader("Upgrader");
    Reader reader = new Reader("Reader");
    reader.start();
    // sleepThread(500);
    /* @NEvent("check1") */
    System.out.println("c1");
    readers = lock.getReadLockCount();
    assertEquals(1, readers);

    upgrader.start();
    // sleepThread(500);
    /* @NEvent("check2") */
    System.out.println("c2");
    readers = lock.getReadLockCount();
    assertEquals(2, readers);

    // synchronized (upgrader) { // writer upgrades from RL to WL, this should
    // fail
    // upgrader.notify();
    // }
    // sleepThread(500);

    /* @NEvent("check3") */
    System.out.println("c3");
    readers = lock.getReadLockCount();
    assertEquals(2, readers);
    writers = lock.getWriteHoldCount();
    assertEquals(0, writers);

    // synchronized (reader) { // reader unlocks its RL, now writer should be
    // able
    // // to upgrade to a WL
    // reader.notify();
    // }
    /* @NEvent("afterCheck3") */
    reader.join();
    System.out.println("c3.5");
    /* @NEvent("check4") */
    System.out.println("c4");
    readers = lock.getReadLockCount();
    assertEquals(1, readers);
    writers = lock.getWriteHoldCount();
    assertEquals(1, writers);

    // synchronized (upgrader) { // writer releases WL
    // upgrader.notify();
    // }
    /* @NEvent("beforeCheck5") */
    // sleepThread(500);
    /* @NEvent("check5") */
    System.out.println("c5");
    readers = lock.getReadLockCount();
    assertEquals(0, readers);
    writers = lock.getWriteHoldCount();
    assertEquals(0, writers);

    upgrader.join(3000);
    assertTrue(
        "Known failure. See JBCACHE-461; This is due to a potential bug in ReentrantWriterPreferenceReadWriteLock !",
        upgrader.wasUpgradeSuccessful());
    try {
      this.tearDown();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private class Reader extends Thread {

    public Reader(String name) {
      super(name);
    }

    public void run() {
      try {
        rl.lock();
        fireEvent("afterLock");
        // synchronized (this) {
        // this.wait();
        // }
        fireEvent("finish");
      } catch (Exception e) {
      } finally {
        rl.unlock();
      }
    }
  }

  private class Upgrader extends Thread {
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
        fireEvent("afterLock");
        // synchronized (this) {
        // this.wait();
        // }
        fireEvent("beforeSecondLock");
        wl.lock();
        fireEvent("afterSecondLock");
        upgradeSuccessful = true;

        // synchronized (this) {
        // this.wait();
        // }
        fireEvent("beforeUnlock");
        rl.unlock();
        fireEvent("afterUnlock");
      } catch (Exception e) {
      } finally {
        wl.unlock();
        rl.unlock();
      }
    }
  }

  static void sleepThread(long timeout) {
    try {
      Thread.sleep(timeout);
    } catch (InterruptedException e) {
    }
  }

}
