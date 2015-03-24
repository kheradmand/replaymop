package replaymop.preprocessing.instrumentation;


import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;


public class Agent {
	public static void premain(String agentArgs, Instrumentation inst){
		System.out.println("registering ArrayElementAccessToMethodCallTransformer...");
		inst.addTransformer(new ArrayElementAccessToMethodCallTransformer(inst), true);
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
		System.out.println("finished preloaded classes");
	}
}
