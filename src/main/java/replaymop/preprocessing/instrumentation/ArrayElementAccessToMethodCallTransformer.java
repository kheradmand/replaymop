package replaymop.preprocessing.instrumentation;

import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

import com.runtimeverification.rvpredict.instrumentation.InstrumentUtils;
import com.runtimeverification.rvpredict.metadata.ClassFile;

public class ArrayElementAccessToMethodCallTransformer implements
		ClassFileTransformer {
	
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
                        && !name.startsWith("java")
                        && !name.startsWith("sun")) {
                    System.err.println("[Java-agent] missed to intercept class load: " + cls);
                }
            }
    }
    
    public ArrayElementAccessToMethodCallTransformer(Instrumentation instrumentation){
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
			ClassWriter writer = new ClassWriter(reader,
					loader);
			

			ClassVisitor transformer = new ClassVisitor(Opcodes.ASM5, writer) {

				@Override
				public MethodVisitor visitMethod(int access, String name,
						String desc, String signature, String[] exceptions) {
					// TODO Auto-generated method stub
					
					
					return new MethodVisitor(Opcodes.ASM5, super.visitMethod(
							access, name, desc, signature, exceptions)) {

						static final String arrayClass = "replaymop/preprocessing/instrumentation/Array";

						void replaceGet(String type)
								throws NoSuchMethodException, SecurityException {
							Method method = null;
							if (type.equals("")){
								for (java.lang.reflect.Method m : Array.class.getMethods())
									if (m.getName().equals("get"))
										method = Method.getMethod(m);
										
								
							}else
								method = Method.getMethod(Array.class
									.getMethod("get" + type, Object.class,
											int.class));
							assert method != null;
							mv.visitMethodInsn(Opcodes.INVOKESTATIC,
									arrayClass, method.getName(),
									method.getDescriptor(), false);
						}

						void replaceSet(Class<?> type)
								throws NoSuchMethodException, SecurityException {
							Method method = Method.getMethod(Array.class
									.getMethod("set", Object.class, int.class,
											type));
							mv.visitMethodInsn(Opcodes.INVOKESTATIC,
									arrayClass, method.getName(),
									method.getDescriptor(), false);
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

						}
					};
				}
			};

			reader.accept(transformer, ClassReader.EXPAND_FRAMES);

			byte[] ret = writer.toByteArray();
			 (new ClassReader(ret)).accept(new TraceClassVisitor(null, new
			 ASMifier(), new PrintWriter(
			 System.out)), ClassReader.EXPAND_FRAMES);
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

class ClassWriter extends org.objectweb.asm.ClassWriter {

    private final ClassLoader loader;

    public ClassWriter(ClassReader classReader, ClassLoader loader) {
        super(classReader, org.objectweb.asm.ClassWriter.COMPUTE_FRAMES);
        this.loader = loader == null ? ClassLoader.getSystemClassLoader() : loader;
    }

    /**
     * The default implementation is fundamentally flawed because its use of
     * reflection to look up the class hierarchy. See <a href=
     * "http://chrononsystems.com/blog/java-7-design-flaw-leads-to-huge-backward-step-for-the-jvm"
     * >Java 7 Bytecode Verifier: Huge backward step for the JVM</a>.
     */
    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        ClassFile class1 = ClassFile.getInstance(loader, type1);
        ClassFile class2 = ClassFile.getInstance(loader, type2);

        if (class1 == null || class2 == null) {
            throw new RuntimeException("Unable to find the common superclass of " + type1 + " and "
                    + type2);
        }

        if (class1.isAssignableFrom(class2)) {
            return type1;
        } else if (class2.isAssignableFrom(class1)) {
            return type2;
        }

        if (class1.isInterface() || class2.isInterface()) {
            return "java/lang/Object";
        } else {
            do {
                class1 = class1.getSuperclass();
            } while (!class1.isAssignableFrom(class2));
            return class1.getClassName();
        }
    }

}

