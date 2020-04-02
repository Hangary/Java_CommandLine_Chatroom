package Server;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;

class User {

    private String username;
    private String password;

    private boolean online;
    private ServerConnection serverConnection;

    private List<User> blockedUsers;
    private LinkedList<String> messages;

    private LocalDateTime lastOnlineTime;
    private LocalDateTime blockUntil;

    User(String username, String password) {
        this.username = username;
        this.password = password;
        this.online = false;
        this.serverConnection = null;
        this.blockedUsers = new LinkedList<User>();
        this.messages = new LinkedList<String>();
    }

    Boolean verify(String inputPassword) {
        return inputPassword.equals(password);
    }

    String getUserName() {
        return username;
    }

    boolean isOnline() {
        return online;
    }

    boolean isOnline(int seconds) {
        if (isOnline())
            return true;
        if (lastOnlineTime == null)
            return false;
        else
            return ChronoUnit.SECONDS.between(lastOnlineTime, LocalDateTime.now()) <= seconds;
    }

    void logIn(ServerConnection serverConnection) {
        this.online = true;
        this.serverConnection = serverConnection;
        this.lastOnlineTime = LocalDateTime.now();
    }

    void logOut() {
        this.online = false;
        this.serverConnection = null;
        this.lastOnlineTime = LocalDateTime.now();
    }

    boolean isBlock(User user) {
        return this.blockedUsers.contains(user);
    }

    boolean isBlockedForLogin() {
        if (blockUntil == null)
            return false;
        else
            return LocalDateTime.now().compareTo(blockUntil) <= 0;
    }

    void blockForLogin(int seconds) {
        this.blockUntil = LocalDateTime.now().plusSeconds(seconds);
    }

    int getBlockForLoginRestTime() {
        if (blockUntil == null)
            return 0;
        else
            return (int) ChronoUnit.SECONDS.between(blockUntil, LocalDateTime.now());
    }

    void block(User blockedUser) {
        this.blockedUsers.add(blockedUser);
    }

    void unblock(User blockedUser) { this.blockedUsers.remove(blockedUser); }


    void addMessage(String message) {
        this.messages.add(message);
    }

    LinkedList<String> getMessages() {
        return messages;
    }

    ServerConnection getServerConnection() {
        return this.serverConnection;
    }

}
