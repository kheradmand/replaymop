package replaymop.output.aspectj;

import replaymop.parser.rs.ReplaySpecification;

public class AspectJProcessor {
	ReplaySpecification spec;
	Aspect aspect;
	private AspectJProcessor(ReplaySpecification spec){
		this.spec = spec;
		aspect = new Aspect(spec.fileName);
	}
	
	void startProcessing(){
		
	}
	
	public static Aspect process(ReplaySpecification spec){
		AspectJProcessor processor = new AspectJProcessor(spec);
		processor.startProcessing();
		return processor.aspect;
	}
}
