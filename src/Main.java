import modules.cluster.ClusterJwt;

public class Main {

    public static void main(String[] args) {
        SimpleHttpServer.key = ClusterJwt.key;

        EverythingAtHomeServer server = new EverythingAtHomeServer();
        server.start();

        SimpleHttpServer httpServer = new SimpleHttpServer(8080);
        httpServer.start(true);

        // Add shutdown hook to stop the server gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop();
        }));
    }
}
