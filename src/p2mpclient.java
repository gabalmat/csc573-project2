
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
	private static final int NUM_BYTES_IN_HEADER = 8;
	private static final short DATA_PACKET_VALUE = (short) 0b0101010101010101;
	
	private static int sequenceNum = 0;
	private static int mss;
	
	public static void main(String[] args) throws IOException {
		
		ArrayList<String> servers = new ArrayList<>();
		
		// Get command line arguments
		// add hostname of each server to the array list
		// get file name
		// get MSS (max segment size)
		
		// mss and filename are hard-coded below for now
		mss = 512;
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
		ByteBuffer bb = ByteBuffer.allocate(NUM_BYTES_IN_HEADER + data.length);
		
		byte[] headerBytes = getHeader(data);
		bb.put(headerBytes);
		bb.put(data);
		
		byte[] test = bb.array();
		
		System.out.println("HEY");
		
		// Send bytes here....
	}
	
	/*
	 * Gets the header as an 8 element byte[] array
	 */
	private static byte[] getHeader(byte[] data) {
		
		// Create a ByteBuffer for storing the bytes
		ByteBuffer bb = ByteBuffer.allocate(NUM_BYTES_IN_HEADER);
		
		// Add the 32-bit sequence number
		bb.putInt(sequenceNum);
		
		// Add the 16-bit checksum
		short checksum = getChecksum(data);
		bb.putShort(checksum);
		
		// Add the 16-bit data packet header value
		bb.putShort(DATA_PACKET_VALUE);
		
		sequenceNum += data.length;
		
		return bb.array();
	}
	
	/*
	 * Computes the 16-bit checksum and returns the ASCII value of each
	 * character in a byte[] array
	 */
	private static short getChecksum(byte[] data) {
		long sum = 0;
		
		// Combine every 2 bytes of data into a 16-bit word and add to sum
		for (int i = 0; i < data.length; i+=2) {
			if (i+1 < data.length) {
				sum += bytesToShort(data[i], data[i+1]);
			} else {
				sum += bytesToShort(data[i], (byte)0);
			}
		}
		
		// Wrap any overflow so that we're guaranteed a 16-bit value
		while ((sum >> Short.SIZE) != 0) {
			sum = (sum & 0xFFFF) + (sum >> Short.SIZE);
		}
		
		// Take the one's complement
		sum = ~sum;
		
		return (short) sum;
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
