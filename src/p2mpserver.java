import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.Random;

public class p2mpserver implements Runnable {
    private static int ACK_HEADER_FIELD_1 = 0x0000;
    private static int ACK_HEADER_FIELD_2 = 0xAAAA;
    private static String NEWLINE = "\r\n";

    private Integer portNumber;
    private String fileName;
    private Double probOfError;
    private boolean runProgram;
    private DatagramSocket udpSock;
    private byte[] buffer = new byte[256]; //TODO: What should buffer size be?

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

        String msg = "Command line args are (" +
                "port #: " +
                this.portNumber.toString() +
                ", file-name: " +
                this.fileName +
                ", p: " +
                this.probOfError +
                ")";
        _printMessage(msg);

        this.runProgram = true;
        createACK(454);
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

                // process data
                int seqNum = processRequest(rcvPacket.getData());

                if(seqNum != -1) {
                    // create ACK segment
                    String ack = createACK(seqNum);

                    // create the datagram to encapsulate the ack
                    // modify data of the packet with the ACK
                    DatagramPacket sndPacket = rcvPacket;
                    sndPacket.setData(ack.getBytes());

                    // send ACK segment
                    udpSock.send(sndPacket);
                }

                // clear buffer
                this.buffer = new byte[256];
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

        int sequenceNum = getSequenceNumber(data);

        // 1. check R value
        if(!checkR()) { sequenceNum = -1; }

        // 2. compute checksum
        if(!computeChecksum()) { sequenceNum = -1; }

        // 3. check if segment is in-sequence
        if(!checkSequence()) { sequenceNum = -1; }

        // 4. all is good. write data to file
        writeToFile(data);

        return sequenceNum;
    }

    /**
     * ACK Segment
     * the 32-bit sequence number that is being ACKed
     * a 16-bit field that is all zeroes, and
     * a 16-bit field that has the value **1010101010101010**,
     * @param seqNum A 32-bit integer (use hex)
     * @return The segment (string)
     */
    private String createACK(int seqNum) {

        // hex for 10101010 is 0xAA and hex for 1010101010101010 is 0xAAAA
        // can use Integer.valueOf or Integer.parseInt to convert from String

        //  ACK Segment Output Example
        //
        //  <32 bit sequence number>
        //  0000000000000000
        //  1010101010101010
        //

        String ackStr = seqNum +
                NEWLINE +
                ACK_HEADER_FIELD_1 +
                NEWLINE +
                ACK_HEADER_FIELD_2 +
                NEWLINE;
        _printMessage("ACK to be sent out:\r\n" + ackStr);
        return ackStr;
    }

    private int getSequenceNumber(byte[] data) {
        // TODO: Implement
        return -1;
    }

    private boolean computeChecksum() {
        //TODO: Implement
        return false;
    }

    private boolean checkSequence() {
        //TODO: Implement
        return false;
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
    private short bytesToShort(byte a, byte b) {
        short sh = (short) a;
        sh <<= 8;

        return (short)(sh | b);
    }

    private static void _printMessage(String message) {
        System.out.println("\r\np2mpServer: " + message);
    }

    private static void _printError(String message) {
        System.out.println("\r\np2mpServer [Error]: " + message);
    }
}
