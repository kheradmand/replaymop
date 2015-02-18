package edu.illinois.imunit.examples.sysunit;
import org.junit.runner.RunWith;
import edu.illinois.imunit.IMUnitRunner;
import static edu.illinois.imunit.IMUnit.fireEvent;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.sysunit.mesh.CommandGroup;
import org.sysunit.mesh.MeshTestBase;

import edu.illinois.imunit.Schedule;

@RunWith(IMUnitRunner.class)
public class CommandGroupTest extends MeshTestBase {
  private boolean touched;

  public void setUp() {
    this.touched = false;
  }

  @Test
  @Schedule(name = "noneInFlight", value = "afterTouch@groupThread->beforeCheck@main")
  public void testNoneInFlight() throws Exception {
    this.setUp();
    Map map = new HashMap();

    CommandGroup group = new CommandGroup(map);

    group.add(1);
    group.add(2);
    group.add(3);

    waitFor(group);

    //Thread.sleep(1000);
    fireEvent("beforeCheck");
    assertTouched();
    this.tearDown();
  }

  public void testInFlight() throws Exception {
    Map map = new HashMap();

    CommandGroup group = new CommandGroup(map);

    group.add(1);
    group.add(2);
    group.add(3);

    map.put("1", Boolean.TRUE);
    map.put("2", Boolean.TRUE);
    map.put("3", Boolean.TRUE);
    map.put("4", Boolean.TRUE);

    waitFor(group);

    Thread.sleep(1000);

    assertNotTouched();

    synchronized (map) {
      map.remove("1");
      map.notifyAll();
    }

    Thread.sleep(200);
    assertNotTouched();

    synchronized (map) {
      map.remove("2");
      map.notifyAll();
    }

    Thread.sleep(200);
    assertNotTouched();

    synchronized (map) {
      map.remove("4");
      map.notifyAll();
    }

    Thread.sleep(200);
    assertNotTouched();

    synchronized (map) {
      map.remove("3");
      map.notifyAll();
    }

    Thread.sleep(200);
    assertTouched();
  }

  void waitFor(final CommandGroup group) {
    Thread thr = new Thread() {
      public void run() {
        Thread.currentThread().setName("groupThread");
        try {
          group.waitFor();
          touch();
          fireEvent("afterTouch");
        } catch (InterruptedException e) {
          fail(e.getMessage());
        }
      }
    };

    thr.start();
  }

  void touch() {
    this.touched = true;
  }

  void assertTouched() {
    if (!this.touched) {
      fail("expected to be touched");
    }
  }

  void assertNotTouched() {
    if (this.touched) {
      fail("expected to be not touched");
    }
  }
}
