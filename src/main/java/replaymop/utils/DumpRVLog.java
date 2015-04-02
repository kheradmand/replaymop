package replaymop.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
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

public class DumpRVLog {

	OfflineLoggingFactory metaData;
	TraceCache trace;

	boolean importantOnly = false;
	
	public DumpRVLog(File logFolder, boolean importantOnly) {
		Configuration config = new Configuration();
		config.outdir = logFolder.toString();
		metaData = new OfflineLoggingFactory(config);
		trace = new TraceCache(metaData);
		this.importantOnly = importantOnly;
	}

	@Override
	public String toString() {
		String ret = "";
		for (int i = 1; metaData.getVarSig(i) != null; i++){
			ret += (String.format("%d: %s", i, metaData.getVarSig(i)));
			ret += "\n";
		}
		ret += ("--");
		ret += "\n";

		try {
			EventItem eventItem;
			for (int index = 1; ((eventItem = trace.getNextEvent()) != null); index++) {
				Event event = EventUtils.of(eventItem);
				EventType eventType = event.getType();

				if (!important(eventType) && importantOnly)
					continue;

				ret += index + "\t" + event + " " + metaData.getStmtSig(event.getLocId())
						+ "\t" + eventItem.ADDRL + " " + eventItem.ADDRR;
				ret += "\n";
				// TODO: schedule, thread creation order
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
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

	

	static class Parameters {
		@Parameter(description = "Trace folder")
		public List<String> inputFolder;

		@Parameter(names = "-output", description = "Output file (standard output if not specified)")
		public String outputFile;
		
		@Parameter(names = "-important-only", description = "Dump important events only")
		public boolean importantOnly = false;
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

		PrintStream out = System.out;
		if (parameters.outputFile != null)
			out = new PrintStream(new File(parameters.outputFile));

		File inputFolder = new File(parameters.inputFolder.get(0));

		DumpRVLog dump = new DumpRVLog(inputFolder, parameters.importantOnly);
		
		out.println(dump);

	}

}
