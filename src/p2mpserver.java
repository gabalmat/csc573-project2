import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class p2mpserver implements Runnable {
    private static int ACK_HEADER_FIELD_1 = 0x0000;
    private static int ACK_HEADER_FIELD_2 = 0xAAAA;
    private static int ALL_16_ONES = 0xFFFF;
    private static String NEWLINE = "\r\n";
    private static int MAX_UDP_BYTES = 65535;
    private static int NUM_BYTES_IN_HEADER = 8;

    private Integer portNumber;
    private String fileName;
    private Double probOfError;
    private boolean runProgram;
    private DatagramSocket udpSock;
    private byte[] buffer;
    private ArrayList<Integer> seqNumbersArray;

    public static void main(String[] args) {
        int portNumber; String fileName; double p;

        try {
            // parse inputs and verify
            if (args.length != 3) {
                throw new IllegalArgumentException("Missing required args: " +
                        "port#, file-name, p");
            }

            portNumber = Integer.parseInt(args[0]);
            if(portNumber != 7735) {
                throw new IllegalArgumentException("Port number must be 7735");
            }

            fileName = args[1];
            if(fileName.isEmpty()) {
                throw new IllegalArgumentException("File name cannot be empty");
            }

            p = Double.parseDouble(args[2]);
            if (p <= 0 || p >= 1) {
                throw new IllegalArgumentException("The probability value p" +
                        " must be between 0 < p < 1");
            }
        }
        catch (Exception e) {
            _printError(e.toString());
            return;
        }

        // start the program
        p2mpserver server = new p2mpserver(portNumber, fileName, p);
        server.run();
    }

    /**
     * Constructor
     */
    public p2mpserver(int portNumber, String fileName, double probOfError) {
        this.portNumber = portNumber;
        this.fileName = fileName;
        this.probOfError = probOfError;
        this.runProgram = true;
        this.seqNumbersArray = new ArrayList<Integer>();
        this.buffer = new byte[MAX_UDP_BYTES];

        String msg = "Command line args are (" +
                "port #: " +
                this.portNumber.toString() +
                ", file-name: " +
                this.fileName +
                ", p: " +
                this.probOfError +
                ")";
        _printMessage(msg);
    }

    @Override
    public void run() {
        _printMessage("Listening for connections...");
        this.udpSock = createUDPSocket();
        if(this.udpSock == null) { return; }

        try {
            while(this.runProgram) {

                // stay here and listen for incoming data
                DatagramPacket rcvPacket = new DatagramPacket(buffer, buffer.length);
                udpSock.receive(rcvPacket);

                /* At this point the buffer is full */

                // process only the bytes sent from client
                int seqNum = processRequest(Arrays.copyOfRange(rcvPacket.getData(), 0, rcvPacket.getLength()));

                if(seqNum != -1) {
                    // create ACK segment
                    byte[] segmentAck = createACK(seqNum);

                    // create the datagram to encapsulate the ack
                    // modify data of the packet with the ACK
                    rcvPacket.setData(segmentAck);

                    // send ACK segment
                    udpSock.send(rcvPacket);
                }

                // clear buffer
                this.buffer = new byte[MAX_UDP_BYTES];
            }
        }
        catch (Exception e) {
           _printError(e.getMessage());
           e.printStackTrace();
        }
        finally {
            this.udpSock.close();
        }
    }

    private int processRequest(byte[] data) {
        _printMessage("The data size received: " + data.length);
        
        // Split data into header field portions and data portion
        byte[] sequenceBytes = Arrays.copyOfRange(data, 0, 4);
        byte[] checksumBytes = new byte[4];
        byte[] _checksumBytes = Arrays.copyOfRange(data, 4, 6);
        checksumBytes[0] = 0;
        checksumBytes[1] = 0;
        checksumBytes[2] = _checksumBytes[0];
        checksumBytes[3] = _checksumBytes[1];

        byte[] dataBytes = Arrays.copyOfRange(data, NUM_BYTES_IN_HEADER, data.length);

        // Get the sequence number
        int sequenceNum = ByteBuffer.wrap(sequenceBytes).getInt();

        // 1. check R value
        if(!checkR()) { sequenceNum = -1; }

        // 2. compute checksum
        int checksum = ByteBuffer.wrap(checksumBytes).getInt();

        if(!verifyChecksum(dataBytes, checksum)) { sequenceNum = -1; }

        // 3. check if segment is in-sequence
        sequenceNum = checkSequence(data, sequenceNum);
//        if(!checkSequence(data, sequenceNum)) { sequenceNum = -1; }

        // 4. all is good. write data to file
        writeToFile(data);

        return sequenceNum;
    }

    private byte[] createACK(int seqNum) {

        // hex for 10101010 is 0xAA and hex for 1010101010101010 is 0xAAAA
        // can use Integer.valueOf or Integer.parseInt to convert from String

        //  ACK Segment Output Example
        //
        //  <32 bit sequence number>
        //  0000000000000000
        //  1010101010101010
        //
        char one = (char)(ACK_HEADER_FIELD_1);
        char two = (char)(ACK_HEADER_FIELD_2);

        int size = ((Integer.SIZE)/8) + ((Character.SIZE * 2)/8);
        ByteBuffer bf = ByteBuffer.allocate(size);
        bf.putInt(seqNum);
        bf.putChar((char)ACK_HEADER_FIELD_1);
        bf.putChar((char)ACK_HEADER_FIELD_2);

        _printMessage("ACK # to be sent out: " + seqNum);
        return bf.array();
    }

    private boolean verifyChecksum(byte[] data, int checksum) {

        ByteBuffer ba = ByteBuffer.allocate(data.length + (Integer.SIZE)/8);
        ba.putInt(checksum);
        ba.put(data);
        byte [] input = ba.array();

    	long dataSum = getSumOfData(input);

    	// Add to checksum and return true if result is 1111111111111111
        return dataSum == 0xFFFF;
    }

    private int checkSequence(byte[] data, int seqNum) {

        //  Client Segment Example
        //  sequence number based on data size
        //
        //  <32 bit sequence number>
        //  <16 bit checksum number>
        //  0101010101010101 (16 bit)
        //
        int mss = getMSSValue(data);

        // use an temporary variable for sequence number
        int _seqNum = seqNum + mss;

        // get the last in-sequence segment number
        int size = this.seqNumbersArray.size();

        // check if this is 1st sequence number
        if (size == 0) {
            this.seqNumbersArray.add(_seqNum);
            return _seqNum;
        }

        /* at this point, there is a sequence number history */

        int lastSeqNum = this.seqNumbersArray.get(size - 1);

        // check if seq # is in-sequence
        if((lastSeqNum + mss) == seqNum) {
            return _seqNum;
        }
        else {
            return -1;
        }
    }

    private void writeToFile(byte[] data) {
        try {
            // Initialize a pointer in file using OutputStream
            String path = System.getProperty("user.dir") + "/" + this.fileName + ".txt";
            File file = new File(path);
            OutputStream os = new FileOutputStream(file);

            // Starts writing the bytes in it
            os.write(data);

            // Close the file
            os.close();
        }
        catch (Exception e) {
           _printError(e.getMessage());
           e.printStackTrace();
        }
    }

    private int getMSSValue(byte[] data) {
        //
        // Size of each element in data is a byte
        // MSS represents size of payload only
        //
        return data.length - NUM_BYTES_IN_HEADER;
    }

    private boolean checkR() {
        //the server will generate a random number r in (0,1)
        double r = getRandomR();
        if(r <= this.probOfError) {
            // received packet is discarded and no other action is taken
            return false;
        }
        return true;
    }

    private double getRandomR() {
        Random rand = new Random();
        double r = 0.0;

        while(r == 0.0) {
            // nextFloat() is [0,1)...need (0,1)
            r = rand.nextDouble();
        }
        return r;
    }

    private DatagramSocket createUDPSocket() {
        try {
            return new DatagramSocket(this.portNumber);
        }
        catch (Exception e){
            _printError(e.toString());
            e.printStackTrace();
            return null;
        }
    }

    /*
     * Takes 2 bytes and concatenates them to create a 16-bit word
     */
    private long bytesToShort(byte a, byte b) {
        long sh = (long) a;
        sh <<= 8;

        return (sh | b);
    }

    private static void _printMessage(String message) {
        System.out.println("\r\np2mpServer: " + message);
    }

    private static void _printError(String message) {
        System.out.println("\r\np2mpServer [Error]: " + message);
    }
    
    private long getSumOfData(byte[] data) {

        long sum = 0;

        // Combine every 2 bytes of data into a 16-bit word and add to sum
        for (int i = 0; i < data.length; i += 2) {
            if (i + 1 < data.length) {
                sum += bytesToShort(data[i], data[i + 1]);
            } else {
                sum += bytesToShort(data[i], (byte) 0);
            }
        }

        // Wrap any overflow so that we're guaranteed a 16-bit value
        while ((sum >> Short.SIZE) != 0) {
            sum = (sum & 0xFFFF) + (sum >> Short.SIZE);
            sum = sum +1;
        }

        _printMessage("The checksum should be 16 1s. Is it? " + Long.toBinaryString(sum));

        return sum;
    }
}
