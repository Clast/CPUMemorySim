import java.io.*;
import java.util.Arrays;
import java.util.Scanner;
import java.lang.Runtime;

/**
 * Process representing Memory in a computer. Provides a memory array which can be read and written to
 * using text commands across stdout and stdin.
 * 
 * Initializes itself when launched with a file name.
 * 
 * @author Daniel Rich
 *
 */

public class Memory 
{

	public static int[] Memory = new int[2000];

	public static void main(String[] args)

	{
		String file = args[0]; //Target of program to run

		//Initialize Memory to 0's
		Arrays.fill(Memory, 0);
		
		//Open and begin reading the file
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			int memindex = 0;
			int result = -1;
			while ((line = br.readLine()) != null) {

				//Replace all extra characters after the first space with nothing
				line = line.replaceAll(" .+$", "");

				//If the line is blank, skip it
				if (line.equals(""))
				{
					continue;
					//System.out.print("I'llbeignored");
				}

				//If line starts with ., redirect to another mem location
				if (line.startsWith("."))
				{
					line = line.substring(1);
					result = Integer.parseInt(line);
					memindex = result;
					continue;
				}

				//Last minute trimming in case of non-caught strange characters
				result = Integer.parseInt(line.trim() );
				
				//Save number to memory loc.
				Memory[memindex] = result;
				memindex++;

			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//End Initialization

		Scanner sc = new Scanner(System.in);
		String input = "init";
		int address;
		int data;

		//Listen for input from stdinput
		while (!input.equals("quit"))
		{
			input = sc.nextLine();

			//Read command, format "raddress"
			if(input.startsWith("r"))
			{
				input = input.substring(1);
				address = Integer.parseInt(input);
				read(address);
				continue;
			}

			//Write command, format "waddress data"
			if(input.startsWith("w"))
			{
				input = input.substring(1);
				String[] values = input.split(" ");
				address = Integer.parseInt(values[0]); 
				data = Integer.parseInt(values[1]);
				write(address,data);
				continue;
			}

		}


	}
	
	/**
	 * Reads data from given address
	 * @param address Address to read memory from
	 * @result is printed to stdout
	 */
	static public void read(int address)
	{
		System.out.println(Memory[address]);
		System.out.flush();
	}

	/**
	 * Writes data at given memory address
	 * @param address Address to write to
	 * @param data Data to write to
	 */
	static public void write(int address, int data)
	{
		Memory[address] = data;
	}
}