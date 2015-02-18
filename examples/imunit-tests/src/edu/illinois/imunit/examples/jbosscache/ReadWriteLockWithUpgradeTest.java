package edu.illinois.imunit.examples.jbosscache;
import org.junit.runner.RunWith;
import edu.illinois.imunit.IMUnitRunner;
import static edu.illinois.imunit.IMUnit.fireEvent;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.jboss.cache.lock.ReadWriteLockWithUpgrade;
import org.junit.Test;

import edu.illinois.imunit.Schedule;
import edu.illinois.imunit.Schedules;

/**
 * NonBlockingWriterLock is a read/write lock (with upgrade) that has
 * non-blocking write lock acquisition on existing read lock(s).
 * <p>
 * Note that the write lock is exclusive among write locks, e.g., only one write
 * lock can be granted at one time, but the write lock is independent of the
 * read locks. For example, a read lock to be acquired will be blocked if there
 * is existing write lock, but will not be blocked if there are mutiple read
 * locks already granted to other owners. On the other hand, a write lock can be
 * acquired as long as there is no existing write lock, regardless how many read
 * locks have been granted.
 * 
 * @author <a href="mailto:cavin_song@yahoo.com">Cavin Song</a> April 22, 2004
 * @version 1.0
 */
@RunWith(IMUnitRunner.class)
public class ReadWriteLockWithUpgradeTest {
  static final ReadWriteLockWithUpgrade lock_ = new ReadWriteLockWithUpgrade();
  static long SLEEP_MSECS = 500;
  Vector<Object> lockResult = new Vector<Object>();
  int NO_MORE_OP = 0;
  int INVOKE_READ = 1;
  int INVOKE_WRITE = 2;
  int INVOKE_UPGRADE = 3;

  public void tearDown() {
    cleanLockingResult();
  }

  /***************************************************************/
  /* Utility functions to creat threads for RL, WL and UL */
  /***************************************************************/
  /**
   * Creates a new thread and acquires a read lock with a timeout value
   * specified by the caller. Optionally, the caller can request a second read
   * or write lock after the first read lock request. The locking result is
   * stored in a vector with the following format:
   * <p>
   * <DL>
   * <DD>'case number'-'thread name'-[RL|WL|UL]-[0|1]
   * </DL>
   * <p>
   * where:
   * <DL>
   * <DD>'case number' is the passed in test case # by the caller.
   * <DD>'thread name' is the passed in thread name by the caller.
   * <DD>RL - indicating was doing read lock request.
   * <DD>WL - indicating was doing write lock request.
   * <DD>UL - indicating was doing upgrade lock request.
   * <DD>0 - indicating the locking request failed.
   * <DD>1 - indicating the locking request succeeded.
   * </DL>
   * <p/>
   * After all threads in each test case terminate, the test case should make
   * the following call to verify the test result:
   * <DL>
   * <DD>asssertTrue(checkLockingResult(expected-result-string);
   * </DL>
   * <p/>
   *'expected-result-string' is the locking result string described above. For
   * example, "8-t1-RL-0" means that thread t1 in test case #8 doing a Read Lock
   * request expects the operation to fail. If the expected result string can't
   * be found then the test case is considered FAILED (ie, either the read lock
   * request was successful or did not complete).
   * <p/>
   * Each test case should also call cleanLockingResult() to reset result vector
   * for the next test cases.
   */

  class ReadThread extends Thread {

    private String caseNum;
    private String name;
    volatile CountDownLatch notifyBeforeSecondOp = new CountDownLatch(1);
    volatile CountDownLatch notifyBeforeFinish = new CountDownLatch(1);
    volatile CountDownLatch waitLatch;
    private String errMsg;
    private int secondOP;

    ReadThread(String caseNum, String name, String errMsg, int secondOP,
        CountDownLatch waitLatch) {
      this.caseNum = caseNum;
      this.name = name;
      this.errMsg = errMsg;
      this.secondOP = secondOP;
      this.waitLatch = waitLatch;
    }

    public void run() {
      Thread.currentThread().setName(name);
      Lock rlock = lock_.readLock();
      try {
        fireEvent("beforeFirstLock");
        if (!rlock.tryLock(0, TimeUnit.MILLISECONDS)) {
          fireEvent("errorLock");
          String str = caseNum + "-" + name + "-RL-0";
          postLockingResult(str);
          processNotifications();
          return;
        }
        fireEvent("afterFirstLock");
        // OK, read lock obtained, sleep and release it.
        String str = caseNum + "-" + name + "-RL-1";
        postLockingResult(str);
        processNotifications();

        if (secondOP == INVOKE_READ) {
          acquireReadLock(caseNum, name, errMsg);
        } else if (secondOP == INVOKE_WRITE) {
          acquireWriteLock(caseNum, name, errMsg);
        } else if (secondOP == INVOKE_UPGRADE) {
          acquireUpgradeLock(caseNum, name, errMsg);
        }
        fireEvent("beforeFirstUnlock");
        rlock.unlock();
        notifyBeforeFinish.countDown();
      } catch (Exception ex) {
          ex.printStackTrace();
      }
    }

    private void processNotifications() throws InterruptedException {
      notifyBeforeSecondOp.countDown();
      //OPWAIT if (waitLatch != null)
      //OPWAIT   waitLatch.await(10, TimeUnit.SECONDS);
    }
  }

  private class WriteThread extends Thread {
    String caseNum;
    String name;
    volatile CountDownLatch notifyLatch = new CountDownLatch(1);
    volatile CountDownLatch waitLatch;
    String errMsg;
    int secondOP;

    WriteThread(String caseNum, String name, String errMsg, int secondOP,
        CountDownLatch waitLatch) {
      this.caseNum = caseNum;
      this.name = name;
      this.errMsg = errMsg;
      this.secondOP = secondOP;
      this.waitLatch = waitLatch;
    }

    public void run() {
      Thread.currentThread().setName(name);
      try {
        Lock wlock = lock_.writeLock();
        fireEvent("beforeFirstLock");
        if (!wlock.tryLock(0, TimeUnit.MILLISECONDS)) {
          fireEvent("errorLock");
          String str = caseNum + "-" + name + "-WL-0";
          postLockingResult(str);
          processNotifications();
          return;
        }
        fireEvent("afterFirstLock");
        // OK, write lock obtained, sleep and release it.
        String str = caseNum + "-" + name + "-WL-1";
        postLockingResult(str);

        processNotifications();

        if (secondOP == INVOKE_READ) {
          acquireReadLock(caseNum, name, errMsg);
        } else if (secondOP == INVOKE_WRITE) {
          acquireWriteLock(caseNum, name, errMsg);
        } else if (secondOP == INVOKE_UPGRADE) {
          acquireUpgradeLock(caseNum, name, errMsg);
        }

        fireEvent("beforeFirstUnlock");
        wlock.unlock();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    private void processNotifications() throws InterruptedException {
      notifyLatch.countDown();
      //OPWAIT if (waitLatch != null)
      //OPWAIT   waitLatch.await(10, TimeUnit.SECONDS);
    }
  }

  class UpgradeThread extends Thread {
    String caseNum;
    String name;
    private CountDownLatch notifyBeforeFinish = new CountDownLatch(1);
    private CountDownLatch notifyBeforeSecondOp = new CountDownLatch(1);
    private CountDownLatch beforeUpgrateWait;
    private CountDownLatch beforeFinishWait;
    String errMsg;
    int secondOP;

    UpgradeThread(String caseNum, String name, String errMsg, int secondOP,
        CountDownLatch beforeUpgrateWait) {
      this.caseNum = caseNum;
      this.name = name;
      this.errMsg = errMsg;
      this.secondOP = secondOP;
      this.beforeUpgrateWait = beforeUpgrateWait;
    }

    public void run() {
      try {
        Lock rlock = lock_.readLock();
        Lock wlock;
        fireEvent("beforeFirstLock");
        if (!rlock.tryLock(0, TimeUnit.MILLISECONDS)) {
          fireEvent("errorLock");
          String str = caseNum + "-" + name + "-UL-0";
          postLockingResult(str);
          notifyBeforeFinish.countDown();
          if (beforeUpgrateWait != null)
            beforeUpgrateWait.await(10, TimeUnit.SECONDS);
          return;
        }
        fireEvent("afterFirstLock");
        // OK, read lock obtained, sleep and upgrade it later.
        notifyBeforeSecondOp.countDown();
        //OPWAIT if (beforeUpgrateWait != null)
        //OPWAIT   beforeUpgrateWait.await(10, TimeUnit.SECONDS);
        String str = caseNum + "-" + name + "-UL-";
        if ((wlock = lock_.upgradeLockAttempt(0)) == null) {
          str += "0";
        } else {
          str += "1";
        }
        postLockingResult(str);
        // Sleep again and then release the lock.
        // Thread.sleep(SLEEP_MSECS);
        if (wlock != null) {
          wlock.unlock();
        }
        rlock.unlock();
        notifyBeforeFinish.countDown();
        //OPWAIT if (beforeFinishWait != null)
        //OPWAIT    beforeFinishWait.await(10, TimeUnit.SECONDS);
      } catch (Exception ex) {
      }
    }

  }

  /***************************************************************/
  /* Utility functions to acquire RL and WL (no thread) */
  /***************************************************************/
  /**
   * This routine tries to acquire a read lock with a timeout value passed in by
   * the caller. It then stores the locking result in the result vector
   * depending on the outcome of the request.
   */

  protected void acquireReadLock(final String caseNum, final String name,
      final String errMsg) {
    try {
      Lock rlock = lock_.readLock();
      fireEvent("beforeReadLock");
      if (!rlock.tryLock(0, TimeUnit.MILLISECONDS)) {
        fireEvent("errorReadLock");
        String str = caseNum + "-" + name + "-RL-0";
        postLockingResult(str);
        return;
      }
      // OK, read lock obtained, sleep and release it.
      fireEvent("afterReadLock");
      String str = caseNum + "-" + name + "-RL-1";
      postLockingResult(str);
      // Thread.sleep(SLEEP_MSECS);
      rlock.unlock();
    } catch (Exception ex) {
    }
  }

  /**
   * Same as {@link #acquireReadLock acquireReadLock()} except it's for write
   * lock request.
   */
  protected void acquireWriteLock(final String caseNum, final String name,
      final String errMsg) {
    try {
      Lock wlock = lock_.writeLock();
      fireEvent("beforeWriteLock");
      if (!wlock.tryLock(0, TimeUnit.MILLISECONDS)) {
        fireEvent("errorWriteLock");
        String str = caseNum + "-" + name + "-WL-0";
        postLockingResult(str);
        return;
      }
      // OK, write lock obtained, sleep and release it.
      fireEvent("afterWriteLock");
      String str = caseNum + "-" + name + "-WL-1";
      postLockingResult(str);
      // Thread.sleep(SLEEP_MSECS);
      wlock.unlock();
    } catch (Exception ex) {
    }
  }

  /**
   * Same as {@link #acquireReadLock acquireReadLock()} except it's for upgrade
   * lock request.
   */
  protected void acquireUpgradeLock(final String caseNum, final String name,
      final String errMsg) {
    try {
      Lock ulock = null;
      fireEvent("beforeUpLock");
      if ((ulock = lock_.upgradeLockAttempt(0)) == null) {
        fireEvent("errorUpLock");
        String str = caseNum + "-" + name + "-UL-0";
        postLockingResult(str);
        return;
      }
      fireEvent("afterUpLock");
      // OK, write lock obtained, sleep and release it.
      String str = caseNum + "-" + name + "-UL-1";
      postLockingResult(str);
      Thread.sleep(SLEEP_MSECS);
      ulock.unlock();
    } catch (Exception ex) {
    }
  }

  /***************************************************************/
  /* Synchronized methods handling locking result vector */
  /***************************************************************/
  /**
   * Clean/remove all locking results in the vector.
   */
  protected synchronized void cleanLockingResult() {
    lockResult.removeAllElements();
  }

  /**
   * Post a locking result to the vector for later verification.
   */
  protected synchronized void postLockingResult(Object obj) {
    // Make sure we only have one in the vector
    // if (!checkLockingResult((String)obj))
    lockResult.addElement(obj);
  }

  /**
   * Check if a given expected locking result is in the vector.
   */
  protected synchronized boolean checkLockingResult(String expected) {
    boolean rc = false;
    for (int i = 0; i < lockResult.size(); i++) {
      Object ele = lockResult.elementAt(i);
      String str = (String) ele;
      if (expected.equals(str)) {
        rc = true;
        break;
      }
    }
    return rc;
  }

  /***************************************************************/
  /* T e s t C a s e s */
  /***************************************************************/
  /**
   * Case #10 - T1 acquires RL, T2 acquires RL followed by WL.
   */
  //WAIT@Test
  //WAIT@Schedule(name = "writeWithMultipleReaders", value = "errorWriteLock@t2->beforeFirstUnlock@t1,"
  //WAIT   + "afterFirstLock@t1->beforeWriteLock@t2")
  public void testWriteWithMultipleReaders() throws Exception {
    String caseNum = "10";
    ReadThread t1 = new ReadThread(caseNum, "t1",
        "1st read lock attempt failed", NO_MORE_OP, null);
    ReadThread t2 = new ReadThread(caseNum, "t2",
        "2nd read lock attempt failed", INVOKE_WRITE,  t1.notifyBeforeSecondOp);

    t1.start();
    t2.start();
    t1.join();
    t2.join();
    //t1.join(3000);
    //t2.join(3000);
    assertTrue(checkLockingResult(caseNum + "-t1-RL-1")
        && checkLockingResult(caseNum + "-t2-RL-1")
        && checkLockingResult(caseNum + "-t2-WL-0"));
    cleanLockingResult();
    // possilbe deadlock check
    if (t1.isAlive() || t2.isAlive()) {
      fail("Possible deadlock resulted in testRead.");
    }
  }

  /**
   * Case #11 - T1 acquires RL followed by WL, T2 acquires RL.
   */
  //WAIT@Test
  //WAIT@Schedule(name = "upgradeWithMultipleReadersOn1", value = "errorWriteLock@t1->beforeFirstUnlock@t2,"
  //WAIT    + "afterFirstLock@t2->beforeWriteLock@t1")
  public void testUpgradeWithMultipleReadersOn1() throws Exception {
    String caseNum = "11";
    ReadThread t1 = new ReadThread(caseNum, "t1",
        "1st read lock attempt failed", INVOKE_WRITE, null);
    ReadThread t2 = new ReadThread(caseNum, "t2",
        "2nd read lock attempt failed", NO_MORE_OP, t1.notifyBeforeSecondOp);

    t1.start();
    t2.start();
    t1.join();
    t2.join();
    //t1.join(3000);
    //t2.join(3000);
    assertTrue(checkLockingResult(caseNum + "-t1-RL-1")
        && checkLockingResult(caseNum + "-t2-RL-1")
        && checkLockingResult(caseNum + "-t1-WL-0"));
    cleanLockingResult();
    // possilbe deadlock check
    if (t1.isAlive() || t2.isAlive()) {
      fail("Possible deadlock resulted in testRead.");
    }
  }

  /**
   * Case #2 - T1 acquires RL followed by UL.
   */
  public void testUpgradeReadLock() throws Exception {
    String caseNum = "2";
    Thread t1 = new ReadThread(caseNum, "t1", "1st read lock attempt failed",
        INVOKE_UPGRADE, null);

    t1.start();
    t1.join();
    //t1.join(3000);
    assertTrue(checkLockingResult(caseNum + "-t1-RL-1")
        && checkLockingResult(caseNum + "-t1-UL-1"));
    cleanLockingResult();
  }

  /**
   * Case #3 - T1 acquires RL followed by WL.
   */
  public void testReadThenWrite() throws Exception {
    String caseNum = "3";
    acquireReadLock(caseNum, "t1", "1st read lock attempt failed");
    acquireWriteLock(caseNum, "t1.1", "2nd write lock attempt failed");
    assertTrue(checkLockingResult(caseNum + "-t1-RL-1")
        && checkLockingResult(caseNum + "-t1.1-WL-1"));
    cleanLockingResult();
  }

  /**
   * Case #5 - T1 acquires WL followed by RL.
   */
  public void testWriteThenRead() throws Exception {
    String caseNum = "5";
    acquireWriteLock(caseNum, "t1", "1st write lock attempt failed");
    acquireReadLock(caseNum, "t1.1", "2nd read lock attempt failed");
    assertTrue(checkLockingResult(caseNum + "-t1-WL-1")
        && checkLockingResult(caseNum + "-t1.1-RL-1"));
    cleanLockingResult();
  }

  /**
   * Case #6 - T1 acquires RL, T2 acquires RL.
   */
  //WAIT@Test
  //WAIT@Schedule(name = "multipleReadlock", value = "afterFirstLock@t1->check1@main,"
  //WAIT    + "afterFirstLock@t2->check2@main, check3@main->beforeFirstUnlock@t1, check4@main->beforeFirstUnlock@t2")
  public void testMultipleReadlock() throws Exception {
    String caseNum = "6";
    Thread t1 = new ReadThread(caseNum, "t1", "1st read lock attempt failed",
        NO_MORE_OP, null);
    Thread t2 = new ReadThread(caseNum, "t2", "2nd read lock attempt failed",
        NO_MORE_OP, null);

    t1.start();
    t2.start();
    fireEvent("check1");
    fireEvent("check2");
    fireEvent("check3");
    fireEvent("check4");
    t1.join();
    t2.join();
    //t1.join(3000);
    //t2.join(3000);
    assertTrue(checkLockingResult(caseNum + "-t1-RL-1")
        && checkLockingResult(caseNum + "-t2-RL-1"));
    cleanLockingResult();
    // possilbe deadlock check
    if (t1.isAlive() || t2.isAlive()) {
      fail("Possible deadlock resulted in testRead.");
    }
  }

  /**
   * Case #8 - T1 acquires WL, T2 acquires RL.
   */
  //WAIT@Test
  //WAIT@Schedule(name = "writeWithExistingReader", value = "errorLock@t2->beforeFirstUnlock@t1,"
  //WAIT    + "afterFirstLock@t1->beforeFirstLock@t2")
  public void testWriteWithExistingReader() throws Exception {
    String caseNum = "8";
    CountDownLatch waitFor = new CountDownLatch(1);
    ReadThread t1 = new ReadThread(caseNum, "t1",
        "1st write lock attempt failed", NO_MORE_OP, waitFor);
    WriteThread t2 = new WriteThread(caseNum, "t2",
        "2nd read lock attempt failed", NO_MORE_OP, waitFor);

    t1.start();
    t2.start();

    //OPWAIT t1.notifyBeforeSecondOp.await();
    //OPWAIT t2.notifyLatch.await();
    //OPWAIT waitFor.countDown();

    //OPWAIT waitFor.countDown();
    t1.join();
    t2.join();
    //t1.join(3000);
    //t2.join(3000);

    // there is NO guarantee as to which thread will get the lock!!
    boolean t0GetsLock = checkLockingResult(caseNum + "-t1-RL-1")
        && checkLockingResult(caseNum + "-t2-WL-0");
    boolean t1GetsLock = checkLockingResult(caseNum + "-t1-RL-0")
        && checkLockingResult(caseNum + "-t2-WL-1");

    assert !(t0GetsLock && t1GetsLock); // both can't be true
    assert t0GetsLock || t1GetsLock; // one must be true

    cleanLockingResult();
    // possilbe deadlock check
    if (t1.isAlive() || t2.isAlive()) {
      fail("Possible deadlock resulted in testRead.");
    }
  }

  /**
   * Case #13 - T1 acquires RL, T2 acquires WL.
   */
  @Test
  @Schedule(name = "readWithExistingWriter", value = "errorLock@t2->beforeFirstUnlock@t1,"
      + "afterFirstLock@t1->beforeFirstLock@t2")
  public void testReadWithExistingWriter() throws Exception {
    String caseNum = "13";
    CountDownLatch waitFor = new CountDownLatch(1);
    WriteThread t1 = new WriteThread(caseNum, "t1",
        "1st write lock attempt failed", NO_MORE_OP, waitFor);
    ReadThread t2 = new ReadThread(caseNum, "t2",
        "2nd read lock attempt failed", NO_MORE_OP, waitFor);

    t1.start();
    t2.start();

    //OPWAIT t1.notifyLatch.await();
    //OPWAIT t2.notifyBeforeSecondOp.await();
    //OPWAIT waitFor.countDown();

    t1.join();
    t2.join();
    //t1.join(3000);
    //t2.join(3000);

    // there is NO guarantee as to which thread will get the lock!!
    boolean t0GetsLock = checkLockingResult(caseNum + "-t1-WL-1")
        && checkLockingResult(caseNum + "-t2-RL-0");
    boolean t1GetsLock = checkLockingResult(caseNum + "-t1-WL-0")
        && checkLockingResult(caseNum + "-t2-RL-1");

    assert !(t0GetsLock && t1GetsLock); // both can't be true
    assert t0GetsLock || t1GetsLock; // one must be true

    cleanLockingResult();
    // possilbe deadlock check
    if (t1.isAlive() || t2.isAlive()) {
      fail("Possible deadlock resulted in testRead.");
    }
  }

  /**
   * Case #14 - T1 acquires WL, T2 acquires WL.
   */
  //WAIT@Test
  //WAIT@Schedules( {
  //WAIT    @Schedule(name = "multipleWritelocks1", value = "afterFirstLock@t1->beforeFirstLock@t2, errorLock@t2->beforeFirstUnlock@t1"),
  //WAIT    @Schedule(name = "multipleWritelocks2", value = "afterFirstLock@t2->beforeFirstLock@t1, errorLock@t1->beforeFirstUnlock@t2") })
  public void testMultipleWritelocks() throws Exception {
    String caseNum = "14";
    CountDownLatch waitFor = new CountDownLatch(1);
    WriteThread t1 = new WriteThread(caseNum, "t1",
        "1st write lock attempt failed", NO_MORE_OP, /* waitFor */null);
    WriteThread t2 = new WriteThread(caseNum, "t2",
        "2nd write lock attempt failed", NO_MORE_OP, /* waitFor */null);

    t1.start();
    t2.start();
    fireEvent("beforeUnlock");
    //OPWAIT t1.notifyLatch.await();
    //OPWAIT t2.notifyLatch.await();
    //OPWAIT waitFor.countDown();
    
    t1.join();
    t2.join();
    //t1.join(3000);
    //t2.join(3000);

    // there is NO guarantee as to which thread will get the lock!!
    boolean t0GetsLock = checkLockingResult(caseNum + "-t1-WL-1")
        && checkLockingResult(caseNum + "-t2-WL-0");
    boolean t1GetsLock = checkLockingResult(caseNum + "-t1-WL-0")
        && checkLockingResult(caseNum + "-t2-WL-1");

    assert !(t0GetsLock && t1GetsLock); // both can't be true
    assert t0GetsLock || t1GetsLock; // one must be true

    cleanLockingResult();
    // possilbe deadlock check
    if (t1.isAlive() || t2.isAlive()) {
      fail("Possible deadlock resulted in testRead.");
    }
  }

  /**
   * Case #7 - T1 acquires RL, T2 acquires UL.
   */
//  error, can not successfully run  
//  @NTest
//  @NSchedule(name = "upgradeWithExistingReader", value = "errorLock@t2->beforeFirstUnlock@t1,"
//      + "afterFirstLock@t1->beforeFirstLock@t2")
  public void testUpgradeWithExistingReader() throws Exception {
    String caseNum = "7";
    CountDownLatch waitFor = new CountDownLatch(1);
    ReadThread t1 = new ReadThread(caseNum, "t1",
        "1st read lock attempt failed", NO_MORE_OP, /* waitFor */null);
    UpgradeThread t2 = new UpgradeThread(caseNum, "t2",
        "2nd upgrade lock attempt failed", NO_MORE_OP, /*
                                                        * t1.notifyBeforeSecondOp
                                                        */null);
    t2.beforeFinishWait = waitFor;

    t1.start();
    t2.start();
    // t2.notifyBeforeFinish.await(10, TimeUnit.SECONDS);
    // waitFor.countDown();
    
    t1.join();
    t2.join();
    //t1.join(3000);
    //t2.join(3000);
    assertTrue(checkLockingResult(caseNum + "-t1-RL-1"));
    assertTrue(checkLockingResult(caseNum + "-t2-UL-1"));
    cleanLockingResult();
    // possilbe deadlock check
    if (t1.isAlive() || t2.isAlive()) {
      fail("Possible deadlock resulted in testRead.");
    }
  }

  /**
   * Case #9 - T1 acquires RL, T2 acquires RL followed by UL.
   */
//error, can not successfully run  
  // @NTest
  // @NSchedule(name = "upgradeWithMultipleReaders", value =
  // "errorUpLock@t2->beforeFirstUnlock@t1,"
  // + "afterFirstLock@t1->beforeUpLock@t2")
  public void testUpgradeWithMultipleReaders() throws Exception {
    String caseNum = "9";
    CountDownLatch waitFor = new CountDownLatch(1);
    ReadThread t1 = new ReadThread(caseNum, "t1",
        "1st read lock attempt failed", NO_MORE_OP, /* waitFor */null);
    ReadThread t2 = new ReadThread(caseNum, "t2",
        "2nd read lock attempt failed", INVOKE_UPGRADE, /*
                                                         * t1.notifyBeforeSecondOp
                                                         */null);

    t1.start();
    t2.start();

    t1.notifyBeforeSecondOp.await(10, TimeUnit.SECONDS);
    t2.notifyBeforeFinish.await(10, TimeUnit.SECONDS);
    waitFor.countDown();

    t1.join();
    t2.join();
    //t1.join(3000);
    //t2.join(3000);
    assertTrue(checkLockingResult(caseNum + "-t1-RL-1"));
    assertTrue(checkLockingResult(caseNum + "-t2-RL-1"));
    assertTrue(checkLockingResult(caseNum + "-t2-UL-1"));
    cleanLockingResult();
    // possilbe deadlock check
    if (t1.isAlive() || t2.isAlive()) {
      fail("Possible deadlock resulted in testRead.");
    }
  }
}
