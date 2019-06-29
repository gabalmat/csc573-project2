import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Random;

public class p2mpserver implements Runnable {
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
    }

    @Override
    public void run() {
        this.udpSock = createUDPSocket();
        if(this.udpSock == null) { return; }

        try {
            while(this.runProgram) {

                // stay here and listen for incoming data
                DatagramPacket rcvPacket = new DatagramPacket(buffer, buffer.length);
                udpSock.receive(rcvPacket);

                /* At this point client has no more data to send */
                //TODO: At this point is buffer data a segment or a packet?

                // process data
                handleRequest(rcvPacket.getData());

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

    private void handleRequest(byte[] data) {
        _printMessage("The data size received: " + data.length);

        // 1. check R value
        if(!checkR()) { return; }

        // 2. compute checksum
        if(!computeChecksum()) { return; }

        // 3. check if segment is in-sequence
        if(!checkSequence()) { return; }

        // 4. all is good. write data to file
        writeToFile(data);
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
            r = rand.nextDouble(); // nextFloat() is [0,1)...need (0,1)
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

    private static void _printMessage(String message) {
        System.out.println("\r\np2mpServer: " + message);
    }

    private static void _printError(String message) {
        System.out.println("\r\np2mpServer [Error]: " + message);
    }
}
