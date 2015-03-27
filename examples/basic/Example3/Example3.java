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
 * 
 * @author ali kheramdand
 *
 */
public class Example3 {
	int[] ia;
	Something[] oa;

	static class Something {
		int x;
	}

	public static void main(String[] args) {

		(new Example3()).start();
	}

	void start() {
		System.out.println("start");
		ia = new int[] { 1, 2, 3 };
		oa = new Something[] { new Something(), new Something(),
				new Something() };

		try {
			System.out.println("before thread creation");
			MyThread t1 = new MyThread();
			// MyThread t2 = new MyThread();
			System.out.println("before thread start");
			t1.start();
			// t2.start();
			System.out.println("before join");
			t1.join();
			System.out.println("after join");
			// t2.join();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	class MyThread extends Thread {

		public void run() {
			System.out.println("My thread run");
			synchronized (this) {
				//replaymop.preprocessing.instrumentation.Array.beforeGet(ia, 0);
				System.out.println(ia[0]);
				System.out.println(oa[0]);
				int i = ia[0];
				Something o = oa[0];
				System.out.println(i);
				System.out.println(o);
				o.x = 12;
				System.out.println(oa[0].x);
				System.out.println("multi-dim");
				int[][] x = new int[][] { { 1, 2 }, { 3, 4 } };
				x[0][0] = 10;
				System.out.println(x[1]);
				System.out.println(x[1][1]);
			}

		}
	}
}
