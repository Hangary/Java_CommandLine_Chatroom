package Client;

import java.io.BufferedReader;
import java.net.InetAddress;
import java.net.Socket;

public class ServerThread extends Thread {

    private ClientMain client;
    private BufferedReader inFromServer;

    ServerThread(ClientMain client, BufferedReader inFromServer) {
        this.client = client;
        this.inFromServer = inFromServer;
    }

    private void processServerInput(String input) throws Exception {
        if (input.trim().equals(""))
            return;

        String[] inputTokens = input.split(" ");
        String header = inputTokens[0];

        switch (header) {
            case "youare":
                client.setUsername(input.split(" ")[1]);
                break;
            case "p2p":
                client.display("Trying to connect: " + input);
                PrivateConnection connection = new PrivateConnection(
                        new Socket(InetAddress.getByName(input.split(" ")[2].replace("/", "")),
                                Integer.parseInt(input.split(" ")[3]))
                );
                connection.start();
                connection.sendMessage(String.format("thisis %s\n", client.getUsername()));
                client.addPrivateConnection(connection);
                break;
            case "close":
                client.close();
                break;
            default:
                client.display(input);
        }
    }


    public void run() {
        try {
            while (true) {
                if (inFromServer.ready())
                    processServerInput(inFromServer.readLine());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
