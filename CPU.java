import java.io.*;
import java.util.Scanner;
import java.lang.Runtime;

/**
 * CPU class, representing a CPU in a computer.
 * Implements an instruction set, timer and system interrupts.
 * Launches a memory process which it communicates with via a defined protocol over stdin/out
 * 
 * @author Daniel
 *
 */
public class CPU {

	public static int PC, SP, IR, AC, X, Y;

	public static PrintWriter pw;
	public static InputStream is;
	public static OutputStream os;
	public static Scanner sc;

	public static int execcounter;
	public static boolean kernelMode;
	public static boolean end;

	public static int timer;
	public static String programTarget;


	public static void main(String[] args) {
		// TODO Auto-generated method stub

		CPU.PC = 0; //Program counter register
		CPU.execcounter = 0; //Number of commands executed since last timer interrupt
		CPU.SP = 999; //Stack pointer
		CPU.kernelMode = false; //Kernel mode / interrupt active flag.
		CPU.end = false; //Termination flag
		
		CPU.timer = Integer.parseInt(args[1]); //Parse execcounter from args
		CPU.programTarget = args[0]; //Parse program target from args

		try
		{            
			int x;
			Runtime rt = Runtime.getRuntime();

			Process proc = rt.exec("java Memory " + programTarget); //Launch the memory process

			is = proc.getInputStream();
			os = proc.getOutputStream();

			pw = new PrintWriter(os);
			sc = new Scanner(is);

			while (!CPU.end)
			{
				
				//CPU follows the fetch->Execute->Interrupt cycle.
				//Timer interrupts are executed after the execution that triggers them.
				//System interrupts are executed at the call of the instruction.
				
				IR = fetch(); //Most recently fetched instruction stored in IR register
				execute(); //Execute command in IR register

				//Triggers timer interrupt. No nested interrupts allowed.
				if (execcounter > timer && !CPU.kernelMode) 
				{
					interrupt(1);
				}

			}
			
			//When CPU is done executing, terminate Memory process.
			pw.println("quit");
			pw.flush();


			//Cleanup of streams and Memory process
			proc.waitFor();
			int exitVal = proc.exitValue();
			pw.close();
			os.close();

			System.out.println("Process exited: " + exitVal);

		} 
		catch (Throwable t)
		{
			t.printStackTrace();
		}


	}

	public static int fetch() //Fetch the next instruction at PC and increment PC
	{
		int fetched = readMem(PC);
		PC++;
		//System.out.println(fetched);
		return fetched;
	}

	/**
	 * Reads memory from Memory process
	 * @param addr Address to read
	 * @return data read
	 */
	public static int readMem(int addr) 
	{
		//Prevent user programs from accessing system memory.
		if (addr >= 1000 && !CPU.kernelMode)
		{
			int temp = CPU.PC - 1;
			System.out.println("Fatal Error. Attempting to read from protected memory.");
			//Trace debug info, commented out for execution.
			//System.out.println(addr + " " +  CPU.IR + " " + CPU.SP);
			System.exit(0);
			return -1;
		}
		else
		{
			//Send read command to Memory and store result.
			pw.println("r"+addr);
			pw.flush();
			String line = sc.nextLine();
			return Integer.parseInt(line);
		}

	}
	
	/**
	 * Writes data to memory. Prevents user from writing to system memory.
	 * @param addr Address to write to
	 * @param data Data to write
	 */
	public static void writeMem(int addr, int data)
	{
		//Ensure user is not writing to system memory
		if (addr >= 1000 && !CPU.kernelMode)
		{
			System.out.println("Fatal Error. Attempting to write to protected memory. ");
			System.exit(0);
		}
		else
		{
			
			pw.println("w"+addr+ " " + data);
			pw.flush();
		}

	}

	/**
	 * Pushes data to the stack at given SP
	 * @param data 
	 */
	public static void push(int data)
	{
		writeMem(CPU.SP, data);
		//System.out.println("Pushing " + data + " at adr: " + CPU.SP);
		CPU.SP = CPU.SP - 1;


	}

	/**
	 * Returns data from stack
	 * @return data
	 */
	public static int pop()
	{
		CPU.SP = CPU.SP + 1;
		int temp = readMem(CPU.SP);
		return temp;
	}


	/**
	 * Sets the CPU into Kernel mode, pushes state of processor to stack (PC, SP), 
	 * adjusts SP to system stack, and adjusts PC to appropriate location
	 * @param type Interrupt to process. 1 for Timer, 2 for System interrupt
	 */
	public static void interrupt(int type)
	{
		CPU.kernelMode = true;
		int previousSP = CPU.SP;

		CPU.SP = 1999;

		push(previousSP);
		push(CPU.PC);

		switch(type)
		{
		case 1: //Timer
			CPU.PC = 1000;
			break;
		case 2: //Interrupt
			CPU.PC = 1500;
			break;
		}


	}

	/**
	 * Fetches the argument for an instruction
	 * @return the arg
	 */
	public static int fetchArg()
	{
		return fetch();
	}

	/**
	 * Executes the instruction stored in IR register
	 */
	public static void execute()
	{
		int arg;
		switch (IR) {
		case 1:  //Load arg into AC
			arg = fetchArg();
			CPU.AC = arg;
			break;
		case 2:  //Load value at address into the AC
			arg = fetchArg();
			arg = readMem(arg);
			CPU.AC = arg; 	
			break;
		case 3:  //Load value from address found in address
			arg = fetchArg(); //Get address
			arg = readMem(arg); //Load value from address
			arg = readMem(arg); //Load value from address
			CPU.AC = arg;
			break;
		case 4:  //Load value from addr+X
			arg = fetchArg();
			arg = readMem(arg + CPU.X);
			CPU.AC = arg;
			break;
		case 5:  //Load  value from addr+Y
			arg = fetchArg();
			arg = readMem(arg + CPU.Y);
			CPU.AC = arg;
			break;
		case 6:  //Load value from SP+X
			int temp = CPU.SP + CPU.X;
			CPU.AC = readMem(CPU.SP + CPU.X + 1);
			//System.out.println("Reading from adr:" + (temp) + " Which is: " + CPU.AC);
			break;
		case 7:  //Store AC value in given address
			arg = fetchArg();
			writeMem(arg, CPU.AC);
			break;
		case 8:  //Store random int[1-100] in AC
			arg = 1 + (int)(Math.random() * 100); 
			CPU.AC = arg;
			break;
		case 9:  //Write AC as an int/char to the screen
			arg = fetchArg();
			if (arg == 1)
				System.out.print(CPU.AC);
			if (arg == 2)
				System.out.print((char) CPU.AC);
			break;
		case 10: //Add X to AC
			CPU.AC = CPU.AC + CPU.X;
			break;
		case 11: //Add Y to AC
			CPU.AC = CPU.AC + CPU.Y;
			break;
		case 12: //Sub X from AC
			CPU.AC = CPU.AC - CPU.X;
			break;
		case 13:  //Sub Y from AC
			CPU.AC = CPU.AC - CPU.Y;
			break;
		case 14:  //Copy AC to X
			CPU.X = CPU.AC;
			break;
		case 15:  //Copy X to AC
			CPU.AC = CPU.X;
			break;
		case 16:  //Copy AC to Y
			CPU.Y = CPU.AC;
			break;
		case 17:  //Copy Y to AC
			CPU.AC = CPU.Y;
			break;
		case 18:  //Copy AC to the SP
			CPU.SP = CPU.AC;
			break;
		case 19:  //Copy SP to the AC
			CPU.AC = CPU.SP;
			break;
		case 20:  //Jump PC to arg
			arg = fetchArg();
			CPU.PC = arg;
			break;
		case 21:  //Jump if AC is 0
			arg = fetchArg();
			if(AC == 0)
			{		
				CPU.PC = arg;
			}		
			break;
		case 22:  //Jump if AC != 0
			arg = fetchArg();
			if(AC != 0)
			{
				CPU.PC = arg;
			}	
			break;
		case 23: //Push return onto stack, jump to the address
			arg = fetchArg();
			push(CPU.PC);
			CPU.PC = arg;
			break;
		case 24: //Pop return address from stack and jump to it
			CPU.PC = pop();
			break;
		case 25:  //Increment X
			CPU.X = CPU.X + 1;
			break;
		case 26:  //Decrement X
			CPU.X = CPU.X - 1;
			break;
		case 27:  //Push AC onto stack
			push(CPU.AC);
			break;
		case 28:  //Pop AC from stack
			arg = pop();
			CPU.AC = arg;
			break;
		case 29:  //Perform system call
			interrupt(2);
			break;
		case 30:  //iRet, return from system call
			//Pop PC off and replace
			//Pop SP off and replace
			//Turn off kernel mode
			CPU.PC = pop();
			CPU.SP = pop();
			CPU.kernelMode = false;
			CPU.execcounter = 0;
			break;
		case 50:  //Quits the CPU
			CPU.end = true;
			break;


		}
		//Increments the exec counter if the CPU is not in kernel mode and the instruction
		//Was not a system call for an interrupt.
		if (!CPU.kernelMode && CPU.IR != 30)
			CPU.execcounter = execcounter + 1;
	}

}
