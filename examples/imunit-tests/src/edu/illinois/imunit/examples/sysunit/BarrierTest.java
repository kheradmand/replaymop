package edu.illinois.imunit.examples.sysunit;
import org.junit.runner.RunWith;
import edu.illinois.imunit.IMUnitRunner;
import static edu.illinois.imunit.IMUnit.fireEvent;

import org.junit.Test;
import org.sysunit.util.Barrier;
import org.sysunit.util.UtilTestBase;

import edu.illinois.imunit.Schedule;
import edu.illinois.imunit.Schedules;

@RunWith(IMUnitRunner.class)
public class BarrierTest extends UtilTestBase {
  private int touches;

  public void setUp() {
    this.touches = 0;
  }

  public void tearDown() {
    this.touches = 0;
  }

  @Test
  @Schedule(name = "oneThread", value = "afterTouch@blockThread->beforeCheck@main")
  public void testOneThread() throws Exception {
    this.setUp();
    Barrier barrier = new Barrier(1);

    assertEquals(0, barrier.getNumWaitingThreads());

    assertEquals(1, barrier.getNumThreads());

    runThread(barrier);

    //Thread.currentThread().sleep(1000);
    
    fireEvent("beforeCheck");
    assertTouches(1);
    this.tearDown();
  }

  @Test
  @Schedule(name = "fiveThreads", value = "afterTouch@t1->beforeCheck@main, " +
  		"afterTouch@t2->beforeCheck@main, afterTouch@t3->beforeCheck@main, " + 
    "afterTouch@t4->beforeCheck@main, afterTouch@t5->beforeCheck@main")
  public void testFiveThreads() throws Exception {
    this.setUp();
    Barrier barrier = new Barrier(5);

    assertEquals(0, barrier.getNumWaitingThreads());

    assertEquals(5, barrier.getNumThreads());

    runThread(barrier, "t1");
    runThread(barrier, "t2");
    runThread(barrier, "t3");
    runThread(barrier, "t4");
    runThread(barrier, "t5");

    //Thread.currentThread().sleep(1000);
    
    fireEvent("beforeCheck");
    assertTouches(5);
    this.tearDown();
  }

  @Test
  @Schedule(name = "barrierReuse", value = "afterTouch@t1->beforeCheck@main, " +
      "afterTouch@t2->beforeCheck@main, afterTouch@t3->beforeCheck@main, " + 
    "afterTouch@t4->beforeCheck@main, afterTouch@t5->beforeCheck@main, " + 
    "afterTouch@t6->beforeSecondCheck@main, " +
    "afterTouch@t7->beforeSecondCheck@main, afterTouch@t8->beforeSecondCheck@main, " + 
  "afterTouch@t9->beforeSecondCheck@main, afterTouch@t10->beforeSecondCheck@main")
  public void testBarrierReuse() throws Exception {
    this.setUp();
    Barrier barrier = new Barrier(5);

    runThread(barrier, "t1");
    runThread(barrier, "t2");
    runThread(barrier, "t3");
    runThread(barrier, "t4");
    runThread(barrier, "t5");

    //Thread.currentThread().sleep(1000);
    fireEvent("beforeCheck");
    

    assertTouches(5);

    runThread(barrier, "t6");
    runThread(barrier, "t7");
    runThread(barrier, "t8");
    runThread(barrier, "t9");
    runThread(barrier, "t10");

    //Thread.currentThread().sleep(1000);
    fireEvent("beforeSecondCheck");
    

    assertTouches(10);
    this.tearDown();
  }

  @Test
  @Schedule(name = "barrierStepWise", value = "[beforeBlock:afterBlock]@t1->beforeCheck1@main, " +
      "[beforeBlock:afterBlock]@t2->beforeCheck2@main, [beforeBlock:afterBlock]@t3->beforeCheck3@main, " + 
    "[beforeBlock:afterBlock]@t4->beforeCheck4@main,"+" afterTouch@t1->beforeCheck5@main, " +
      "afterTouch@t2->beforeCheck5@main, afterTouch@t3->beforeCheck5@main, " + 
    "afterTouch@t4->beforeCheck5@main, afterTouch@t5->beforeCheck5@main")
  public void testBarrierStepWise() throws Exception {
    this.setUp();
    Barrier barrier = new Barrier(5);

    runThread(barrier, "t1");

    //Thread.currentThread().sleep(500);
    fireEvent("beforeCheck1");
    assertTouches(0);
    assertEquals(1, barrier.getNumWaitingThreads());

    runThread(barrier, "t2");
    //Thread.currentThread().sleep(500);
    fireEvent("beforeCheck2");
    assertTouches(0);
    assertEquals(2, barrier.getNumWaitingThreads());

    runThread(barrier, "t3");
    //Thread.currentThread().sleep(500);
    fireEvent("beforeCheck3");
    assertTouches(0);
    assertEquals(3, barrier.getNumWaitingThreads());

    runThread(barrier, "t4");
    //Thread.currentThread().sleep(500);
    fireEvent("beforeCheck4");
    assertTouches(0);
    assertEquals(4, barrier.getNumWaitingThreads());

    runThread(barrier, "t5");
    //Thread.currentThread().sleep(500);
    fireEvent("beforeCheck5");
    assertTouches(5);
    assertEquals(0, barrier.getNumWaitingThreads());
    this.tearDown();
  }

  void runThread(final Barrier barrier) {
    this.runThread(barrier, "blockThread");
  }
  
  void runThread(final Barrier barrier, String name) {
    Thread t = new Thread() {
      public void run() {
        try {
          fireEvent("beforeBlock");
          barrier.block();
          fireEvent("afterBlock");
        } catch (InterruptedException e) {
          fail("caught InterruptedException");
        }
        touch();
        fireEvent("afterTouch");
      }
    };
    t.setName(name);
    t.start();
//    new Thread() {
//      public void run() {
//        Thread.currentThread().setName("blockThread");
//        try {
//          fireEvent("beforeBlock");
//          barrier.block();
//          fireEvent("afterBlock");
//        } catch (InterruptedException e) {
//          fail("caught InterruptedException");
//        }
//
//        touch();
//        fireEvent("afterTouch");
//      }
//    }.start();
  }

  synchronized void touch() {
    ++this.touches;
  }

  void assertTouches(int num) {
    if (num == this.touches) {
      return;
    }

    fail("expected <" + num + "> touches, but found <" + this.touches + ">");
  }
}
