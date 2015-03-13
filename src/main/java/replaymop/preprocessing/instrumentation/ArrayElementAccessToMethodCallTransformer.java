package replaymop.preprocessing.instrumentation;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public class ArrayElementAccessToMethodCallTransformer implements ClassFileTransformer {
	@Override
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {

		System.out.println("transformer called");
		
		if (exclude(className))
			return null;

		ClassReader reader = new ClassReader(classfileBuffer);
		ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
		
		ClassVisitor transformer = new ClassVisitor(Opcodes.ASM5) {
			
			@Override
			public MethodVisitor visitMethod(int access, String name,
					String desc, String signature, String[] exceptions) {
				// TODO Auto-generated method stub
				return new MethodVisitor(Opcodes.ASM5, super.visitMethod(
						access, name, desc, signature, exceptions)) {
					@Override
					public void visitInsn(int opcode) {
						// TODO Auto-generated method stub
						Type owner = Type.getType(Array.class);
						Type type;
						Method getMethod = null;
						Method setMethod = null;
						try {
						switch (opcode) {
						case Opcodes.AALOAD:
							getMethod = Method.getMethod(Array.class.getMethod("get", Object.class, int.class));
						case Opcodes.BALOAD:
							getMethod = Method.getMethod(Array.class.getMethod("get", byte.class, int.class));
						case Opcodes.CALOAD:
							getMethod = Method.getMethod(Array.class.getMethod("get", char.class, int.class));
						case Opcodes.SALOAD:
							getMethod = Method.getMethod(Array.class.getMethod("get", short.class, int.class));
						case Opcodes.IALOAD:
							getMethod = Method.getMethod(Array.class.getMethod("get", int.class, int.class));
						case Opcodes.FALOAD:
							getMethod = Method.getMethod(Array.class.getMethod("get", float.class, int.class));
						case Opcodes.DALOAD:
							getMethod = Method.getMethod(Array.class.getMethod("get", double.class, int.class));
						case Opcodes.LALOAD:
							getMethod = Method.getMethod(Array.class.getMethod("get", long.class, int.class));
							
							
								//getMethod = Method.getMethod(Array.class.getMethod("get", (Class)type.getClass(), Type.INT_TYPE.getClass()));
						
							((GeneratorAdapter)mv).invokeStatic(owner, getMethod);
							break;
						case Opcodes.AASTORE:
							setMethod = Method.getMethod(Array.class.getMethod("set", Object.class, int.class, Object.class));
						case Opcodes.BASTORE:
							setMethod = Method.getMethod(Array.class.getMethod("set", Object.class, int.class, byte.class));
						case Opcodes.CASTORE:
							setMethod = Method.getMethod(Array.class.getMethod("set", Object.class, int.class, char.class));
						case Opcodes.SASTORE:
							setMethod = Method.getMethod(Array.class.getMethod("set", Object.class, int.class, short.class));
						case Opcodes.IASTORE:
							setMethod = Method.getMethod(Array.class.getMethod("set", Object.class, int.class, int.class));
						case Opcodes.FASTORE:
							setMethod = Method.getMethod(Array.class.getMethod("set", Object.class, int.class, float.class));
						case Opcodes.DASTORE:
							setMethod = Method.getMethod(Array.class.getMethod("set", Object.class, int.class, double.class));
						case Opcodes.LASTORE:
							setMethod = Method.getMethod(Array.class.getMethod("set", Object.class, int.class, long.class));
							
							
//								setMethod = Method.getMethod(Array.class.getMethod("set", type.getClass(), int.class, int.class));
							
							((GeneratorAdapter)mv).invokeStatic(owner, setMethod);
							break;
						default:
							super.visitInsn(opcode);
							break;

						}
						} catch (NoSuchMethodException | SecurityException e) {
							e.printStackTrace();
						}

					}
				};
			}
		};
		
		reader.accept(transformer, ClassReader.EXPAND_FRAMES);
		return writer.toByteArray();
	}

	boolean exclude(String cname) {
		return cname.startsWith("replaymop");
	}

}
