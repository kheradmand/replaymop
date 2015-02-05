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
	
	
	
	
	void generateThreadCreationOrderEnforcer(){
		aspect.body.replace("%THREAD_CREATION_ORDER%", spec.threadOrder.toString());
		
	}
	
	void generateSyncPointCuts(){
		//for (String func : spec.be)
	}
	
	void startGeneration(){
		
		
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
