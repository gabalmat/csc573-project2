
import javax.xml.crypto.Data;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads data from file specified in command line
 * 
 * Implements the sending side of the Stop-and-Wait Protocol
 * 
 */
public class p2mpclient {
	private static final int NUM_BYTES_IN_HEADER = 8;
	private static final char DATA_PACKET_VALUE = 0b0101010101010101;
	private static final int SERVER_PORT_NUMBER = 7735;
	private static final int SERVER_RESPONSE_BYTES = 8;
	
	private static int sequenceNum = 0;
	private static int mss;
	private static Map<InetAddress, DatagramSocket> serverSockets = new HashMap<>();
	
	public static void main(String[] args) throws IOException {
		
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

		// Add one server (localhost) for now
		InetAddress receiverAddress = InetAddress.getByName("127.0.0.1");
		DatagramSocket socket = new DatagramSocket();
		socket.setSoTimeout(500);
		serverSockets.put(receiverAddress, socket);

		
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
		
		byte[] segmentBytes = bb.array();

		System.out.println("The mss to send: " + data.length);
		System.out.println();


        // Send data to servers
		for (Map.Entry<InetAddress, DatagramSocket> entry : serverSockets.entrySet()) {
			boolean keepSending = true;
			InetAddress receiverAddress = entry.getKey();
			DatagramSocket socket = entry.getValue();

			// Hardcode the receiver address to be localhost for now...
			DatagramPacket outPacket = new DatagramPacket(segmentBytes, segmentBytes.length,
					receiverAddress, SERVER_PORT_NUMBER);

			socket.setSoTimeout(500);

			byte[] responseBuf = new byte[SERVER_RESPONSE_BYTES];
			DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length);

			socket.send(outPacket);

			while (keepSending) {
				try {
					socket.receive(responsePacket);
					keepSending = false;
				}
				catch (SocketTimeoutException e) {
					socket.send(outPacket);
					System.out.println("Sent packet again");
					continue;
				}
			}

			System.out.println("Received " + responsePacket.getLength() + " ACK bytes from server");
		}

		sequenceNum += data.length;
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
		char checksum = (char)getChecksum(data);
		bb.putChar(checksum);
		
		// Add the 16-bit data packet header value
		bb.putChar(DATA_PACKET_VALUE);
		

		System.out.println("\r\nSequence Number to send: " + (sequenceNum));
		System.out.println("Checksum to send: " + Integer.toBinaryString(checksum));
		System.out.println("Data flag to send: " + Integer.toBinaryString(DATA_PACKET_VALUE));
		
		return bb.array();
	}
	
	/*
	 * Computes and returns the 16-bit checksum as a short
	 */
    private static long getChecksum(byte[] buf) {
		int length = buf.length;
		int i = 0;

		long sum = 0;
		long data;

		// Handle all pairs
		while (length > 1) {
			// Corrected to include @Andy's edits and various comments on Stack Overflow
			data = (((buf[i] << 8) & 0xFF00) | ((buf[i + 1]) & 0xFF));
			sum += data;
			// 1's complement carry bit correction in 16-bits (detecting sign extension)
			if ((sum & 0xFFFF0000) > 0) {
				sum = sum & 0xFFFF;
				sum += 1;
			}

			i += 2;
			length -= 2;
		}

		// Handle remaining byte in odd length buffers
		if (length > 0) {
			// Corrected to include @Andy's edits and various comments on Stack Overflow
			sum += (buf[i] << 8 & 0xFF00);
			// 1's complement carry bit correction in 16-bits (detecting sign extension)
			if ((sum & 0xFFFF0000) > 0) {
				sum = sum & 0xFFFF;
				sum += 1;
			}
		}

		// Final 1's complement value correction to 16-bits
		sum = ~sum;
		sum = sum & 0xFFFF;
		return sum;
	}

    private static void _printError(String message) {
        System.out.println("\r\np2mpClient [Error]: " + message);
    }
}
