import java.util.concurrent.locks;

public aspect %NAME% {


	//===========================thread creation begin===========================
	
	final Lock  threadCreationLock = new ReentrantLock();
	final Condition threadCreated = threadCreationLock.newCondition();
		
	final long[] threadOrder = %THREAD_CREATION_ORDER%;
	int threadOrderIndex = 0;
		
	before(): call(java.lang.Thread.new(*)){
		threadCreationLock.lock();
		while (threadOrderIndex < threadOrder.length &&
			 threadOrder[threadOrderIndex] != Thread.currentThread().getId()){
			try{
				threadCreated.await()
			}catch (InterruptedException e){
		
			}
		}
	}
		
	after(): call(java.lang.Thread.new(*)){
		threadOrderIndex++
		headMatched.signalAll()
		threadCreationLock.unlock();
	}
	
	//===========================thread creation end===========================
	
	//===========================sync pointcut begin===========================
	
	
	beforeSync(): %BEFORE_SYNC_POINTCUTS% ;
	
	afterSync(): %AFTER_SYNC_POINTCUTS% ;
	
	before(): beforeSync() &&  !cflow(adviceexecution()){
		enforceSchedule();
	}
	
	after(): afterPointcuts() &7 !cflow(adviceexecution()){
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
				if (schedule_count[threadScheduleIndex]){
					threadScheduleIndex++;
					this.notifyAll();
				}
			} 	
		}
	}
	
	//===========================sched enforce end===========================



}