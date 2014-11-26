import java.util.*;

public class os {
	static int currentTime;
	static int jobsInJobTable;
	static int roundRobinSlice;
	static int currentBaseAddress;
	static int currentSize;
	static int currentTimeSlice;
	static int lastJobProcessed;
	
	static JobLink currentJob;
	static JobLink lastJob;
	static List<JobLink> readyQueue = new ArrayList<JobLink>();
	static TreeMap jobTable;
	static TreeMap FST;
	
	public static void startup(){
		currentTime = 0;
		jobsInJobTable = 0;
		roundRobinSlice = 4;
		
		lastJobProcessed = 0;
		
		jobTable = new TreeMap();
		FST = new TreeMap();
		FST.put(99, 0);
		
		sos.offtrace();
	}

	/**
	  *	This function is called when a new job enters the system.
	  * Check to see if there are jobs in the system, if there is,
	  * save the current status of the last running program.  Then
	  * put the new job into the job table and see if there is room in memory
	  */
	public static void Crint(int a[], int p[]){
		currentTime = p[5];
		setCurrentJob(p);
		
		/*
		 * If not the first job into the system, the previously running job
		 * should be placed back on the ready queue
		 */
		if(lastJobProcessed != 0){
			// Update job table of last job
			if(jobTable.containsKey(lastJobProcessed))
				if(lastJob.inCore)
					if(!lastJob.Blocked){
						System.out.println("here");
						readyQueue.add(lastJob);
					}
		}
		
		jobsInJobTable++;
		
		/*
		 * If we have room, put the job in our job table
		 */
		if(jobsInJobTable < 50){
			JobLink nextJob = new JobLink(p[1], p[2], p[3], p[4], p[5], false, false);
			jobTable.put(nextJob.JobNumber, nextJob);

			/*
			 * Place job in memory
             */
			MemoryManager(a, p, 0);
		}

		printJobTable();
	}
	
	public static void Svc(int a[],int[]p){
		currentTime = p[5];
		
		setCurrentJob(p);
		TimeManager(a, p);
		
		if(a[0] == 5){
			// Terminate job
			MemoryManager(a, p, 1);
		}else if(a[0] == 6){
			Swapper(a, p, 1);
		}else if(a[0] == 7){
			Swapper(a, p, 2);
		}
		
		currentJob.JobPrintStatus();
	}
	
	public static void Tro(int a[],int[]p){
		currentTime = p[5];
		
		setCurrentJob(p);		
		TimeManager(a, p);
		
		//Timer-Run-Out
		if(currentJob.CPUTimeUsed >= currentJob.MaxCPU){
			MemoryManager(a, p, 1);
			// Remove program from memory
		}else{
			readyQueue.add(currentJob);
			CPUScheduler(a,p);
			// Put program back on ready queue
		}
	}
	
	/*
	 * Job is moved from drum to main memory, ready to process
	 */
	public static void Drmint(int a[], int p[]){
		setCurrentJob(p);
		currentTime = p[5];
		currentJob.inCore = true;
		
		readyQueue.add(currentJob);
		CPUScheduler(a,p);
	}
	
	public static void Dskint(int a[], int p[]){
		currentTime = p[5];
		
		setCurrentJob(p);		
		//TimeManager(a, p);
		if(!currentJob.inCore){
			currentJob = lastJob;
		}
		
		currentJob.Blocked = false;
		currentJob.ioCompleted++;
		currentJob.ioLeft--;
		currentJob.JobPrintStatus();
		
		if(currentJob.ioLeft == 0){
			readyQueue.add(currentJob);
			a[0] = 2;
			CPUScheduler(a,p);
		}
	}
	
	public static void TimeManager(int a[], int p[]){
		lastJob.CPUTimeUsed += currentTime - lastJob.lastTimeProcessing;
		lastJob.CPUTimeLeft = lastJob.MaxCPU - lastJob.CPUTimeUsed;
		
		//currentJob.JobPrintStatus();
		//lastJob.JobPrintStatus();
	}
	
	public static void setCurrentJob(int p[]){
		currentJob = (JobLink) jobTable.get(p[1]);
		lastJob = (JobLink) jobTable.get(lastJobProcessed);
	}
	
	public static void Dispatcher(int a[], int p[]){
		lastJobProcessed = currentJob.JobNumber;
		setCurrentJob(p);
		lastJob.lastTimeProcessing = currentTime;
		
		if(currentJob.CPUTimeLeft > roundRobinSlice){
			a[0] = 2;
			p[2] = currentBaseAddress;
			p[3] = currentSize;
			p[4] = roundRobinSlice;	
		}else{
			a[0] = 2;
			p[2] = currentBaseAddress;
			p[3] = currentSize;
			p[4] = currentJob.CPUTimeLeft;
		}
		
	}
	
	public static void CPUScheduler(int a[], int p[]){
		currentTime = p[5];
		if(!readyQueue.isEmpty()){
			currentJob = readyQueue.get(0);
			
			currentBaseAddress = currentJob.address;
			currentSize = currentJob.JobSize;
			readyQueue.remove(0);
			Dispatcher(a, p);
		}else{
			//a[0] = 1;
		}
	}
	
	public static void Swapper(int a[], int p[], int function){
		// Function( 0 = place in main memory, 1 = do I/O )
		if(function == 0){
			//a[0] = 1;
			currentJob.address = currentBaseAddress;
			
			sos.siodrum(currentJob.JobNumber, currentJob.JobSize, currentBaseAddress, 0);
			if(lastJobProcessed != 0){
				// Update job table of last job
				if(jobTable.containsKey(lastJobProcessed))
					if(lastJob.inCore)
						if(!lastJob.Blocked){
							System.out.println("here");
							readyQueue.add(lastJob);
							CPUScheduler(a,p);
						}
			}
		}else if(function == 1){
			currentJob.doingIO = true;
			currentJob.ioLeft++;
			sos.siodisk(currentJob.JobNumber);

			readyQueue.add(currentJob);
			CPUScheduler(a,p);
		}else if(function == 2){
			//currentJob.ioLeft++;
			a[0] = 1;
			currentJob.Blocked = true;
			if(!currentJob.doingIO)
				sos.siodisk(currentJob.JobNumber);
			
			if(currentJob.ioLeft == 0){
				readyQueue.add(currentJob);
				CPUScheduler(a,p);
			}
		}
		
		
	}
	
	public static void MemoryManager(int a[], int p[], int function){
		// Function( 0 = place in main memory, 1 = remove from FST and JobTable)
		setCurrentJob(p);
		
		if(function == 0){
			if((int) currentJob.JobSize <= (int) FST.lastKey()){
				currentSize = (int) FST.lastKey();
				currentBaseAddress = (int)FST.get(FST.lastKey());
				
				FST.remove(FST.lastKey());
				FST.put(currentSize - currentJob.JobSize, currentBaseAddress + currentJob.JobSize);
				Swapper(a, p, 0);
				CPUScheduler(a,p);
			}else{
				currentJob = lastJob;
				lastJobProcessed = currentJob.JobNumber;
			}
		}else if(function == 1){
			FST.put(currentJob.JobSize, currentJob.address);
			jobTable.remove(currentJob.JobNumber);
			
			a[0] = 1;
			
			
			// Add code to combine contiguous pieces of memory
			CPUScheduler(a,p);
		}
		
		CPUScheduler(a,p);

	}	
	
     public static void printFST(){
	         // Get a set of the entries
	         Set set = FST.entrySet();
	         // Get an iterator
	         Iterator i = set.iterator();
	         // Display elements
	 
	         System.out.println();
	         System.out.println("FREE SPACE TABLE");
	         System.out.println("Size  || Address");
	 
	         while(i.hasNext()) {
	        	 Map.Entry me = (Map.Entry)i.next();
	             System.out.print(me.getKey() + "              ");
	             System.out.println(me.getValue());
	         }
     }

	public static void printJobTable(){
			 JobLink temp;
	         // Get a set of the entries
	         Set set = jobTable.entrySet();
	         // Get an iterator
	         Iterator i = set.iterator();
	         // Display elements
	 
	         System.out.println();
	         System.out.println("FREE SPACE TABLE");
	         System.out.println("Size  || Address");
	 
	         while(i.hasNext()) {
	        	 Map.Entry me = (Map.Entry)i.next();
	             System.out.print(me.getKey() + "              ");
				 temp = (JobLink) me.getValue();
				 temp.JobPrintStatus();
	         }


    }
}
