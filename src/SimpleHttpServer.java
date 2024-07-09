import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;

import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;

public class SimpleHttpServer {
    private final int port;

    public SimpleHttpServer(int port){
        this.port = port;
    }

    public void start() {
        try {
            // 尝试启动HTTPS服务器
            startHttpsServer();
        } catch (Exception e) {
            System.err.println("Failed to start HTTPS server, falling back to HTTP: " + e.getMessage());
            // 如果启动HTTPS服务器失败，则启动HTTP服务器
            try {
                startHttpServer();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void startHttpsServer() throws Exception {
        // 加载SSL证书
        char[] passphrase = new char[0]; // 证书密码
        KeyStore ks = KeyStore.getInstance("PKCS12");
        FileInputStream fis = new FileInputStream("cert.pfx");
        ks.load(fis, passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(this.port), 0);
        createHttpContext(httpsServer);

        httpsServer.setExecutor(null); // creates a default executor
        httpsServer.start();
        System.out.println("HTTPS server started on port " + this.port);
    }

    private void startHttpServer() throws Exception {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(this.port), 0);
        createHttpContext(httpServer);

        httpServer.setExecutor(null); // creates a default executor
        httpServer.start();
        System.out.println("HTTP server started on port " + this.port);
    }

    private static void createHttpContext(HttpServer server){
        server.createContext("/openbmclapi-agent/challenge", new HttpHandler() {
            @Override
            public void handle(HttpExchange httpExchange) throws IOException {
                try {
                    httpExchange.sendResponseHeaders(418, 32);
                    OutputStream os = httpExchange.getResponseBody();
                    os.write(Utils.generateRandomHexString(32).getBytes());
                    os.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
