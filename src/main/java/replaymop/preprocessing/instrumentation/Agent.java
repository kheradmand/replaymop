package replaymop.preprocessing.instrumentation;


import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;


public class Agent {
	public static boolean debug = false;
	
	public static void premain(String agentArgs, Instrumentation inst){
		if (agentArgs != null && agentArgs.equals("debug"))
			debug = true;
		if (debug)
			System.out.println("registering ArrayElementAccessToMethodCallTransformer...");
		inst.addTransformer(new ArrayElementAccessLogger(inst), true);
		for (Class<?> c : inst.getAllLoadedClasses()) {
            if (inst.isModifiableClass(c)) {
                try {
                    inst.retransformClasses(c);
                } catch (UnmodifiableClassException e) {
                    // should not happen
                    e.printStackTrace();
                }
            } else {
                /* TODO(YilongL): Shall(can) we register fields of these
                 * unmodifiable classes too? We know for sure that primitive
                 * classes and array class are unmodifiable. And if these are
                 * the only unmodifiable classes then there is no field for us
                 * to register (even the `length' field of an array object is
                 * accessed by a specific bytecode instruction `arraylength'. */
            }
        }
		if (debug)
			System.out.println("finished preloaded classes");
	}
}
