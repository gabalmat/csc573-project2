
public class p2mpserver implements Runnable {
    private Integer portNumber;
    private String fileName;
    private Float probOfError;

    public static void main(String[] args) {
        int portNumber; String fileName; float p;

        // parse inputs and verify
        if (args.length != 3) {
            _printError("Missing required args: port#, file-name, p");
            return;
        }

        try {
            fileName = args[1];

            portNumber = Integer.parseInt(args[0]);
            if(portNumber != 7735) {
                throw new IllegalArgumentException("Port number must be 7735");
            }

            p = Float.parseFloat(args[2]);
            if (p < 0 || p > 1) {
                throw new IllegalArgumentException("The probability value p" +
                        " must be between 0 < p <= 1");
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
    public p2mpserver(int portNumber, String fileName, float probOfError) {
        this.portNumber = portNumber;
        this.fileName = fileName;
        this.probOfError = probOfError;

        String msg = "Server started. Arguments are (" +
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
        _printMessage("Inside run() method");
    }

    private static void _printMessage(String message) {
        System.out.println("\r\np2mpServer: " + message);
    }

    private static void _printError(String message) {
        System.out.println("\r\np2mpServer [Error]: " + message);
    }
}
