package replaymop.preprocessing.instrumentation;

import java.io.PrintWriter;
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
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

import com.runtimeverification.rvpredict.instrumentation.InstrumentUtils;
import com.runtimeverification.rvpredict.metadata.ClassFile;

public class ArrayElementAccessToMethodCallTransformer implements ClassFileTransformer {
	@Override
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {

		System.out.println("transformer called on " + className);
		
			
		ClassFile classFile = ClassFile.getInstance(loader, className, classfileBuffer);
		if (!InstrumentUtils.needToInstrument(classFile) || exclude(className))
			return null;
		
		System.out.println("Allowed " + className);

		ClassReader reader = new ClassReader(classfileBuffer);
		ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
		
		ClassVisitor transformer = new ClassVisitor(Opcodes.ASM5, writer) {
			
			
			@Override
			public MethodVisitor visitMethod(int access, String name,
					String desc, String signature, String[] exceptions) {
				// TODO Auto-generated method stub
				return new MethodVisitor(Opcodes.ASM5, super.visitMethod(
						access, name, desc, signature, exceptions)) {
					
					 static final String arrayClass = "replaymop/preprocessing/instrumentation/Array";
					
					void replaceGet(String type) throws NoSuchMethodException, SecurityException{
						Method method = Method.getMethod(Array.class.getMethod("get" + type, Object.class, int.class));
						mv.visitMethodInsn(Opcodes.INVOKESTATIC, arrayClass, method.getName(), method.getDescriptor(), false);
					}
					
					void replaceSet(Class<?> type) throws NoSuchMethodException, SecurityException {
						Method method = Method.getMethod(Array.class.getMethod("set", Object.class, int.class, type));
						mv.visitMethodInsn(Opcodes.INVOKESTATIC, arrayClass, method.getName(), method.getDescriptor(), false);
					}
					
					@Override
					public void visitInsn(int opcode) {
						try {
						switch (opcode) {
						case Opcodes.AALOAD:
							replaceGet("");
							break;
						case Opcodes.BALOAD:
							replaceGet("byte");
							break;
						case Opcodes.CALOAD:
							replaceGet("char");
							break;
						case Opcodes.SALOAD:
							replaceGet("short");
							break;
						case Opcodes.IALOAD:
							replaceGet("int");
							break;
						case Opcodes.FALOAD:
							replaceGet("float");
							break;
						case Opcodes.DALOAD:
							replaceGet("double");
							break;
						case Opcodes.LALOAD:
							replaceGet("long");
							break;		
						case Opcodes.AASTORE:
							replaceSet(Object.class);
							break;
						case Opcodes.BASTORE:
							replaceSet(byte.class);
							break;
						case Opcodes.CASTORE:
							replaceSet(char.class);
							break;
						case Opcodes.SASTORE:
							replaceSet(short.class);
							break;
						case Opcodes.IASTORE:
							replaceSet(int.class);
							break;
						case Opcodes.FASTORE:
							replaceSet(float.class);
							break;
						case Opcodes.DASTORE:
							replaceSet(double.class);
							break;
						case Opcodes.LASTORE:
							replaceSet(long.class);
							break;						
						default:
							super.visitInsn(opcode);
							break;

						}
						} catch (NoSuchMethodException | SecurityException e) {
							e.printStackTrace();
						}
						//System.out.println("hello world!");

					}
				};
			}
		};
		
		reader.accept(transformer, ClassReader.EXPAND_FRAMES);
		
		byte [] ret = writer.toByteArray();
		//(new ClassReader(ret)).accept(new TraceClassVisitor(null, new ASMifier(), new PrintWriter(
        //        System.out)), ClassReader.EXPAND_FRAMES);
	
		return ret;
	}

	boolean exclude(String cname) {
		return cname.startsWith("replaymop") || cname.startsWith("java") || cname.startsWith("sun");
	}

}
