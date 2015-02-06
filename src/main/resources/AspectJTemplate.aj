import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;


public aspect %NAME% {


	//===========================thread creation begin===========================
	
	final Lock  threadCreationLock = new ReentrantLock();
	final Condition threadCreated = threadCreationLock.newCondition();
		
	final long[] threadOrder = %THREAD_CREATION_ORDER%;
	int threadOrderIndex = 0;
		
	before(): call(java.lang.Thread+.new(*)){
		threadCreationLock.lock();
		while (threadOrderIndex < threadOrder.length &&
			 threadOrder[threadOrderIndex] != Thread.currentThread().getId()){
			try{
				threadCreated.await();
			}catch (InterruptedException e){
		
			}
		}
	}
		
	after(): call(java.lang.Thread+.new(*)){
		threadOrderIndex++;
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
	
	pointcut afterSync(): %AFTER_SYNC_POINTCUTS% ;
	
	before(): beforeSync() &&  !cflow(adviceexecution()){
		enforceSchedule();
	}
	
	after(): afterSync() && !cflow(adviceexecution()){
		enforceSchedule();
	}
	
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
				schedule_count[threadScheduleIndex]--;
				if (schedule_count[threadScheduleIndex] == 0){
					threadScheduleIndex++;
					this.notifyAll();
				}
			} 	
		}
	}
	
	//===========================sched enforce end===========================



}