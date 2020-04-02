package Client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.LinkedList;

public class PrivateConnection extends Thread {

    String username;
    private Socket peerSocket;
    private DataOutputStream outToPeer;
    private BufferedReader inFromPeer;
    private boolean bExit;

    private LinkedList<String> messages;

    PrivateConnection(Socket socket) throws Exception {
        this.peerSocket = socket;
        this.bExit = false;
        this.outToPeer = new DataOutputStream(peerSocket.getOutputStream());
        this.inFromPeer = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
        this.messages = new LinkedList<>();
        ClientMain.display("A new private connection has been established.");
    }

    @Override
    public void run() {
        try {
            while (!bExit && peerSocket.isConnected()) {
                if (inFromPeer.ready())
                    processPeerRequest();
                else if (messages.size() > 0) {
                    do {
                        String message = messages.poll();
                        outToPeer.writeBytes(message + "\n");
                    } while (messages.size() > 0);
                }
            }
            // if the user close
            outToPeer.close();
            peerSocket.close();
        } catch (Exception e) {
            System.out.println("An error happened on the connection for " + this.username + ":\n");
            e.printStackTrace();
        }
    }


    // process input from user
    private void processPeerRequest() throws Exception {
        String peerMessage = inFromPeer.readLine();
        String header = peerMessage.split(" ")[0].toLowerCase();
        if (header.equals("stopprivate")) {
            bExit = true;
            ClientMain.display("stop private connection with " + peerMessage.split(" ")[1]);
        } else if (header.equals("thisis"))
            this.username = peerMessage.split(" ")[1];
        else
            ClientMain.display(peerMessage);
    }

    void sendMessage(String message) {
        this.messages.add(message);
    }

    void stopprivate() {
        this.bExit = true;
    }

    boolean isStopped() {
        return this.bExit || this.peerSocket.isClosed();
    }

}

