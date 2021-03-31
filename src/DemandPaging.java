import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;


class Frame {

    int ID;
  //  boolean isOccupied;
 //   int startTime;
    int processID;
    int pageNumber;
    int lru;

	public Frame(int ID) {
		this.ID = ID;
	//	this.isOccupied = false;
		this.processID = -1;
		this.pageNumber = -1;
	//	this.startTime = -1;
		this.lru=-1;
	}
	
	public boolean equals(Frame rhs) {
		return this.ID == rhs.ID;
	}

	public int getID() {
		return this.ID;
	}

	public int getProcessID() {
		return this.processID;
	}

	public int getPageNumber() {
		return this.pageNumber;
	}



}

class Process {

	int ID;
    int size;
	//JobMixProbability jobMix;
    int page;
    int remainingReferences;
    int currentWord;
    int numEvictions;
    int runningSumPageResidencyTime;
    int[] load; 
	int faults;
    int quantum;

	public Process(int ID, int size, int numRefs, int page) {
		this.ID = ID;
		this.size = size;
		this.remainingReferences = numRefs;
		//this.jobMix = jobMix;
		this.quantum = 3;

		this.currentWord = (111 * this.ID) % this.size;
		this.page=this.currentWord/page;
		this.load = new int[size/page];
		
		
		
		

		this.faults = 0;
		this.numEvictions = 0;
		this.runningSumPageResidencyTime = 0;
	}
	
	public double getAvgResidencyTime() {
		return  runningSumPageResidencyTime / ( numEvictions * 1.0);
	}

	public int getID() {
		return this.ID;
	}


	public int getSize() {
		return this.size;
	}

	public int getCurrentWord() {
		return this.currentWord;
	}

}



public class DemandPaging {
	static int M, P, S, J, N;
	//static int mode, numProcesses, currRandPos = 1;
	static int processesNum;
	static String R;
	private static LinkedList<Process> processes;
	private static LinkedList<Frame> frames;
	static double[][] probabilities; 
	static LinkedList<Integer> FIFO = new LinkedList<Integer>(); // keep track of order in which frames where used
//	private static Object scanner;
	private static Scanner scanner;
	
	public static void main(String[] args) throws FileNotFoundException {
		DemandPaging.scanner = new Scanner(new File(
				"random-numbers.txt"));
		
		double A, B, C, y;
		int remainTime, usedFrames = 0; 
		int currentTime = 1, hitIndex = -1, evictPos = -1;
		boolean fault = true, hit = false;
		

		if(args.length != 7) 
			throw new IllegalArgumentException ("Wrong number of inputs.Provide the following 7 arguments: M P S J N R 0");
		
		M = Integer.parseInt(args[0]); // machine size
		P = Integer.parseInt(args[1]); // page size
		S = Integer.parseInt(args[2]); // process size
		J = Integer.parseInt(args[3]); // job mix
		N = Integer.parseInt(args[4]); 
		R = args[5]; 
		
		initialize();

		
		int availFrames=M/P;

		for (int i = 0; i < availFrames; i++) 
			frames.add(new Frame(i));
		
		remainTime = N * processesNum;
		Process currentProcess=processes.get(0);
		

		while(remainTime>0) {
			for (int i=0; i<frames.size(); i++) {
				if (frames.get(i).processID == currentProcess.ID && frames.get(i).pageNumber == currentProcess.page) {
					hitIndex = i; // frame at which there was a hit
					hit = true;
					fault = false;
					break;
				}
			}
			
			if (hit) {
				frames.get(hitIndex).lru = currentTime;
			} else if (usedFrames < availFrames) {
				for (int i = frames.size() - 1; i >= 0; i--) {
					if (frames.get(i).pageNumber == -1 && frames.get(i).processID == -1) {
						FIFO.add(i); 
						frames.get(i).pageNumber = currentProcess.page; 
						frames.get(i).processID = currentProcess.ID; 
						frames.get(i).lru = currentTime; 
						currentProcess.faults++; // increment faults for that process
						currentProcess.load[currentProcess.page] = currentTime; 
						usedFrames++; 
						break; 
					}
				
			}
				
		} else if (fault) {
			currentProcess.faults++; // increment faults for that process
			currentProcess.load[currentProcess.page] = currentTime; // set load remainTime for this page of the process
			if (R.equals("lru")) {
				int min=frames.get(0).lru;
				int index=0;
				for (int i = 0; i < frames.size(); i++) {
					if (frames.get(i).lru < min) {
						min = frames.get(i).lru;
						index = i;
					}
				}
				evictPos=index;
				
			
			} else if (R.equals("fifo")) {
				evictPos = FIFO.remove(); 
				FIFO.add(evictPos);
			} else if (R.equals("random")) {
				evictPos = getRand()%availFrames; // get index based on random-numbers
			} else {
				System.out.println("Wrong replacement algorithm.");
			}
			processes.get(frames.get(evictPos).processID-1).numEvictions++;
			processes.get(frames.get(evictPos).processID-1).runningSumPageResidencyTime +=
					currentTime -processes.get(frames.get(evictPos).processID-1).load[frames.get(evictPos).pageNumber];
			frames.get(evictPos).lru=currentTime;
			frames.get(evictPos).pageNumber=currentProcess.page;
			frames.get(evictPos).processID=currentProcess.ID;
			currentProcess.load[currentProcess.page]=currentTime;
		}
			
		y = getRand()/(Integer.MAX_VALUE + 1d); // get random quotient y
		A = probabilities[currentProcess.ID-1][0]; // get probabilities for current process
		B = probabilities[currentProcess.ID-1][1];
		C = probabilities[currentProcess.ID-1][2];
		
		if (y < A) { // update next word 
			currentProcess.currentWord = (currentProcess.currentWord + 1+S)%S;
		} else if (y < (A+B)) {
			currentProcess.currentWord = (currentProcess.currentWord - 5 + S)%S;
		} else if (y < (A+B+C)) {
			currentProcess.currentWord = (currentProcess.currentWord + 4 +S)%S;
		} else {
			currentProcess.currentWord = getRand()%S; 
		}			
				
		currentProcess.page = currentProcess.currentWord/P; 
		currentProcess.quantum--; 
		currentProcess.remainingReferences--; // decrement references for that process so we know when to stop
		
		if (currentProcess.remainingReferences == 0 || currentProcess.quantum == 0) { 
			currentProcess.quantum = 3; 
			currentProcess = processes.get(currentProcess.ID % processesNum); 
		}
		
		remainTime--; 
		currentTime++; 
		hit = false; 
		fault = true; 
	}
		

		printReport();
						
		
	}
	
	

	private static void initialize() {
		processes = new LinkedList<Process>();
		switch (J) {
		case 1:
			processesNum=1;
			processes.add(new Process(1, S, N, P));
			probabilities = new double[][]{{1,0,0,0}};

			break;

		case 2:
			processesNum=4;
			for (int i = 1; i < processesNum+1; i++)
				processes.add(new Process(i, S, N, P));
			probabilities = new double[][]{{1,0,0,0}, {1,0,0,0}, {1,0,0,0}, {1,0,0,0}};

			break;

		case 3:
			processesNum=4;
			for (int i = 1; i < processesNum+1; i++)
				processes.add(new Process(i, S, N, P));
			probabilities = new double[][]{{0,0,0,0}, {0,0,0,0}, {0,0,0,0}, {0,0,0,0}};

			break;

		case 4:
			processesNum=4;
			processes.add(new Process(1, S, N, P));
			processes.add(new Process(2, S, N, P));
			processes.add(new Process(3, S, N, P));
			processes.add(new Process(4, S, N, P));
			probabilities = new double[][]{{0.75,0.25,0,0}, {0.75, 0, 0.25, 0},
				{0.75, 0.125, 0.125, 0}, {0.5, 0.125, 0.125, 0.25}};
			
			break;
		
		
		}		
		frames = new LinkedList<Frame>();
	}



	private static void printReport() {
		System.out.println("The machine size is " + M + ".");
		System.out.println("The page size is " + P + ".");
		System.out.println("The process size is " + S + ".");
		System.out.println("The job mix number is " + J + ".");
		System.out.println("The number of references per process is " + N
				+ ".");
		System.out.println("The replacement algorithm is " + R + ".");
		System.out.printf("The level of debugging output is 0.\n\n");	
		
		
		int totFaults = 0;
		int totResidency = 0;
		int totEvictions = 0;
		boolean shouldDisplay = true;
		
		for (int i=0; i<processes.size(); i++) {
			Process p=processes.get(i);
			System.out.print("Process " + p.ID + " had "
					+ +p.faults + " faults");

			if (Double.isNaN(p.getAvgResidencyTime()))
				System.out
						.print(".\n\tWith no evictions, the average residence is undefined.\n");

			else
				System.out.print(" and " + p.getAvgResidencyTime()
						+ " average residency.\n");

			totFaults += p.faults;
			totEvictions += p.numEvictions;
			totResidency += p.runningSumPageResidencyTime;
		}
		if (totResidency <= 0)
			shouldDisplay = false;

		System.out.print("\nThe total number of faults is " + totFaults + " ");
		if (shouldDisplay)
			System.out.print("and the overall average residency is "
					+ (totResidency * 1.0 / totEvictions) + ".");
		else
			System.out
					.println("\n\tWith no evictions, the overall average residence is undefined.");

		
	}



	private static int getRand() {
		if (DemandPaging.scanner.hasNextInt())
			return DemandPaging.scanner.nextInt();

		return 0;
	}
	

	

}
