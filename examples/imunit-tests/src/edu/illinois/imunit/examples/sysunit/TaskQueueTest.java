package edu.illinois.imunit.examples.sysunit;
import org.junit.runner.RunWith;
import edu.illinois.imunit.IMUnitRunner;
import static edu.illinois.imunit.IMUnit.fireEvent;

import org.junit.Test;
import org.sysunit.util.TaskQueue;
import org.sysunit.util.UtilTestBase;

import edu.illinois.imunit.Schedule;

@RunWith(IMUnitRunner.class)
public class TaskQueueTest extends UtilTestBase {

  private Runnable task;

  public void setUp() {
    this.task = null;
  }

  public void tearDown() {
    this.task = null;
  }

  @Test
  @Schedule(name = "addAndNext", value = "[beforeSet:afterSet]@nextThread->beforeCheck@main," + 
      "afterSet@nextThread->beforeSecondCheck@main")
  public void testAddAndNext() throws Exception {
    this.setUp();
    final Runnable theTask = new Runnable() {
      public void run() {

      };
    };

    final TaskQueue queue = new TaskQueue();

    Thread addTaskThread = new Thread() {
      public void run() {
        queue.addTask(theTask);
      }
    };

    Thread nextTaskThread = new Thread() {
      public void run() {
        try {
          fireEvent("beforeSet");
          setTask(queue.nextTask());
          fireEvent("afterSet");
        } catch (InterruptedException e) {
          fail("caught InterruptedException");
        }
      }
    };
    
    nextTaskThread.setName("nextThread");
    nextTaskThread.start();

    //Thread.sleep(1000);

    fireEvent("beforeCheck");
    assertNull("not task fetched", getTask());

    addTaskThread.start();

    //Thread.sleep(1000);

    fireEvent("beforeSecondCheck");
    assertSame(theTask, this.task);
    this.tearDown();
  }

  @Test
  @Schedule(name = "interrupt", value = "[beforeNext:afterNext]@nextThread->beforeCheck@main," + 
      "afterThrow@nextThread->beforeSecondCheck@main, afterAdd@addThread->beforeSecondCheck@main")
  public void testInterrupt() throws Exception {
    this.setUp();
    final Runnable theTask = new Runnable() {
      public void run() {

      };
    };

    final TaskQueue queue = new TaskQueue();

    Thread addTaskThread = new Thread() {
      public void run() {
        queue.addTask(theTask);
        fireEvent("afterAdd");
      }
    };
    addTaskThread.setName("addThread");
    
    Thread nextTaskThread = new Thread() {
      public void run() {
        try {
          fireEvent("beforeNext");
          queue.nextTask();
          fireEvent("afterNext");
          fail("should have caught InterruptedException");
        } catch (InterruptedException e) {
          fireEvent("afterThrow");
          // expected and correct;
        }
      }
    };

    nextTaskThread.setName("nextThread");
    nextTaskThread.start();

    //Thread.sleep(1000);

    fireEvent("beforeCheck");
    assertNull("not task fetched", getTask());

    nextTaskThread.interrupt();

    addTaskThread.start();

    //Thread.sleep(1000);

    fireEvent("beforeSecondCheck");
    assertNull("not task fetched", getTask());
    this.tearDown();
  }

  void setTask(Runnable task) {
    this.task = task;
  }

  Runnable getTask() {
    return this.task;
  }
}
