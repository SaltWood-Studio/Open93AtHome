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
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;

public class SimpleHttpServer {

    public static void main(String[] args) {
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

    private static void startHttpsServer() throws Exception {
        // 加载SSL证书
        char[] passphrase = "password".toCharArray(); // 证书密码
        KeyStore ks = KeyStore.getInstance("PKCS12");
        FileInputStream fis = new FileInputStream("cert.pfx");
        ks.load(fis, passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(8443), 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            public void configure(HttpsParameters params) {
                params.setNeedClientAuth(false);
                params.setCipherSuites(getSSLContext().getDefaultSSLParameters().getCipherSuites());
                params.setProtocols(getSSLContext().getDefaultSSLParameters().getProtocols());

                // Get the default parameters
                params.setSSLParameters(getSSLContext().getDefaultSSLParameters());
            }
        });

        httpsServer.createContext("/", new TeapotHandler());
        httpsServer.setExecutor(null); // creates a default executor
        httpsServer.start();
        System.out.println("HTTPS server started on port 8443");
    }

    private static void startHttpServer() throws Exception {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(8080), 0);
        httpServer.createContext("/", new TeapotHandler());
        httpServer.setExecutor(null); // creates a default executor
        httpServer.start();
        System.out.println("HTTP server started on port 8080");
    }

    static class TeapotHandler implements HttpHandler {
        public void handle(HttpExchange t) {
            try {
                String response = "I'm a teapot.";
                t.sendResponseHeaders(418, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
