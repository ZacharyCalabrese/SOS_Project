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
    private static boolean currentlyDoingIo;
    
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
        
        roundRobinSlice = 500;
        
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
            System.out.println(lastRunningJobPCB.getJobNumber());
            readyQueue.add(lastRunningJobPCB);
        }
        
        if(JobTable.contains(p[1])){
            // PLACE JOB BACK INTO CORE THAT WE ALREADY HAVE DONE MATH FOR
        }else{
            currentWorkingJobPCB = new processControlBlock(p[1], p[2], p[3], p[4], p[5]);
            JobTable.addJob(currentWorkingJobPCB);
            MemoryManager(currentWorkingJobPCB, 0);
        }
        
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
            while(readyQueue.contains(lastRunningJobPCB))
                readyQueue.remove(lastRunningJobPCB);
            while(ioQueue.contains(lastRunningJobPCB))
                ioQueue.remove(lastRunningJobPCB);                                                    
                                                                                                                                          
            lastRunningJobPCB.removeInCore();
            FreeSpaceTable.addSpace(lastRunningJobPCB);
            JobTable.removeJob(lastRunningJobPCB);
            currentWorkingJobPCB = null;
            lastRunningJobPCB = null;            
        }
        /* When a job requests to do IO
          * - Increment the io count
          * - Add the job to the io queue
          * - Call IO manager
        */
        else if(a[0] == 6){
            System.out.println(lastRunningJobPCB.getJobNumber());
            lastRunningJobPCB.incrementIoCount();
            System.out.println("Break2");
            ioQueue.add(lastRunningJobPCB);
            System.out.println("Break3");
            ioManager();
            System.out.println("Break4");
        }else if(a[0] == 7){
        System.out.println("breakhere");
            if(lastRunningJobPCB.getIoCount() != 0){        
                lastRunningJobPCB.blockJob();
                while(readyQueue.contains(lastRunningJobPCB))
                    readyQueue.remove(lastRunningJobPCB);
                lastRunningJobPCB = null;                    
            }else{
                readyQueue.add(lastRunningJobPCB);
            }
        }
        
        cpuScheduler(a, p);
        
        JobTable.printJobTable();
    }
    
    public static void Tro(int a[], int p[]){
        lastRunningJobPCB.calculateTimeProcessed(p[5]);
        JobTable.printJobTable();
            System.out.println("Job to run: " + lastRunningJobPCB.getJobNumber());        
                    //readyQueue.remove(currentWorkingJobPCB);
        if(currentWorkingJobPCB.getCpuTimeUsed() >= currentWorkingJobPCB.getMaxCpuTime()){
            System.out.println("here1");
            while(readyQueue.contains(lastRunningJobPCB))
                readyQueue.remove(lastRunningJobPCB);
            while(ioQueue.contains(lastRunningJobPCB))
                ioQueue.remove(lastRunningJobPCB);                                                     

            lastRunningJobPCB.removeInCore();
            FreeSpaceTable.addSpace(lastRunningJobPCB);
            JobTable.removeJob(lastRunningJobPCB);
            currentWorkingJobPCB = null;
            lastRunningJobPCB = null;
            MemoryManager(currentWorkingJobPCB, 1);
        }else{
                    System.out.println("here2");
            readyQueue.add(currentWorkingJobPCB);
        }
        
        cpuScheduler(a, p);
    }
    
    public static void Dskint(int a[], int p[]){
        currentlyDoingIo = false;
        
        for (processControlBlock t : ioQueue)
            System.out.println("proces number: " + t.getJobNumber() + ", IOLeft: " + t.getIoCount());
            
            
    
        if(lastRunningJobPCB != null){
            lastRunningJobPCB.calculateTimeProcessed(p[5]);
            if(!lastRunningJobPCB.getBlockedStatus())
                readyQueue.add(lastRunningJobPCB);
        }
        
        System.out.println("ReadyQueue");
        for (processControlBlock t : readyQueue)
            System.out.println("proces number: " + t.getJobNumber() + ", IOLeft: " + t.getIoCount());        
        
        currentWorkingJobPCB = lastJobToIo;
        //ioQueue.remove(0);
        
        currentWorkingJobPCB.decrementIoCount();
        currentWorkingJobPCB.unlatchJob();
        
        if(currentWorkingJobPCB.getIoCount() == 0){
            while(ioQueue.contains(currentWorkingJobPCB))
                ioQueue.remove(currentWorkingJobPCB);
            if(currentWorkingJobPCB.getBlockedStatus()){
                currentWorkingJobPCB.unblockJob();
                readyQueue.add(currentWorkingJobPCB);
            }else{
                readyQueue.add(currentWorkingJobPCB);
            } 
        }
        
        if(!ioQueue.isEmpty()){
//        readyQueue.add(currentWorkingJobPCB);
            ioManager();
            //currentWorkingJobPCB = ioQueue.get(0);
            //sos.siodisk(currentWorkingJobPCB.getJobNumber());
            //readyQueue.add(currentWorkingJobPCB);
        }
        
        cpuScheduler(a, p);   
        
        JobTable.printJobTable(); 
    }
    
    public static void Drmint(int a[], int p[]){
    
        if(lastRunningJobPCB != null){
            lastRunningJobPCB.calculateTimeProcessed(p[5]);
            readyQueue.add(lastRunningJobPCB);
        }
    
        currentWorkingJobPCB = drumQueue.get(0);
        drumQueue.remove(0);
        
        if(!currentWorkingJobPCB.getInCoreStatus()){
            currentWorkingJobPCB.putInCore();
            readyQueue.add(currentWorkingJobPCB);
        }else{
            currentWorkingJobPCB.removeInCore();
        }
        
        if(!drumQueue.isEmpty()){
            currentWorkingJobPCB = drumQueue.get(0);
            sos.siodrum(currentWorkingJobPCB.getJobNumber(), currentWorkingJobPCB.getJobSize(), currentWorkingJobPCB.getAddress(), 0);
        }
        
        cpuScheduler(a, p);        
        JobTable.printJobTable();
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
                readyQueue.add(currentWorkingJobPCB);
            }
            else
                readyQueue.add(lastRunningJobPCB);
        }
        System.out.println(currentWorkingJobPCB.getJobNumber());
        
    }
    
    public static void cpuScheduler(int a[], int p[]){
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
                    dispatcher(a, p);
                    possible = false;
                }else{
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
            }
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
            FreeSpaceTable.printFST();
            tempAddress = FreeSpaceTable.findSpaceForJob(job);
            System.out.println(tempAddress);
            FreeSpaceTable.printFST();
            
            System.out.println(FreeSpaceTable.getWorstSpaceSize());
            if(tempAddress >= 0){
                Swapper(job, 0);
                
                while(working){
                    tempAddress = -1;
                    job = longTermScheduler.get(0);
                    tempAddress = FreeSpaceTable.findSpaceForJob(job);
                    System.out.println(tempAddress);
                    FreeSpaceTable.printFST();
                    if(tempAddress >= 0){
                        Swapper(job, 0);
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
                        Swapper(job, 0);
                        longTermScheduler.remove(0);
                        job = null;
                    }else{
                        working = false;
                    }                    
                }            
        
        }
    }
    
    public static void Swapper(processControlBlock job, int function){
        if(function == 0){ // INDICATES MOVING FRUM DRUM TO MEMORY
            if(drumQueue.isEmpty())
                sos.siodrum(job.getJobNumber(), job.getJobSize(), job.getAddress(), 0);
            drumQueue.add(job);
        }
        
    }
}
