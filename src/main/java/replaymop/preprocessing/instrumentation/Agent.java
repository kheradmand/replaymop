package replaymop.preprocessing.instrumentation;


import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;


public class Agent {
	public static void premain(String agentArgs, Instrumentation inst){
		System.out.println("registering ArrayElementAccessToMethodCallTransformer...");
		inst.addTransformer(new ArrayElementAccessToMethodCallTransformer());
	}
}
