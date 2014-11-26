public class JobLink{

	public int JobNumber;
	public int Priority;
	public int JobSize;
	public int MaxCPU;
	public int CurrentTime;
	public int CPUTimeLeft;
	public int CPUTimeUsed;
	public int lastTimeProcessing;
	public int address;
	public int ioLeft;
	public int ioCompleted;
	public boolean latched;
	public boolean doingIO;
	public boolean inCore;
	public boolean Blocked;
	public JobLink next;

	public JobLink(int JobNumber, int Priority, int JobSize, int MaxCPU, int CurrentTime, boolean Blocked, boolean inCore){
		this.JobNumber = JobNumber;
		this.Priority = Priority;
		this.JobSize = JobSize;
		this.MaxCPU = MaxCPU;
		CPUTimeLeft = MaxCPU;
		CPUTimeUsed = 0;
		lastTimeProcessing = 0;
		this.CurrentTime = CurrentTime;
		this.Blocked = Blocked;
		this.inCore = inCore;
	}

	public int getJobSize(){ return JobSize; }

	public void JobPrintStatus(){
		System.out.println("JobNo: " + JobNumber + " Priority: " + Priority + " Processed: " + CPUTimeUsed + " MAX CPU: " + MaxCPU + " JobSize: " + JobSize + " Address: " + address + " inCore: " + inCore + " Blocked: " + Blocked + " IOLEFT: " + ioLeft + " IOCompleted " + ioCompleted);
	}
}
