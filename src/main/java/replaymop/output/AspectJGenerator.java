package replaymop.output;

import replaymop.output.aspectj.Aspect;
import replaymop.parser.rs.ReplaySpecification;

public class AspectJGenerator {
	ReplaySpecification spec;
	Aspect aspect;
	private AspectJGenerator(ReplaySpecification spec){
		this.spec = spec;
		aspect = new Aspect(spec.fileName + "Aspect");
	}
	
	
	void addCode(String code){
		aspect.body += code + "\n";
	}
	
	
	void generateImports(){
		addCode("import java.util.concurrent.locks;");
	}
	
	void generateThreadCreationOrderEnforcer(){
		addCode("final Lock  threadCreationLock = "
				+ "new ReentrantLock();");
		addCode("final Condition threadCreated = threadCreationLock.newCondition();");
		
		addCode("final long[] threadOrder = " + spec.threadOrder.toString() + ";");
		addCode("int threadOrderIndex = 0");
		
		addCode("before(): call(java.lang.Thread.new(*)){");
		addCode("threadCreationLock.lock();");
		addCode("while (threadOrderIndex < threadOrder.length && threadOrder[threadOrderIndex] != Thread.currentThread().getId()){");
		addCode("try{");
		addCode("threadCreated.await()");
		addCode("catch (InterruptedException e){}");
		addCode("}");
		
		addCode("after(): call(java.lang.Thread.new(*)){");
		addCode("threadOrderIndex++");
		addCode("headMatched.signalAll()");
		addCode("threadCreationLock.unlock();");
		addCode("}");
		
		
		
	}
	
	void generateSyncPointCuts(){
		//for (String func : spec.be)
	}
	
	void startGeneration(){
		addCode("final Object globalLock = new Object();");
		generateImports();
		generateThreadCreationOrderEnforcer();
		//generateVariableLocks();
		//generatePointcuts();
		
	}
	
	public static Aspect generate(ReplaySpecification spec){
		AspectJGenerator generator = new AspectJGenerator(spec);
		generator.startGeneration();
		return generator.aspect;
	}
}
