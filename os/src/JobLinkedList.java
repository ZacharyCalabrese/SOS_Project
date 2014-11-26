public class JobLinkedList{
	public static JobLink first;
	public static int tableSize;
		
	public JobLinkedList(){
		tableSize = 0;
		first = null;
	}

	public boolean isEmpty(){
		return first == null;
	}

	public void insert(int JobNumber, int Priority, int JobSize, int MaxCPU, int CurrentTime, boolean Blocked, boolean inCore){
	    JobLink link; 
		link = new JobLink(JobNumber, Priority, JobSize, MaxCPU, CurrentTime, Blocked, inCore);
		link.next = first;
		first = link;
		tableSize++;
	}

	public JobLink lastIn(){
		return first;
	}

	public void pop(){
		first = first.next;
		tableSize--;
	}

	public void printTable(){
		for(int i = 0; i < tableSize; i++){
			JobLink temp = first;
			temp.JobPrintStatus();
			temp = first.next;
		}
	}	
}
