package replaymop;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;


public aspect %NAME% {
	
	pointcut entryPoint() : execution(public static void main(String[]));

	//===========================thread creation begin===========================
	
	final Lock  threadCreationLock = new ReentrantLock();
	final Condition threadCreated = threadCreationLock.newCondition();
		
	final long[] threadOrder = %THREAD_CREATION_ORDER%;
	int threadOrderIndex = 0;
	
	pointcut threadCreation(): call(java.lang.Thread+.new(..));
		
	before(): threadCreation() {
		threadCreationLock.lock();
		while (threadOrderIndex < threadOrder.length &&
			 threadOrder[threadOrderIndex] != Thread.currentThread().getId()){
			try{
				threadCreated.await();
			}catch (InterruptedException e){
		
			}
		}
	}
		
	after(): threadCreation() {
		threadOrderIndex++;
		%DEBUG_BEGIN%
		if (threadOrderIndex == threadOrder.length)
			System.out.println("thread creation order enforcement completed, no enforcement anymore");
		%DEBUG_END%
		threadCreated.signalAll();
		threadCreationLock.unlock();
	}
	
	//===========================thread creation end===========================
	
	//=============================shared var begin============================
	
	//pointcut sharedVarGet():  %SHARED_VAR_GET%
	
	//pointcut sharedVarSet():  %SHARED_VAR_SET%
	
	pointcut sharedVarAccess(): %SHARED_VAR_ACCESS%;
	//sharedVarGet() || sharedVarSet() 
	
	//==============================shared var end=============================
	
	//===========================sync pointcut begin===========================
	
	
	pointcut beforeSync(): %BEFORE_SYNC_POINTCUTS% 
			sharedVarAccess() ;
	
	%DISABLE_AFTER_SYNC%pointcut afterSync(): %AFTER_SYNC_POINTCUTS% ;
	
	before(): beforeSync() &&  !cflow(adviceexecution()){
		enforceSchedule();
	}
	
	%DISABLE_AFTER_SYNC%after(): afterSync() && !cflow(adviceexecution()){
	%DISABLE_AFTER_SYNC%	cd enforceSchedule();
	%DISABLE_AFTER_SYNC%}
	
	//===========================sync pointcut end===========================
	
	//==========================sched enforce begin==========================
	
	final Object threadScheduleLock = new Object();
	
	final long[] schedule_thread = %SCHEDULE_THERAD%;
	int[] schedule_count = %SCHEDULE_COUNT%;
	int threadScheduleIndex = 0;
	
	void enforceSchedule(){
		synchronized(threadScheduleLock){ 
			long id = Thread.currentThread().getId();
			while (threadScheduleIndex < schedule_thread.length && schedule_thread[threadScheduleIndex] != id){
				try{
					threadScheduleLock.wait();
				}catch (InterruptedException e){
				}
			}
			if (threadScheduleIndex < schedule_thread.length){
				if (schedule_count[threadScheduleIndex] > 0){
					%DEBUG_BEGIN% System.out.println("-- " + id); %DEBUG_END%
					schedule_count[threadScheduleIndex]--;
				}else{
					%DEBUG_BEGIN% System.out.println("0- " + id); %DEBUG_END%
					incrementSchedIndexAndNotifyAll();
					//enforce context switch right before next sync event:
					enforceSchedule();
					//note that this function will be called recursively only once (when sched_count = 0)
				}
			} 	
		}
	}
	
	/* if schedule_count for current thread reaches 0, enforcer does not notify 
	other threads until the next sync event in the current thread happens. 
	if the user also includes thread termination as one sync event in the 
	schedule_count, this will result in unlimited wait of other threads.
	the following advice takes care of this situation. 
	note that this advice should be after the sync point cut advice. 
	TODO: capture all possible terminations of a thread. is this enough?
	TODO: UPDATE: well it seems that such mechanism is required regardless of counting thread and as sync event or not*/
	after(): execution(* Thread+.run()) || entryPoint(){
		synchronized(threadScheduleLock){ 
			long id = Thread.currentThread().getId();
			if (threadScheduleIndex < schedule_thread.length && schedule_thread[threadScheduleIndex] == id){
				if (schedule_count[threadScheduleIndex] == 0){
					%DEBUG_BEGIN%
					System.out.println("0e " + id);
					%DEBUG_END%
					incrementSchedIndexAndNotifyAll();
				}
				//TODO: what if > 0
			} 	
		}	
	
	}
	
	private void incrementSchedIndexAndNotifyAll(){
		threadScheduleIndex++;
		%DEBUG_BEGIN%
		if (threadScheduleIndex == schedule_thread.length)
			System.out.println("thread schedule order enforcement completed, no enforcement anymore");
		%DEBUG_END%
		threadScheduleLock.notifyAll();
	}
	
	//===========================sched enforce end===========================

	//============================debug info begin===========================
	
	%DEBUG_BEGIN%
	//before(): beforeSync() && !cflow(adviceexecution()) {
	//	printThread();
	//}
	
	//after(): afterSync() && !cflow(adviceexecution()){
	//	printThread();
	//}
	
	//void printThread(){
	//	System.out.printf("%d sync event\n", Thread.currentThread().getId());
	//}
	%DEBUG_END%
	
	
	//============================debug info begin===========================

}