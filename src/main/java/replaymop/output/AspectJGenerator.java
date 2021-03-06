package replaymop.output;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import replaymop.Parameters;
import replaymop.output.aspectj.Aspect;
import replaymop.parser.RVPredictLogParser;
import replaymop.replayspecification.ReplaySpecification;
import replaymop.replayspecification.ScheduleUnit;

public class AspectJGenerator {
	ReplaySpecification spec;
	Aspect aspect;
	Parameters params;

	private AspectJGenerator(ReplaySpecification spec, Parameters params) {
		this.spec = spec;
		this.params = params;
		aspect = new Aspect(spec.fileName + "Aspect");
	}

	static <T> String printList(List<T> list) {
		return list.toString().replace("[", "{").replace("]", "}");
	}

	void generateThreadCreationOrder() {
		aspect.setParameter("THREAD_CREATION_ORDER",
				printList(spec.threadOrder));
	}

	String mockVariablePointcuts = "";

	void generateShareVariableAccessPointCut() {
		StringJoiner pointcuts = new StringJoiner(" ||\n\t\t\t");
		for (replaymop.replayspecification.Variable var : spec.shared) {
			if (var.type.equals("array")) {
				aspect.setParameter("ARRAY_POINTCUT_BEGIN", "");
				aspect.setParameter("ARRAY_POINTCUT_END", "");
				pointcuts.add("array_access()");
			} else if (var.name.equals(RVPredictLogParser.MOCK_STATE_FIELD)) {
				pointcuts.add(handleMockVariable(var));
			} else {
				pointcuts.add(String.format("set(%s %s)", var.type, var.name));
				pointcuts.add(String.format("get(%s %s)", var.type, var.name));
			}
		}
		aspect.setParameter("SHARED_VAR_ACCESS", pointcuts.toString());
		aspect.setParameter("COLLECTION_POINTCUT_BEGIN", "/*");
		aspect.setParameter("COLLECTION_POINTCUT_END", "*/");
		aspect.setParameter("MAP_POINTCUT_BEGIN", "/*");
		aspect.setParameter("MAP_POINTCUT_END", "*/");
		aspect.setParameter("ARRAY_POINTCUT_BEGIN", "/*");
		aspect.setParameter("ARRAY_POINTCUT_END", "*/");
	}

	String handleMockVariable(replaymop.replayspecification.Variable var) {
		try {
			Class<?> typeClass = Class.forName(var.type);
			if (Collection.class.isAssignableFrom(typeClass)) {
				aspect.setParameter("COLLECTION_POINTCUT_BEGIN", "");
				aspect.setParameter("COLLECTION_POINTCUT_END", "");
				return String.format("(collection_access() && target(%s))", var.type);

			}else if (Map.class.isAssignableFrom(typeClass)){
				aspect.setParameter("MAP_POINTCUT_BEGIN", "");
				aspect.setParameter("MAP_POINTCUT_END", "");
				return String.format("(map_access() && target(%s))", var.type);
			}else {
				System.err.println("mocked type is not supported " + typeClass);
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	void generateBeforeSyncPointCut() {
		StringJoiner pointcuts = new StringJoiner(" ||\n\t\t\t");
		if (spec.beforeMonitorEnter)
			pointcuts.add("lock()");
		if (spec.beforeMonitorExit)
			pointcuts.add("unlock()");
		if (spec.afterThreadBegin) { // trick: before run execution = after
										// thread begin
			pointcuts.add("execution(* Thread+.run())");
			pointcuts.add("entryPoint()"); // to handle the main thread
		}
		for (String sync : spec.beforeSync) {
			pointcuts.add(String.format("call(%s)", sync));
		}
		aspect.setParameter("BEFORE_SYNC_POINTCUTS", pointcuts.toString()
				+ (pointcuts.length() == 0 ? "" : " ||"));
	}

	void generateAfterSyncPointCut() {
		StringJoiner pointcuts = new StringJoiner(" ||\n\t\t\t");
		if (spec.afterMonitorExit)
			pointcuts.add("unlock()");
		if (spec.beforeThreadEnd) { // trick: after run execution = before
									// thread end
			pointcuts.add("execution(* Thread+.run())");
			pointcuts.add("entryPoint()"); // to handle the main thread
		}
		for (String sync : spec.afterSync) {
			pointcuts.add(String.format("call(%s)", sync));
		}
		if (pointcuts.length() > 0) {
			aspect.setParameter("AFTER_SYNC_POINTCUTS", pointcuts.toString());
			aspect.setParameter("DISABLE_AFTER_SYNC", "");
		} else
			aspect.setParameter("DISABLE_AFTER_SYNC", "//");
	}

	void generateThreadSchedule() {
		List<Long> threads = new ArrayList<>();
		List<Integer> counts = new ArrayList<>();
		for (ScheduleUnit unit : spec.schedule) {
			threads.add(unit.thread);
			counts.add(unit.count);
		}
		aspect.setParameter("SCHEDULE_THERAD", printList(threads));
		aspect.setParameter("SCHEDULE_COUNT", printList(counts));

	}

	void handleDebugInfo() {
		aspect.setParameter("DEBUG_BEGIN", params.debug_runtime ? "" : "/*");
		aspect.setParameter("DEBUG_END", params.debug_runtime ? "" : "*/");
	}

	void startGeneration() {
		generateThreadCreationOrder();
		// generateVariableLocks(); //TODO: complete this part
		generateShareVariableAccessPointCut();
		generateBeforeSyncPointCut();
		generateAfterSyncPointCut();
		generateThreadSchedule();
		handleDebugInfo();

	}

	public static Aspect generate(ReplaySpecification spec, Parameters params) {
		AspectJGenerator generator = new AspectJGenerator(spec, params);
		generator.startGeneration();
		return generator.aspect;
	}
}
