package replaymop.parser.rs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReplaySpecification {
	public String fileName;
	public Set<Long> threads;
	public List<Long> threadOrder;
	public boolean allShared = false;
	public Set<Variable> shared;
	public Set<String> beforeSync;
	public boolean beforeMonitorEnter = false;
	public Set<String> afterSync;
	public boolean afterMonitorExit = false;
	public List<ScheduleUnit> schedule;
	
	public static final String[] beforeSyncDefault = {
		"void java.lang.Thread.join()",
	};
	
	public static final String[] afterSyncDefault = {
		
	};
	
	
	
	public ReplaySpecification() {
		threads = new HashSet<Long>();
		threadOrder = new ArrayList<Long>();
		shared = new HashSet<ReplaySpecification.Variable>();
		beforeSync = new HashSet<String>();
		afterSync = new HashSet<String>();
		schedule = new ArrayList<ReplaySpecification.ScheduleUnit>();
	}
	
	public void addBeforeSyncDefault(){
		beforeMonitorEnter = true;
		beforeSync.addAll(Arrays.asList(beforeSyncDefault));
	}
	
	public void addAfterSyncDefault(){
		afterMonitorExit = true;
		afterSync.addAll(Arrays.asList(afterSyncDefault));
		
	}
	
	public static class Variable{
		public String type;
		public String name;
	}
	
	public static class ScheduleUnit{
		public Long thread;
		public int count;
	}
	
}
