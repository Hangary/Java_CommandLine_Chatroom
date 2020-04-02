package Server;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

class UserControl {

    static private UserControl instance;

    private HashMap<String, User> users;
    private File credentials;

    private UserControl(File file) throws Exception {
        this.credentials = file;
        this.init();
    }

    private void init() throws Exception {
        users = new HashMap<String, User>();
        Scanner sc = new Scanner(credentials);

        while (sc.hasNextLine()) {
            String[] line = sc.nextLine().split(" ", 2);
            users.put(line[0], new User(line[0], line[1]));
        }
    }

    Boolean verifyPassword(String accountName, String password) {
        User user = users.get(accountName);
        return user != null && user.verify(password);
    }

    static UserControl getInstance(File credentials) throws Exception {
        if (instance == null || instance.credentials != credentials) {
            instance = new UserControl(credentials);
        }
        return instance;
    }

    void logIn(String account, ServerConnection serverConnection) {
        User user = users.get(account);
        user.logIn(serverConnection);
        notifyUserStatus(user, user.getUserName() + " logged in");
    }

    void logOut(String account) {
        User user = users.get(account);
        user.logOut();
        notifyUserStatus(user, user.getUserName() + " logged out");
    }

    private void notifyUserStatus(User changed_user, String status_message) {
        getOnlineUsers().forEach(user -> {
            if (!user.equals(changed_user) && !changed_user.isBlock(user))
                user.addMessage(status_message);
        });
    }

    User getUser(String userName) {
        return users.get(userName);
    }

    List<User> getOnlineUsers() {
        return users.values().stream()
                .filter(User::isOnline)
                .collect(Collectors.toList());
    }

    List<User> getOnlineUsersWithin(int seconds) {
        return users.values().stream()
                .filter(user -> user.isOnline(seconds))
                .collect(Collectors.toList());
    }

}
