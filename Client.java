import java.net.InetAddress;
import Client.ClientMain;

public class Client {
    // main program
    public static void main(String[] args) throws Exception {
        if (args.length != 2){
            System.out.println("Usage: java UDPClinet localhost PortNo");
            System.exit(1);
        }

        ClientMain client = new ClientMain(
                InetAddress.getByName(args[0]),
                Integer.parseInt(args[1]));
        client.run();
    }
}
