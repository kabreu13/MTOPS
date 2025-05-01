# Abstract
A simlulated Operating System by use of a HYPO Machine Simulator and a MTOPS Operating System. These systems encompass a wide array of topics including hardware simulation, assembly language programming, operating system kernel functions, data structures, and software development methodologies.

## Hypothetical PC Hardware Simulation Machine
This machine accepts a file written in its' machine language, reads each line, processes it, saves in it memory, performs the logic and displays its' contents. The main function oversees several tasks: initializing the system, receiving a command from the user to execute a program, loading the program into memory, setting the Program Counter (PC), executing the program, and displaying memory contents before and after execution. Error handling is included for each task.
- **AbsoluteLoader()** loads a machine language program into memory from a specified file and returns the value to set the Program Counter. It returns negative values for loading errors.
- **CPU()** executes the loaded program, performing error checks and updating the clock. It returns the execution status.
- **InitializeSystem()** initializes hardware and variables.
- **DumpMemory()** displays memory contents, including GPRs and the clock, for a specified range. Error checking is performed.

## Multitasking Timesharing OPerating System
MTOPS provides essential features grouped into process management, memory management, interprocess communication, timer management, and character I/O support. It enables multitasking with priority-based scheduling, dynamic memory allocation, inter-process communication, system clock management, and character I/O operations. These features form a strong foundation for timesharing and multitasking application systems, supporting efficient resource management, process coordination, and user interaction.

## System Initialization and Control
- **InitializeSystem()** Initializes memory, CPU registers, OS/User memory blocks, and sets up a null process.
- **OS_Main()** Main OS loop handling process scheduling, memory management, and interrupts.
- **main()** Entry point of the Java program, calls OS_Main().

## Process Management
- **CreateProcess()** Loads a program, sets up its PCB and stack, inserts it into the Ready Queue.
- **InitializePCB()** Initializes default values for a new PCB in memory.
- **TerminateProcess()** Frees stack and PCB memory of a terminated process.
- **SelectProcessFromRQ()** Picks the highest-priority process from the Ready Queue.
- **InsertIntoRQ()** Inserts a PCB into the Ready Queue based on priority.
- **InsertIntoWQ()** Inserts a PCB into the Waiting Queue.
- **SearchAndRemovePCBfromWQ()** Finds and removes a PCB from the Waiting Queue by PID.

## Context Switching
- **SaveContext()** Saves CPU register values to the PCB.
- **Dispatcher()** Restores CPU register values from the PCB.

## Memory Management
- **AllocateOSMemory()** / **FreeOSMemory()** Allocate and free memory from the OS memory block.
- **AllocateUserMemory()** / **FreeUserMemory()** Allocate and free memory from the user memory block.

## Interrupt Handling
- **CheckAndProcessInterrupt()** Reads interrupt type from user input and routes to the appropriate ISR.
- **ISRrunProgramInterrupt()** Handles process creation from user input.
- **ISRinputCompletionInterrupt()** ISRoutputCompletionInterrupt(): Handle input/output completion by modifying relevant PCB.
- **ISRshutdownSystem()** Terminates all processes and prepares system shutdown.

## Monitoring
- **PrintPCB()** Outputs the contents of a PCB.
- **PrintQueue()** Outputs all PCBs in a given queue.

## Tests
### Test#1
This program allocates 150 words (locations) of dynamic memory (from the Hypo user dynamic memory) using Mem_alloc system call. It sets the allocated memory with numbers starting at a random number R and multiplying it by a fixed number M where M is from 2 to 10. That is, fill the location as MxR where M alternates the sign. You select the value of R and M. Filled value will be alternating positive and negative number. For example, let R=1 and M=2. Then, first location will be 1x2=2, second location will be 1x-2=-2, third location will be 1x2=2, and so on.
After setting the memory, the program frees the allocated dynamic memory using Mem_free system call. Then the OS halts.

### Test#2
This program pushes ten 4-digit numbers to the stack and then pops them out. This is repeat M times using a loop. Then, it halts. M is in the range 10 <= M <= 20. The user selects the value for M in the test2 machine code file. For example, if the 4-digit value is 5210, then the value 5210 will be pushed on the stack and then popped. This repeats M times and then the OS halts.

### Test#3
This program allocates 10 words (locations) of dynamic memory. User sets a word of length 5 to 10 characters in the test3 machine code file. For example, if I sets the name as Bob, then the word length will be 3. Then, the name is read into the allocated memory, one character at a time using io_getc system call. After reading all the letters in the word, the name is displayed/printed one letter at a time using io_putc system call. Finally, the allocated memory is released and the OS halts.


###### CSCI 465: Operating Systems Internals: Capstone Project
###### Course and Project Outline by Prof. Suban Krishnamoorthy at FSU
