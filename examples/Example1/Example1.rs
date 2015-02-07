
threads: 1 , 10 , 11 ;

thread_creation_order: 1 , 1 ;

shared: 
	int Example1.x , 
	Object Example1.lock ; 
		
before_sync: 
	default
	;
	
after_sync:
	default
	;
	
schedule: 1 x 7 , 10 x 5 , 1 x 2 , 10 x 2 ;

