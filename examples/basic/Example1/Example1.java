/*******************************************************************************
 * Copyright (c) 2015 University of Illinois
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

/**
 * Example2 contains races on x, but slightly more complicated than Example
 * with more children threads.
 * 
 * @author jeffhuang
 *		   ali kheramand
 */
public class Example1 {
	static Object lock = new Object();
	
	static int x=0;
	
	public static void main(String[] args)
	{
		try
		{
			System.out.println("before thread creation");
			MyThread t1 = new MyThread();
			MyThread t2 = new MyThread();
			System.out.println("before thread start");
			t1.start();
			t2.start();
			System.out.println("before main lock");
			synchronized(lock)
			{
				x++;
			}
			System.out.println("after main lock");
			
			x=0;//race here
			System.out.println("after t1 join");
			t1.join();
			System.out.println("after t2 join");
			t2.join();
				
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}
	
	static class MyThread extends Thread
	{
		
		public void run()
		{
			System.out.println("MyThread run");
			//sleep for 10ms to let main thread go first 
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
			System.out.println("MyThread before lock");
			synchronized(lock)
			{
				System.out.println("MyThread inside lock");
				x++;
			}
			System.out.println("MyThread after lock");
			
			System.out.println(1/x);//may throw divide by zero exception

		}
	}
}
