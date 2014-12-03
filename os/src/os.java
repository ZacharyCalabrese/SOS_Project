import java.util.ArrayList;
import java.util.List;

public class os{
    private static jobTable JobTable;
    private static freeSpaceTable FreeSpaceTable;
    private static List<processControlBlock> readyQueue = new ArrayList<processControlBlock>();
    private static List<processControlBlock> drumQueue = new ArrayList<processControlBlock>();
    private static List<processControlBlock> ioQueue = new ArrayList<processControlBlock>();
    private static processControlBlock lastRunningJobPCB;
    private static processControlBlock currentWorkingJobPCB;
    private static int roundRobinSlice;
    
    public static void startup(){
        JobTable = new jobTable(50);
        FreeSpaceTable = new freeSpaceTable(99);
        
        lastRunningJobPCB = null;
        currentWorkingJobPCB = null;
        
        roundRobinSlice = 500;
        
        sos.ontrace();
    }
    
    public static void Crint(int a[], int p[]){
        if(lastRunningJobPCB != null){
            lastRunningJobPCB.calculateTimeProcessed(p[5]);
            System.out.println(lastRunningJobPCB.getJobNumber());
            readyQueue.add(lastRunningJobPCB);
        }
        
        if(JobTable.contains(p[1])){
            // PLACE JOB BACK INTO CORE THAT WE ALREADY HAVE DONE MATH FOR
        }else{
            System.out.println("Here");
            currentWorkingJobPCB = new processControlBlock(p[1], p[2], p[3], p[4], p[5]);
            JobTable.addJob(currentWorkingJobPCB);
            MemoryManager(currentWorkingJobPCB, 0);
            // PLACE NEW JOB INTO MEMORY
        }
        
                cpuScheduler(a, p);
        
                    JobTable.printJobTable();
    }
    
    public static void Svc(int a[], int p[]){
        lastRunningJobPCB.calculateTimeProcessed(p[5]);
        
        currentWorkingJobPCB = lastRunningJobPCB;
        
        if(a[0] == 5){
            readyQueue.remove(currentWorkingJobPCB);
            readyQueue.remove(currentWorkingJobPCB);        
            readyQueue.remove(currentWorkingJobPCB);
            readyQueue.remove(currentWorkingJobPCB);
            readyQueue.remove(currentWorkingJobPCB);
            readyQueue.remove(currentWorkingJobPCB);
            readyQueue.remove(currentWorkingJobPCB);
                        readyQueue.remove(currentWorkingJobPCB);
            readyQueue.remove(currentWorkingJobPCB);        
            readyQueue.remove(currentWorkingJobPCB);
            readyQueue.remove(currentWorkingJobPCB);
            readyQueue.remove(currentWorkingJobPCB);
            readyQueue.remove(currentWorkingJobPCB);
            readyQueue.remove(currentWorkingJobPCB);             
            currentWorkingJobPCB.removeInCore();
            FreeSpaceTable.addSpace(currentWorkingJobPCB);
            JobTable.removeJob(lastRunningJobPCB);
            currentWorkingJobPCB = null;
            lastRunningJobPCB = null;
        }else if(a[0] == 6){
            System.out.println(currentWorkingJobPCB.getJobNumber());
            currentWorkingJobPCB.incrementIoCount();
                        System.out.println("Break2");
            ioQueue.add(currentWorkingJobPCB);
                        System.out.println("Break3");
            ioManager();
                        System.out.println("Break4");
        }else if(a[0] == 7){
        System.out.println("breakhere");
            if(currentWorkingJobPCB.getIoCount() != 0){
            lastRunningJobPCB = null;
            
                currentWorkingJobPCB.blockJob();
                            readyQueue.remove(currentWorkingJobPCB);
            readyQueue.remove(currentWorkingJobPCB);        
            readyQueue.remove(currentWorkingJobPCB);
            readyQueue.remove(currentWorkingJobPCB);
            readyQueue.remove(currentWorkingJobPCB);
            readyQueue.remove(currentWorkingJobPCB);
            readyQueue.remove(currentWorkingJobPCB);
                        readyQueue.remove(currentWorkingJobPCB);        
            readyQueue.remove(currentWorkingJobPCB);
            readyQueue.remove(currentWorkingJobPCB);
            readyQueue.remove(currentWorkingJobPCB);
            
            }else{
                readyQueue.add(currentWorkingJobPCB);
            }
        }
        
        cpuScheduler(a, p);
        
        JobTable.printJobTable();
    }
    
    public static void Tro(int a[], int p[]){
    
        lastRunningJobPCB.calculateTimeProcessed(p[5]);
        JobTable.printJobTable();
            System.out.println("Job to run: " + lastRunningJobPCB.getJobNumber());        
        currentWorkingJobPCB = lastRunningJobPCB;
                    readyQueue.remove(currentWorkingJobPCB);
        if(currentWorkingJobPCB.getCpuTimeUsed() >= currentWorkingJobPCB.getMaxCpuTime()){
            System.out.println("here1");
            readyQueue.remove(currentWorkingJobPCB);        
            readyQueue.remove(currentWorkingJobPCB);
            readyQueue.remove(currentWorkingJobPCB);
            readyQueue.remove(currentWorkingJobPCB);
            readyQueue.remove(currentWorkingJobPCB);
            readyQueue.remove(currentWorkingJobPCB);
            currentWorkingJobPCB.removeInCore();
            FreeSpaceTable.addSpace(currentWorkingJobPCB);
            JobTable.removeJob(currentWorkingJobPCB);
            currentWorkingJobPCB = null;
            lastRunningJobPCB = null;
        }else{
                    System.out.println("here2");
            readyQueue.add(currentWorkingJobPCB);
        }
        
        cpuScheduler(a, p);
    }
    
    public static void Dskint(int a[], int p[]){
        if(lastRunningJobPCB != null){
            lastRunningJobPCB.calculateTimeProcessed(p[5]);
            if(!lastRunningJobPCB.getBlockedStatus())
                readyQueue.add(lastRunningJobPCB);
        }
        
        currentWorkingJobPCB = ioQueue.get(0);
        ioQueue.remove(0);
        
        currentWorkingJobPCB.decrementIoCount();
        currentWorkingJobPCB.unlatchJob();
        
        if(currentWorkingJobPCB.getIoCount() == 0){
            if(currentWorkingJobPCB.getBlockedStatus()){
                currentWorkingJobPCB.unblockJob();
                readyQueue.add(currentWorkingJobPCB);
            }else{
                readyQueue.add(currentWorkingJobPCB);
            }
                
        }else{
            ioQueue.add(currentWorkingJobPCB);
        }
        
        if(!ioQueue.isEmpty()){
            currentWorkingJobPCB = ioQueue.get(0);
            sos.siodisk(currentWorkingJobPCB.getJobNumber());
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
    
    public static void ioManager(){
        currentWorkingJobPCB = ioQueue.get(0);
        currentWorkingJobPCB.latchJob();
        if(ioQueue.size() == 1){
            sos.siodisk(currentWorkingJobPCB.getJobNumber());   
        }
        readyQueue.add(currentWorkingJobPCB);
        
    }
    
    public static void cpuScheduler(int a[], int p[]){
        if(!readyQueue.isEmpty()){
            System.out.println(readyQueue.size());
            currentWorkingJobPCB = readyQueue.get(0);
            if(currentWorkingJobPCB.getBlockedStatus() == false){
            System.out.println("Job to run: " + currentWorkingJobPCB.getJobNumber());
            readyQueue.remove(0);
            dispatcher(a, p);
            }else{
            System.out.println(currentWorkingJobPCB.getJobNumber());
            a[0] = 1;
            currentWorkingJobPCB = null;
            lastRunningJobPCB = null;
            }
        }else{
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
        if(function == 0){ // SEE IF THE JOB FITS IN FREE SPACE TALBLE, IF IT DOES, PLACE IN MEMORY
            System.out.println("here1");
            FreeSpaceTable.printFST();
            tempAddress = FreeSpaceTable.findSpaceForJob(job);
            System.out.println(tempAddress);
            FreeSpaceTable.printFST();
            if(tempAddress >= 0){
                System.out.println("here2");
                Swapper(job, 0);
                // PLACE IN MEMORY AT ADDRESS
            }else{
                // REMOVE A JOB FROM MEMORY and TRY TO FIND SPACE AGAIN
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
