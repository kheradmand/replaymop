
public class Example2 {

	
	public static void main(String[] args)
	{
		try
		{
			Thread t1 = new MyThread1();
			Thread t2 = new MyThread2();

			t1.start();
			t2.start();
			
			System.out.println(System.out);
			
			t1.join();
			
			t2.join();
				
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}
	
	static class MyThread1 extends Thread
	{
		
		public void run()
		{
			System.out.println("1");
		}
	}
	
	static class MyThread2 extends Thread
	{
		
		public void run()
		{
			System.out.println("2");
		}
	}
}
