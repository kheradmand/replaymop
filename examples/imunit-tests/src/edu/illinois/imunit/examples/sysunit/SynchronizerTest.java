package edu.illinois.imunit.examples.sysunit;
import org.junit.runner.RunWith;
import edu.illinois.imunit.IMUnitRunner;
import static edu.illinois.imunit.IMUnit.fireEvent;

import org.junit.Test;
import org.sysunit.InconsistentSyncException;
import org.sysunit.sync.SyncTestBase;
import org.sysunit.sync.Synchronizer;
import org.sysunit.sync.SynchronizerCallback;

import edu.illinois.imunit.Schedule;

@RunWith(IMUnitRunner.class)
public class SynchronizerTest extends SyncTestBase implements
    SynchronizerCallback {
  private int touches;

  @Test
  @Schedule(name = "valid", value = "[beforeSync:afterSync]@t1->beforeCheck@main")
  public void testValid() throws Exception {
    Synchronizer synchronizer = new Synchronizer(2, this);

    Thread thr1 = runThread(synchronizer, "one", false, "t1");

    //Thread.sleep(1000);
    fireEvent("beforeCheck");
    assertTouches(0);

    Thread thr2 = runThread(synchronizer, "one", false, "t2");

    thr1.join();
    thr2.join();

    assertTouches(2);
  }

  public void testInconsistent() throws Exception {
    Synchronizer synchronizer = new Synchronizer(2, this);

    Thread thr1 = runThread(synchronizer, "one", true, "t1");
    Thread thr2 = runThread(synchronizer, "two", true, "t2");

    thr1.join();
    thr2.join();

    assertTouches(0);
  }

  void assertTouches(int num) {
    if (this.touches == num) {
      return;
    }

    fail("expected <" + num + "> touches but found <" + this.touches + ">");
  }

  Thread runThread(final Synchronizer synchronizer, final String syncPoint,
      final boolean expectInconsistent){
    return runThread(synchronizer, syncPoint, expectInconsistent, "runThread");
  }
  
  Thread runThread(final Synchronizer synchronizer, final String syncPoint,
      final boolean expectInconsistent, String name) {
    Thread thr = new Thread() {
      public void run() {
        try {
          fireEvent("beforeSync");
          synchronizer.sync(syncPoint, getName());
          fireEvent("afterSync");

          if (expectInconsistent) {
            fail("expected InconsistentSyncException");
          }

          touch();
        } catch (InconsistentSyncException e) {
          if (!expectInconsistent) {
            fail(e.getMessage());
          }
        } catch (Exception e) {
          fail(e.getMessage());
        }
      }
    };
    thr.setName(name);
    thr.start();

    return thr;
  }

  synchronized void touch() {
    ++this.touches;
  }

  public void notifyFullyBlocked(Synchronizer synchronizer) {
    synchronizer.unblock();
  }

  public void notifyInconsistent(Synchronizer synchronizer) {

  }
}
