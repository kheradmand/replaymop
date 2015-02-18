/*
 *  Copyright 2003-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.illinois.imunit.examples.apache.collections;
import org.junit.runner.RunWith;
import edu.illinois.imunit.IMUnitRunner;
import static edu.illinois.imunit.IMUnit.fireEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUnderflowException;
import org.apache.commons.collections.buffer.BlockingBuffer;
import org.junit.Test;

import edu.illinois.imunit.Schedule;
import edu.illinois.imunit.Schedules;

/**
 * Extension of {@link AbstractTestObject} for exercising the {@link BlockingBuffer} implementation.
 *
 * @author Janek Bogucki
 * @author Phil Steitz
 * @version $Revision: 348428 $
 * @since Commons Collections 3.0
 */
@RunWith(IMUnitRunner.class)
public class TestBlockingBuffer extends AbstractTestObject {

  public TestBlockingBuffer() {
    super("TestBlockingBuffer");
  }

  public Object makeObject() {
    return BlockingBuffer.decorate( new MyBuffer() );
  }

  public boolean isEqualsCheckable() {
    return false;
  }

  /**
   * Tests {@link BlockingBuffer#get()} in combination with {@link BlockingBuffer#add(Object)}.
   */
  @Test
  @Schedule("[beforeGet]@main->beforeAdd@addThread")
  public void testGetWithAdd() {
    Buffer blockingBuffer = BlockingBuffer.decorate( new MyBuffer() );
    Object obj = new Object();
    new NonDelayedAdd( blockingBuffer, obj,"addThread" ).start();

    // verify does not throw BufferUnderflowException; should block until other thread has added to the buffer .
    fireEvent("beforeGet");
    assertSame(obj, blockingBuffer.get() );
    fireEvent("afterGet");  
  }

  /**
   * Tests {@link BlockingBuffer#get()} in combination with {@link BlockingBuffer#addAll(java.util.Collection)}.
   */
  @Test
  @Schedule("[beforeGet]@main->beforeAddAll@addAllThread")
  public void testGetWithAddAll() {
    Buffer blockingBuffer = BlockingBuffer.decorate( new MyBuffer() );
    Object obj = new Object();
    new NonDelayedAddAll( blockingBuffer, obj, "addAllThread" ).start();

    // verify does not throw BufferUnderflowException; should block until other thread has added to the buffer .
    fireEvent("beforeGet");
    assertSame(obj, blockingBuffer.get() );
    fireEvent("afterGet");
  }

  /**
   * Tests {@link BlockingBuffer#remove()} in combination with {@link BlockingBuffer#add(Object)}.
   */
  @Test
  @Schedule("[beforeRemove]@main->beforeAdd@addThread")
  public void testRemoveWithAdd() {
    Buffer blockingBuffer = BlockingBuffer.decorate( new MyBuffer() );
    Object obj = new Object();
    new NonDelayedAdd( blockingBuffer, obj, "addThread" ).start();

    // verify does not throw BufferUnderflowException; should block until other thread has added to the buffer .
    fireEvent("beforeRemove");
    assertSame(obj, blockingBuffer.remove() );
    fireEvent("afterRemove");  
  }

  /**
   * Tests {@link BlockingBuffer#remove()} in combination with {@link BlockingBuffer#addAll(java.util.Collection)}.
   */
  @Test
  @Schedule("[beforeRemove]@main->beforeAddAll@addThread")
  public void testRemoveWithAddAll() {
    Buffer blockingBuffer = BlockingBuffer.decorate( new MyBuffer() );
    Object obj = new Object();
    new NonDelayedAddAll( blockingBuffer, obj,"addThread" ).start();

    // verify does not throw BufferUnderflowException; should block until other thread has added to the buffer .
    fireEvent("beforeRemove");
    assertSame(obj, blockingBuffer.remove() );
    fireEvent("afterRemove");
  }

  @Test
  @Schedules({
    @Schedule(name = "RemoveWithAddAllTimeout", value = "afterException@main->beforeAddAll@addAllThread") })
  public void testRemoveWithAddAllTimeout() {
    Buffer blockingBuffer = BlockingBuffer.decorate( new MyBuffer(), 100 );
    Object obj = new Object();
    new DelayedAddAll( blockingBuffer, obj, 500 ).start();
    try {
       blockingBuffer.remove();
     }
    catch( BufferUnderflowException e ) {
      fireEvent("afterException");
    }
  }
  //-----------------------------------------------------------------------

  /**
   * Tests {@link BlockingBuffer#get()} in combination with {@link BlockingBuffer#add(Object)} using multiple read
   * threads.
   * <p/>
   * Two read threads should block on an empty buffer until one object is added then both threads should complete.
   */
  @Test
  @Schedules({
    @Schedule(name = "BlockedGetWithAdd", value = "[beforeGet:afterGet]@readThread1->beforeAdd@main," +
        "[beforeGet:afterGet]@readThread2->beforeAdd@main," + 
        "afterGet@readThread1->afterAdd@main," +"afterGet@readThread2->afterAdd@main") })
  public void testBlockedGetWithAdd() throws InterruptedException {
    Buffer blockingBuffer = BlockingBuffer.decorate( new MyBuffer() );
    Object obj = new Object();

    // run methods will get and compare -- must wait for add
    Thread thread1 = new ReadThread( blockingBuffer, obj, "BlockedGetWithAdd", "readThread1" );
    Thread thread2 = new ReadThread( blockingBuffer, obj, "BlockedGetWithAdd", "readThread2" );
    thread1.start();
    thread2.start();

    // give hungry read threads ample time to hang
    try {
     // Thread.sleep(100);
    } catch (Exception e) {
      e.printStackTrace();
    }

    // notifyAll should allow both read threads to complete
    fireEvent("beforeAdd");
    blockingBuffer.add( obj );
    
    // allow notified threads to complete 
    // Thread.sleep(100);
    fireEvent("afterAdd");

    // There should not be any threads waiting.
    // if(thread1.isAlive() || thread2.isAlive()) {
    //  fail("Live thread(s) when both should be dead.");
    // }
  }

  //-----------------------------------------------------------------------

  /**
   * Tests {@link BlockingBuffer#get()} in combination with {@link BlockingBuffer#addAll(java.util.Collection)} using
   * multiple read threads.
   * <p/>
   * Two read threads should block on an empty buffer until a singleton is added then both threads should complete.
   */
  @Test
  @Schedules({
    @Schedule(name = "BlockedGetWithAddAll", value = "[beforeGet:afterGet]@readThread1->beforeAddAll@main," +
        "[beforeGet:afterGet]@readThread2->beforeAddAll@main," + 
        "afterGet@readThread1->afterAddAll@main," +"afterGet@readThread2->afterAddAll@main") })
  public void testBlockedGetWithAddAll() throws InterruptedException {
    Buffer blockingBuffer = BlockingBuffer.decorate( new MyBuffer() );
    Object obj = new Object();

    // run methods will get and compare -- must wait for addAll
    Thread thread1 = new ReadThread( blockingBuffer, obj,"BlockedGetWithAddAll", "readThread1" );
    Thread thread2 = new ReadThread( blockingBuffer, obj,"BlockedGetWithAddAll", "readThread2" );
    thread1.start();
    thread2.start();

    // give hungry read threads ample time to hang
    try {
      // Thread.sleep(100);
     } catch (Exception e) {
       e.printStackTrace();
     }

    // notifyAll should allow both read threads to complete
    fireEvent("beforeAddAll");
    blockingBuffer.addAll( Collections.singleton( obj ) );
    
    // allow notified threads to complete 
    // Thread.sleep(100);
    fireEvent("afterAddAll");

    // There should not be any threads waiting.
    //assertFalse("BlockedGetWithAddAll", thread1.isAlive() || thread2.isAlive());
//    if( thread1.isAlive() || thread2.isAlive() ) {
//      fail( "Live thread(s) when both should be dead." );
//    }
  }

  //-----------------------------------------------------------------------

  /**
   * Tests interrupted {@link BlockingBuffer#get()}.
   */
  @Test
  @Schedules({
    @Schedule(name = "InterruptedGet", value = "[beforeGet:afterGet]@readThread->beforeInterrupt@main," + 
        "finishAddException@readThread->afterInterrupt@main") })
  public void testInterruptedGet() throws InterruptedException {
    Buffer blockingBuffer = BlockingBuffer.decorate( new MyBuffer() );
    Object obj = new Object();

    // spawn a read thread to wait on the empty buffer
    ArrayList exceptionList = new ArrayList();
    Thread thread = new ReadThread( blockingBuffer, obj, exceptionList, "InterruptedGet", "readThread" );
    thread.start();

    try {
      //Thread.sleep(100); //INS-SLEEP
    } catch (Exception e1) {
      e1.printStackTrace();
    }
    // Interrupting the thread should cause it to throw BufferUnderflowException
    fireEvent("beforeInterrupt");
    thread.interrupt();

    // Chill, so thread can throw and add message to exceptionList
    // Thread.sleep(100);
    fireEvent("afterInterrupt");

    assertTrue("InterruptedGet",
        exceptionList.contains( "BufferUnderFlow" ) );
    // assertFalse("InterruptedGet", thread.isAlive());
//    if( thread.isAlive() ) {
//      fail( "Read thread has hung." );
//    }

  }

  //-----------------------------------------------------------------------

  /**
   * Tests {@link BlockingBuffer#remove()} in combination with {@link BlockingBuffer#add(Object)} using multiple read
   * threads.
   * <p/>
   * Two read threads should block on an empty buffer until one object is added then one thread should complete. The
   * remaining thread should complete after the addition of a second object.
   * @throws InterruptedException 
   */
  
  /**@TO-DO  Can't specify thread is ended, and or relationship between schedules*/
  @Test
  @Schedules({
    @Schedule(name = "BlockedRemoveWithAdd", value = "[beforeRemove: afterRemove]@readThread1->beforeFirstAdd@main," + 
        "[beforeRemove: afterRemove]@readThread2->beforeFirstAdd@main") })
  public void testBlockedRemoveWithAdd() throws InterruptedException {
    Buffer blockingBuffer = BlockingBuffer.decorate( new MyBuffer() );
    Object obj = new Object();

    // run methods will remove and compare -- must wait for add
    Thread thread1 = new ReadThread( blockingBuffer, obj, null, "remove", "BlockedRemoveWithAdd", "readThread1" );
    Thread thread2 = new ReadThread( blockingBuffer, obj, null, "remove", "BlockedRemoveWithAdd", "readThread2" );
    thread1.start();
    thread2.start();

    // give hungry read threads ample time to hang
    try {
      // Thread.sleep(100);
     } catch (Exception e) {
       e.printStackTrace();
     }

    fireEvent("beforeFirstAdd");
    blockingBuffer.add( obj );

    // allow notified threads to complete 
    try {
      // Thread.sleep(100);
     } catch (Exception e) {
       e.printStackTrace();
     }

    // There should be one thread waiting.
    //assertTrue( "BlockedRemoveWithAdd", thread1.isAlive() ^ thread2.isAlive() );
    fireEvent("beforeSecondAdd");
    blockingBuffer.add( obj );

    // allow notified thread to complete 
    try {
      // Thread.sleep(100);
     } catch (Exception e) {
       e.printStackTrace();
     }


    // There should not be any threads waiting.
    thread1.join();
    thread2.join();
    //assertFalse( "BlockedRemoveWithAdd", thread1.isAlive() || thread2.isAlive() );
    //if( thread1.isAlive() || thread2.isAlive() ) {
      //fail( "Live thread(s) when both should be dead." );
    //}
  }

  //-----------------------------------------------------------------------

  /**
   * Tests {@link BlockingBuffer#remove()} in combination with {@link BlockingBuffer#addAll(java.util.Collection)}
   * using multiple read threads.
   * <p/>
   * Two read threads should block on an empty buffer until a singleton collection is added then one thread should
   * complete. The remaining thread should complete after the addition of a second singleton.
   * @throws InterruptedException 
   */
  /**@TO-DO Can't specify thread is ended, and or relationship between schedules*/
  @Test
  @Schedules({
    @Schedule(name = "BlockedRemoveWithAddAll1", value = "[beforeRemove: afterRemove]@readThread1->beforeFirstAddAll@main," + 
        "[beforeRemove:afterRemove]@readThread2->beforeFirstAddAll@main") })
  public void testBlockedRemoveWithAddAll1() throws InterruptedException {
    Buffer blockingBuffer = BlockingBuffer.decorate( new MyBuffer() );
    Object obj = new Object();

    // run methods will remove and compare -- must wait for addAll
    Thread thread1 = new ReadThread( blockingBuffer, obj, null, "remove",  "BlockedRemoveWithAddAll1", "readThread1" );
    Thread thread2 = new ReadThread( blockingBuffer, obj, null, "remove",  "BlockedRemoveWithAddAll1", "readThread2");
    thread1.start();
    thread2.start();

    // give hungry read threads ample time to hang
    try {
      // Thread.sleep(100);
     } catch (Exception e) {
       e.printStackTrace();
     }

    fireEvent("beforeFirstAddAll");
    blockingBuffer.addAll( Collections.singleton( obj ) );

    // allow notified threads to complete 
    try {
      // Thread.sleep(100);
     } catch (Exception e) {
       e.printStackTrace();
     }
    // There should be one thread waiting.
    //assertTrue( "BlockedRemoveWithAddAll1", thread1.isAlive() ^ thread2.isAlive() );
    fireEvent("beforeSecondAddAll");
    blockingBuffer.addAll( Collections.singleton( obj ) );

    // allow notified thread to complete 
    try {
      // Thread.sleep(100);
     } catch (Exception e) {
       e.printStackTrace();
     }
    // There should not be any threads waiting.
    thread1.join();
    thread2.join();
    //assertFalse( "BlockedRemoveWithAddAll1", thread1.isAlive() || thread2.isAlive() );
    //if( thread1.isAlive() || thread2.isAlive() ) {
      //fail( "Live thread(s) when both should be dead." );
    //}
  }

  //-----------------------------------------------------------------------

  /**
   * Tests {@link BlockingBuffer#remove()} in combination with {@link BlockingBuffer#addAll(java.util.Collection)}
   * using multiple read threads.
   * <p/>
   * Two read threads should block on an empty buffer until a collection with two distinct objects is added then both
   * threads should complete. Each thread should have read a different object.
   * @throws InterruptedException 
   */
  @Test
  @Schedules({
    @Schedule(name = "BlockedRemoveWithAddAll2", value = "[beforeNullRemove: afterNullRemove]@readThread1->beforeAddAll@main," + 
        "[beforeNullRemove: afterNullRemove]@readThread2->beforeAddAll@main," + 
        "afterNullRemove@readThread1->afterAddAll@main," + "afterNullRemove@readThread2->afterAddAll@main") })
  public void testBlockedRemoveWithAddAll2() throws InterruptedException {
    Buffer blockingBuffer = BlockingBuffer.decorate( new MyBuffer() );
    Object obj1 = new Object();
    Object obj2 = new Object();
    Set objs = Collections.synchronizedSet( new HashSet() );
    objs.add( obj1 );
    objs.add( obj2 );

    // run methods will remove and compare -- must wait for addAll
    Thread thread1 = new ReadThread( blockingBuffer, objs, "remove", "BlockedRemoveWithAddAll2", "readThread1" );
    Thread thread2 = new ReadThread( blockingBuffer, objs, "remove", "BlockedRemoveWithAddAll2", "readThread2" );
    thread1.start();
    thread2.start();

    // give hungry read threads ample time to hang
    try {
      // Thread.sleep(100);
     } catch (Exception e) {
       e.printStackTrace();
     }

    fireEvent("beforeAddAll");
    blockingBuffer.addAll( objs );
    // allow notified threads to complete 
    try {
      // Thread.sleep(100);
     } catch (Exception e) {
       e.printStackTrace();
     }
    fireEvent("afterAddAll");
    assertEquals( "BlockedRemoveWithAddAll2", 0, objs.size() );

    // There should not be any threads waiting.
    thread1.join();
    thread2.join();
    //assertFalse( "BlockedRemoveWithAddAll1", thread1.isAlive() || thread2.isAlive() );
    //if( thread1.isAlive() || thread2.isAlive() ) {
      //fail( "Live thread(s) when both should be dead." );
    //}
  }

  //-----------------------------------------------------------------------

  /**
   * Tests interrupted remove.
   */
  @Test
  @Schedules({
    @Schedule(name = "InterruptedRemove", value = "[beforeRemove: afterRemove]@readThread->beforeInterrupt@main," +
    		"finishAddException@readThread->afterInterrupt@main") })
  public void testInterruptedRemove() throws InterruptedException {
    Buffer blockingBuffer = BlockingBuffer.decorate( new MyBuffer() );
    Object obj = new Object();

    // spawn a read thread to wait on the empty buffer
    ArrayList exceptionList = new ArrayList();
    Thread thread = new ReadThread( blockingBuffer, obj, exceptionList, "remove", "InterruptedRemove", "readThread" );
    thread.start();
    try {
      //Thread.sleep(100); //INS-SLEEP
    } catch (Exception e1) {
      e1.printStackTrace();
    }

    // Interrupting the thread should cause it to throw BufferUnderflowException
    fireEvent("beforeInterrupt");
    thread.interrupt();
    // Chill, so thread can throw and add message to exceptionList
    // Thread.sleep(100);
    fireEvent("afterInterrupt");
    assertTrue( "InterruptedRemove",
        exceptionList.contains( "BufferUnderFlow" ) );
    //assertFalse("InterruptedRemove",thread.isAlive());
    //if( thread.isAlive() ) {
      //fail( "Read thread has hung." );
    //}

  }

  protected static class NonDelayedAdd extends Thread {

    Buffer buffer;

    Object obj;

    long delay = 100;

    public NonDelayedAdd( Buffer buffer, Object obj, String name ) {
      this.buffer = buffer;
      this.obj = obj;
      this.setName(name);
    }



    public void run() {
      try {
       //  wait for other thread to block on get() or remove()
       // Thread.sleep(delay);
      } catch (Exception e) {
      }
      fireEvent("beforeAdd");
      buffer.add( obj );
      fireEvent("afterAdd");
    }
  }

  protected static class NonDelayedAddAll extends Thread {

    Buffer buffer;

    Object obj;

    long delay = 1000;
    
    public NonDelayedAddAll( Buffer buffer, Object obj, String name ) {
      this.buffer = buffer;
      this.obj = obj;
      this.setName(name);
    }


    public void run() {
      try {
      // wait for other thread to block on get() or remove()
//       Thread.sleep(delay);
      } catch (Exception e) {
      }
      fireEvent("beforeAddAll");
      buffer.addAll( Collections.singleton( obj ) );
      fireEvent("afterAddAll");
    }
  }
  
  protected static class DelayedAdd extends Thread {

    Buffer buffer;

    Object obj;

    long delay = 1000;

    public DelayedAdd( Buffer buffer, Object obj, long delay ) {
        this.buffer = buffer;
        this.obj = obj;
        this.delay = delay;
    }

    DelayedAdd( Buffer buffer, Object obj ) {
        super();
        this.buffer = buffer;
        this.obj = obj;
    }

    public void run() {
        try {
            Thread.currentThread().setName("addThread");
            // wait for other thread to block on get() or remove()
            //Thread.sleep( delay );
            fireEvent("beforeAdd");
        }
        catch( Exception e ) {
        }
        buffer.add( obj );
    }
}

protected static class DelayedAddAll extends Thread {

    Buffer buffer;

    Object obj;

    long delay = 100;

    public DelayedAddAll( Buffer buffer, Object obj, long delay ) {
        this.buffer = buffer;
        this.obj = obj;
        this.delay = delay;
    }

    DelayedAddAll( Buffer buffer, Object obj ) {
        super();
        this.buffer = buffer;
        this.obj = obj;
    }

    public void run() {
        try {
            Thread.currentThread().setName("addAllThread");
            // wait for other thread to block on get() or remove()
            //Thread.sleep( delay );
            fireEvent("beforeAddAll");
        }
        catch( Exception e ) {
        }
        buffer.addAll( Collections.singleton( obj ) );
    }
}

  protected static class ReadThread extends Thread {

    Buffer buffer;

    Object obj;

    ArrayList exceptionList = null;

    String action = "get";

    String scheduleName = "";
    
    Set objs;

    ReadThread( Buffer buffer, Object obj, String scheduleName, String name ) {
      super();
      this.buffer = buffer;
      this.obj = obj;
      this.scheduleName = scheduleName;
      this.setName(name);
      }

    ReadThread( Buffer buffer, Object obj, ArrayList exceptionList, String scheduleName, String name ) {
      super();
      this.buffer = buffer;
      this.obj = obj;
      this.exceptionList = exceptionList;
      this.scheduleName = scheduleName;
      this.setName(name);
    }

    ReadThread( Buffer buffer, Object obj, ArrayList exceptionList, String action, String scheduleName, String name ) {
      super();
      this.buffer = buffer;
      this.obj = obj;
      this.exceptionList = exceptionList;
      this.action = action;
      this.scheduleName = scheduleName;
      this.setName(name);
    }

    ReadThread( Buffer buffer, Set objs, String action, String scheduleName, String name ) {
      super();
      this.buffer = buffer;
      this.objs = objs;
      this.action = action;
      this.scheduleName = scheduleName;
      this.setName(name);
    }

    public void run() {
      try {
        if( action == "get" ) {
          fireEvent("beforeGet");
          assertSame(scheduleName, obj, buffer.get() );
          fireEvent("afterGet");        
        }
        else {
          if( null != obj ) {
            fireEvent("beforeRemove");
            assertSame(scheduleName, obj, buffer.remove() );
            fireEvent("afterRemove");
          }
          else {
            fireEvent("beforeNullRemove");
            assertTrue(scheduleName, objs.remove( buffer.remove() ) );
            fireEvent("afterNullRemove");
          }
        }
      }
      catch( BufferUnderflowException ex ) {
        exceptionList.add( "BufferUnderFlow" );
      fireEvent("finishAddException");
      }
    }
  }

  protected static class MyBuffer extends LinkedList implements Buffer {

    public Object get() {
      if( isEmpty() ) {
        throw new BufferUnderflowException();
      }
      return get( 0 );
    }

    public Object remove() {
      if( isEmpty() ) {
        throw new BufferUnderflowException();
      }
      return remove( 0 );
    }
  }



  public String getCompatibilityVersion() {
    return "3.1";
  } 

  //    public void testCreate() throws Exception {
  //        Buffer buffer = BlockingBuffer.decorate(new UnboundedFifoBuffer());
  //        writeExternalFormToDisk((java.io.Serializable) buffer,
  //        "D:/dev/collections/data/test/BlockingBuffer.emptyCollection.version3.1.obj");
  //        buffer = BlockingBuffer.decorate(new UnboundedFifoBuffer());
  //        buffer.add("A");
  //        buffer.add("B");
  //        buffer.add("C");
  //        writeExternalFormToDisk((java.io.Serializable) buffer,
  //        "D:/dev/collections/data/test/BlockingBuffer.fullCollection.version3.1.obj");
  //    }
}
