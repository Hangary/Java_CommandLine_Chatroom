import Server.ServerMain;

public class Server {
    public static void main(String[] args) throws Exception {
        // Get command line argument.
        if (args.length != 3) {
            System.out.println("Required arguments: port, block_duration, time_out");
            return;
        }
        ServerMain webServer = new ServerMain(Integer.parseInt(args[0]));
        ServerMain.block_duration = Integer.parseInt(args[1]);
        ServerMain.timeout = Integer.parseInt(args[2]);
        webServer.run();
    }
}
