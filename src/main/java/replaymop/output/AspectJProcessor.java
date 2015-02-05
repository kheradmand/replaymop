package replaymop.output;

import replaymop.output.aspectj.Aspect;
import replaymop.parser.rs.ReplaySpecification;

public class AspectJProcessor {
	ReplaySpecification spec;
	Aspect aspect;
	private AspectJProcessor(ReplaySpecification spec){
		this.spec = spec;
		aspect = new Aspect(spec.fileName + "Aspect");
	}
	
	void startProcessing(){
		
	}
	
	public static Aspect process(ReplaySpecification spec){
		AspectJProcessor processor = new AspectJProcessor(spec);
		processor.startProcessing();
		return processor.aspect;
	}
}
