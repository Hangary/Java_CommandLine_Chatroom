# Java_CommandLine_Chatroom

To run the program:

```
# 1. complie all java codes
javac */*.java javac *.java

# 2. run the Server
# java Server [port] [block_duration] [time_out] 
java Server 2050 30 30

# 3. run the Client
# java Client [server_ip_address] [port] 
java Client localhost 2050
```

## System Description

- **Server**: After being started, the server would first create a server socket, which waits for a connection from a client.
  - When it receives a new connection, the server would create a new thread ServerConnection in charge of this new connection.
  - The thread would authenticate the client first. After authenticating, the client can send their commands and the thread would respond correspondingly.
- **Client**: After being started, the client would start three threads:
  1. `UserThread`: in charge of dealing with the user input and send it to the server
  2. `ServerThread`: in charge of dealing with the client input and display it to the user
  3. `P2PThread`: in charge of dealing with the peer input or output and display it to the user

## Application Layer Message Design

To keep messages between the server and clients as simple as possible, there are only few kinds of message formats:
- **From server to client**:
  1. `youare [username]`: this message is used to set the username of the client after auth. 
  2. `p2p [username] [ip_address] [port]`: this message is used to transfer the peer-to- peer information of another client.
  3. `close`: this message is used to close the client. 
  4. `text` (Default): other messages would be displayed directly.
- **From client to server**: the client would send user commands directly to the client.
- **From client to another client (peer-to-peer)**:
  1. `thisis [username]`: this message is used to inform the peer client the username. 
  2. `stopprivate [username]`: this message is used to inform the peer client stop the connection with the user.
  3. `text` (Default): other messages would be displayed directly.
