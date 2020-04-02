package Client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.LinkedList;

public class ClientMain {

    private String username;
    private Socket socket;
    private BufferedReader inFromUser;
    private BufferedReader inFromServer;
    private DataOutputStream outToServer;
    private LinkedList<PrivateConnection> privateConnections;

    public ClientMain(InetAddress serverAddress, int serverPort) throws Exception {
        this.socket = new Socket(serverAddress, serverPort);
        this.inFromUser =
                new BufferedReader(new InputStreamReader(System.in));
        this.inFromServer =
                new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.outToServer =
                new DataOutputStream(socket.getOutputStream());
        this.privateConnections = new LinkedList<PrivateConnection>();
    }

    int getLocalPort() {
        return socket.getLocalPort();
    }

    void addPrivateConnection(PrivateConnection connection) {
        this.privateConnections.add(connection);
    }

    void removePrivateConnection(PrivateConnection connection) {
        this.privateConnections.remove(connection);
    }

    // remove all closed p2p connection
    void removeClosedPrivateConnections() {
        for (PrivateConnection connection : privateConnections) {
            if (connection.isStopped())
                privateConnections.remove(connection);
        }
    }

    PrivateConnection getPrivateConnection(String username) {
        for (PrivateConnection connection : this.privateConnections) {
            if (connection.username != null && connection.username.equals(username))
                return connection;
            }
        return null;
    }

    String getUsername() {
        return this.username;
    }

    void setUsername(String username) {
        this.username = username;
    }

    static void display(String message) {
        final String ANSI_YELLOW_BOLD = "\033[1;33m";
        final String ANSI_RESET = "\u001B[0m";
        System.out.println(ANSI_YELLOW_BOLD + message + ANSI_RESET);
    }

    void close() throws Exception {
        this.socket.close();
        privateConnections.forEach(connection -> connection.sendMessage("stopprivate " + this.username + "\n"));
        System.exit(0);
    }

    public void run() {
        (new P2PThread(this)).start();
        (new ServerThread(this, inFromServer)).start();
        (new UserThread(this, inFromUser, outToServer)).start();
    }
    
}
