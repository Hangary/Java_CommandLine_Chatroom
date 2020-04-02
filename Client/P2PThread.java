package Client;

import java.net.ServerSocket;
import java.net.Socket;

public class P2PThread extends Thread {

    private ClientMain client;

    P2PThread(ClientMain client) {
        this.client = client;
    }

    private void runNewConnection(Socket socket) {
        try {
            PrivateConnection connection = new PrivateConnection(socket);
            connection.start();
            connection.sendMessage(String.format("thisis %s\n", client.getUsername()));
            client.addPrivateConnection(connection);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(client.getLocalPort() + 1);
            //System.out.println("P2P is listening on port: " + (socket.getLocalPort() + 1));
            while (true)
                runNewConnection(serverSocket.accept());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
