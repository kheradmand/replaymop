
threads: 1 , 10 ;

thread_creation_order: 1  ;

shared: 
	int Example0.x , 
	Object Example0.lock ; 
		
before_sync: 
	default
	;
	
after_sync:
	default
	;
	
schedule: 1 x 2 , 10 x 5 , 1 x 4 , 10 x 1 ;