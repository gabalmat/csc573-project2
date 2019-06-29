
public class p2mpServer implements Runnable {

    public static void main(String[] args) {
        p2mpServer server = new p2mpServer();
        server.run();
    }


    /**
     * Constructor
     */
    public p2mpServer() {
        printMessage("Server started");
    }

    private void printMessage(String message) {
        String builder = "\r\np2mpServer: " + message;
        System.out.println(builder);
    }

    @Override
    public void run() {
        printMessage("Inside run() method");
    }
}
