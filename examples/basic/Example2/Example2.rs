
threads: 1 , 10 , 11 ;

thread_creation_order: 1 , 1 ;

shared:
 	java.io.PrintStream java.lang.System.out 
;		
		
		
before_sync: 
	default
	;
	
after_sync:
	default
	;
	
schedule: 11 x 1 , 10 x 1 ;

