package edu.illinois.imunit.examples.blockingbuffer;

import static edu.illinois.imunit.IMUnit.fireEvent;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import edu.illinois.imunit.IMUnitRunner;
import edu.illinois.imunit.Schedule;
import edu.illinois.imunit.Schedules;

/**
 * IMunit tests for {@link BlockingBuffer}.
 * 
 * @author Vilas Jagannath (vbangal2@illinois.edu), <br/>
 *         Milos Gligoric (gliga@illinois.edu), <br/>
 *         Dongyun Jin (djin3@illinois.edu), <br/>
 *         Qingzhou Luo (qluo2@illinois.edu).
 * 
 */
@RunWith(IMUnitRunner.class)
public class BlockingBufferTest {

    public static class Box<T> {
        public T val;
    }

    @Test
    @Schedules({ @Schedule(name = "DeadlockCausedByUnencounteredBeforeEvent", value = "afterputter@putThread -> beforeget"),
            @Schedule(name = "BlockingEventThreadNeverBlocks", value = "[afterput] -> beforeget@getThread"),
            @Schedule(name = "AfterEventNeverEncountered", value = "[beforeget]@getThread -> beforeputter"),
            @Schedule(name = "ParsingError", value = "[beforeget] - beforeput@putThread") })
    public void testGetAndPutAllErrors() throws InterruptedException {
        testGetAndPut();
    }

    @Test
    @Schedules({ @Schedule(name = "PutBeforeGet1", value = "afterput@putThread -> beforeget"),
            @Schedule(name = "PutBeforeGet2", value = "afterput -> beforeget@getThread"),
            @Schedule(name = "GetBeforePut1", value = "[beforeget]@getThread -> beforeput"),
            @Schedule(name = "GetBeforePut2", value = "[beforeget] -> beforeput@putThread") })
    public void testGetAndPut() throws InterruptedException {
        final BlockingBuffer<Integer> bb = new BlockingBuffer<Integer>(1);
        final Box<Integer> box = new Box<Integer>();

        Thread getThread = new Thread(new Runnable() {
            @Override
            public void run() {
                fireEvent("beforeget");
                box.val = bb.get();
                fireEvent("afterget");
            }
        }, "getThread");
        Thread putThread = new Thread(new Runnable() {
            @Override
            public void run() {
                fireEvent("beforeput");
                bb.put(3);
                fireEvent("afterput");
            }
        }, "putThread");

        getThread.start();
        putThread.start();
        getThread.join();
        putThread.join();

        assertTrue(box.val == 3);
    }

}
