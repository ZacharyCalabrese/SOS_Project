import java.util.*;

public class osCpy{
    static int currentTime;
    
    // How many jobs are currently in the job table?
    static int numberOfJobs;
	static TreeMap jobTable;
	static TreeMap FST;
	// What jobs are ready to run?
	static List<JobLink> readyQueue = new ArrayList<JobLink>();
	static int jobOrder;
	static boolean isThereAJobToRun;
	static int roundRobinSlice;
	static int currentBaseAddress;
	static int currentSize;
	static int currentTimeSlice;

	public static void startup(){
		currentTime = 0;
		jobOrder = 0;
		numberOfJobs = 0;
		isThereAJobToRun = false;
		roundRobinSlice = 4;
		currentTimeSlice = roundRobinSlice;
		jobTable = new TreeMap();
		// Create a tree map for our FST, and initially set to one large value at address 0 of size 99
		
	
		/**
		 * | Size   |  Address |
		 * --------------------
		 * |  99    |    0     |
		 * -------------------- 
		 */
		FST = new TreeMap();
		FST.put(99, 0);
		
		sos.ontrace();
	}

	/**
	 * When a job comes into the system, check if we have enough room to put it in our
	 * job table.  If we do, add it to the table and increase the count of how many jobs we have.
	 * Then, run the memory manager and see if we can put the job in memory.
	 * @param a
	 * @param p
	 */
	@SuppressWarnings("unchecked")
	public static void Crint(int a[], int p[]){
		JobLink nextJob = new JobLink(p[1], p[2], p[3], p[4], p[5], false, false);
		
		currentTime = p[5];
		if(numberOfJobs < 50){
			jobTable.put(p[1], nextJob);
			numberOfJobs++;
		}
		
		nextJob.JobPrintStatus();
		MemoryManager(a, p, 0, false);
	}

	/**
	 * 
	 * @param a
	 * @param p
	 */
	public static void Svc(int a[],int[]p){
		JobLink job = (JobLink) jobTable.get(p[1]);
		currentTime = p[5];
		
		TimeManager(a, p);
		
		// Terminate
		if(a[0] == 5){
			MemoryManager(a, p, p[1], true);
		}
		
		// The job is requesting another disk I/O operating
		if(a[0] == 6){
			if(!job.doingIO){
				sos.siodisk(p[1]);
				job.latched = true;
				isThereAJobToRun = false;
			}
			job.doingIO = true;
			
			readyQueue.add(job);
			CPUScheduler(a,p);
		}
		if(a[0] == 7){
			job.Blocked = true;
			job.doingIO = true;
			a[0] = 1;
			
			if(job.latched != true){
				sos.siodrum(job.JobNumber, job.JobSize, job.address, 1);
				MemoryManager(a, p, p[1], true);
				job.inCore = false;
			}
			
			if(!job.doingIO){
		    	sos.siodisk(p[1]);
		    	isThereAJobToRun = false;
			}
			
			if(job.Blocked)
				readyQueue.remove(job);
			else
				readyQueue.add(job);
			
			CPUScheduler(a,p);
			System.out.println(numberOfJobs);
		}
	}

	public static void Tro(int a[],int[]p){
		JobLink job = (JobLink) jobTable.get(p[1]);
		TimeManager(a, p);
		currentTime = p[5];
		
		//Timer-Run-Out
		if(p[4] == job.CPUTimeUsed){
			readyQueue.remove(job);
			MemoryManager(a, p, p[1], true);
			// Remove program from memory
		}else{
			readyQueue.add(job);
			CPUScheduler(a,p);
			// Put program back on ready queue
		}
	}

	public static void Dskint(int a[],int[]p){
		//Disk interrupt
		System.out.println("DIO");
		JobLink job = (JobLink) jobTable.get(p[1]);
		
		job.latched = false;
		job.doingIO = false;
		currentTime = p[5];
		a[0] = 2;
		readyQueue.add(job);
		CPUScheduler(a,p);
	}

	public static void Drmint(int a[],int[]p){
		//Drum interrupt
		JobLink job = (JobLink) jobTable.get(p[1]);
		
		if(!job.inCore){
			job.inCore = true;
			a[0] = 1;
			MemoryManager(a, p, 0, false);
		}else{
			job.Blocked = false;
			readyQueue.add(job);
			currentTime = p[5];
			CPUScheduler(a,p);	
		}		
	}

	@SuppressWarnings("unchecked")
	public static void MemoryManager(int a[], int []p, int jobNumber, boolean terminate){
		int address = 0;
		int size = 0;
		int tmpSize;
		
		printFST();
		
		JobLink lastJobIn = (JobLink) jobTable.get(jobTable.lastKey());
		JobLink toTerminate = (JobLink) jobTable.get(jobNumber);
		
		/**
		 * Check to see if the memory manager should make room for a new job,
		 * or add a job in memory
		 */
		if(terminate){
			size = toTerminate.JobSize;
			address = toTerminate.address;
			
			FST.put(size, address);
			//jobTable.remove(jobNumber);
			a[0] = 1;
		}else{
			/**
			 * Check to see if the size of the last added job
			 * is less than the largest size available, if 
			 * it is, take the space away from the FST, and
	         * get the job ready to run 
			*/
			
			// Check the size of the current job versus the largest size available
			if ((int)lastJobIn.JobSize <= (int)FST.lastKey()){
				// Get the address that corresponds with the appropriate size in our FST
				address = (int)FST.get(FST.lastKey()); 
				// Get the size that corresponds with the appropriate address in our FST
				size = (int)FST.lastKey();
				
				// Remove the entry from our FST
				FST.remove(FST.lastKey());
				
				// Put the new entry in the FST
				FST.put(size - lastJobIn.JobSize, address + lastJobIn.JobSize);
				
				// Put the job in main memory
				sos.siodrum(lastJobIn.JobNumber, lastJobIn.JobSize, address, 0);
				
				lastJobIn.inCore = true;
				lastJobIn.address = address;
				
				// Put the job on the ready cue indicating that it can run
				readyQueue.add(lastJobIn);
			}
		}
		
		printFST();
	}

	public static void CPUScheduler(int a[], int p[]){
		JobLink nextJob;
		if(!readyQueue.isEmpty()){
			nextJob = readyQueue.get(0);
			nextJob.lastTimeProcessing = p[5];
			currentBaseAddress = nextJob.address; 
			currentSize = nextJob.JobSize;
			isThereAJobToRun = true;
			
			readyQueue.remove(0);
			Dispatcher(a,p);
		}
	}

	public static void TimeManager(int a[], int p[]){
		JobLink job = (JobLink) jobTable.get(p[1]);
		currentTime = p[5];
		
		job.CPUTimeUsed = job.lastTimeProcessing - currentTime;
		job.CPUTimeLeft = job.MaxCPU - job.CPUTimeUsed;

	}

	/**
	 * Set the appropriate values for p and a
	 * @param a
	 * @param p
	 */
	public static void Dispatcher(int a[], int p[]){
		JobLink job = (JobLink) jobTable.get(p[1]);
		
		if(isThereAJobToRun){
			job.lastTimeProcessing = p[5];
			a[0] = 2;
			p[2] = currentBaseAddress; // Base address of job to run
			p[3] = currentSize; // The size of the job to run
			p[4] = currentTimeSlice; // The time slice for the current job
		}else{
			a[0] = 1;
		}
	}

	public void Swapper(){
		
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
}
