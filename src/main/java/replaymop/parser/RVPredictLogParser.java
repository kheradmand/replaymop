package replaymop.parser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.EventItem;
import com.runtimeverification.rvpredict.log.OfflineLoggingFactory;
import com.runtimeverification.rvpredict.trace.Event;
import com.runtimeverification.rvpredict.trace.EventType;
import com.runtimeverification.rvpredict.trace.EventUtils;
import com.runtimeverification.rvpredict.trace.MemoryAccessEvent;
import com.runtimeverification.rvpredict.trace.TraceCache;

import replaymop.Main;
import replaymop.Parameters;
import replaymop.ReplayMOPException;
import replaymop.replayspecification.ReplaySpecification;
import replaymop.replayspecification.ScheduleUnit;
import replaymop.replayspecification.Variable;

public class RVPredictLogParser extends Parser {
	public static final long FIRST_THREAD_ID = 11L; // it is 10 but agents add
													// new thread
	public static final String MOCK_STATE_FIELD = "$state";

	OfflineLoggingFactory metaData;
	TraceCache trace;

	Map<Integer, Set<Long>> locIdThreadAccessSet = new HashMap<Integer, Set<Long>>();
	// distinguish array and and non array
	Map<Integer, List<ScheduleUnit>> locIdinSchedUnit = new HashMap<Integer, List<ScheduleUnit>>();
	Set<Long> threads = new TreeSet<Long>();

	public RVPredictLogParser(File logFolder) {
		Configuration config = new Configuration();
		config.outdir = logFolder.toString();
		metaData = new OfflineLoggingFactory(config);
		trace = new TraceCache(metaData);
		this.spec = new ReplaySpecification();
		spec.fileName = logFolder.getName().toUpperCase();
		initSpec();

		if (Main.parameters.debug) {
			for (int i = 1; metaData.getVarSig(i) != null; i++)
				System.out.println(String.format("%d: %s", i,
						metaData.getVarSig(i)));
			System.out.println("--");
		}
	}

	private void initSpec() {
		// TODO: sharedvars, threads
		spec.addAfterSyncDefault();
		spec.addBeforeSyncDefault();
		spec.shared.add(new Variable("array", "0")); // TODO: temp
		// rv-predict does not log notify
		spec.beforeSync.remove("void java.lang.Object.notify()");
		spec.beforeSync.remove("void java.lang.Object.notifyAll()");
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

	void addEventToSchedule(Long eventThread) {
		int lastIndex = spec.schedule.size() - 1;
		if (spec.schedule.isEmpty()
				|| spec.schedule.get(lastIndex).thread != eventThread)
			spec.schedule.add(new ScheduleUnit(eventThread, 1));
		else
			spec.schedule.get(lastIndex).count++;
	}

	@Override
	protected void startParsing() throws ReplayMOPException {
		try {
			EventItem eventItem;
			for (int index = 1; ((eventItem = trace.getNextEvent()) != null); index++) {
				Event event = EventUtils.of(eventItem);
				EventType eventType = event.getType();

				threads.add(event.getTID());

				if (Main.parameters.debug)
					System.out.println("----" + event + " "
							+ metaData.getStmtSig(event.getLocId()) + "\t"
							+ eventItem.ADDRL + " " + eventItem.ADDRR);

				if (!important(eventType))
					continue;

				addEventToSchedule(event.getTID());

				int loc = -eventItem.ADDRR;
				if (event instanceof MemoryAccessEvent && loc > 0 /* TODO:tem */) {
					if (!locIdThreadAccessSet.keySet().contains(loc))
						locIdThreadAccessSet.put(loc, new HashSet<Long>());
					locIdThreadAccessSet.get(loc).add(event.getTID());
					if (locIdinSchedUnit.get(loc) == null)
						locIdinSchedUnit
								.put(loc, new ArrayList<ScheduleUnit>());
					locIdinSchedUnit.get(loc).add(
							spec.schedule.get(spec.schedule.size() - 1));
				}
				if (Main.parameters.debug)
					System.out
							.println(spec.schedule.get(spec.schedule.size() - 1).count
									+ ": "
									+ event
									+ " "
									+ metaData.getStmtSig(event.getLocId())
									+ "\t"
									+ eventItem.ADDRL + " " + eventItem.ADDRR);
				// TODO: schedule, thread creation order
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new ReplayMOPException("error in parsing log file");
		}
		generateSpec();
		
		if (Main.parameters.debug){
			System.out.println("---");
			System.out.println(spec);
		}
	}

	void generateSpec() {
		for (Map.Entry e : locIdThreadAccessSet.entrySet()) {
			int loc = (int) e.getKey();
			if (((Set) e.getValue()).size() > 1) {
				String varSig = metaData.getVarSig(loc);
				if (Main.parameters.debug)
					System.out.println(varSig);
				addSharedVariable(varSig);
			} else {
				// when realized that some variable is not shared, we do not
				// instrument access to it, so we should remove the
				// corresponding event from thread schedule
				for (ScheduleUnit unit : locIdinSchedUnit.get(loc))
					unit.count--;
			}
		}
		Map<Long, Long> logThreadToRealThread = new HashMap<Long, Long>();
		logThreadToRealThread.put(1L, 1L);
		Long realThread = FIRST_THREAD_ID;
		for (Long logThread : threads) {
			assert realThread != FIRST_THREAD_ID || logThread == 1L;
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

	private void addSharedVariable(String varSig) {
		//
		// spec.shared.add(new Variable("*", varSig.replace("/", ".")
		// .replace("$", "")));

		Variable var = new Variable();
		int dotIndex = varSig.lastIndexOf('.');
		if (dotIndex == -1)
			dotIndex = 0;
		String varName = varSig.substring(dotIndex + 1);
		varSig = varSig.replace("$", ".");
		varSig = varSig.replace("/", ".");
		if (varName.startsWith("$")) {
			assert dotIndex != 0;
			if (varName.equals(MOCK_STATE_FIELD)) {
				var.type = varSig.substring(0, dotIndex);
				var.name = varName;
			} else {
				System.err.println("unsupported mock variable" + varName);
			}
		} else {
			var.type = "*";
			var.name = varSig;
		}

		spec.shared.add(var);

	}

	static class Parameters {
		@Parameter(description = "Trace folder")
		public List<String> inputFolder;

		@Parameter(names = "-output", description = "Output directory")
		public String outputFolder;
	}

	static public void main(String[] args) throws IOException {

		Parameters parameters = new Parameters();
		JCommander parameterParser;
		try {
			parameterParser = new JCommander(parameters, args);
		} catch (ParameterException pe) {
			System.err.println(pe.getMessage());
			return;
		}
		if (parameters.inputFolder == null
				|| !(new File(parameters.inputFolder.get(0))).isDirectory()) {
			parameterParser.usage();
			System.exit(1);
		}

		if (parameters.outputFolder == null)
			parameters.outputFolder = parameters.inputFolder.get(0);

		File inputFolder = new File(parameters.inputFolder.get(0));

		Parser parser = new RVPredictLogParser(inputFolder);
		ReplaySpecification spec = parser.parse();

		File outputFile = new File(inputFolder + File.separator + spec.fileName
				+ ".rs");

		FileWriter out = new FileWriter(outputFile);
		out.write(spec.toString());

		out.close();

	}

}
