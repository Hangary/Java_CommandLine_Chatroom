package Client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.util.Arrays;

public class UserThread extends Thread {

    private ClientMain client;
    private BufferedReader inFromUser;
    private DataOutputStream outToServer;


    UserThread(ClientMain client, BufferedReader inFromUser, DataOutputStream outToServer) {
        this.client = client;
        this.inFromUser = inFromUser;
        this.outToServer = outToServer;
    }


    private void processUserInput(String input) throws Exception {
        // if command is empty, do not send
        if (input.trim().equals(""))
            return;

        String[] inputTokens = input.split(" ");
        String header = inputTokens[0];

        switch (header) {
            case "private":
                if (input.split(" ").length < 3) {
                    ClientMain.display("Error. Incorrect private command.");
                } else {
                    String targetUser = input.split(" ")[1];
                    PrivateConnection targetConnection = client.getPrivateConnection(targetUser);
                    if (targetConnection == null) {
                        ClientMain.display(String.format("Error. Private messaging to %s not enabled", targetUser));
                    } else {
                        String[] msgTokens = input.split(" ");
                        String message = String.join(" ", Arrays.asList(msgTokens).subList(2, msgTokens.length));
                        targetConnection.sendMessage(client.getUsername() + "(private): " + message);
                    }
                }
                break;
            case "stopprivate":
                if (input.split(" ").length != 2) {
                    ClientMain.display("Error. Incorrect stopprivate command.");
                } else {
                    String targetUser = input.split(" ")[1];
                    PrivateConnection targetConnection = client.getPrivateConnection(targetUser);
                    if (targetConnection == null) {
                        ClientMain.display(String.format("Error. Private messaging to %s is not enabled", targetUser));
                    } else {
                        targetConnection.sendMessage("stopprivate " + client.getUsername() + "\n");
                        targetConnection.stopprivate();
                        client.removePrivateConnection(targetConnection);
                    }
                }
                break;
            case "logout":
                outToServer.writeBytes(input + '\n');
                client.close();
                break;
            default:
                outToServer.writeBytes(input + '\n');
        }
    }

    public void run() {
        try {
            while (true) {
                client.removeClosedPrivateConnections();
                if (inFromUser.ready())
                    processUserInput(inFromUser.readLine());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
