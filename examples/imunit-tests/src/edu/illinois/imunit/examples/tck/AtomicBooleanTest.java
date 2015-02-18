package edu.illinois.imunit.examples.tck;
import org.junit.runner.RunWith;
import edu.illinois.imunit.IMUnitRunner;
import static edu.illinois.imunit.IMUnit.fireEvent;

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 */

import junit.framework.*;
import org.junit.Test;

import edu.illinois.imunit.Schedule;
import edu.illinois.imunit.Schedules;

import java.util.concurrent.atomic.*;
import java.io.*;
import static org.junit.Assert.*;

@RunWith(IMUnitRunner.class)
public class AtomicBooleanTest {
//    public static void main(String[] args) {
//        junit.textui.TestRunner.run(suite());
//    }
//    public static Test suite() {
//        return new TestSuite(AtomicBooleanTest.class);
//    }


    /**
     * compareAndSet in one thread enables another waiting for value
     * to succeed
     */
//    @NTest
//    @NSchedules({
//      @NSchedule(name = "GetBeforePut", value = 
//          "setTrue2False@main->isTrue@testThread")})
    public void testCompareAndSetInMultipleThreads() throws Exception {
        final AtomicBoolean ai = new AtomicBoolean(true);
        Thread t = new Thread(new Runnable() {
            public void run() {
                //while (!ai.compareAndSet(false, true)){
                //  Thread.yield();
                //}
                /* @NEvent("isTrue") */
            }}, "testThread");

        t.start();
        assertTrue("GetBeforePut",ai.compareAndSet(true, false));
        /* @NEvent("setTrue2False") */
        t.join();
        assertFalse(t.isAlive());
    }


}

