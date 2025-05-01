//Project By: Kyle Abreu

/***********************************************
 * Function: MTOPS
 * ---------------------------------------------
 * Task performed:
 * MTOPS, the Multi-Tasking Operating System, provides essential features 
 * grouped into process management, memory management, interprocess communication, 
 * timer management, and character I/O support. It enables multitasking with 
 * priority-based scheduling, dynamic memory allocation, interprocess communication, 
 * system clock management, and character I/O operations. These features form a 
 * strong foundation for timesharing and multitasking application systems, 
 * supporting efficient resource management, process coordination, and user interaction.
 *
 * Input Parameters:
 *   None
 *
 * Output Parameters:
 *   None
 *
 * Function Return Value:
 *   int - Returns a status code indicating the success or failure of the OS
 ************************************************/

//mainMemory Sections
// 0------
// *     |
// *     | Programmable Area
// *     |
// 2999---
//
// 3000---
// *     |
// *     | User Free Area
// *     |
// 5999---
// 
// 6000---
// *     |
// *     | OS Free Area
// *     |
// 9999---

import java.util.Scanner;
import java.io.File;

public class OS {
	// Global Variables
	// Declare Hardware Components as Global Variable
	int mainMemory[]; // Main Memory
	int GPR[];        // General Purpose Register
	int MAR;   	      // Memory Address Register 
	int MBR;          // Memory Buffer Register
	int CLOCK;        // Scheduler
	int IR;           // Instruction Register
	int PSR;          // Processor PSR Register
	int PC;           // Program Counter
	int SP;			  // Stack Pointer
	
	int PID;
	int runningPCBptr;
	int status;
	final int SHUTDOWN = -1;
	
	// Status is OK	
	final int OK = 0;
	
	// End Of List	
	final int EOL = -1;
	
	// Declare RQ, WQ, OSFreeList and UserFreeList variables of type int and initialize them to End of List
	int RQ = EOL;		    // Ready Queue set to empty list
	int WQ = EOL;		    // Waiting Queue set to empty list
	int OSFreeList = EOL;	// OS Free memory list set to empty list;
	int UserFreeList = EOL;	// User free memory list is set to empty list;

	
	// Valid Memory Sizes	
	final int VALID_PROGRAM_MIN = 0;
	final int VALID_PROGRAM_MAX = 2999;
	final int VALID_USER_FREE_MIN = 3000;
	final int VALID_USER_FREE_MAX = 5999;
	final int VALID_OS_FREE_MIN = 6000;
	final int VALID_OS_FREE_MAX = 9999;
	
	// I/O Event Codes	
	final int INPUT_START_EVENT = 1;
	final int OUTPUT_START_EVENT = 2;
	final int INPUT_COMPLETION_EVENT = 3;
	final int OUTPUT_COMPLETION_EVENT = 4;
		
	// Modes
	final int OS_MODE = 1;
	final int USER_MODE = 2;
	
	// Global Errors	
	final int INVALID_MEMORY_ADDRESS_ERROR = -2;
	final int INVALID_SIZE_OR_MEMORY_ADDRESS_ERROR = -3;

	// PCB Finals	
	final int PCB_SIZE = 18;
	final int PCB_STACK_SIZE = 20;
	final int DEFAULT_PRIORITY = 128;
	
	// PCB States	
	final int READY_STATE = 1;
	final int WAITING_STATE = 2;
	final int RUNNING_STATE = 3;
	
	// PCB Indexes	
	final int NEXT_POINTER_INDEX = 0;
	final int PID_INDEX = 1;
	final int STATE_INDEX = 2;
	final int WAITING_CODE_INDEX = 3;
	final int PRIORITY_INDEX = 4;
	final int STACK_ADDRESS_INDEX = 5;
	final int STACK_SIZE_INDEX = 6;
	final int GPR_0_INDEX = 7;
	final int GPR_1_INDEX = 8;
	final int GPR_2_INDEX = 9;
	final int GPR_3_INDEX = 10;
	final int GPR_4_INDEX = 11;
	final int GPR_5_INDEX = 12;
	final int GPR_6_INDEX = 13;
	final int GPR_7_INDEX = 14;
	final int SP_INDEX = 15;
	final int PC_INDEX = 16;
	final int PSR_INDEX = 17;

	//Time	
	final int TIME_SLICE = 200;
	
	//Global Scanner	
	Scanner input = new Scanner(System.in);
		
	/***********************************************
	 * Function: CreateProcess By: Kyle Abreu
	 * ---------------------------------------------
	 * Task performed:
	 *   Creates a new process by loading a program from a specified file into memory, initializing its Process Control Block (PCB), 
	 *   and inserting it into the Ready Queue (RQ). The function handles memory allocation for the PCB and user stack, 
	 *   program loading, and updating PCB fields such as program counter (PC), stack pointer (SP), and process priority.
	 *
	 * Input Parameters:
	 *   filename (String) - The name of the file containing the program to be loaded.
	 *   priority (int) - The priority level of the process, which affects its position in the RQ.
	 *
	 * Output Parameters:
	 *   None - All modifications are made to the system's memory and process control structures directly.
	 *
	 * Function Return Value:
	 *   int - Returns a status code indicating the success or failure of the operation. Possible values are defined by constants:
	 *         - CP_ALLOCATE_PCB_MEMORY_ERROR: Indicates failure to allocate memory for the PCB.
	 *         - CP_LOADING_PROGRAM_ERROR: Indicates failure to load the program from the specified file.
	 *         - CP_ALLOCATE_USER_MEM_ERROR: Indicates failure to allocate memory for the user stack.
	 *         - OK: Indicates successful creation and insertion of the process into the Ready Queue.
	 ************************************************/
	int CreateProcess(String filename, int priority)
	{
		int OSPtr;
		int UserFreePtr;
		int value;
		
		final int CP_ALLOCATE_PCB_MEMORY_ERROR = -1;
		final int CP_LOADING_PROGRAM_ERROR = -2;
		final int CP_ALLOCATE_USER_MEM_ERROR = -3;
		
		// Allocate space for Process Control Block
		// return value contains address or error
		OSPtr = AllocateOSMemory(PCB_SIZE);
	      
		// Check for error and return error code, if memory allocation failed
	      if(OSPtr < 0) {
	    	  System.err.println("CP_ALLOCATE_PCB_MEMORY_ERROR");
	    	  return(CP_ALLOCATE_PCB_MEMORY_ERROR);
	      }	      

	      // Initialize PCB passing PCBptr as argument
	      InitializePCB(OSPtr);
	      
	      // Load the program
	      value = AbsoluteLoader(filename);
	      
	      // Check for error and return error code, if loading program failed
	      if(value < 0) {
	    	  System.err.println("CP_LOADING_PROGRAM_ERROR");
	    	  return(CP_LOADING_PROGRAM_ERROR);
	      }

	      // store PC value in the PCB of the process
	      mainMemory[OSPtr + PC_INDEX] = value;
	    		  
	      // Allocate stack space from user free list
	      UserFreePtr = AllocateUserMemory(PCB_STACK_SIZE);
	    		  
	      // check for error	    		  
	      if(UserFreePtr < 0) {
	    	  
	    	  // User memory allocation failed
	    	  // Free allocated PCB space
	    	  FreeOSMemory(OSPtr, PCB_SIZE);
	    	  System.err.println("CP_ALLOCATE_USER_MEM_ERROR");
	    	  // return error code		
	    	  return(CP_ALLOCATE_USER_MEM_ERROR);  
	      }

	      // Store stack information in the PCB: SP, ptr, and size
	      // Set SP in the PCB = ptr + Stack Size. Empty stack is high address, full is low address
	      mainMemory[OSPtr + SP_INDEX] = UserFreePtr + 20; //empty stack
	      
	      // Set stack start address in the PCB to ptr
	      mainMemory[OSPtr + STACK_ADDRESS_INDEX] = UserFreePtr;
	      
	      // Set stack size in the PCB = Stack Size
	      mainMemory[OSPtr + STACK_SIZE_INDEX] = 20;

	      // Set priority in the PCB = priority
	      mainMemory[OSPtr + PRIORITY_INDEX] = priority;

	      // Dump program area
	      DumpMemory("Dumping Program Area:", 0, 200);
	      
	      // Print PCB passing PCBptr
	      PrintPCB(OSPtr);

	      // Insert PCB into Ready Queue
	      status = InsertIntoRQ(OSPtr);

	     return(status);
	}  // end of CreateProcess() function

	/***********************************************
	 * Function: InitializePCB By: Kyle Abreu
	 * ---------------------------------------------
	 * Task performed:
	 *   Initializes a Process Control Block (PCB) to default values. This includes 
	 *   setting all fields within the PCB to zero, allocating a unique process ID (PID), 
	 *   and setting the state and priority fields to their default states.
	 *
	 * Input Parameters:
	 *   PCBptr (int) - An integer pointer to the PCB area in main memory.
	 *   
	 * Output Parameters:
	 *   None - The function modifies the PCB directly in memory and does not return data.
	 *
	 * Function Return Value:
	 *   void - No return value as the function performs operations directly on memory.
	 ************************************************/
	void InitializePCB (int PCBptr)
	{

		//Set next PCB pointer field in the PCB = EOL;  // EOL is a constant set to -1
		mainMemory[PCBptr] = PCBptr + PCB_SIZE;
		
		// Allocate PID and set it in the PCB. PID zero is invalid
	    // Set PID field in the PCB to = ProcessID++;  // ProcessID is global variable initialized to 1
	    mainMemory[PCBptr + PID_INDEX] = PID++;
	    
	    //Set state field in the PCB = ReadyState;    // ReadyState is a constant set to 1
	    mainMemory[PCBptr + STATE_INDEX] = READY_STATE;
	    
	    // Set priority field in the PCB = Default Priority;  // DefaultPriority is a constant set to 128
	    mainMemory[PCBptr + PRIORITY_INDEX] = DEFAULT_PRIORITY;
	}  // end of InitializePCB

	/***********************************************
	 * Function: PrintPCB By: Kyle Abreu
	 * ---------------------------------------------
	 * Task performed:
	 *   Prints various values stored in the Process Control Block (PCB), including the PCB start address,
	 *   state, process ID (PID), reason for waiting, program counter (PC), stack pointer (SP), priority, 
	 *   and stack information. Additionally, it prints the values of the General Purpose Registers (GPRs)
	 *   associated with the PCB.
	 *
	 * Input Parameters:
	 *   PCBptr (int) - An integer representing the memory address of the PCB to be printed.
	 *  
	 * Output Parameters:
	 *   None - The function prints information to the console and does not return data.
	 *
	 * Function Return Value:
	 *   void - No return value as the function performs printing directly to the console.
	 ************************************************/
	void PrintPCB(int PCBptr) {
		String stateTemp = "";
		switch(mainMemory[PCBptr + STATE_INDEX]) {
		  case 1:
			  stateTemp = "Ready";
			    break;
		  case 2:
			  stateTemp = "Waiting";
			    break;
		  case 3:
			  stateTemp = "Running";
			    break;			  
		  default:
			  stateTemp = "Ready";
		}

		String reasonTemp = "";
		switch(mainMemory[PCBptr + WAITING_CODE_INDEX]) {
		  case 3:
			  reasonTemp = "INPUT_COMPLETION_EVENT";
			    break;
		  case 4:
			  reasonTemp = "OUTPUT_COMPLETION_EVENT";
			    break;
		  default:
			  reasonTemp = "CPU_ALLOCATION_EVENT";
		}

		System.out.println("PCB Info:\n"
						 + "PCB Address = " + PCBptr + "\n"
						 + "Next PCBptr = " + mainMemory[PCBptr] + "\n"
						 + "PID = " + mainMemory[PCBptr + PID_INDEX] + "\n"
						 + "State = "  + stateTemp + "\n"
						 + "Reason for Waiting = " + reasonTemp + "\n"
						 + "PC = "  + mainMemory[PCBptr + PC_INDEX] + "\n"
						 + "SP = "  + mainMemory[PCBptr + SP_INDEX] + "\n"
						 + "Priority = "  + mainMemory[PCBptr + PRIORITY_INDEX] + "\n"
						 + "Stack Info:" + "\n"
						 + "Start Address = " + mainMemory[PCBptr + STACK_ADDRESS_INDEX] +"\n"
						 + "Size = " + mainMemory[PCBptr + STACK_SIZE_INDEX]);
		// Print PCB GPRs
		System.out.println("GPRs:\n"
				+ "         G0       G1       G2       G3"
				+ "       G4       G5       G6       G7       SP       PC");

		System.out.printf("         %-8d %-8d %-8d %-8d %-8d %-8d %-8d %-8d %-8d %-8d%n",
				mainMemory[PCBptr + GPR_0_INDEX], mainMemory[PCBptr + GPR_1_INDEX], 
				mainMemory[PCBptr + GPR_2_INDEX], mainMemory[PCBptr + GPR_3_INDEX], 
				mainMemory[PCBptr + GPR_4_INDEX], mainMemory[PCBptr + GPR_5_INDEX], 
				mainMemory[PCBptr + GPR_6_INDEX], mainMemory[PCBptr + GPR_7_INDEX],
				mainMemory[PCBptr + SP_INDEX], mainMemory[PCBptr + PC_INDEX]);
		System.out.println("-------------------------------------------------------------------------------------");
	}  // end of PrintPCB() function

	/***********************************************
	 * Function: PrintQueue By: Kyle Abreu
	 * ---------------------------------------------
	 * Task performed:
	 *   Prints the contents of a given queue, which can be either the Ready Queue or the Waiting Queue.
	 *   It walks through the queue from the given pointer until the end of the list, printing each PCB
	 *   as it moves from one PCB to the next.
	 *
	 * Input Parameters:
	 *   Qptr (int) - An integer representing the memory address of the queue to be printed.
	 *  
	 * Output Parameters:
	 *   None - The function prints information to the console and does not return data.
	 *
	 * Function Return Value:
	 *   int - Returns a status code indicating the success or failure of the operation. 
	 *         Possible values are defined by constants:
	 *         - OK: Indicates successful execution.
	 *
	 ************************************************/
	int PrintQueue(int Qptr) {
		// Walk thru the queue from the given pointer until end of list
		// Print each PCB as you move from one PCB to the next

		int currentPCBPtr = Qptr;

		if(currentPCBPtr == EOL) {
			System.out.println("Reached End Of List");
			System.out.println("-------------------------------------------------------------------------------------");
			return(OK);
		}

		// Walk thru the queue
		while(mainMemory[currentPCBPtr] != EOL) {
			System.out.println("Dumping PID: " + mainMemory[currentPCBPtr + PID_INDEX]);
			System.out.println("-------------------------------------------------------------------------------------");
			PrintPCB(currentPCBPtr);
			currentPCBPtr = mainMemory[currentPCBPtr];
		}  // end of while loop

		return (OK);
	}  // end of PrintQueue() function

	/***********************************************
	 * Function: InsertIntoRQ By: Kyle Abreu
	 * ---------------------------------------------
	 * Task performed:
	 *   Inserts a given Process Control Block (PCB) into the Ready Queue (RQ) according to the Priority Round Robin algorithm.
	 *   The Ready Queue is maintained as an ordered list, with the PCB with the highest priority at the front of the queue.
	 *   The function inserts the PCB into the appropriate position in the queue based on its priority.
	 *
	 * Input Parameters:
	 *   PCBptr (int) - An integer representing the memory address of the PCB to be inserted into the Ready Queue.
	 *  
	 * Output Parameters:
	 *   None - The function modifies the RQ directly in memory and does not return data.
	 *
	 * Function Return Value:
	 *   int - Returns a status code indicating the success or failure of the operation. 
	 *         Possible values are defined by constants:
	 *         - OK: Indicates successful insertion into the Ready Queue.
	 *         - INVALID_MEMORY_ADDRESS_ERROR: Indicates an invalid memory address for the PCB.
	 ************************************************/
	int InsertIntoRQ (int PCBptr) {		
		// Insert PCB according to Priority Round Robin algorithm
		// Use priority in the PCB to find the correct place to insert.
		int previousPtr = EOL;
		int currentPtr = RQ;
	
		// Check for invalid PCB memory address
		if((PCBptr < VALID_OS_FREE_MIN) || (PCBptr > VALID_OS_FREE_MAX - PCB_SIZE)) {
			System.err.println("INVALID_MEMORY_ADDRESS_ERROR");
			return(INVALID_MEMORY_ADDRESS_ERROR);
		}
	
		mainMemory[PCBptr + STATE_INDEX] = READY_STATE;		// set state to ready state
		mainMemory[PCBptr] = EOL;	 // set next pointer to end of list
	
		if( RQ == EOL)	{// RQ is empty
			RQ = PCBptr;
			return(OK);
		}
	
		// Walk thru RQ and find the place to insert
		// PCB will be inserted at the end of its priority
	
		while(currentPtr != EOL) {
			if(mainMemory[PCBptr + PRIORITY_INDEX] > mainMemory[currentPtr + PRIORITY_INDEX]) {
				// found the place to insert
				if(previousPtr == EOL) {
					// Enter PCB in the front of the list as first entry
					mainMemory[PCBptr + NEXT_POINTER_INDEX] = RQ;
					RQ = PCBptr;
					return(OK);
				}
				// enter PCB in the middle of the list
				mainMemory[PCBptr + NEXT_POINTER_INDEX] = mainMemory[previousPtr + NEXT_POINTER_INDEX];
				mainMemory[previousPtr + NEXT_POINTER_INDEX] = PCBptr;
				return(OK);
			}
			else  // PCB to be inserted has lower or equal priority to the current PCB in RQ
			{	// go to the next PCB in RQ
				previousPtr = currentPtr;
				currentPtr = mainMemory[currentPtr + NEXT_POINTER_INDEX];
			}
		}  // end of while loop
	
		// Insert PCB at the end of the RQ
		mainMemory[previousPtr + NEXT_POINTER_INDEX] = PCBptr;
	    return(OK);     
	}  // end of InsertIntoRQ() function
	
	/***********************************************
	 * Function: InsertIntoWQ By: Kyle Abreu
	 * ---------------------------------------------
	 * Task performed:
	 *   Inserts a given PCB (Process Control Block) into the Waiting Queue (WQ). 
	 *   This function sets the state of the PCB to waiting and updates the next PCB 
	 *   pointer to indicate the end of the list.
	 *
	 * Input Parameters:
	 *   PCBptr (int) - An integer pointer indicating the position of the PCB in main memory.
	 *   
	 * Output Parameters:
	 *   None - The function modifies the PCB directly in memory and does not explicitly
	 *          return any data, though it does return a status code.
	 *
	 * Function Return Value:
	 *   int - Returns OK to indicate successful insertion into the WQ.
	 *	 - INVALID_MEMORY_ADDRESS_ERROR: Indicates an invalid memory address for the PCB.
	 ************************************************/
	int InsertIntoWQ (int PCBptr) {
		// Insert the given PCB at the front of WQ
	
		// Check for invalid PCB memory address
		if((PCBptr < VALID_OS_FREE_MIN) || (PCBptr > VALID_OS_FREE_MAX - PCB_SIZE)) {
			System.err.println("INVALID_MEMORY_ADDRESS_ERROR");
			return(INVALID_MEMORY_ADDRESS_ERROR);
		 }
		
		mainMemory[PCBptr + STATE_INDEX] = WAITING_STATE;	// set state to waiting state
		mainMemory[PCBptr + NEXT_POINTER_INDEX] = EOL;	 // set next pointer to end of list
		
		WQ = PCBptr;
		
		return(OK);
	}  // end of InsertIntoWQ () function

	/***********************************************
	 * Function: SelectProcessFromRQ By: Kyle Abreu
	 * ---------------------------------------------
	 * Task performed:
	 *   Selects the first process from the Ready Queue (RQ) to allocate CPU.
	 *   Returns the pointer to the selected Process Control Block (PCB).
	 *
	 * Input Parameters:
	 *   None
	 *  
	 * Output Parameters:
	 *   None
	 *
	 * Function Return Value:
	 *   int - Returns an integer representing the memory address of the selected PCB.
	 *         If RQ is empty, returns EOL (End Of List) indicating no process is available.
	 ************************************************/
	int SelectProcessFromRQ()
	{
		//Declare PCBptr as type int and initialize to RQ;  // first entry in RQ
		int PCBptr = RQ;
		if(RQ != EOL) {
		      // Remove first PCB from RQ
		     RQ = mainMemory[RQ + NEXT_POINTER_INDEX];
		}

		// Set next pointer to EOL in the PCB
		if(PCBptr == EOL) {
			return(PCBptr);
		}
		else {
			mainMemory[PCBptr] = EOL;
			return(PCBptr);
		}
		
	}  // end of SelectProcessFromRQ() function

	/***********************************************
	 * Function: SaveContext By: Kyle Abreu
	 * ---------------------------------------------
	 * Task performed:
	 *   Saves the CPU context of the running process into its Process Control Block (PCB).
	 *   CPU context includes General Purpose Registers (GPRs), Stack Pointer (SP), Program Counter (PC), and Processor Status Register (PSR).
	 *
	 * Input Parameters:
	 *   PCBptr (int) - An integer representing the memory address of the PCB of the running process.
	 *  
	 * Output Parameters:
	 *   None - The function modifies the PCB directly in memory and does not return data.
	 *
	 * Function Return Value:
	 *   None
	 ************************************************/
	void SaveContext(int PCBptr)
	{
		// Assume PCBptr is a valid pointer.

		//Copy all CPU GPRs into PCB using PCBptr with or without using loop
		for(int i = 0; i < GPR.length; i++) {
			mainMemory[PCBptr + GPR_0_INDEX + i] = GPR[i];
		}

		//Set SP field in the PCB = SP
		mainMemory[PCBptr + SP_INDEX] = SP;
				
		//Set PC field in the PCB = PC
		mainMemory[PCBptr + PC_INDEX] = PC;
		
		//Set PSR field in the PCB = PSR
		mainMemory[PCBptr + PSR_INDEX] = PSR;

		return;
	}  // end of SaveContext() function

	/***********************************************
	 * Function: Dispatcher By: Kyle Abreu
	 * ---------------------------------------------
	 * Task performed:
	 *   Restores the CPU context from the given Process Control Block (PCB) into the CPU registers.
	 *   This function is responsible for executing the context switch and is often referred to as the Dispatcher.
	 *
	 * Input Parameters:
	 *   PCBptr (int) - An integer representing the memory address of the PCB of the selected process.
	 *  
	 * Output Parameters:
	 *   None - The function modifies the CPU registers directly and does not return data.
	 *
	 * Function Return Value:
	 *   None
	 ************************************************/
	void Dispatcher(int PCBptr)
	{
		// PCBptr is assumed to be correct.

		// Copy CPU GPR register values from given PCB into the CPU registers
		// This is opposite of save CPU context
		for(int i = 0; i < GPR.length; i++) {
			GPR[i] = mainMemory[PCBptr + GPR_0_INDEX + i];
		}
		
		// Restore SP from given PCB
		SP = mainMemory[PCBptr + SP_INDEX];
		
		// Restore PC from given PCB
		PC = mainMemory[PCBptr + PC_INDEX];

		// Set system mode to User mode
		PSR = USER_MODE;
	}  // end of Dispatcher() function
	
	/***********************************************
	 * Function: TerminateProcess By: Kyle Abreu
	 * ---------------------------------------------
	 * Task performed:
	 *   Frees the resources allocated to a process. This includes releasing the stack memory
	 *   and the memory allocated for the Process Control Block (PCB) itself.
	 *
	 * Input Parameters:
	 *   PCBptr (int) - An integer pointer indicating the position of the PCB in main memory.
	 *
	 * Output Parameters:
	 *   None - The function does not return any data.
	 *
	 * Function Return Value:
	 *   void - No return value as the function performs operations directly on memory.
	 ************************************************/
	void TerminateProcess (int PCBptr)
	{
		// Return stack memory using stack start address and stack size in the given PCB
		FreeUserMemory(mainMemory[PCBptr + STACK_ADDRESS_INDEX], mainMemory[PCBptr + STACK_SIZE_INDEX]);

		// Return PCB memory using the PCBptr
		FreeOSMemory(PCBptr, PCB_SIZE);

		return;
	}  // end of TerminateProcess function()

	/***********************************************
	 * Function: AllocateOSMemory By: Kyle Abreu
	 * ---------------------------------------------
	 * Task performed:
	 *   Allocates memory from the Operating System (OS) free space, which is organized as a linked list.
	 *
	 * Input Parameters:
	 *   RequestedSize (int) - The size of memory requested by the caller.
	 *  
	 * Output Parameters:
	 *   None
	 *
	 * Function Return Value:
	 *   int - Returns an integer representing the memory address of the allocated memory block.
	 *         If memory allocation fails due to full OS memory or invalid size, returns an error code.
	 *         Possible error codes are defined as constants:
	 *         - OS_MEMORY_FULL_ERROR: Indicates that the OS memory is full.
	 *         - INVALID_SIZE_ERROR: Indicates that the requested memory size is invalid.
	 ************************************************/
	int AllocateOSMemory (int RequestedSize) {  // return value contains address or error
		// Allocate memory from OS free space, which is organized as link list
		final int OS_MEMORY_FULL_ERROR = -1;
		final int INVALID_SIZE_ERROR = -2;

		if(OSFreeList == EOL) {
			//display no free OS memory error;
			System.err.println("OS_MEMORY_FULL_ERROR");
			return(OS_MEMORY_FULL_ERROR);   // ErrorNoFreeMemory is constant set to < 0
		}
		if(RequestedSize <= 0) {
			//display invalid size error;
			System.err.println("INVALID_SIZE_ERROR");
			return(INVALID_SIZE_ERROR);  // ErrorInvalidMemorySize is constant < 0
		}
		if(RequestedSize == 1) {
			RequestedSize = 2;  // Minimum allocated memory is 2 locations
		}
	
		int CurrentPtr = OSFreeList;
		int PreviousPtr = EOL;
		
		while (CurrentPtr != EOL) {
			// Check each block in the link list until block with requested memory size is found
			if(mainMemory[CurrentPtr + 1] == RequestedSize)
			{  // Found block with requested size.  Adjust pointers
				if(CurrentPtr == OSFreeList)  // first block
				{	
					OSFreeList = mainMemory[CurrentPtr]; // first entry is pointer to next block
					mainMemory[CurrentPtr] = EOL;  // reset next pointer in the allocated block
					return(CurrentPtr);	// return memory address
				}
				else  // not first black
				{
					mainMemory[PreviousPtr] = mainMemory[CurrentPtr];  // point to next block
					mainMemory[CurrentPtr] = EOL;  // reset next pointer in the allocated block
					return(CurrentPtr);    // return memory address
				}
			}
			else if(mainMemory[CurrentPtr + 1] > RequestedSize)
			{  // Found block with size greater than requested size
				if(CurrentPtr == OSFreeList)  // first block
				{
					mainMemory[CurrentPtr + RequestedSize] = mainMemory[CurrentPtr];  // move next block ptr
					mainMemory[CurrentPtr + RequestedSize + 1] = mainMemory[CurrentPtr + 1] - RequestedSize;
					OSFreeList = CurrentPtr + RequestedSize;  // address of reduced block
					mainMemory[CurrentPtr] = CurrentPtr + RequestedSize;  // reset next pointer in the allocated block
					return(CurrentPtr);	// return memory address
				}
				else  // not first block
				{
					mainMemory[CurrentPtr + RequestedSize] = mainMemory[CurrentPtr];  // move next block ptr
					mainMemory[CurrentPtr + RequestedSize + 1] = mainMemory[CurrentPtr + 1] - RequestedSize;
					mainMemory[PreviousPtr] = CurrentPtr + RequestedSize;  // address of reduced block
					mainMemory[CurrentPtr] = EOL;  // reset next pointer in the allocated block
					return(CurrentPtr);	// return memory address
				}
			}
			else  // small block 
			{  // look at next block
				PreviousPtr = CurrentPtr;
				CurrentPtr = mainMemory[CurrentPtr];
			}
		} // end of while CurrentPtr loop
	
		//display no free OS memory error;
		System.err.println("OS_MEMORY_FULL_ERROR");
		return(OS_MEMORY_FULL_ERROR);   // ErrorNoFreeMemory is constant set to < 0
	}  // end of AllocateOSMemory() function

	/***********************************************
	 * Function: FreeOSMemory By: Kyle Abreu
	 * ---------------------------------------------
	 * Task performed:
	 *   Frees a block of memory in the operating system's space. The function inserts the 
	 *   freed block back into the OS free list, which is organized as a linked list.
	 *
	 * Input Parameters:
	 *   PCBptr (int) - An integer pointer indicating the position of the block in OS memory to be freed.
	 *   size (int) - The size of the memory block to be freed.
	 *   
	 * Output Parameters:
	 *   None - The function modifies the OS free list directly and does not return data.
	 *
	 * Function Return Value:
	 *   int - Returns OK if the memory was successfully freed, or an error code if the 
	 *         memory address is invalid or the size is incorrect.
	 *   	 - INVALID_MEMORY_ADDRESS_ERROR - Returned if the PCBptr is outside the valid OS memory range.
	 *   	 - INVALID_SIZE_OR_MEMORY_ADDRESS_ERROR - Returned if the requested size is invalid or
	 *                                          the PCBptr with size exceeds the valid OS memory range.
	 ************************************************/
	int FreeOSMemory (int PCBptr, int size) { // return value contains OK or error code
		if (PCBptr < VALID_OS_FREE_MIN || PCBptr > VALID_OS_FREE_MAX) {	// Address range is given in the class
	    //display invalid address error message;
	    	 System.err.println("INVALID_MEMORY_ADDRESS_ERROR");
	    	 return(INVALID_MEMORY_ADDRESS_ERROR);  // ErrorInvalidMemoryAddress is constantset to  < 0
	    }
	    
	    if(size == 1) { // check for minimum allocated size, which is 2 even if user asks for 1 location
	    	size = 2;  // minimum allocated size
	    }
	    else if(size < 1 || (PCBptr + size) <  VALID_OS_FREE_MIN || (PCBptr + size) >= VALID_OS_FREE_MAX) {
		// display invalid size or address error message
   	 		System.err.println("INVALID_SIZE_OR_MEMORY_ADDRESS_ERROR");
   	 		return(INVALID_SIZE_OR_MEMORY_ADDRESS_ERROR);
	    } 

		// Return memory to OS free space.  Insert at the beginning of the link list
	    // Insert the given free block at the beginning of the OS free list
	    //Make the given free block point to free block pointed by OS free List;
	    mainMemory[PCBptr] = OSFreeList;
	    //Set the free block size in the given free block;
	    mainMemory[PCBptr + STACK_SIZE_INDEX] = size;
	    //Set OS Free List point to the given free block;
	    OSFreeList = PCBptr;
	    return (OK);
	}  // end of FreeOSMemory() function

	/***********************************************
	 * Function: AllocateUserMemory By: Kyle Abreu
	 * ---------------------------------------------
	 * Task performed:
	 *   Attempts to allocate a block of memory of the requested size from the user free space,
	 *   which is managed as a linked list. If successful, returns the address of the allocated
	 *   block. Otherwise, returns an error code.
	 *
	 * Input Parameters:
	 *   RequestedSize (int) - The size of the memory block requested by the user.
	 *   
	 * Output Parameters:
	 *   None - The function returns an integer representing either the start address of the
	 *          allocated block or an error code.
	 *
	 * Function Return Value:
	 *   int - The start address of the allocated memory block if successful, or an error code
	 *         if there is no sufficient free memory or if the requested size is invalid.
	 *   	 - USER_MEMORY_FULL_ERROR - Returned if there is no sufficient free memory to satisfy the request.
	 *   	 - INVALID_SIZE_ERROR - Returned if the requested size is zero or negative.
	 ************************************************/
	int AllocateUserMemory (int RequestedSize) { // return value contains address or error code
	
		// Allocate memory from User free space, which is organized as link list
		final int USER_MEMORY_FULL_ERROR = -1;
		final int INVALID_SIZE_ERROR = -2;

		if(UserFreeList == EOL) {
			//display no free User memory error;
			System.err.println("USER_MEMORY_FULL_ERROR");
			return(USER_MEMORY_FULL_ERROR);   // ErrorNoFreeMemory is constant set to < 0
		}
		if(RequestedSize <= 0) {
			//display invalid size error;
			System.err.println("INVALID_SIZE_ERROR");
			return(INVALID_SIZE_ERROR);  // ErrorInvalidMemorySize is constant < 0
		}
		if(RequestedSize == 1) {
			RequestedSize = 2;  // Minimum allocated memory is 2 locations
		}
	
		int CurrentPtr = UserFreeList;
		int PreviousPtr = EOL;
		
		while (CurrentPtr != EOL) {
			// Check each block in the link list until block with requested memory size is found
			if(mainMemory[CurrentPtr + 1] == RequestedSize)
			{  // Found block with requested size.  Adjust pointers
				if(CurrentPtr == UserFreeList)  // first block
				{	
					UserFreeList = mainMemory[CurrentPtr]; // first entry is pointer to next block
					mainMemory[CurrentPtr] = EOL;  // reset next pointer in the allocated block
					return(CurrentPtr);	// return memory address
				}
				else  // not first black
				{
					mainMemory[PreviousPtr] = mainMemory[CurrentPtr];  // point to next block
					mainMemory[CurrentPtr] = EOL;  // reset next pointer in the allocated block
					return(CurrentPtr);    // return memory address
				}
			}
			else if(mainMemory[CurrentPtr + 1] > RequestedSize)
			{  // Found block with size greater than requested size
				if(CurrentPtr == UserFreeList)  // first block
				{
					mainMemory[CurrentPtr + RequestedSize] = mainMemory[CurrentPtr];  // move next block ptr
					mainMemory[CurrentPtr + RequestedSize + 1] = mainMemory[CurrentPtr + 1] - RequestedSize;
					UserFreeList = CurrentPtr + RequestedSize;  // address of reduced block
					mainMemory[CurrentPtr] = EOL;  // reset next pointer in the allocated block
					return(CurrentPtr);	// return memory address
				}
				else  // not first block
				{
					mainMemory[CurrentPtr + RequestedSize] = mainMemory[CurrentPtr];  // move next block ptr
					mainMemory[CurrentPtr + RequestedSize + 1] = mainMemory[CurrentPtr + 1] - RequestedSize;
					mainMemory[PreviousPtr] = CurrentPtr + RequestedSize;  // address of reduced block
					mainMemory[CurrentPtr] = EOL;  // reset next pointer in the allocated block
					return(CurrentPtr);	// return memory address
				}
			}
			else  // small block 
			{  // look at next block
				PreviousPtr = CurrentPtr;
				CurrentPtr = mainMemory[CurrentPtr];
			}
		} // end of while CurrentPtr loop
	
		//display no free User memory error;
		System.err.println("USER_MEMORY_FULL_ERROR");
		return(USER_MEMORY_FULL_ERROR);   // ErrorNoFreeMemory is constant set to < 0
	}  // end of AllocateUserMemory() function

	/***********************************************
	 * Function: FreeUserMemory By: Kyle Abreu
	 * ---------------------------------------------
	 * Task performed:
	 *   Returns memory to the User free space and inserts the returned free block at the beginning of the linked list.
	 *
	 * Input Parameters:
	 *   PCBptr (int) - An integer representing the memory address of the block to be freed.
	 *   size (int) - The size of the block to be freed.
	 *  
	 * Output Parameters:
	 *   None
	 *
	 * Function Return Value:
	 *   int - Returns an integer representing the status of the operation:
	 *         - OK: Indicates successful memory deallocation.
	 *         - INVALID_MEMORY_ADDRESS_ERROR: Indicates an invalid memory address error.
	 *         - INVALID_SIZE_OR_MEMORY_ADDRESS_ERROR: Indicates an invalid size or memory address error.
	 ************************************************/
	int FreeUserMemory (int PCBptr, int size) {  // return value contains OK or error code
	// Return memory to User free space.  
	// Insert the returned free block at the beginning of the link list

	     if (PCBptr < VALID_USER_FREE_MIN || PCBptr > VALID_USER_FREE_MAX) { // user memory area is given in the class
		//display invalid address error message
	    	 System.err.println("INVALID_MEMORY_ADDRESS_ERROR");
	    	 return(INVALID_MEMORY_ADDRESS_ERROR);
	     }
		// Check for invalid size and minimum size
	     if(size == 1) { // check for minimum allocated size, which is 2, even if user asks for 1 location
	     
	    	 size = 2;  // minimum allocated size
	     }
	     else if(size < 1 || (PCBptr + size) <  VALID_USER_FREE_MIN || (PCBptr + size) >= VALID_USER_FREE_MAX ) {
		//display invalid size or address error message
	    	 System.err.println("INVALID_SIZE_OR_MEMORY_ADDRESS_ERROR");
	    	 return(INVALID_SIZE_OR_MEMORY_ADDRESS_ERROR);
	     } 
		// Insert the free block at the beginning of the link list
	    //Make the given free block point to free block pointed by User free List;
	    mainMemory[PCBptr] = UserFreeList;
	    //Set the free block size in the given free block;
	    mainMemory[PCBptr + STACK_SIZE_INDEX] = size;
	    //Set User Free List point to the given free block;
	    UserFreeList = PCBptr;

	    return (OK);
	}  // end of FreeUserMemory() function

	/***********************************************
	 * Function: CheckAndProcessInterrupt By: Kyle Abreu
	 * ---------------------------------------------
	 * Task performed:
	 *   Reads an interrupt ID number from the user input and processes the interrupt based on the ID.
	 *
	 * Input Parameters:
	 *   None
	 *  
	 * Output Parameters:
	 *   None
	 *
	 * Function Return Value:
	 *   void - No return value as the function processes interrupts directly.
	 *   	  - Displays an error message for an invalid interrupt ID.
	 ************************************************/
	void CheckAndProcessInterrupt() {
		// Prompt and read interrupt ID
		System.out.println("Enter interrupt ID:\n"
						 + "0 � no interrupt\n"
						 + "1 � run program\n"
						 + "2 � shutdown system\n"
						 + "3 � Input operation completion (io_getc)\n"
						 + "4 � Output operation completion (io_putc)");
		// Read interrupt ID
		String interruptID  = input.nextLine();
		
		// Display the interrupt value that was read
		System.out.println("Interrupt Selected: " + interruptID);
		
		// Process interrupt
		switch(interruptID)
		{
			case "0":  	// No interrupt
				break;

			case "1": 	// Run program
				//call ISR run Program Interrupt function;
				ISRrunProgramInterrupt();
				break;

			case "2": 	// Shutdown system
				//call ISR shutdown System Interrupt function
				ISRshutdownSystem();
				//set system shutdown status in a global variable to check in main and exit
				status = SHUTDOWN;
				break; 

			case "3": 	// Input operation completion � io_getc
				//call ISR input Completion Interrupt function
				ISRinputCompletionInterrupt();
				break;

			case "4": 	// Output operation completion � io_putc
				//call ISR output Completion Interrupt function
				ISRoutputCompletionInterrupt();
				break;

			default:		// Invalid Interrupt ID
				//Display invalid interrupt ID message
				System.out.println("Invalid interrupt ID");
				break;
		}  // end of switch InterruptID
	}  // end of CheckAndProcessInterrupt() function

	/***********************************************
	 * Function: ISRrunProgramInterrupt By: Kyle Abreu
	 * Run Program Interrupt Service Routine (ISR)
	 * ---------------------------------------------
	 * Task performed:
	 *   Reads a filename from the user input and creates a process using the specified filename.
	 *
	 * Input Parameters:
	 *   None
	 *  
	 * Output Parameters:
	 *   None
	 *
	 * Function Return Value:
	 *   void - No return value as the function creates a process directly.
	 ************************************************/
	void ISRrunProgramInterrupt() {
		// Prompt and read filename
		
		System.out.println("Enter the executable filename: ");
		
		//Set filename to next line
		String filename = input.nextLine() + ".txt";
		
		//Call Create Process passing filename and Default Priority as arguments;
		CreateProcess(filename, DEFAULT_PRIORITY);
	}  // end of ISRrunProgram() function

	/************************************************
	 * Function: ISRinputCompletionInterrupt By: Kyle Abreu
	 * -------------------------------------
	 * Task performed:
	 *   Handles the input completion interrupt by reading the PID of the process completing the io_getc operation
	 *   and reading one character from the keyboard (input device). Stores the character in the General Purpose 
	 *   Register (GPR) in the Process Control Block (PCB) of the process.
	 *
	 * Input Parameters:
	 *   None
	 *  
	 * Output Parameters:
	 *   None
	 *
	 * Function Return Value:
	 *   void - No return value as the function handles the interrupt directly.
	 *        - Prints an error message if the provided PID does not match any process in the Waiting Queue (WQ) or Ready Queue (RQ).
	 ************************************************/
	void ISRinputCompletionInterrupt() {
		//Prompt and read PID of the process completing input completion interrupt;
		System.out.println("Enter PID of the process completing input completion interrupt: ");
		PID = input.nextInt();
		
		
		//Search WQ to find the PCB having the given PID
		int PCBptr = SearchAndRemovePCBfromWQ(PID);
		
		if (PCBptr != EOL) {
			//Read one character from standard input device keyboard;
			System.out.println("Enter one character: ");
			int c = (int) input.next().charAt(0);
			
			//Store the character in the GPR in the PCB
			// type cast char to int
			mainMemory[PCBptr + GPR_1_INDEX] = c;
			
			//Set process state to Ready in the PCB;
			mainMemory[PCBptr + STATE_INDEX] = READY_STATE;
			
			//Insert PCB into RQ;
			InsertIntoRQ(PCBptr);
			return;
		}
		
		//If no matching PCB is found in WQ and RQ, print invalid pid as error message;
		System.err.println("INVALID_PID_ERROR");
		return;
	}  // end of ISRinputCompletionInterrupt() function

	/************************************************
	 * Function: ISRoutputCompletionInterrupt By: Kyle Abreu
	 * --------------------------------------
	 * Task performed:
	 *   Handles the output completion interrupt by reading the PID of the process completing the io_putc operation
	 *   and displaying one character on the monitor (output device) from the GPR in the PCB of the process.
	 *
	 * Input Parameters:
	 *   None
	 *  
	 * Output Parameters:
	 *   None
	 *
	 * Function Return Value:
	 *   void - No return value as the function handles the interrupt.
	 *        - Prints an error message if no matching PCB is found in both WQ and RQ for the provided PID.
	 ************************************************/
	void ISRoutputCompletionInterrupt()
	{
		//Prompt and read PID of the process completing output completion interrupt;
		System.out.println("Enter PID of the process completing output completion interrupt: ");
		PID = input.nextInt();

		//Search WQ to find the PCB having the given PID
		int PCBptr = SearchAndRemovePCBfromWQ(PID);

		if(PCBptr != EOL){
			//Remove PCB from the WQ;
			//Print the character in the GPR in the PCB;
			System.out.println("Character: " + (char) mainMemory[PCBptr + GPR_1_INDEX]);
			
			//Set process state to Ready in the PCB;
			mainMemory[PCBptr + STATE_INDEX] = READY_STATE;
			
			//Insert PCB into RQ;
			InsertIntoRQ(PCBptr);
		}

		//If no matching PCB is found in WQ and RQ, print invalid pid as error message;
		System.err.println("INVALID_PID_ERROR");
		return;
	}  // end of ISRonputCompletionInterrupt() function

	/************************************************
	 * Function: ISRshutdownSystem By: Kyle Abreu
	 * ---------------------------
	 * Task performed:
	 *   Handles the shutdown system interrupt by terminating all processes in the Ready Queue (RQ) and Waiting Queue (WQ)
	 *   and exits from the program. This is the only place that the operating system program should exit.
	 *
	 * Input Parameters:
	 *   None
	 *  
	 * Output Parameters:
	 *   None
	 *
	 * Function Return Value:
	 *   void - No return value as the function terminates processes and exits the program.
	 ************************************************/
	void ISRshutdownSystem()
	{
		// Terminate all processes in RQ one by one
		int PCBptr = RQ;	// set ptr to first PCB pointed by RQ
		while(PCBptr != EOL) {
			RQ = mainMemory[PCBptr];
			// Call Terminate Process passing ptr as argument;
			TerminateProcess(PCBptr);
			PCBptr = RQ;
		}
		// Terminate all processed in WQ one by one

		PCBptr = WQ;	// set ptr to first PCB pointed by RQ
		while(PCBptr != EOL) {
			WQ = mainMemory[PCBptr];
			// Call Terminate Process passing ptr as argument;
			TerminateProcess(PCBptr);
			PCBptr = WQ;
		}
		return;
	}  // end of ISRshutdownSystem() function

	/***********************************************
	 * Function: SearchAndRemovePCBfromWQ By: Kyle Abreu
	 * ---------------------------------------------
	 * Task performed:
	 *   Searches the Waiting Queue (WQ) for a PCB with the provided PID. If found, removes the PCB from WQ and 
	 *   returns its pointer. If no match is found, returns an error code indicating that the PID was not found.
	 *
	 * Input Parameters:
	 *   int pid - The PID of the process to be searched in the Waiting Queue (WQ).
	 *  
	 * Output Parameters:
	 *   None
	 *
	 * Function Return Value:
	 *   int - Returns the pointer to the PCB if the PID is found in WQ. Returns EOL (End of List) code if no match 
	 *         is found, indicating that the PID was not found.
	 *       - Prints an error message if the PID is not found in WQ.
	 ***********************************************/
	int SearchAndRemovePCBfromWQ (int pid)
	{
		int currentPCBptr = WQ;
		int previousPCBptr = EOL;

		// Search WQ for a PCB that has the given pid
		// If a match is found, remove it from WQ and return the PCB pointer
		while (currentPCBptr != EOL)
		{
			if(mainMemory[currentPCBptr + PID_INDEX] == pid)
			{
				// match found, remove from WQ
				if(previousPCBptr == EOL)
				{	// first PCB
					WQ = mainMemory[currentPCBptr];
				}
				else
				{	// not first PCB
					mainMemory[previousPCBptr] = mainMemory[currentPCBptr];
				}
				mainMemory[currentPCBptr] = EOL;
				return(currentPCBptr);
			}
			previousPCBptr = currentPCBptr;
			currentPCBptr = mainMemory[currentPCBptr];
		}  // end while currentPCBptr

		// No matching PCB is found, display pid message and return End of List code
		//Display pid not found message;
		System.err.println("PID not found in WQ");

		return (EOL);
	}  // SearchAndRemovePCBfromWQ
	
	/***********************************************
	 * Function: InitializeSystem By: Kyle Abreu
	 * ---------------------------------------------
	 * Task performed:
	 *	Revised initialize system
	 *	Modify initialize system function to create OS free list, 
	 *	user free list, and null process with lowest priority of zero.
	 *   Initializes the system by setting up main memory, CPU registers, OS free list, user free list, and creating 
	 *   the null process with the lowest priority of zero.
	 *
	 * Input Parameters:
	 *   None
	 *  
	 * Output Parameters:
	 *   None
	 *
	 * Function Return Value:
	 *   None
	 /***********************************************/
	void InitializeSystem() {
		// Initialize all hardware component to zero: Main memory and CPU registers
			mainMemory = new int[10000];
			GPR = new int[8];
			MAR = 0;
			MBR = 0;
			CLOCK = 0;
			IR = 0;
			PSR = 0;
			PC = 0;
			SP = 0;
			
			PID = 1;
			runningPCBptr = 0;
			status = 0;
			
			// Initialize all mainMemory locations to 0
			for(int i = 0; i < mainMemory.length; i++) 
			{
				mainMemory[i] = 0;
			}

			// Initialize all GPR locations to 0		
			for(int i = 0; i < GPR.length; i++) 
			{
				GPR[i] = 0;
			}

			// Create User free list using the free block address and size
			// Set User Free List = start address
			UserFreeList = VALID_USER_FREE_MIN;
			// Set the next user free block pointer = End Of List
			mainMemory[VALID_USER_FREE_MIN] = EOL;
			// Set second location in the free block = size of free block
			mainMemory[3001] = 3000;
			
			// Create OS free list using the free block address and size
			// Set OS Free List = start address given in the class
			OSFreeList = VALID_OS_FREE_MIN;
			// Set next OS free block pointer = End Of List
			mainMemory[VALID_OS_FREE_MIN] = EOL;
			// Set second location in the free block = size of free block
			mainMemory[6001] = 4000;

			//	Call Create Process function passing Null Process Executable File and priority zero as arguments
			CreateProcess("NULL_Process_EXE.txt", 0);

	}  // end InitializeSystem() function

	public static void main(String[] Args) {
		OS os = new OS();
		int mainStatus = os.OS_Main();
		if(mainStatus == -1) {
			System.out.println("OS Status: SHUTDOWN");
		}
		else {
			System.out.println("OS Status: " + mainStatus);
		}
	}
	
	/***********************************************
	 * Function: OS_Main By: Kyle Abreu
	 * ---------------------------------------------
	 * Task performed:
	 *   Serves as the main control loop for the operating system. Manages process scheduling,
	 *   memory allocation, and interrupt handling until the system is shut down.
	 *
	 * Input Parameters:
	 *   None - The function is called without parameters.
	 *   
	 * Output Parameters:
	 *   None - The function prints status messages and errors to the console but does not return data.
	 *
	 * Function Return Value:
	 *   int - Returns a status code indicating the reason for termination, such as SHUTDOWN, or
	 *         an error code in the case of an unexpected system error.
	 *       - SHUTDOWN - Returned when the operating system is shutting down normally.
	 *       - UNKNOWN_SYSTEM_ERROR - Returned if an undefined error occurs during operation, 
	 *                                typically indicating an error that does not match any known error condition.
	 *       - TIMESLICE_EXPIRED - Returned when the currently running process's time slice has expired.
	 *       - Any negative value other than UNKNOWN_SYSTEM_ERROR - Indicates a halt or runtime error that 
	 *         has caused the process to terminate unexpectedly.
	 *       - INPUT_START_EVENT - Indicates that an input event has started, causing the current process 
	 *                             to wait for its completion.
	 *       - OUTPUT_START_EVENT - Indicates that an output event has started, causing the current process 
	 *                              to wait for its completion.
	 ************************************************/
	int OS_Main() {
		final int UNKNOWN_SYSTEM_ERROR = -404;
		final int TIMESLICE_EXPIRED = 0;
		
		// Run until shutdown

		InitializeSystem();

		while (status != SHUTDOWN) {
			// Check and process interrupt
			// Call Check and Process Interrupt function and store return status
			CheckAndProcessInterrupt();
			
			if(status == SHUTDOWN) { //interrupt is shutdown, exit from main;
				return(SHUTDOWN);
			}
			// Dump RQ and WQ
			System.out.println("Dumping RQ:");
			PrintQueue(RQ);
			System.out.println("Dumping WQ:");
			PrintQueue(WQ);
			
			// Dump Memory
			DumpMemory("Dynamic Memory Area before CPU scheduling: ", VALID_USER_FREE_MIN, 250);
			DumpMemory("Dump OS: ", VALID_OS_FREE_MIN, 250);
	
			// Select next process from RQ to give CPU
			runningPCBptr = SelectProcessFromRQ();  // call the function
	
			// Perform restore context using Dispatcher
			// Call Dispatcher function with Running PCB ptr as argument
			Dispatcher(runningPCBptr);
	
			// Dump RQ
			System.out.println("RQ after selecting process from RQ:");
			PrintQueue(RQ);
			
			// Dump running PCB
			PrintPCB(runningPCBptr);
	
			// Execute instructions of the running process using the CPU
			status = CPUexecuteProgram();  // call the function
	
			// Dump dynamic memory area
			DumpMemory("After execute program: ", VALID_USER_FREE_MIN, 250);
			DumpMemory("Dump OS: ", VALID_OS_FREE_MIN, 250);
	
			// Check return status � reason for giving up CPU
			if(status == TIMESLICE_EXPIRED)
			{
				//Save CPU Context
				SaveContext(runningPCBptr);
				
				//Insert running process PCB into RQ;
				InsertIntoRQ(runningPCBptr);
				//Set Running PCB ptr = End Of List;
				runningPCBptr = EOL;
			}
			else if(status < 0)  // Halt or run-time error
			{
				//Terminate running Process;
				TerminateProcess(runningPCBptr);
				//Set Running PCB ptr = End Of List ;
				runningPCBptr = EOL;
			}
			else if(status == INPUT_START_EVENT)		// io_getc
			{
				//Set reason for waiting in the running PCB to Input Completion Event;
				mainMemory[runningPCBptr + WAITING_CODE_INDEX] = INPUT_COMPLETION_EVENT;
				InsertIntoWQ(runningPCBptr);
				runningPCBptr = EOL;
			}
			else if(status == OUTPUT_START_EVENT)		// io_putc
			{
				//Set reason for waiting in the running PCB to Output Completion Event;
				mainMemory[runningPCBptr + WAITING_CODE_INDEX] = OUTPUT_COMPLETION_EVENT;
				InsertIntoWQ(runningPCBptr);
				runningPCBptr = EOL;
			}
			else
			{	// Unknown programming error
				System.err.println("UNKNOWN_SYSTEM_ERROR");
				return(UNKNOWN_SYSTEM_ERROR);
			}
		}  // end of while not shutdown loop

		//Print OS is shutting down message;
		System.out.println("OS is Shutting Down");
		return(status);  // Terminate Operating System
	}  // end of main function
	 
	//************************************************************
	//Function: CPUexecuteProgram By: Kyle Abreu
	//
	//Task Description:
	//Fetch Cycle:
	//Fetch (read) the first word of the instruction pointed by PC into MBR
	//Instruction needing more words (2 word and 3 word instructions) are fetched 
	//based on instruction (opCode) 
	//when the operand 1 and operand 2 values are fetched using modes
	//	
	//Decode Cycle:	
	//Decode the first word of the instruction into opCode, 
	//operand 1 mode and operand 1 GPR and operand 2 mode and operand 2 GPR
	//using integer division and modulo operators
	//Five fields in the first word of any instruction is:
	//opCode, Operand 1 mode, operand 1 GPR, Operand 2 mode, Operand 2 GPR
	//
	//Execute Cycle:
	//In the execute cycle, fetch operand value(s) based on the opcode 
	//since different opcode has different number of operands
	//Halt, Branch and System Call instructions have no operands
	//Push and Pop instructions have 1 operand
	//Conditional branch instructions have one operand
	//Add, Subtract, Multiply, Divide and Move instructions have 2 operands
	//System call has no operand
	//
	//Input Parameters:
	//None
	//	
	//Output parameters:
	//None
	//
	//Function Return Value:
	//		 0 PSR									OK
	//		-1 INVALID_ADDRESS_ERROR				Invalid pc in fetch cycle
	//		-2 OPCODE_ERROR							Invalid opcode
	//		-3 OP1_MODE_ERROR						op1Mode < 0 or op1Mode > 6
	//		-4 OP1_GPR_ERROR						op2GPR < 0 or op2GPR > 7
	//		-5 OP2_MODE_ERROR						op2Mode < 0 or op2Mode > 6
	//		-6 OP2_GPR_ERROR						op2GPR < 0 or op2GPR > 7
	//		-7 HALT									Halt process
	//		-8 OP1_FETCH_ERROR_IN_ADD				Error fetching op1 in add
	//		-9 OP2_FETCH_ERROR_IN_ADD				Error fetching op2 in add
	//		-10 IMMEDIATE_MODE_ERROR_IN_ADD			Error storing sum
	//		-11 OP1_FETCH_ERROR_IN_SUBTRACT			Error fetching op1 in subtract
	//		-12 OP2_FETCH_ERROR_IN_SUBTRACT			Error fetching op2 in subtract
	//		-13 IMMEDIATE_MODE_ERROR_IN_SUBTRACT	Error storing difference
	//		-14 OP1_FETCH_ERROR_IN_MULTIPLY			Error fetching op1 in subtract
	//		-15 OP2_FETCH_ERROR_IN_MULTIPLY			Error fetching op2 in subtract
	//		-16 IMMEDIATE_MODE_ERROR_IN_MULTIPLY	Error storing product
	//		-17 OP1_FETCH_ERROR_IN_DIVIDE			Error fetching op1 in subtract
	//		-18 OP2_FETCH_ERROR_IN_DIVIDE			Error fetching op2 in subtract
	//		-19 IMMEDIATE_MODE_ERROR_IN_DIVIDE		Error storing quotient
	//		-20 DIVIDE_BY_ZERO_ERROR				Cannot divide by zero
	//		-21 OP1_FETCH_ERROR_IN_MOVE				Error fetching op1 in subtract
	//		-22 OP2_FETCH_ERROR_IN_MOVE				Error fetching op2 in subtract
	//		-23 IMMEDIATE_MODE_ERROR_IN_MOVE		Error storing moved value
	//		-24 INVALID_PC_ERROR_IN_B				Invalid pc in branch
	//		-25 OP1_FETCH_ERROR_IN_BM				Error fetching op1 in branch on minus
	//		-26 INVALID_PC_ERROR_IN_BM				Invalid pc in branch on minus
	//		-27 OP1_FETCH_ERROR_IN_BP				Error fetching op1 in branch on plus
	//		-28 INVALID_PC_ERROR_IN_BP				Invalid pc in branch on plus
	//		-29 OP1_FETCH_ERROR_IN_BZ				Error fetching op1 in branch on zero
	//		-30 INVALID_PC_ERROR_IN_BZ				Invalid pc in branch on zero
	//		-31 OP1_FETCH_ERROR_IN_PUSH				Error fetching op1 in push
	//		-32 STACK_OVERFLOW_ERROR				PC is at top of stack
	//		-33 OP1_FETCH_ERROR_IN_POP				Error fetching op1 in pop
	//		-34 STACK_UNDERFLOW_ERROR				PC is at bottom of stack
	//		-35	INVALID_PC_ERROR_IN_SYSCALL			PC Error in Syscall
	//		-36 INVALID_OPCODE_ERROR_IN_DEFAULT		default switch case
	//************************************************************
	int CPUexecuteProgram() {
		// Error codes
		final int INVALID_ADDRESS_ERROR = -1;
		final int OPCODE_ERROR = -2;
		final int OP1_MODE_ERROR = -3;
		final int OP1_GPR_ERROR = -4;
		final int OP2_MODE_ERROR = -5;
		final int OP2_GPR_ERROR = -6;
		final int HALT = -7;
		final int OP1_FETCH_ERROR_IN_ADD = -8;
		final int OP2_FETCH_ERROR_IN_ADD = -9;
		final int IMMEDIATE_MODE_ERROR_IN_ADD = -10;
		final int OP1_FETCH_ERROR_IN_SUBTRACT = -11;
		final int OP2_FETCH_ERROR_IN_SUBTRACT = -12;
		final int IMMEDIATE_MODE_ERROR_IN_SUBTRACT = -13;
		final int OP1_FETCH_ERROR_IN_MULTIPLY = -14;
		final int OP2_FETCH_ERROR_IN_MULTIPLY = -15;
		final int IMMEDIATE_MODE_ERROR_IN_MULTIPLY = -16;
		final int OP1_FETCH_ERROR_IN_DIVIDE = -17;
		final int OP2_FETCH_ERROR_IN_DIVIDE = -18;
		final int IMMEDIATE_MODE_ERROR_IN_DIVIDE = -19;
		final int DIVIDE_BY_ZERO_ERROR = -20;
		final int OP1_FETCH_ERROR_IN_MOVE = -21;
		final int OP2_FETCH_ERROR_IN_MOVE = -22;
		final int IMMEDIATE_MODE_ERROR_IN_MOVE = -23;
		final int INVALID_PC_ERROR_IN_B = -24;
		final int OP1_FETCH_ERROR_IN_BM = -25;
		final int INVALID_PC_ERROR_IN_BM = -26;
		final int OP1_FETCH_ERROR_IN_BP = -27;
		final int INVALID_PC_ERROR_IN_BP = -28;
		final int OP1_FETCH_ERROR_IN_BZ = -29;
		final int INVALID_PC_ERROR_IN_BZ = -30;
		final int OP1_FETCH_ERROR_IN_PUSH = -31;
		final int STACK_OVERFLOW_ERROR = -32;
		final int OP1_FETCH_ERROR_IN_POP = -33;
		final int STACK_UNDERFLOW_ERROR = -34;
		final int INVALID_PC_ERROR_IN_SYSCALL = -35;
		final int INVALID_OPCODE_ERROR_IN_DEFAULT = -36;
		
		// Local Variables		
		int opCode = 0;
		int remainder = 0;
		int op1Mode = 0;
		int op1GPR = 0;
		int op2Mode = 0;
		int op2GPR = 0;
		int op1Value = 0;
		int op2Value = 0;

		// Time
		int TIME_LEFT = TIME_SLICE;  // Timeslice is a constant set to 200 clock ticks
		
		// While not Halt
		while (PC != 0 && status >= 0 && TIME_LEFT > 0) {
	    	  
			// Fetch Cycle
			
			if((VALID_PROGRAM_MIN <= PC && PC <= VALID_PROGRAM_MAX)) { // PC to size of program
				// Set MAR to PC value and advance PC by 1 to point to next word
				// Read Hypo memory content pointed by MAR into MBR
				MAR = PC++;
				MBR = mainMemory[MAR];
			}
			else {
				// Display invalid address and error message;
				System.err.println("INVALID_ADDRESS_ERROR");
				return(INVALID_ADDRESS_ERROR);
			}
			// Copy MBR value into instruction register IR;
			IR = MBR;
			

			// Decode cycle
			
			// Get opCode
			if(IR / 10000 >= 0 && IR / 10000 <= 12) {
				opCode = IR / 10000;
				remainder = IR % 10000;
			}
			else
			{
				System.err.println("OPCODE_ERROR: The opcode is invalid");
				return(OPCODE_ERROR);
			}
			
			// Check for invalid GPR# and invalid mode and return error code	
			if(remainder / 1000 >= 0 && remainder / 1000 <= 6) 
			{
				op1Mode = remainder / 1000;
				remainder = remainder % 1000;
			} 
			else 
			{
				System.err.println("OP1_MODE_ERROR: The operand 1 mode is invalid");
				return(OP1_MODE_ERROR);
			}
			
			if(remainder / 100 >= 0 && remainder / 100 <= 7) 
			{
				op1GPR = remainder / 100;
				remainder = remainder % 100;
			} 
			else 
			{
				System.err.println("OP1_GPR_ERROR: The operand 1 GPR is invalid");
				return(OP1_GPR_ERROR);
			}
			
			if(remainder / 10 >= 0 && remainder / 10 <= 6) 
			{
				op2Mode = remainder / 10;
				remainder = remainder % 10;
			} 
			else 
			{
				System.err.println("OP2_MODE_ERROR: The operand 2 mode is invalid");
				return(OP2_MODE_ERROR);
			}
			
			if(remainder >= 0 && remainder <= 7) 
			{
				op2GPR = remainder;

			} 
			else 
			{
				System.err.println("OP2_GPR_ERROR: The operand 2 GPR is invalid");
				return(OP2_GPR_ERROR);
			}
			
			// Execute Cycle
			switch (opCode){
				// Halt		
				case 0:  
					// Display halt instruction is encountered message
					System.err.println("HALT");
					// Increment CLOCK 
			    	CLOCK = CLOCK + 12;
			    	TIME_LEFT = TIME_LEFT - 12;
			    	return (HALT);
			    // Add	
				case 1: 
					status = FetchOperand(op1Mode,op1GPR);
					// check for error and return error code
					if(status !=0) 
					{
						System.err.println("OP1_FETCH_ERROR_IN_ADD");
						return(OP1_FETCH_ERROR_IN_ADD);
					} 
					else 
					{
						// Set MBR to op1Value				
					op1Value = MBR;					
					}
	
					
					status = FetchOperand(op2Mode,op2GPR);
					// check for error and return error code
					if(status !=0) 
					{
						System.err.println("OP2_FETCH_ERROR_IN_ADD");
						return(OP2_FETCH_ERROR_IN_ADD);					
					} 
					else 
					{
						// Add the operand values
					MBR = op1Value + MBR;					
					}

					// Store the MBR into Op1 location
					if (op1Mode == 1) 
					{  
						GPR[op1GPR] = MBR;
					}
					// Error: Destination operand cannot be immediate value				
					else if (op1Mode == 6) //is Immediate Mode
					{ 
						System.err.println("IMMEDIATE_MODE_ERROR_IN_ADD");
					    return(IMMEDIATE_MODE_ERROR_IN_ADD);
					}
					else
					{
						// Store MBR in hypo memory at location Op1Address;					
					mainMemory[MAR] = MBR;
					}
	
					// Increment CLOCK
		    		CLOCK = CLOCK + 3;
		    		TIME_LEFT = TIME_LEFT - 3;
				break;
				
				// Subtract	
				case 2:  
					// check for error and return error code
					status = FetchOperand(op1Mode,op1GPR);
					
					if(status !=0) 
					{
						System.err.println("OP1_FETCH_ERROR_IN_SUBTRACT");
						return(OP1_FETCH_ERROR_IN_SUBTRACT);					
					} 
					else 
					{
						// Set MBR to op1Value						
						op1Value = MBR;						
					}
					
					status = FetchOperand(op2Mode,op2GPR);
					// check for error and return error code
					if(status !=0) 
					{
						System.err.println("OP2_FETCH_ERROR_IN_SUBTRACT");
						return(OP2_FETCH_ERROR_IN_SUBTRACT);						
					} 
					else 
					{
						// Subtract the operand values
						MBR = op1Value - MBR;						
					}

					// Store the MBR into Op1 location
					if (op1Mode == 1) 
					{  
						GPR[op1GPR] = MBR;
					}
					// Error: Destination operand cannot be immediate value				
					else if (op1Mode == 6) //is Immediate Mode
					{ 
						System.err.println("IMMEDIATE_MODE_ERROR_IN_SUBTRACT");
					    return(IMMEDIATE_MODE_ERROR_IN_SUBTRACT);
					}
					else
					{
						// Store MBR in hypo memory at location Op1Address;
						mainMemory[MAR] = MBR;	
					}
					// Increment CLOCK
			    	CLOCK = CLOCK + 3;
			    	TIME_LEFT = TIME_LEFT - 3;
				break;
				// Multiply
				case 3:  
					status = FetchOperand(op1Mode,op1GPR);
					// check for error and return error code
					if(status !=0) 
					{
						System.err.println("OP1_FETCH_ERROR_IN_MULTIPLY");
						return(OP1_FETCH_ERROR_IN_MULTIPLY);						
					} 
					else 
					{
						// Set MBR to op1Value						
						op1Value = MBR;						
					}			
					status = FetchOperand(op2Mode,op2GPR);
					// check for error and return error code
					if(status !=0) 
					{
						System.err.println("OP2_FETCH_ERROR_IN_MULTIPLY");
						return(OP2_FETCH_ERROR_IN_MULTIPLY);						
					} 
					else 
					{
						// Multiply the operand values
						MBR = op1Value * MBR;						
					}

					// Store the MBR into Op1 location
					if (op1Mode == 1) 
					{  
						GPR[op1GPR] = MBR;
					}
					// Error: Destination operand cannot be immediate value				
					else if (op1Mode == 6) //is Immediate Mode
					{ 
						System.err.println("IMMEDIATE_MODE_ERROR_IN_MULTIPLY");
					    return(IMMEDIATE_MODE_ERROR_IN_MULTIPLY);
					}
					else
					{
						// Store MBR in hypo memory at location Op1Address;					
						mainMemory[MAR] = MBR;
					}
					// Increment CLOCK
			    	CLOCK = CLOCK + 6;
			    	TIME_LEFT = TIME_LEFT - 6;
				break;
				
				// Divide
				case 4:	
					status = FetchOperand(op1Mode,op1GPR);
					// check for error and return error code
					if(status !=0) 
					{
						System.err.println("OP1_FETCH_ERROR_IN_DIVIDE");
						return(OP1_FETCH_ERROR_IN_DIVIDE);					
					} 
					else 
					{
						// Set MBR to op1Value						
						op1Value = MBR;						
					}
					
					// check for error and return error code					
					status = FetchOperand(op2Mode,op2GPR);

					if(status != 0) 
					{
						System.err.println("OP2_FETCH_ERROR_IN_DIVIDE");
						return(OP2_FETCH_ERROR_IN_DIVIDE);
					}

					// Check for division by zero
					if(MBR == 0) 
					{
						System.err.println("DIVIDE_BY_ZERO_ERROR");
					    return(DIVIDE_BY_ZERO_ERROR);
					}
					
					// Divide the operand values					
					else 
					{
						MBR = op1Value / MBR;
					}
					

					// Store the MBR into Op1 location
					if (op1Mode == 1) 
					{  
						GPR[op1GPR] = MBR;
					}
					// Error: Destination operand cannot be immediate value				
					else if (op1Mode == 6) //is Immediate Mode
					{ 
						System.err.println("IMMEDIATE_MODE_ERROR_IN_DIVIDE");
					    return(IMMEDIATE_MODE_ERROR_IN_DIVIDE);
					}
					else
					{
						// Store MBR in hypo memory at location Op1Address;					
						mainMemory[MAR] = MBR;
					}
					// Increment CLOCK
			    	CLOCK = CLOCK + 6;
			    	TIME_LEFT = TIME_LEFT - 6;
				break;
				
				// Move	
				case 5: 
					status = FetchOperand(op1Mode,op1GPR);
					// check for error and return error code
					if(status !=0) 
					{
						System.err.println("OP1_FETCH_ERROR_IN_MOVE");
						return(OP1_FETCH_ERROR_IN_MOVE);						
					} 
					else 
					{
						// Set MBR to op1Value)				
						op1Value = MBR;						
					}
				
					status = FetchOperand(op2Mode,op2GPR);
					//check for error and return error code;
					if(status !=0) 
					{
						System.err.println("OP2_FETCH_ERROR_IN_MOVE");
						return(OP2_FETCH_ERROR_IN_MOVE);						
					} 
					else 
					{
						// Set op2Value to MBR
						op2Value = MBR;						
					}

					// Store the MBR into Op1 location
					if (op1Mode == 1) 
					{  
						GPR[op1GPR] = op2Value;
					}
					// Error: Destination operand cannot be immediate value				
					else if (op1Mode == 6) //is Immediate Mode
					{ 
						System.err.println("IMMEDIATE_MODE_ERROR_IN_MOVE");
					    return(IMMEDIATE_MODE_ERROR_IN_MOVE);
					}
					else
					{
						// Store MBR in hypo memory at location Op1Address;					
						mainMemory[MAR] = op2Value;			
					}		
					// Increment CLOCK
			    	CLOCK = CLOCK + 2;
			    	TIME_LEFT = TIME_LEFT - 2;
				break;
				
				// Branch	
				case 6:
					//If PC value is in the valid range  
					if(VALID_PROGRAM_MIN <= PC && PC <= VALID_PROGRAM_MAX) 
					{ 
						//Set PC to memory[PC]
						PC = mainMemory[PC];
					} 
					else 
					{
						//Print error message and return error code	
						System.err.println("INVALID_PC_ERROR_B");
						return(INVALID_PC_ERROR_IN_B);
					}
					// Increment CLOCK
			    	CLOCK = CLOCK + 2;
			    	TIME_LEFT = TIME_LEFT - 2;
				break;
				
				// Branch on Minus	
				case 7:  
					status = FetchOperand(op1Mode, op1GPR);
					// Check for error and return error code
					if(status !=0) 
					{
						System.err.println("OP1_FETCH_ERROR_IN_BM");
						return(OP1_FETCH_ERROR_IN_BM);						
					}
	
					if(MBR < 0)
					{
						// If PC value is in the valid Range
						if(VALID_PROGRAM_MIN <= PC && PC <= VALID_PROGRAM_MAX)
						{
							//Set PC to memory[PC]					
							PC = mainMemory[PC];
						}
						else
						{
							// Print error message and return runtime error code
							System.err.println("INVALID_PC_ERROR_IN_BM");
							return(INVALID_PC_ERROR_IN_BM);						
						}	
					}
					else
					{
						// Skip branch address to go to next instruction
						PC++;
					}
					// Increment CLOCK
			    	CLOCK = CLOCK + 4;
			    	TIME_LEFT = TIME_LEFT - 4; 
				break;
				
				// Branch on plus
				case 8:  
					status = FetchOperand(op1Mode, op1GPR);
					// Check for error and return error code
					if(status !=0)
					{
						System.err.println("OP1_FETCH_ERROR_IN_BP");
						return(OP1_FETCH_ERROR_IN_BP);						
					}

					if(MBR > 0)
					{
						// If PC value is in the valid Range
						if(VALID_PROGRAM_MIN <= PC && PC <= VALID_PROGRAM_MAX)
						{
							//Set PC to memory[PC]					
							PC = mainMemory[PC];
						}
						else
						{
							// Print error message and return runtime error code
							System.err.println("INVALID_PC_ERROR_IN_BP");
							return(INVALID_PC_ERROR_IN_BP);						
						}						
					}
					else
					{
						// Skip branch address to go to next instruction
						PC++;
					}
					// Increment CLOCK
			    	CLOCK = CLOCK + 4;
			    	TIME_LEFT = TIME_LEFT - 4;
				break;
				
				// Branch on zero
				case 9:  
					status = FetchOperand(op1Mode, op1GPR);
					// Check for error and return error code
					if(status !=0)
					{
						System.err.println("OP1_FETCH_ERROR_IN_BZ");
						return(OP1_FETCH_ERROR_IN_BZ);						
					}	
					if(MBR == 0)
					{
						// If PC value is in the valid Range
						if(VALID_PROGRAM_MIN <= PC && PC <= VALID_PROGRAM_MAX) 
						{
							//Set PC to memory[PC]					
							PC = mainMemory[PC];
						}
						else 
						{
							// Print error message and return runtime error code
							System.err.println("INVALID_PC_ERROR_IN_BZ");
							return(INVALID_PC_ERROR_IN_BZ);						
						}						
					}
					else
					{
						// Skip branch address to go to next instruction
						PC++;
					}
					// Increment CLOCK
			    	CLOCK = CLOCK + 4;
			    	TIME_LEFT = TIME_LEFT - 4;
				break;
				
				// Push � if stack is not full	
				case 10:
					status = FetchOperand(op1Mode, op1GPR);
					//Check for error and return error code
					if(status !=0)
					{
						System.err.println("OP1_FETCH_ERROR_IN_PUSH");
						return(OP1_FETCH_ERROR_IN_PUSH);						
					}
					
					op1Value = MBR;
					//Check if SP is at stack upper limit					
					if(SP == VALID_PROGRAM_MAX) 
					{	
						//return error code
						System.err.println("STACK_OVERFLOW_ERROR");
						return(STACK_OVERFLOW_ERROR);
					} 
					else 
					{
						// Store Op1Value on stack pointed by SP
						SP++;
						mainMemory[SP] = op1Value;
						System.out.println("Value Pushed: " + op1Value);
					}
					// Increment CLOCK
			    	CLOCK = CLOCK + 2;
			    	TIME_LEFT = TIME_LEFT - 2;
				break;
				
				// Pop � if stack is not empty				
				case 11:  
					status = FetchOperand(op1Mode, op1GPR);
					//Check for error and return error code
					if(status !=0)
					{
						System.err.println("OP1_FETCH_ERROR_IN_POP");
						return(OP1_FETCH_ERROR_IN_POP);						
					}
					
					//Check if SP is at stack lower limit					
					if(SP == 0) 
					{	
						//return error code
						System.err.println("STACK_UNDERFLOW_ERROR");
						return(STACK_UNDERFLOW_ERROR);
					} 
					else 
					{
						// Store Op1Value on stack pointed by SP
						GPR[op1GPR] = mainMemory[SP];
						SP--;
						System.out.println("Value Popped: " + GPR[op1GPR]);
					}
					// Increment CLOCK
			    	CLOCK = CLOCK + 2;
			    	TIME_LEFT = TIME_LEFT - 2;
				break;
					
				//System Call					
				case 12:
					// Check if PC value is in the invalid range
					if(VALID_PROGRAM_MIN <= PC && PC <= VALID_PROGRAM_MAX) {
						// next word has system call ID					
						MBR = mainMemory[PC];
						PC++;
						// call SystemCall to process it		
						status = SystemCall(MBR);
					} else {
						// Print error message and return runtime error code						
						System.out.println("INVALID_PC_ERROR_IN_SYSCALL");
						return(INVALID_PC_ERROR_IN_SYSCALL);
					}
					
					CLOCK = CLOCK + 12;
					TIME_LEFT = TIME_LEFT - 12;
				break;
	

				// Invalid opCode				
				default:  
					// Display invalid opCode error message
					System.err.println("INVALID_OPCODE_ERROR_IN_DEFAULT");
					return(INVALID_OPCODE_ERROR_IN_DEFAULT);
					// end of switch opCode					
			}  
			// end of while loop			
		}  
		System.out.println(status);
	    return(status);
	}  // end of CPUexecuteProgram() function

	/******************************************************************************************************************
	 * Function: SystemCall By: Kyle Abreu
	 * ----------------------------------------------------------------------------------------------------------------
	 * Task performed:
	 *   Executes the system call specified by the SystemCallID parameter. Input parameter values for each system call
	 *   are passed through GPRs, and output parameter values are returned by the operating system through GPRs. 
	 *   Sets the system mode to OS mode on entry into the system call function and sets it back to user mode before
	 *   returning. Implements each system call as a separate function for modularity and ease of management.
	 *
	 * Input Parameters:
	 *   SystemCallID - An integer representing the ID of the system call to execute.
	 *  
	 * Output Parameters:
	 *   None
	 *
	 * Function Return Value:
	 *   An integer representing the status of the system call execution.
	 ******************************************************************************************************************/
	int SystemCall(int SystemCallID){
		PSR = OS_MODE;	// Set system mode to OS mode

		status  = OK;

		switch(SystemCallID){
			case 1:	// Create process � user process is creating a child process. 
				System.err.println("Create process system call not implemented.");
				break;

			case 2:  // Delete process
				System.err.println("Delete process system call not implemented.");
				break;

			case 3:  // process inquiry
				System.err.println("Process inquiry system call not implemented");
				break;

			case 4:	 // Dynamic memory allocation: Allocate user free memory system call
				status = MemAllocSystemCall();
				break;

			case 5:	 // Free dynamically allocated user memory system call
				status = MemFreeSystemCall();
				break;
				
			case 6:  // msg_send
				System.err.println("Send Message system call not implemented");
				break;
				
			case 7:  // msg_receive
				System.err.println("Receive Message system call not implemented");
				break;
				
			case 8:	 // io_getc  system call
				status = io_getcSystemCall();
				break;

			case 9:	 // io_putc system call
				status = io_putcSystemCall();
				break;

			case 10:  // time_get
				System.err.println("Get Time system call not implemented");
				break;
				
			case 11:  // time_set
				System.err.println("Set Time system call not implemented");
				break;

			default: // Invalid system call ID
				System.err.println("Invalid system call ID");
				break;
		}  // end of SystemCallID switch statement

		PSR = USER_MODE;	// Set system mode to user mode

		return (status);
	}  // end of SystemCall() function

	/***********************************************
	 * Function: MemAllocSystemCall By: Kyle Abreu
	 * ---------------------------------------------
	 * Task performed:
	 *   Attempts to allocate a block of user memory of a specified size. If the allocation
	 *   is successful, it returns the memory address of the allocated block, otherwise it returns 
	 *   an error code.
	 *
	 * Input Parameters:
	 *   Size (int) - The size of the memory block to allocate, expected in GPR[2].
	 *   
	 * Output Parameters:
	 *   None - The function sets the allocation status and possibly the memory address in the 
	 *          general purpose registers (GPRs).
	 *
	 * Function Return Value:
	 *   int - The value of GPR[0] after attempting the allocation, indicating success (OK) or 
	 *         containing an error code if the allocation failed.
	 *       - INVALID_SIZE_OR_MEMORY_ADDRESS_ERROR - Returned if the requested size is out of the valid 
	 *                                    		  memory range.
	 ************************************************/
	int MemAllocSystemCall()
	{
		// Allocate memory from user free list
		// Return status from the function is either the address of allocated memory or an error code

		//Declare int Size and set it to GPR2 value;
		int size = GPR[2];

		// Add code here to check for size out of range
		if(size < 0 || size > VALID_USER_FREE_MAX - VALID_USER_FREE_MIN) {
			System.err.println("INVALID_SIZE_OR_MEMORY_ADDRESS_ERROR");
			return(INVALID_SIZE_OR_MEMORY_ADDRESS_ERROR);
		}

		// Check size of 1 and change it to 2
		if(size  == 1)
			size = 2;
		
		// Allocate User Memory passing Size argument;
		GPR[1] = AllocateUserMemory(size);
		if(GPR[1] < 0){
			GPR[0] = GPR[1];	// Set GPR0 to have the return status
		}
		else{
			GPR[0] = OK;
		}

		//Display Mem_alloc system call, and parameters GPR0, GPR1, GPR2
		System.out.println("Mem_alloc system call: Parameters\n"
						 + "GPR0: " + GPR[0] + "\n"
						 + "GPR1: " + GPR[1] + "\n" 
						 + "GPR2: " + GPR[2]);

		return GPR[0];
	}  // end of MemAllocaSystemCall() function

	/***********************************************
	 * Function: MemFreeSystemCall By: Kyle Abreu
	 * ---------------------------------------------
	 * Task performed:
	 *   Returns the dynamically allocated user memory to the user free list. 
	 *   The memory address to be freed is provided in GPR1, and the size of the memory block 
	 *   is specified in GPR2. The function returns the status of the operation in GPR0.
	 *
	 * Input Parameters:
	 *   None - directly uses values from general-purpose registers (GPRs) GPR1 for memory 
	 *   address and GPR2 for the size of the memory to be released.
	 *   
	 * Output Parameters:
	 *   None - directly outputs the status of the memory release operation in GPR0.
	 *
	 * Function Return Value:
	 *   int - Returns the status code in GPR0 indicating success (OK) or an error code.
	 *	 - INVALID_SIZE_OR_MEMORY_ADDRESS_ERROR - Returned if the size specified in GPR2 is 
	 *                                          out of the valid memory range.
	 ************************************************/
	int MemFreeSystemCall()
	{
		// Declare int Size setting it to GPR2 value;
		int size = GPR[2];

		// Add code to check for size out of range
		if(size < 0 || size > VALID_USER_FREE_MAX - VALID_USER_FREE_MIN) {
			System.err.println("INVALID_SIZE_OR_MEMORY_ADDRESS_ERROR");
			return(INVALID_SIZE_OR_MEMORY_ADDRESS_ERROR);
		}
		// Check size of 1 and change it to 2
		if(size  == 1) {
			size = 2;
		}

		GPR[0] = FreeUserMemory(GPR[1], size);

	    //Display Mem_free system call, and parameters GPR0, GPR1, GPR2;
		System.out.println("Mem_free system call: Parameters\n"
				 + "GPR0: " + GPR[0] + "\n"
				 + "GPR1: " + GPR[1] + "\n" 
				 + "GPR2: " + GPR[2]);

		return(GPR[0]);
	}  // end of MemAllocaSystemCall() function
	 
	/******************************************************************************************************************
	 * Function: io_getcSystemCall By: Kyle Abreu
	 * ----------------------------------------------------------------------------------------------------------------
	 * Task performed:
	 *   Simulates the io_getc system call, which reads one character from the standard input device keyboard.
	 *   This system call returns the start of input operation event code.
	 *
	 *	In real system, the device is instructed to do the io_getc operation and store the value in main memory.  Appropriate parameters are passed to the device.
	 *	The process executing io_getc system call is moved from running to WQ with reason for waiting set to io_getc completion event and process state set to Waiting.
	 *	OS selects the next process from RQ, performs contact switch and gives CPU to running selected process.
	 *	The device performs the input operation (works) in parallel with CPU executing another scheduled process and stores the result in memory.
	 *
	 *	Simulation of performing parallel working of CPU and device requires use of threads.  This will make the project more complex and harder to debug. Hence, this approach will not be used.
	 *
	 *	Instead, for this project, the io_getc operation of reading a character from standard input device keyboard will be performed in the io_getc completion interrupt service routine (ISR) function.
	 *
	 * Input Parameters:
	 *   None
	 *  
	 * Output Parameters:
	 *   None
	 *
	 * Function Return Value:
	 *   An integer representing the start of input operation event code.
	 ******************************************************************************************************************/
	int io_getcSystemCall (){
		//Return start of input operation event code;
		return(INPUT_START_EVENT);
	}  // end of io_getc system call

	/***********************************************
	 * Function: io_putcSystemCall By: Kyle Abreu
	 * ---------------------------------------------
	 * Task performed:
	 *   Simulates the system call for outputting a character to the standard output device.
	 *   In a real system, this would instruct the device to perform the operation and output
	 *   the character from memory. The calling process is set to a waiting state and the system
	 *   selects another process for execution. The actual output operation is simulated to occur
	 *   in the io_putc completion interrupt service routine (ISR) due to the complexity of
	 *   implementing parallelism in this project.
	 *
	 * Input Parameters:
	 *   None - The character to be printed is assumed to be available at a location known to 
	 *          the io_putc completion ISR.
	 *   
	 * Output Parameters:
	 *   None - This function does not directly perform the output operation.
	 *
	 * Function Return Value:
	 *   int - Returns a constant code (OUTPUT_START_EVENT) that signals the start of an 
	 *         output operation, which will be completed by the io_putc completion ISR.
	 ************************************************/
	int io_putcSystemCall (){
		//Return start of output operation event code;
		return(OUTPUT_START_EVENT);

	}  // end of io_putc system call
	
	// ************************************************************
	// Function: AbsoluteLoader By: Kyle Abreu
	//
	// Task Description:
	// Open the file containing HYPO machine user program and 
	// Load the content into HYPO memory.
	// On successful load, return the PC value in the End of Program line.
	// On failure, display appropriate error message and return appropriate error code
	//
	// Input Parameters
	// filename		Name of the Hypo Machine executable file
	//	
	// Output parameters:
	// None
	//
	// Function Return Value:
	// -1 FILE_OPEN_ERROR			   Unable to open the file
	// -2 INVALID_ADDRESS_ERROR		   Invalid address error
	// -3 NO_END_OF_PROGRAM_ERROR	   Missing end of program indicator
	// -4 INVALID_PC_VALUE_ERROR	   Invalid PC value
	// 0 to Valid address range        Successful Load, Valid PC value
	// ************************************************************
	int AbsoluteLoader(String filename) 
	{
		// Declare error codes as final
		final int FILE_OPEN_ERROR = -1;         //Unable to open the file
		final int INVALID_ADDRESS_ERROR = -2;   //Invalid address error
		final int NO_END_OF_PROGRAM_ERROR = -3; //Missing end of program indicator
		final int INVALID_PC_VALUE_ERROR = -4;  //Invalid PC value

		// Declare and initialize local variables
		int address = -1; // First int in file line
		int content = -1; // Second int in file line
		File file;
		Scanner fileScanner;
		
		// Try for FILE_OPEN_ERROR return -1
		try {
			file = new File(filename);
			fileScanner = new Scanner(file);
		} 
		catch (Exception e) {
			// Print error message, and return error code
			System.err.println("FILE_OPEN_ERROR: Unable to open the file");
			return(FILE_OPEN_ERROR);
		}
		// Load the program from the given filename into HYPO Memory		
		// Read from file until end of program or end of file and 
		// store program in HYPO memory
		while(fileScanner.hasNextLine()) { //not end of file
			
			// Read 2 numbers from Hypo machine program: Address and Content
			address = fileScanner.nextInt();
			content = fileScanner.nextInt();
			
			// If address is End Of Program			
			if(address == -1) { 		
				// Close file				
				fileScanner.close();
				
				// Check Content for valid memory range and return Content 
				if(content >= VALID_PROGRAM_MIN && content <= VALID_PROGRAM_MAX)
					return content;
			
				// Else print error message, and return error code
				else 
					System.err.println("INVALID_PC_VALUE_ERROR: Invalid PC value");
					return(INVALID_PC_VALUE_ERROR);
			}
			else if (address >= VALID_PROGRAM_MIN && address <= VALID_PROGRAM_MAX) {	
				// Store Content in the Hypo memory Address
				mainMemory[address] = content;
				SP++;
			}	
			// Display error message, close file and return error code			
			else {
				System.err.println("INVALID_ADDRESS_ERROR: Invalid address error");
				fileScanner.close();
				return(INVALID_ADDRESS_ERROR);
			}		
			// End of while loop
		}
	
		// End of file encountered without End of Program line
		System.err.println("NO_END_OF_PROGRAM_ERROR: Missing end of program indicator");
		fileScanner.close();
		return(NO_END_OF_PROGRAM_ERROR);
	// End of AbsoluteLoader function
	}
		
	// ************************************************************
	// Function: DumpMemory By: Kyle Abreu
	//
	// Task Description:
	// Displays a string passed as one of the input parameter.
	// Displays content of GPRs, SP, PC, PSR, system CLOCK and PSR
	// the content of specified memory locations in a specific format.
	//
	// Input Parameters:
	// String				String to be diSPlayed
	// StartAddress			Start address of memory location
	// Size					Number of locations to dump
	//	
	// Output Parameters:
	// None			
	//
	// Function Return Value:
	// None
	// *****************DumpMemory Example*************************	
	// Memory dump after loading program
	// GPRs:    G0       G1       G2       G3       G4       G5       G6       G7       SP       PC
	//			0        0        0        0        0        0        0        0        15       2       
	// Address: +0       +1       +2       +3       +4       +5       +6       +7       +8       +9
	// 0        0        5        51260    0        11250    1        25060    1        1        85000    
	// 10       1        4        55012    0        0        0        0        0        0        0        
	// 20       0        0        0        0        0        0        0        0        0        0        
	// 30       0        0        0        0        0        0        0        0        0        0        
	// 40       0        0        0        0        0        0        0        0        0        0        
	// 50       0        0        0        0        0        0        0        0        0        0        
	// 60       0        0        0        0        0        0        0        0        0        0        
	// 70       0        0        0        0        0        0        0        0        0        0        
	// 80       0        0        0        0        0        0        0        0        0        0        
	// 90       0        0        0        0        0        0        0        0        0        
	// CLOCK: 0
	// PSR:   0
	// ************************************************************
	void DumpMemory(String string, int startAddress, int size) {
		// Display input parameter String
		System.out.println(string);

		//On invalid startAddress and endAddress display error message and return
		if(startAddress < 0){
			System.err.println("INVALID_START_ADDRESS");
		}
		else if (startAddress + size > VALID_OS_FREE_MAX) {
			System.err.println("INVALID_START_ADDRESS");
		}
		else{

			// Display memory addresses
			System.out.println("Address: +0       +1       +2       +3       +4"
					+ "       +5       +6       +7       +8       +9");

			// Display contents of Hypo machine memory locations specified in the parameters
			// 11 items per line: Address of first value followed by content of 10 locations
			int addr = startAddress;
			int endAddress = startAddress + size;
			while (addr < endAddress){
				
				// Display address of first value in the line
			    System.out.printf("%-8d ", addr);
			    
			    // Display 10 values of memory from startAddress to startAddress+9
			    for (int i = 0; i < 10; i++){
			        if (addr < endAddress){
			        	// Display and increment address
			            System.out.printf("%-8d ", mainMemory[addr]);
			            addr++;
			        }
			        else{
			        	// No more value to print, exit for loop
			        	break;
			        }
			    }
			    // End of for loop
			    System.out.println();
			}
			// Display CLOCK and PSR
			System.out.println("CLOCK: " + CLOCK);
			System.out.println("TIME_SLICE: " + TIME_SLICE);
			System.out.println("PSR:   " + PSR);
			System.out.println("-------------------------------------------------------------------------------------");
		}
	// End of DumpMemory() function
	}
	
	//************************************************************
	// Function: FetchOperand By: Kyle Abreu
	// 
	// Task Description:
	// Fetch the op value from the op address specified by the opMode
	//
	// Input Parameters:
	// opMode			Operand mode value
	// opReg			Operand GPR value
	//	
	// Output parameters:
	// None
	//
	// Function Return Value:
	//	 0 OK									Successful completion
	//	-1 INVALID_ADDRESS_IN_DEFFERED_MODE		MAR out range for mainMemory[] (0-2999)
	//	-2 INVALID_ADDRESS_IN_AUTOINCREMENT		MAR out range for mainMemory[] (0-2999)
	//	-3 INVALID_ADDRESS_IN_AUTODECREMENT		MAR out range for mainMemory[] (0-2999)
	//	-4 INVALID_ADDRESS_IN_DIRECT_MODE		MAR out range for mainMemory[] (0-2999)
	//	-5 INVALID_PC_IN_DIRECT_MODE			PC out range for mainMemory[] (0-2999)
	//	-6 INVALID_PC_IN_IMMEDIATE_MODE			PC out range for mainMemory[] (0-2999
	//	-7 INVALID_MODE							Default switch case
	// ************************************************************
	int FetchOperand(int opMode, int opReg) {
		// Declare and initialize error codes
		final int OK = 0;
		final int INVALID_ADDRESS_IN_DEFFERED_MODE = -1;
		final int INVALID_ADDRESS_IN_AUTOINCREMENT = -2;
		final int INVALID_ADDRESS_IN_AUTODECREMENT = -3;
		final int INVALID_ADDRESS_IN_DIRECT_MODE = -4;
		final int INVALID_PC_IN_DIRECT_MODE = -5;
		final int INVALID_PC_IN_IMMEDIATE_MODE = -6;
		final int INVALID_MODE = -7;
		
		// Fetch operand value based on the operand mode
		switch (opMode) {
		
			// Register Mode		
			case 1:    
				// operand value is in the register				
				MBR = GPR[opReg];  
			break;
			
			// Register deferred mode � MAR is in GPR & value in memory
			case 2: 
				
				// MAR is in the register				
				MAR = GPR[opReg];   
				if(MAR >= VALID_PROGRAM_MIN && MAR <= VALID_USER_FREE_MAX) {
				     MBR = mainMemory[MAR];
				}
				else {
					// Display error and return error code
					System.err.println("INVALID_ADDRESS_IN_DEFFERED_MODE");
					return(INVALID_ADDRESS_IN_DEFFERED_MODE);
				}
			break;
			
			// Autoincrement mode � MAR in GPR & MBR in memory
			case 3: 
				
				// Opaddress is in the register				
				MAR = GPR[opReg];   
				if(MAR >= VALID_PROGRAM_MIN && MAR <= VALID_USER_FREE_MAX) {
					// operand in memory					
				     MBR = mainMemory[MAR];  
				}
				else {
					// Display error and return error code
					System.err.println("INVALID_ADDRESS_IN_AUTOINCREMENT");
					return(INVALID_ADDRESS_IN_AUTOINCREMENT);
				}
				// Increment register content/value by 1
				GPR[opReg]++;    
			break;
			
			// Autodecrement mode
			case 4:  
				
				// Decrement register value (content) by 1				
				--GPR[opReg]; 
				// Op address is in the register				
				MAR = GPR[opReg];   
				if(MAR >= VALID_USER_FREE_MIN && MAR <= VALID_USER_FREE_MAX) {
				     MBR = mainMemory[MAR];
				}
				else {
					// Display error and return error code
					System.err.println("INVALID_ADDRESS_IN_AUTODECREMENT");
					return(INVALID_ADDRESS_IN_AUTODECREMENT);
				}
			break;
			
			// Direct mode � Op address is in the instruction pointed by PC
			case 5:  
				
				// Check for valid address in PC				
				if(VALID_PROGRAM_MIN <= PC && PC <= VALID_PROGRAM_MAX) { 
					// Increment PC after fetching					
					MAR = mainMemory[PC++];
					if(MAR >= VALID_PROGRAM_MIN && MAR <= VALID_USER_FREE_MAX){
					     MBR = mainMemory[MAR];  
					}
					else {
						// Display error and return error code
						System.err.println("INVALID_ADDRESS_IN_DIRECT_MODE");
						return(INVALID_ADDRESS_IN_DIRECT_MODE);
					}
				}
				else {
					// Display error and return error code
					System.err.println("INVALID_PC_IN_DIRECT_MODE");
					return(INVALID_PC_IN_DIRECT_MODE);
				}
			break;
			
			// Immediate mode � Operand value is in the instruction
			case 6: 
				
				// Check for valid address in PC				
				if(VALID_PROGRAM_MIN <= PC && PC <= VALID_PROGRAM_MAX){ 
					// Increment PC after fetching value					
					MBR = mainMemory[PC++];
				}
				else {
					// Display error and return error code
					System.err.println("INVALID_PC_IN_IMMEDIATE_MODE");
					return(INVALID_PC_IN_IMMEDIATE_MODE);
				}
			break;
			
			// Invalid mode
			default:
				// Display error and return error code
				System.err.println("INVALID_MODE");
				return(INVALID_MODE);
				// end of switch opMode			
		}  
		// return success status
		return(OK);
	// end of FetchOperand() function
	}
}
