package replaymop.parser.rs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReplaySpecification {
	
	public Set<Long> threads;
	public List<Long> threadOrder;
	public boolean allShared = false;
	public Set<Variable> shared;
	public Set<String> beforeSync;
	public boolean beforeMonitorEnter = false;
	public Set<String> afterSync;
	public boolean afterMonitorEnter = false;
	public List<ScheduleUnit> schedule;
	
	public ReplaySpecification() {
		threads = new HashSet<Long>();
		threadOrder = new ArrayList<Long>();
		shared = new HashSet<ReplaySpecification.Variable>();
		beforeSync = new HashSet<String>();
		afterSync = new HashSet<String>();
		schedule = new ArrayList<ReplaySpecification.ScheduleUnit>();
	}
	
	public void addBeforeSyncDefualt(){
		
	}
	
	public void addAfterSyncDefualt(){
		
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
