package replaymop.replayspecification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReplaySpecification {
	public String fileName;
	public Set<Long> threads;
	public List<Long> threadOrder;
	public boolean allShared = false;
	public Set<Variable> shared;
	public Set<String> beforeSync;
	public boolean beforeMonitorEnter = false;
	public boolean beforeMonitorExit = false;
	public boolean beforeThreadEnd = false;
	public Set<String> afterSync;
	public boolean afterMonitorExit = false;
	public boolean afterThreadBegin = false;
	public List<ScheduleUnit> schedule;
	public String input;
	
	public static final String[] beforeSyncDefault = {
		"void java.lang.Thread.join()",
		"void java.lang.Object.wait(..)",
		"void java.lang.Object.notify()",
		"void java.lang.Object.notifyAll(..)",
	};
	
	public static final String[] afterSyncDefault = {
	
	};
	
	
	
	public ReplaySpecification() {
		threads = new HashSet<Long>();
		threadOrder = new ArrayList<Long>();
		shared = new HashSet<Variable>();
		beforeSync = new HashSet<String>();
		afterSync = new HashSet<String>();
		schedule = new ArrayList<ScheduleUnit>();
	}
	
	public void addBeforeSyncDefault(){
		beforeMonitorEnter = true;
		beforeMonitorExit = true;
		beforeThreadEnd = true;
		beforeSync.addAll(Arrays.asList(beforeSyncDefault));
	}
	
	public void addAfterSyncDefault(){
		//afterMonitorExit = true;
		//afterThreadBegin = true;
		afterSync.addAll(Arrays.asList(afterSyncDefault));
		
	}
	
	@Override
	public String toString() {
		String ret = "";
		if (!threads.isEmpty())
			ret += "threads: " + joined(threads, " , ") + " ;\n\n";
		
		if (!threadOrder.isEmpty())
			ret+= "thread_creation_order: " + joined(threadOrder, " , ") + " ;\n\n";
		
		if (!shared.isEmpty())
			ret += "shared:\n" + joined(shared, " ,\n") + "\n;\n\n";
		
		if (!schedule.isEmpty())
			ret += "schedule:\n" + joined(schedule, " , ") + " ;\n\n";
		
		//TODO: also print after_sync and before_sync
		
		return ret;
	}
	
	
	<T> String joined(Collection<T> collection, String delim){
		return collection.stream().map(i -> i.toString()).collect(Collectors.joining(delim));
	}
	
	

	
}
