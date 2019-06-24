package client;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Reads data from file specified in command line
 * 
 * Implements the sending side of the Stop-and-Wait Protocol
 * 
 */
public class p2mpclient {
	
	private static BufferedReader reader;
	
	public static void main(String[] args) throws IOException {
		
		int mss = 500;
		ArrayList<String> servers = new ArrayList<>();
		
		// Get command line arguments
		// add hostname of each server to the array list
		// get file name
		// get MSS (max segment size)
		
		String workingDir = System.getProperty("user.dir");
		String filename = "data_small.txt";
		String filepath = workingDir + System.getProperty("file.separator") + filename;
		
		reader = new BufferedReader(new FileReader(filepath));
		
		// Call rdt_send to send segments to servers
		rdt_send(mss);
	}
	
	private static void rdt_send(int mss) throws IOException {
		boolean endOfFile = false;
		
		try {
			
			while (!endOfFile) {
				char[] buffer = new char[mss];
				int numBytesRead = reader.read(buffer, 0, mss);
				
				if (numBytesRead > -1) {
					
					
					
				} else {
					endOfFile = true;
				}
			}
			
		} catch (IOException e) {
			System.out.println("Error reading data from file");
			e.printStackTrace();
			
		} finally {
			reader.close();
		}
		
	}
	
}
