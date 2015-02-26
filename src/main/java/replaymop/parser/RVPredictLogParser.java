package replaymop.parser;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.EventItem;
import com.runtimeverification.rvpredict.log.OfflineLoggingFactory;
import com.runtimeverification.rvpredict.trace.Event;
import com.runtimeverification.rvpredict.trace.EventType;
import com.runtimeverification.rvpredict.trace.EventUtils;
import com.runtimeverification.rvpredict.trace.MemoryAccessEvent;
import com.runtimeverification.rvpredict.trace.TraceCache;

import replaymop.ReplayMOPException;
import replaymop.replayspecification.ReplaySpecification;
import replaymop.replayspecification.ScheduleUnit;
import replaymop.replayspecification.Variable;

public class RVPredictLogParser extends Parser {

	OfflineLoggingFactory metaData;
	TraceCache trace;

	Map<Integer, Set<Long>> locIdThreadAccessSet = new HashMap<Integer, Set<Long>>();
	// distinguish array and and non array
	Set<Long> threads = new TreeSet<Long>();

	public RVPredictLogParser(File logFolder) {
		Configuration config = new Configuration();
		config.outdir = logFolder.toString();
		metaData = new OfflineLoggingFactory(config);
		trace = new TraceCache(metaData);
		initSpec();

		for (int i = 1; metaData.getVarSig(i) != null; i++)
			System.out
					.println(String.format("%d: %s", i, metaData.getVarSig(i)));
		System.out.println("--");
	}

	private void initSpec() {
		// TODO: sharedvars, threads
		this.spec = new ReplaySpecification();
		spec.addAfterSyncDefault();
		spec.addBeforeSyncDefault();
	}

	boolean important(EventType type) {
		switch (type) {
		case READ:
		case WRITE:
		case WRITE_LOCK:
		case WRITE_UNLOCK:
		case READ_LOCK:
		case READ_UNLOCK:
		case WAIT_REL:
			// case WAIT_ACQ:
			// case START:
			// case PRE_JOIN:
		case JOIN:
		case JOIN_MAYBE_FAILED:
			// case CLINIT_ENTER:
			// case CLINIT_EXIT:
			// case BRANCH:
			return true;
		default:
			return false;
		}
	}
	
	
	void addEventToSchedule(Long eventThread){
		int lastIndex = spec.schedule.size() - 1;
		if (spec.schedule.isEmpty() || spec.schedule.get(lastIndex).thread != eventThread)
			spec.schedule.add(new ScheduleUnit(eventThread, 1));
		else
			spec.schedule.get(lastIndex).count++;
	}

	@Override
	protected void startParsing() throws ReplayMOPException {
		try {
			EventItem eventItem;
			for (int index = 1; ((eventItem = trace.getEvent(index)) != null); index++) {
				Event event = EventUtils.of(eventItem);
				EventType eventType = event.getType();

				threads.add(event.getTID());
				

				if (!important(eventType))
					continue;
				
				addEventToSchedule(event.getTID());

				if (event instanceof MemoryAccessEvent) {
					int loc = -eventItem.ADDRR;
					if (!locIdThreadAccessSet.keySet().contains(loc))
						locIdThreadAccessSet.put(loc, new HashSet<Long>());
					locIdThreadAccessSet.get(loc).add(event.getTID());
				}

				System.out.println(eventItem.ADDRL + " " + eventItem.ADDRR);
				System.out.println(event);
				// TODO: schedule, thread creation order
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new ReplayMOPException("error in parsing log file");
		}
		generateSpec();

		System.out.println("---");
		System.out.println(spec);
	}

	void generateSpec() {
		for (Map.Entry e : locIdThreadAccessSet.entrySet()) {
			if (((Set) e.getValue()).size() > 1) {
				int loc = (int) e.getKey();
				String varSig = metaData.getVarSig(loc);
				System.out.println(varSig);
				spec.shared.add(new Variable("*", varSig.replace("/", ".")
						.replace("$", "")));
			}
		}
		Map<Long, Long> logThreadToRealThread = new HashMap<Long, Long>();
		logThreadToRealThread.put(1L, 1L);
		Long realThread = 10L;
		for (Long logThread : threads) {
			assert realThread != 10L || logThread == 1L;
			if (logThread == 1L)
				continue;
			logThreadToRealThread.put(logThread, realThread++);
		}
		spec.threads.addAll(logThreadToRealThread.values());
		
		for (ScheduleUnit unit : spec.schedule)
			unit.thread = logThreadToRealThread.get(unit.thread);
		
		// shared vars
		// thread numbers
	}

	static public void main(String[] args) {
		Parser parser = new RVPredictLogParser(new File(args[0]));
		parser.parse();
	}

}
