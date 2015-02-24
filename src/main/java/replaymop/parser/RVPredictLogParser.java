package replaymop.parser;

import java.io.File;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.EventItem;
import com.runtimeverification.rvpredict.log.OfflineLoggingFactory;
import com.runtimeverification.rvpredict.trace.Event;
import com.runtimeverification.rvpredict.trace.EventUtils;
import com.runtimeverification.rvpredict.trace.TraceCache;

import replaymop.ReplayMOPException;
import replaymop.replayspecification.ReplaySpecification;

public class RVPredictLogParser extends Parser{
	
	OfflineLoggingFactory metaData;
	TraceCache trace;
	
	public RVPredictLogParser(File logFolder) {
		Configuration config = new Configuration();
		config.outdir = logFolder.toString();
		metaData = new OfflineLoggingFactory(config);
		trace = new TraceCache(metaData);
		initSpec();
		
		for (int i=1;metaData.getVarSig(i)!=null;i++)
			System.out.println(String.format("%d: %s", i, metaData.getVarSig(i)));
		System.out.println("--");
	}
	
	private void initSpec(){
		//TODO: sharedvars, threads
		this.spec = new ReplaySpecification();
		spec.addAfterSyncDefault();
		spec.addBeforeSyncDefault();
		
	}
	
	@Override
	protected void startParsing() throws ReplayMOPException{
		try{
			EventItem eventItem;
			for (int index = 1; ((eventItem = trace.getEvent(index)) != null); index++){
				Event event = EventUtils.of(eventItem);
				event.getType();
				spec.threads.add(event.getTID());
				System.out.println(event);
				//TODO: schedule, thread creation order
			}
		}catch(Exception e){
			e.printStackTrace();
			throw new ReplayMOPException("error in parsing log file");
		}
		System.out.println("---");
		System.out.println(spec);
		
	}
	
	static public void main(String[] args){
		Parser parser = new RVPredictLogParser(new File(args[0]));
		parser.parse();
	}
	

}
