package Server;

import javax.management.InstanceNotFoundException;
import java.io.*;
import java.net.*;

public class ServerMain {

    static private final File Credential = new File("credentials.txt");

    static private UserControl userControl;
    static private ServerSocket serverSocket;

    static public int block_duration;
    static public int timeout;

    public ServerMain(int port) throws Exception {
        serverSocket = new ServerSocket(port);
        userControl = UserControl.getInstance(Credential);
        System.out.printf("WebServer is ready on %s:%s:\n", InetAddress.getLocalHost(), port);
    }

    public void run() throws Exception {
        while (true)
            runNewConnection(serverSocket.accept());
    }

    // start a new thread for a new connection
    private void runNewConnection(Socket socket) {
        try {
            ServerConnection serverConnection = new ServerConnection(socket, userControl);
            serverConnection.start();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
