
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Reads data from file specified in command line
 * 
 * Implements the sending side of the Stop-and-Wait Protocol
 * 
 */
public class p2mpclient {
	
	private static int sequenceNum = 0;
	
	public static void main(String[] args) throws IOException {
		
		ArrayList<String> servers = new ArrayList<>();
		
		// Get command line arguments
		// add hostname of each server to the array list
		// get file name
		// get MSS (max segment size)
		
		// mss and filename are hard-coded below for now
		int mss = 512;
		String workingDir = System.getProperty("user.dir");
		String filename = "data_small.txt";
		String filepath = workingDir + System.getProperty("file.separator") + filename;
		File file = new File(filepath);
		
		byte[] fileBytes = Files.readAllBytes(file.toPath());
		int offset = 0;
		
		try {
			while (offset < fileBytes.length) {
				long bytesRemaining = fileBytes.length - offset;
				int dataLen = (int) Math.min(mss, bytesRemaining);
				byte[] data = new byte[dataLen];
				
				data = Arrays.copyOfRange(fileBytes, offset, offset + dataLen);
				
				rdt_send(data);
				
				offset += dataLen;
			}
		} catch (IOException e) {
			System.out.println("Error reading data from file");
			e.printStackTrace();
		}
	}
	
	private static void rdt_send(byte[] data) throws IOException {
		String segment = getHeader(data);
		
		for (byte b : data) {
			segment += b;
		}
		
		System.out.println(segment);
	}
	
	/*
	 * Prepares the header for the segment to be sent to servers
	 */
	private static String getHeader(byte[] data) {
		// Pad the sequence number string with leading zeros so that it contains 16 characters
		String sequence = String.format("%16s",  Integer.toBinaryString(sequenceNum)).replace(' ', '0');
		String checksum = getChecksum(data);
		String dataPacket = "0101010101010101";
		
		sequenceNum++;
		
		return sequence + checksum + dataPacket;
	}
	
	/*
	 * Computes the 16-bit checksum and returns it as a String
	 */
	private static String getChecksum(byte[] data) {
		long sum = 0;
		
		for (int i = 0; i < data.length; i+=2) {
			if (i+1 < data.length) {
				sum += bytesToShort(data[i], data[i+1]);
			} else {
				sum += bytesToShort(data[i], (byte)0);
			}
		}
		
		while ((sum >> Short.SIZE) != 0) {
			sum = (sum & 0xFFFF) + (sum >> Short.SIZE);
		}
		
		sum = ~sum;
		
		return Integer.toBinaryString((int)sum).substring(16);
	}
	
	/*
	 * Takes 2 bytes and concatenates them to create a 16-bit word
	 */
	private static short bytesToShort(byte a, byte b) {
		short sh = (short) a;
		sh <<= 8;
		
		return (short)(sh | b);
	}
	
}
