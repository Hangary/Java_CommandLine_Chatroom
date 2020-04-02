package Server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.stream.Collectors;

class ServerConnection extends Thread {

    private User user;
    private Socket userSocket;
    private BufferedReader inFromUser;
    private DataOutputStream outToUser;
    private UserControl userControl;
    private boolean bExit;

    // timeout detection
    private LocalDateTime timeOfLastRequestFromUser;
    private class TimeoutException extends Exception {;}

    ServerConnection(Socket userSocket, UserControl userControl) throws Exception {
        this.userSocket = userSocket;
        this.bExit = false;
        this.userControl = userControl;
        this.inFromUser = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
        this.outToUser = new DataOutputStream(userSocket.getOutputStream());
        this.timeOfLastRequestFromUser = LocalDateTime.now();
        System.out.println("A new connection for has been established.");
    }

    private int getPort() {
        return this.userSocket.getPort();
    }

    private InetAddress getAddress() {
        return this.userSocket.getInetAddress();
    }

    private boolean whetherTimeout() {
        return ChronoUnit.SECONDS.between(timeOfLastRequestFromUser, LocalDateTime.now()) > ServerMain.timeout;
    }

    private String receiveDate() throws Exception {
        while (!inFromUser.ready()) {
            // if time out, throw Exception
            if (whetherTimeout())
                throw new TimeoutException();
        }
        timeOfLastRequestFromUser = LocalDateTime.now();
        return inFromUser.readLine();
    }

    @Override
    public void run() {
        try {
            // when the user is online
            while (!bExit && userSocket.isConnected()) {
                // if not logged in, auth first
                if (user == null) {
                    authenticate();
                } else {
                    // after auth send messages and process request
                    sendAllMessages();
                    if (whetherTimeout())
                        throw new TimeoutException();
                    if (inFromUser.ready())
                        processRequest();
                }
            }
        } catch (TimeoutException e) {
            try {
                System.out.println("Due to long time of not receiving commands, a user has been logged out.");
                outToUser.writeBytes("Due to long time of not receiving commands. Your account has automatically logged out.\n");
                outToUser.writeBytes("close\n");
            } catch (IOException ioe) {
                System.out.println("An error happened on a connection: " + ioe.getMessage());
            }
        } catch (Exception e) {
            System.out.println("An error happened on a connection: " + e.getMessage());
            //e.printStackTrace();
        } finally {
            // let user logout
            if (this.user != null)
                userControl.logOut(user.getUserName());
            try {
                outToUser.close();
                userSocket.close();
            } catch (IOException ioe) {
                System.out.println("An error happened when closing a connection: " + ioe.getMessage());
            }
        }
    }

    // process input from user
    private void processRequest() throws Exception {
        String request = receiveDate();
        System.out.println(String.format("Receive a Request From %s : '%s'", this.user.getUserName(), request));

        String header = request.split(" ")[0].toLowerCase();
        switch (header) {
            case "logout":
                logout();
                break;
            case "whoelse":
                whoelse();
                break;
            case "whoelsesince":
                whoelsesince(request);
                break;
            case "message":
                message(request);
                break;
            case "broadcast":
                broadcast(request);
                break;
            case "block":
                block(request);
                break;
            case "unblock":
                unblock(request);
                break;
            case "startprivate":
                startprivate(request);
                break;
            default:
                error("Invalid Command.");
        }
    }


    private void authenticate() throws Exception {
        System.out.println("Authenticating a new user");

        // Get the username
        outToUser.writeBytes("Please Input Username: \n");
        String username = receiveDate();

        // get the user
        User user = userControl.getUser(username);
        if (user == null) {
            error("We cannot find this user.");
            return;
        } else if (user.isOnline()) {
            error("This user has already logged in.");
            return;
        } else if (user.isBlockedForLogin()) {
            System.out.println(user.getUserName() + " is blocked until " + user.getBlockForLoginRestTime());
            error("This account is blocked due to multiple login failures. Please try again later.");
            return;
        }

        // check the password
        int try_count = 0;
        outToUser.writeBytes("Please Input Password: \n");
        String password = receiveDate();
        try_count++;

        while (!userControl.verifyPassword(username, password)) {
            if (try_count < 3) {
                outToUser.writeBytes("Invalid Password. Please try again: \n");
            } else {
                outToUser.writeBytes("Invalid Password. Your account has been blocked. Please try again later \n");
                user.blockForLogin(ServerMain.block_duration);
                return;
            }
            password = receiveDate();
            try_count++;
        }

        this.user = userControl.getUser(username);
        userControl.logIn(username, this);
        outToUser.writeBytes(String.format("youare %s\n", username));
        outToUser.writeBytes("Welcome to the greatest messaging application ever! \n");
    }


    private void sendAllMessages() throws Exception {
        LinkedList<String> messages = this.user.getMessages();
        while (messages.size() > 0) {
            String message = messages.poll();
            outToUser.writeBytes(message + "\n");
        }
    }


    private void error(String error_info) throws Exception {
        outToUser.writeBytes(String.format("Error. %s \n", error_info));
    }


    private void whoelse() throws Exception {
        String message = String.join(
                "\n",
                userControl.getOnlineUsers().stream()
                        .map(User::getUserName)
                        .filter(name -> !name.equals(this.user.getUserName()))
                        .collect(Collectors.toList()));
        outToUser.writeBytes(message + "\n");
    }


    private void whoelsesince(String request) throws Exception {
        String[] msgTokens = request.split(" ");
        // if too short
        if (msgTokens.length < 2) {
            error("Incorrect whoelsesince format!");
            return;
        }

        int seconds = Integer.parseInt(msgTokens[1]);
        String message = String.join(
                "\n",
                userControl.getOnlineUsersWithin(seconds).stream()
                        .map(User::getUserName)
                        .filter(name -> !name.equals(this.user.getUserName()))
                        .collect(Collectors.toList()));
        outToUser.writeBytes(message + "\n");
    }


    private void message(String request) throws Exception {
        String[] msgTokens = request.split(" ");
        // if too short
        if (msgTokens.length < 3) {
            error("Incorrect message format!");
            return;
        }

        User targetUser = userControl.getUser(msgTokens[1]);
        // if cannot find this user
        if (targetUser == null) {
            error("Target user does not exist!");
            return;
        } else if (targetUser == this.user) {
            error("You cannot message yourself!");
            return;
        } else if (targetUser.isBlock(this.user)) {
            error("Your message could not be delivered as the recipient has blocked you\n");
            return;
        }

        String message = String.join(" ", Arrays.asList(msgTokens).subList(2, msgTokens.length));
        targetUser.addMessage(String.format("%s: %s", this.user.getUserName(), message));
    }


    private void broadcast(String request) throws Exception {
        String[] msgTokens = request.split(" ");
        // if too short
        if (msgTokens.length < 2) {
            error("Incorrect broadcast format!");
            return;
        }

        String message = String.join(" ", Arrays.asList(msgTokens).subList(1, msgTokens.length));
        boolean blocked = false;
        for (User user : userControl.getOnlineUsers()) {
            if (user.isBlock(this.user))
                blocked = true;
            else if (user != this.user)
                user.addMessage(String.format("%s: %s", this.user.getUserName(), message));
        }
        if (blocked)
            outToUser.writeBytes("Your message could not be delivered to some recipients\n");
    }


    private void block(String request) throws Exception {
        String[] msgTokens = request.split(" ");
        // if too short
        if (msgTokens.length != 2) {
            error("Incorrect block format!");
            return;
        }

        User targetUser = userControl.getUser(msgTokens[1]);
        if (targetUser == null) {
            // if cannot find this user
            error("Incorrect target user!");
            return;
        }

        if (this.user == targetUser) {
            error("Cannot block self");
        } else if (this.user.isBlock(targetUser)) {
            error(String.format("%s was already blocked", targetUser.getUserName()));
        } else {
            this.user.block(targetUser);
            outToUser.writeBytes(String.format("%s is blocked\n", targetUser.getUserName()));
        }
    }


    private void unblock(String request) throws Exception {
        String[] msgTokens = request.split(" ");
        // if too short
        if (msgTokens.length != 2) {
            error("Incorrect unblock format!");
            return;
        }

        User targetUser = userControl.getUser(msgTokens[1]);
        if (targetUser == null) {
            // if cannot find this user
            error("Incorrect target user!");
            return;
        }

        if (this.user.isBlock(targetUser)) {
            this.user.unblock(targetUser);
            outToUser.writeBytes(String.format("%s is unblocked\n", targetUser.getUserName()));
        } else
            error(String.format("%s was not blocked", targetUser.getUserName()));
    }


    private void startprivate(String request) throws Exception {
        String[] msgTokens = request.split(" ");
        // if too short
        if (msgTokens.length != 2) {
            error("Incorrect startprivate format!");
            return;
        }

        User targetUser = userControl.getUser(msgTokens[1]);
        if (targetUser == null) // if cannot find this user
            error("Incorrect target user!");
        else if (targetUser == this.user)
            error("You cannot private messaeg yourself!");
        else if (targetUser.isBlock(this.user))
            error(String.format("%s has blocked you!", targetUser.getUserName()));
        else if (!targetUser.isOnline())
            error(String.format("%s is offline.", targetUser.getUserName()));
        else {
            // if it is on localhost
            String targetAddress = targetUser.getServerConnection().getAddress().getHostAddress();
            if (targetAddress.equals("127.0.0.1"))
                targetAddress = InetAddress.getLocalHost().getHostAddress();
            // "p2p username address port"
            outToUser.writeBytes(String.format(
                    "p2p %s %s %s\n",
                    targetUser.getUserName(), targetAddress, targetUser.getServerConnection().getPort() + 1));
        }
    }


    private void logout() throws Exception {
        bExit = true;
    }

}
