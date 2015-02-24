package replaymop.parser;

import java.io.File;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.EventItem;
import com.runtimeverification.rvpredict.log.OfflineLoggingFactory;
import com.runtimeverification.rvpredict.trace.Event;
import com.runtimeverification.rvpredict.trace.EventUtils;
import com.runtimeverification.rvpredict.trace.TraceCache;

import replaymop.ReplayMOPException;

public class RVPredictLogParser extends Parser{
	
	OfflineLoggingFactory metaData;
	TraceCache trace;
	
	public RVPredictLogParser(File logFolder) {
		Configuration config = new Configuration();
		config.outdir = logFolder.toString();
		metaData = new OfflineLoggingFactory(config);
		trace = new TraceCache(metaData);
		initSpec();
	}
	
	private void initSpec(){
		//TODO: sharedvars, threads
		spec.addAfterSyncDefault();
		spec.addBeforeSyncDefault();
		
	}
	
	@Override
	protected void startParsing() throws ReplayMOPException{
		try{
			EventItem eventItem;
			for (int index = 0; ((eventItem = trace.getEvent(index)) != null); index++){
				Event event = EventUtils.of(eventItem);
				event.getType();
				//TODO: schedule, thread creation order
			}
		}catch(Exception e){
			e.printStackTrace();
			throw new ReplayMOPException("error in parsing log file");
		}
		
	}
	

}
