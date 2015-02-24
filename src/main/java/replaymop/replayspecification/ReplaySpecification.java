package replaymop.replayspecification;

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
	public boolean beforeThreadEnd = false;
	public Set<String> afterSync;
	public boolean afterMonitorExit = false;
	public boolean afterThreadBegin = false;
	public List<ScheduleUnit> schedule;
	public String input;
	
	public static final String[] beforeSyncDefault = {
		"void java.lang.Thread.join()",
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
		beforeThreadEnd = true;
		beforeSync.addAll(Arrays.asList(beforeSyncDefault));
	}
	
	public void addAfterSyncDefault(){
		afterMonitorExit = true;
		//afterThreadBegin = true;
		afterSync.addAll(Arrays.asList(afterSyncDefault));
		
	}
	

	
}
