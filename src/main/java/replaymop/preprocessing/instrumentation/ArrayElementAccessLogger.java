package replaymop.preprocessing.instrumentation;

import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

import com.runtimeverification.rvpredict.instrumentation.InstrumentUtils;
import com.runtimeverification.rvpredict.instrumentation.transformer.ClassWriter;
import com.runtimeverification.rvpredict.instrumentation.transformer.MethodTransformer;
import com.runtimeverification.rvpredict.internal.org.objectweb.asm.ClassReader;
import com.runtimeverification.rvpredict.internal.org.objectweb.asm.ClassVisitor;
import com.runtimeverification.rvpredict.internal.org.objectweb.asm.MethodVisitor;
import com.runtimeverification.rvpredict.internal.org.objectweb.asm.Opcodes;
import com.runtimeverification.rvpredict.internal.org.objectweb.asm.Type;
import com.runtimeverification.rvpredict.internal.org.objectweb.asm.commons.GeneratorAdapter;
import com.runtimeverification.rvpredict.internal.org.objectweb.asm.commons.LocalVariablesSorter;
import com.runtimeverification.rvpredict.internal.org.objectweb.asm.commons.Method;
import com.runtimeverification.rvpredict.internal.org.objectweb.asm.util.ASMifier;
import com.runtimeverification.rvpredict.internal.org.objectweb.asm.util.TraceClassVisitor;
import com.runtimeverification.rvpredict.metadata.ClassFile;

public class ArrayElementAccessLogger implements ClassFileTransformer {

	public Instrumentation instrumentation;

	private static final Set<String> loadedClasses = new HashSet<String>();

	private void checkUninterceptedClassLoading(String cname, Class<?> c) {
		loadedClasses.add(cname.replace("/", "."));
		for (Class<?> cls : instrumentation.getAllLoadedClasses()) {
			String name = cls.getName();
			if (loadedClasses.add(name) && !cls.isArray()
					&& !name.startsWith("replaymop")
					&& !name.startsWith("org.objectweb.asm")
					&& !name.startsWith("com.runtimeverification")
					&& !name.startsWith("java") && !name.startsWith("sun")) {
				System.err
						.println("[Java-agent] missed to intercept class load: "
								+ cls);
			}
		}
	}

	public ArrayElementAccessLogger(Instrumentation instrumentation) {
		this.instrumentation = instrumentation;
	}

	@Override
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		try {
			System.out.println("transformer called on " + className);

			checkUninterceptedClassLoading(className, classBeingRedefined);

			ClassFile classFile = ClassFile.getInstance(loader, className,
					classfileBuffer);
			if (!InstrumentUtils.needToInstrument(classFile)
					|| exclude(className))
				return null;

			System.out.println("Allowed " + className);

			ClassReader reader = new ClassReader(classfileBuffer);
			ClassWriter writer = new ClassWriter(reader, loader);

			ClassVisitor transformer = new ClassVisitor(Opcodes.ASM5, writer) {

				@Override
				public MethodVisitor visitMethod(int access, String name,
						String desc, String signature, String[] exceptions) {
					// TODO Auto-generated method stub

					return new MyMethodVisitor(Opcodes.ASM5, super.visitMethod(
							access, name, desc, signature, exceptions));
				}
			};

			reader.accept(transformer, ClassReader.EXPAND_FRAMES);

			byte[] ret = writer.toByteArray();
			(new ClassReader(ret)).accept(new TraceClassVisitor(null,
					new ASMifier(), new PrintWriter(System.out)),
					ClassReader.EXPAND_FRAMES);
			return ret;
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}

	boolean exclude(String cname) {
		return cname.startsWith("replaymop") || cname.startsWith("java")
				|| cname.startsWith("sun")
				|| cname.startsWith("org/objectweb/asm");
	}

}

class MyMethodVisitor extends MethodVisitor {
	final GeneratorAdapter mv;
	
	public MyMethodVisitor(int arg0) {
		super(arg0);
		mv = (GeneratorAdapter) super.mv;
		// TODO Auto-generated constructor stub
	}

	public MyMethodVisitor(int asm5, MethodVisitor visitMethod) {
		// TODO Auto-generated constructor stub
		super(asm5, visitMethod);
		mv = (GeneratorAdapter) super.mv;
	}

	static final String arrayClass = "replaymop/preprocessing/instrumentation/Array";

	@Override
	public void visitInsn(int opcode) {
		try {
			Type type;
			int value;
			Method method;
			switch (opcode) {
			case Opcodes.AALOAD:
			case Opcodes.BALOAD:
			case Opcodes.CALOAD:
			case Opcodes.SALOAD:
			case Opcodes.IALOAD:
			case Opcodes.FALOAD:
			case Opcodes.DALOAD:
			case Opcodes.LALOAD:
				mv.dup2();
				type = MethodTransformer.getValueType(opcode);
				method = Method.getMethod(Array.class.getMethod("beforeGet"
						+ type, Object.class, int.class));
				assert method != null;
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, arrayClass,
						method.getName(), method.getDescriptor(), false);
				break;
			case Opcodes.AASTORE:
			case Opcodes.BASTORE:
			case Opcodes.CASTORE:
			case Opcodes.SASTORE:
			case Opcodes.IASTORE:
			case Opcodes.FASTORE:
			case Opcodes.DASTORE:
			case Opcodes.LASTORE:
				type = MethodTransformer.getValueType(opcode);
				value = mv.newLocal(type);
				mv.storeLocal(value, type);
				mv.dup2();
				mv.loadLocal(value, type);
				method = Method.getMethod(Array.class.getMethod("beforeSet"
						+ type, Object.class, int.class));
				assert method != null;
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, arrayClass,
						method.getName(), method.getDescriptor(), false);
				break;
			}

			super.visitInsn(opcode);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}

	}
}
