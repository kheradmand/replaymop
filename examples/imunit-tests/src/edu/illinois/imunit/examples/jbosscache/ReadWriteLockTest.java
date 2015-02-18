package edu.illinois.imunit.examples.jbosscache;
import org.junit.runner.RunWith;
import edu.illinois.imunit.IMUnitRunner;
import static edu.illinois.imunit.IMUnit.fireEvent;

import static org.junit.Assert.fail;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.Test;

/**
 * Tests the various ReadWriteLock implementations
 * 
 * @author Bela Ban
 * @version $Id: ReadWriteLockTest.java 7295 2008-12-12 08:41:33Z mircea.markus
 *          $
 */
@RunWith(IMUnitRunner.class)
public class ReadWriteLockTest {
  ReadWriteLock lock;
  Exception ex = null;

  public void setUp() throws Exception {
    ex = null;
  }

  public void tearDown() throws Exception {
    lock = null;
    if (ex != null)
      throw ex;
  }

  public void testMoreWriteReleasesThanAcquisitions()
      throws InterruptedException {
    lock = new ReentrantReadWriteLock();
    lock.writeLock().lock();
    lock.writeLock().unlock();
    try {
      lock.writeLock().unlock();
      fail("Should have barfed");
    } catch (IllegalMonitorStateException imse) {
      // should barf
    }
  }

  public void testMoreReadReleasesThanAcquisitions()
      throws InterruptedException {
    lock = new ReentrantReadWriteLock();
    lock.readLock().lock();
    lock.readLock().unlock();
    try {
      lock.readLock().unlock();
      fail("read locks cannot be released more than acquired");
    } catch (IllegalMonitorStateException illegalStateEx) {

    }
  }

  public void testSimple() throws InterruptedException {
    lock = new ReentrantReadWriteLock();
    lock.readLock().lock();
    // upgrades must be manual; involving giving up the RL first. Sucks.
    // see
    // http://java.sun.com/j2se/1.5.0/docs/api/java/util/concurrent/locks/ReentrantReadWriteLock.html
    lock.readLock().unlock();

    lock.writeLock().lock();
    lock.writeLock().lock();
    lock.readLock().lock();
    lock.readLock().lock();

    // since the thread currently has a WL and we are reentrant, this works
    // without a manual upgrade.
    lock.writeLock().lock();
    lock.writeLock().lock();
  }

  public void testOneWriterMultipleReaders() throws InterruptedException {
    lock = new ReentrantReadWriteLock();

    Writer writer = new Writer("writer");
    Reader reader1 = new Reader("reader1");
    Reader reader2 = new Reader("reader2");

    writer.start();
    reader1.start();
    reader2.start();

    writer.join();
    reader1.join();
    reader2.join();
  }

  class Writer extends Thread {

    public Writer(String name) {
      super(name);
    }

    public void run() {
      try {
        lock.writeLock().lock();
        sleep(1000);
      } catch (InterruptedException e) {
        ex = e;
      } finally {
        lock.writeLock().unlock();
      }
    }
  }

  class Reader extends Thread {

    public Reader(String name) {
      super(name);
    }

    public void run() {
      try {
        lock.readLock().lock();
        sleep(500);
      } catch (InterruptedException e) {
        ex = e;
      } finally {
        lock.readLock().unlock();
      }
    }
  }
}
