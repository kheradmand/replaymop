import java.util.Map;
import java.util.TreeMap;


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
 * 
 * @author ali kheradmand
 *
 */
public class Example5 {
	Map<Integer, Something> sa;
	
	static class Something {
		int x;
	}
	
	public static void main(String[] args)
	{
		
		(new Example5()).start();
	}
	
	void start(){
		System.out.println("main start");

		sa = new TreeMap<Integer, Something>();
		System.out.println("main after map creation");

		sa.put(0, new Something());
		
		try
		{
			
			System.out.println("main before thread creation");
			MyThread t1 = new MyThread();
			MyThread t2 = new MyThread();
			
			
			System.out.println("main before thread start");
			t1.start();
			t2.start();
			
			System.out.println("main before thread join");
			t1.join();
			t2.join();
			
				
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	 class MyThread extends Thread
	{
		
		public void run()
		{
			System.out.println("MyThread run");
			sa.put(1, new Something());
			System.out.println("MyThread after adding element");
			sa.get(1).x = 12;
			System.out.println("MyThread after setting x = 12");
			System.out.println(sa.get(1));
			Map<Integer, Something> kk = sa;
			kk.put(2, new Something());
		}
	}
}
