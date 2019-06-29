
public class p2mpserver implements Runnable {

    public static void main(String[] args) {
        p2mpserver server = new p2mpserver();
        server.run();
    }

    /**
     * Constructor
     */
    public p2mpserver() {
        printMessage("Server started");
    }

    @Override
    public void run() {
        printMessage("Inside run() method");
    }

    private void printMessage(String message) {
        System.out.println("\r\np2mpServer: " + message);
    }
}
