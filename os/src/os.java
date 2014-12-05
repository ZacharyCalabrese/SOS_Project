import java.util.ArrayList;
import java.util.List;

public class os{
    private static jobTable JobTable;
    private static freeSpaceTable FreeSpaceTable;
    private static List<processControlBlock> readyQueue = new ArrayList<processControlBlock>();
    private static List<processControlBlock> drumQueue = new ArrayList<processControlBlock>();
    private static List<processControlBlock> ioQueue = new ArrayList<processControlBlock>();
    private static List<processControlBlock> longTermScheduler = new ArrayList<processControlBlock>();
    private static processControlBlock lastRunningJobPCB;
    private static processControlBlock currentWorkingJobPCB;
    private static processControlBlock lastJobToIo;
    private static int roundRobinSlice;
    private static int blockCount;
    private static boolean currentlyDoingIo, DoingSwap;
    
    /*
        INITIALIZE VARIABLES, TABLES, AND ROUND ROBIN SLICE
    */
    public static void startup(){
        JobTable = new jobTable(50);
        FreeSpaceTable = new freeSpaceTable(100);
        
        lastRunningJobPCB = null;
        currentWorkingJobPCB = null;
        lastJobToIo = null;
        currentlyDoingIo = false;
        DoingSwap = false;
        roundRobinSlice = 100;
        blockCount = 0;
        sos.ontrace();
    }
    
    /*
        Crint is called when a new job enters the system.
        
        Crint checks to see if a job was interrupted, if it
        was, calculate how much time was processed and place that
        job back on the ready queue.
        
        Then check to see if the job entering the system is already in our 
        job table.  That would mean that we moved it out of memory to make 
        space for something else, and we should move it back in.
        
        If that condition is false, create a PCB for the job entering
        the system, add the job to our job table, and try
        to find space in memory for our job
    */
    public static void Crint(int a[], int p[]){
        if(lastRunningJobPCB != null){
            lastRunningJobPCB.calculateTimeProcessed(p[5]);
            readyQueue.add(lastRunningJobPCB);
        }
        
        currentWorkingJobPCB = new processControlBlock(p[1], p[2], p[3], p[4], p[5]);
        JobTable.addJob(currentWorkingJobPCB);
        MemoryManager(currentWorkingJobPCB, 0);
        
        // Only one chance a job goes on the ready queue from this function
        cpuScheduler(a, p);
    }
    
    /*
        Svc is called when a job currently on the system wants some
        sort of service.  The three possibilities are service to 
        terminate (a[0] == 5), service to do I/O (a[0] == 6), and service to be blocked(a[0] == 7).
        
        At the start of this function we should calculate the time processed since we are being interrupted.
        The lastJob should be the same as the current job
    */
    public static void Svc(int a[], int p[]){
        lastRunningJobPCB.calculateTimeProcessed(p[5]);
        
        /* When a job requests to be terminated
          * - Remove from ready queue completely
          * - Remove from io queue completely
          * - set in core bit to false
          * - Add space from exiting job to free space table
          * - remove the job from the job table
          * - set the current working job = null
          * - set the last working job = null
        */
        if(a[0] == 5){
            /* If a job requests to terminate
              * - Check if there is IO left
                * - If IO left, remove from ready queue, check terminate bit
              * - If no IO left, remove from ready queue, IO queue, drum queue
                * - Set in core bit to 0, add space from job back to FST, remove job from jobTable
                * - Set last running job = null
            */
            if(lastRunningJobPCB.getIoCount() > 0){
                while(readyQueue.contains(lastRunningJobPCB))
                    readyQueue.remove(lastRunningJobPCB);
            
                lastRunningJobPCB.terminateJob();
            }else{
                while(readyQueue.contains(lastRunningJobPCB))
                    readyQueue.remove(lastRunningJobPCB);
                while(ioQueue.contains(lastRunningJobPCB))
                    ioQueue.remove(lastRunningJobPCB);                            
                while(drumQueue.contains(lastRunningJobPCB))
                    ioQueue.remove(lastRunningJobPCB);
                                                                                                                                                                  
                lastRunningJobPCB.removeInCore();
                FreeSpaceTable.addSpace(lastRunningJobPCB);
                JobTable.removeJob(lastRunningJobPCB);
                
                lastRunningJobPCB = null;                           
            }
        }
        /* When a job requests to do IO
          * - Increment the io count
          * - Add the job to the io queue
          * - Call IO manager
        */
        else if(a[0] == 6){
            lastRunningJobPCB.incrementIoCount();
            ioQueue.add(lastRunningJobPCB);
            ioManager();
        }
        /* When a job requests to be blocked
          * - If no io pending, no need to block place back on ready queue
          * - If there is io pending, set block bit to true, remove from ready queue
        */
        else if(a[0] == 7){
            if(lastRunningJobPCB.getIoCount() != 0){        
                lastRunningJobPCB.blockJob();
                blockCount++;
                if(blockCount > 5 && !lastRunningJobPCB.getLatchedStatus() && lastRunningJobPCB.getCpuTimeLeft() > 6000 && lastRunningJobPCB.getCpuTimeUsed() < 1000){
                    drumQueue.add(lastRunningJobPCB); 
                while(ioQueue.contains(lastRunningJobPCB))
                    ioQueue.remove(lastRunningJobPCB);                     
                }
                while(readyQueue.contains(lastRunningJobPCB))
                    readyQueue.remove(lastRunningJobPCB);
                    
            }else{
                readyQueue.add(lastRunningJobPCB);
            }
        }
        drumManager();
        cpuScheduler(a, p);
    }
    
    /*
        Tro is called when sos has a timer run out
        
        There are two possibilities
        The first possibility is that a job has exceeded its
        maximum CPU time.  In that case we should handle the job the same
        way we handle a service call 5.
        
        The second possibility is that a job has exceeded its time
        slice that we assigned to it.  In that case, just add the job
        back to the ready queue.
    */
    public static void Tro(int a[], int p[]){
        lastRunningJobPCB.calculateTimeProcessed(p[5]);

        /*  If the job exceeds its maximum time used:
          * - Check if there is IO left
            * - If IO left, remove from ready queue, check terminate bit
          * - If no IO left, remove from ready queue, IO queue, drum queue
            * - Set in core bit to 0, add space from job back to FST, remove job from jobTable
            * - Set last running job = null
            * - Run the MemoryManager function to attempt to put jobs from the long term scheduler into memory
        */
        if(currentWorkingJobPCB.getCpuTimeUsed() >= currentWorkingJobPCB.getMaxCpuTime()){
            if(lastRunningJobPCB.getIoCount() > 0){
                while(readyQueue.contains(lastRunningJobPCB))
                    readyQueue.remove(lastRunningJobPCB);
            
                lastRunningJobPCB.terminateJob();
            }else{
                while(readyQueue.contains(lastRunningJobPCB))
                    readyQueue.remove(lastRunningJobPCB);
                while(ioQueue.contains(lastRunningJobPCB))
                    ioQueue.remove(lastRunningJobPCB);                            
                while(drumQueue.contains(lastRunningJobPCB))
                    ioQueue.remove(lastRunningJobPCB);
                                                                                                                                                                  
                FreeSpaceTable.addSpace(lastRunningJobPCB);
                JobTable.removeJob(lastRunningJobPCB);
                
                lastRunningJobPCB = null;
                MemoryManager(currentWorkingJobPCB, 1);                           
            }        
        }
        /* If the job exceeds its time slice, place back on the ready drive
        */
        else{
            readyQueue.add(currentWorkingJobPCB);
        }

        cpuScheduler(a, p);
    }
    
    /*  
        Dskint is called when an job is done doing IO     
    */
    public static void Dskint(int a[], int p[]){
        JobTable.printJobTable();    
        currentlyDoingIo = false;
        
        /* Process last job
          * - If there was a job running
          * - Calculate the time the job spent processing
          * - and add the job to the ready queue
        */
        if(lastRunningJobPCB != null){
            lastRunningJobPCB.calculateTimeProcessed(p[5]);
            readyQueue.add(lastRunningJobPCB);
        }
        
        /* Check to see if the the last job doing IO is in the Job Table
          * - If it is in the job table
            * - Decrement the IO count in the PCB
            * - Set latch bit to false
            * - Remove the job from the IO queue if it was added unnecessarily
            * - If the job is done doing IO
                * - If the job should be terminated, terminate it
                * - If the job should NOT be terminated, set block bit to false and add to ready queue
        */
        if(JobTable.contains(lastJobToIo.getJobNumber())){            
            lastJobToIo.decrementIoCount();
            lastJobToIo.unlatchJob();
            while(ioQueue.contains(lastJobToIo))
                ioQueue.remove(lastJobToIo);            
            
            if(lastJobToIo.getIoCount() == 0){
                if(lastJobToIo.getTerminatedStatus()){
                    FreeSpaceTable.addSpace(lastJobToIo);
                    JobTable.removeJob(lastJobToIo);
                    
                    lastRunningJobPCB = null;                
                }else if(lastJobToIo.getBlockedStatus()){
                    lastJobToIo.unblockJob();
                    blockCount--;
                    readyQueue.add(lastJobToIo);                
                }else{
                    readyQueue.add(lastJobToIo);
                } 
            }
        }
        
        /* If there are jobs to run, call the IO Manager
        */
        if(!ioQueue.isEmpty()){
            ioManager();
        }
           
        cpuScheduler(a, p);
        JobTable.printJobTable();
    }
    
    /*
        Drmint is called when a job is finish working with the drum
    */
    public static void Drmint(int a[], int p[]){
        DoingSwap = false;
        
        /* Process last job
          * - If there was a job running
          * - Calculate the time the job spent processing
          * - and add the job to the ready queue
        */        
        if(lastRunningJobPCB != null){
            lastRunningJobPCB.calculateTimeProcessed(p[5]);
            readyQueue.add(lastRunningJobPCB);
        }
    
        /* Set the currently working job equal to the first job off the job queue and remove it from the queue
        */
        currentWorkingJobPCB = drumQueue.get(0);
        drumQueue.remove(0);
        
        /* If the job was not "incore", put it in core and on the ready queue
         * If the job was "incore" then the interrupt is because we want to move it out of core so set the incore bit to false
        */
        if(!currentWorkingJobPCB.getInCoreStatus()){
            currentWorkingJobPCB.putInCore();
            //if(currentWorkingJobPCB.getCpuTimeUsed() > 0)
            //    JobTable.addJob(currentWorkingJobPCB);
            readyQueue.add(currentWorkingJobPCB);
        }else{
                    currentWorkingJobPCB.removeInCore();
            FreeSpaceTable.addSpace(currentWorkingJobPCB);
            //JobTable.removeJob(currentWorkingJobPCB);
            longTermScheduler.add(currentWorkingJobPCB);
            while(readyQueue.contains(currentWorkingJobPCB))
                readyQueue.remove(currentWorkingJobPCB);
            while(drumQueue.contains(currentWorkingJobPCB))
                drumQueue.remove(currentWorkingJobPCB);                
        }
        
        /* If drum queue has content on it
          * - Set the currently working job to the first job off the drum queue
          * - If the job is "incore", move from drum to memory
          * - Else move from memory to drum
        */
        if(!drumQueue.isEmpty()){
            drumManager();
        }
        MemoryManager(currentWorkingJobPCB, 1);         
        cpuScheduler(a, p);        
    }
    
    
    /*
        Call this when we want to do IO
        * - Get the job at the top of the IO Queue
        * - If we are currently doing IO, just add to ready queue  
    */
    public static void ioManager(){
        currentWorkingJobPCB = ioQueue.get(0);
        
        readyQueue.add(lastRunningJobPCB);
        if(!currentlyDoingIo){
            currentlyDoingIo = true;
            currentWorkingJobPCB.latchJob();
            lastJobToIo = ioQueue.get(0);
            ioQueue.remove(0);
            sos.siodisk(currentWorkingJobPCB.getJobNumber()); 
            if(!currentWorkingJobPCB.getBlockedStatus()){
                if(!currentWorkingJobPCB.getTerminatedStatus())
                    readyQueue.add(currentWorkingJobPCB);
            }
            else
                readyQueue.add(lastRunningJobPCB);
        }
        System.out.println(currentWorkingJobPCB.getJobNumber());
        
    }
    
    public static void cpuScheduler(int a[], int p[]){
        JobTable.printJobTable();
        boolean possible = true;
        boolean falseStuff = false;
        if(readyQueue.isEmpty()){
            possible = false;
            falseStuff = true;
        }
    
        while(possible){
            System.out.println(readyQueue.size());
            if(readyQueue.size() == 0)
            {
                falseStuff = true;
                break;
            }
            currentWorkingJobPCB = readyQueue.get(0);
            readyQueue.remove(0);
            if(currentWorkingJobPCB != null){
                if(currentWorkingJobPCB.getBlockedStatus() == false ){
                    System.out.println("Job to rusadfsafn: " + currentWorkingJobPCB.getJobNumber());                
                    if(currentWorkingJobPCB.getTerminatedStatus() == false){
                    System.out.println("Job to rusadfsafn: " + currentWorkingJobPCB.getJobNumber());
                    dispatcher(a, p);
                    possible = false;
                    }
                }/*else{
                    
                    //possible = false;
                    falseStuff = true;
                    if(readyQueue.isEmpty()){
                        possible = false;
                    }

                    if(possible)
                        readyQueue.remove(0);
                    //System.out.println(currentWorkingJobPCB.getJobNumber());
                    a[0] = 1;
                    currentWorkingJobPCB = null;
                    lastRunningJobPCB = null;
                }  */ 
            }
        }
        if(falseStuff == true){
            currentWorkingJobPCB = null;
            lastRunningJobPCB = null;
            a[0] = 1;
        }
    }
    
    public static void dispatcher(int a[], int p[]){
        lastRunningJobPCB = currentWorkingJobPCB;
        lastRunningJobPCB.setLastTimeProcessing(p[5]);
        
        if(lastRunningJobPCB.getCpuTimeLeft() > roundRobinSlice){
			a[0] = 2; // Set system to process a job
			p[2] = lastRunningJobPCB.getAddress();
			p[3] = lastRunningJobPCB.getJobSize();
			p[4] = roundRobinSlice;
			
		}else{
			a[0] = 2; // Set system to process a job
			p[2] = lastRunningJobPCB.getAddress();
			p[3] = lastRunningJobPCB.getJobSize();
			p[4] = lastRunningJobPCB.getCpuTimeLeft();
		}
		
    }
        
    public static void MemoryManager(processControlBlock job, int function){
        int tempAddress;
        boolean working = false;
        
        if(longTermScheduler.size() > 0)
            working = true; 
        
        if(function == 0){ // SEE IF THE JOB FITS IN FREE SPACE TALBLE, IF IT DOES, PLACE IN MEMORY
            tempAddress = FreeSpaceTable.findSpaceForJob(job);

            if(tempAddress >= 0 ){
                drumQueue.add(job);
                
                while(working){
                    tempAddress = -1;
                    job = longTermScheduler.get(0);
                    tempAddress = FreeSpaceTable.findSpaceForJob(job);
                    if(tempAddress >= 0){
                        drumQueue.add(job);
                        longTermScheduler.remove(0);
                    }else{
                        working = false;
                    }                    
                }
                // PLACE IN MEMORY AT ADDRESS
            }else{
                longTermScheduler.add(job);
                // REMOVE A JOB FROM MEMORY and TRY TO FIND SPACE AGAIN
            }
        }else if(function == 1){            
            while(working){
                job = longTermScheduler.get(0);
                tempAddress = FreeSpaceTable.findSpaceForJob(job);
                System.out.println(tempAddress);
                FreeSpaceTable.printFST();
                if(tempAddress >= 0){
                    drumQueue.add(job);
                    longTermScheduler.remove(0);
                    job = null;
                }else{
                    working = false;
                }                    
            }            
        }
        
        drumManager();
    }
    
    public static void drumManager(){
            if(drumQueue.size() > 0)
                currentWorkingJobPCB = drumQueue.get(0);
            
            if(!DoingSwap && drumQueue.size() > 0){
                DoingSwap = true;
                if(!currentWorkingJobPCB.getInCoreStatus())
                    sos.siodrum(currentWorkingJobPCB.getJobNumber(), currentWorkingJobPCB.getJobSize(), currentWorkingJobPCB.getAddress(), 0);
                else
                    sos.siodrum(currentWorkingJobPCB.getJobNumber(), currentWorkingJobPCB.getJobSize(), currentWorkingJobPCB.getAddress(), 1);    
            }
    }    
   
}
