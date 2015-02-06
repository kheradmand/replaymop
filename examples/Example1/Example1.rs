
threads: 1, 10, 11;

thread_creation_order: 1, 1;

shared: 
	int Example1.x, 
	Object Example1.lock;
		
before_sync: 
	defualt
	;
	
after_sync:
	defualt
	;
	
schedule: 1x7, 10x5, 1x1, 10x1;

