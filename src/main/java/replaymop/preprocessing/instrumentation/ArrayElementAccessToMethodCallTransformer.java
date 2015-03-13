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
						switch (opcode) {
						case Opcodes.AALOAD:
							type = Type.getObjectType("java/lang/Object");
						case Opcodes.BALOAD:
							type = Type.BYTE_TYPE;
						case Opcodes.CALOAD:
							type = Type.CHAR_TYPE;
						case Opcodes.SALOAD:
							type = Type.SHORT_TYPE;
						case Opcodes.IALOAD:
							type = Type.INT_TYPE;
						case Opcodes.FALOAD:
							type = Type.FLOAT_TYPE;
						case Opcodes.DALOAD:
							type = Type.DOUBLE_TYPE;
						case Opcodes.LALOAD:
							type = Type.LONG_TYPE;
							Method getMethod = null;
							try {
								getMethod = Method.getMethod(Array.class.getMethod("get", type.getClass(), Type.INT_TYPE.getClass()));
							} catch (NoSuchMethodException | SecurityException e) {
								e.printStackTrace();
							}
							((GeneratorAdapter)mv).invokeStatic(owner, getMethod);
							break;
						case Opcodes.AASTORE:
							Type.getObjectType("java/lang/Object");
						case Opcodes.BASTORE:
							type = Type.BYTE_TYPE;
						case Opcodes.CASTORE:
							type = Type.CHAR_TYPE;
						case Opcodes.SASTORE:
							type = Type.SHORT_TYPE;
						case Opcodes.IASTORE:
							type = Type.INT_TYPE;
						case Opcodes.FASTORE:
							type = Type.FLOAT_TYPE;
						case Opcodes.DASTORE:
							type = Type.DOUBLE_TYPE;
						case Opcodes.LASTORE:
							type = Type.LONG_TYPE;
							Method setMethod = null;
							try {
								setMethod = Method.getMethod(Array.class.getMethod("set", type.getClass(), Type.INT_TYPE.getClass(), Type.INT_TYPE.getClass()));
							} catch (NoSuchMethodException | SecurityException e) {
								e.printStackTrace();
							}
							((GeneratorAdapter)mv).invokeStatic(owner, setMethod);
							break;
						default:
							super.visitInsn(opcode);
							break;

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
