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
