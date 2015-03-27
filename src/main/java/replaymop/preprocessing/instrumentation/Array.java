package replaymop.preprocessing.instrumentation;

public class Array {
	public static void beforeGet(Object array, int index){
		if (Agent.debug)
			System.out.println("beforeGet");
	}
	public static void afterGet(Object array, int index){
		
	}
	public static void beforeSet(Object array, int index){
		if (Agent.debug)
			System.out.println("beforeSet");
	}
	public static void afterSet(Object array, int index){
		
	}


}
