import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import modules.http.HandlerWrapper;
import modules.http.Response;

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
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            public void configure(HttpsParameters params) {
                params.setNeedClientAuth(false);
                params.setCipherSuites(getSSLContext().getDefaultSSLParameters().getCipherSuites());
                params.setProtocols(getSSLContext().getDefaultSSLParameters().getProtocols());

                // Get the default parameters
                params.setSSLParameters(getSSLContext().getDefaultSSLParameters());
            }
        });
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
        server.createContext("/socket.io", new HandlerWrapper(){
            @Override
            public Response execute(HttpExchange httpExchange) throws IOException {
                // String request = httpExchange.getRequestHeaders().get("Host").get(0).split(":")[0];
                httpExchange.getResponseHeaders().add("Location", "http://localhost:3000/socket.io");
                Response resp = new Response();
                resp.responseCode = 302;
                return resp;
            }
        });
        server.createContext("/openbmclapi-agent/challenge", new HandlerWrapper(){
            @Override
            public Response execute(HttpExchange httpExchange) throws IOException {
                JSONObject object = new JSONObject();
                object.put("challenge", Utils.generateRandomHexString(32));
                httpExchange.getResponseHeaders().add("Content-Type", "application/json");
                Response resp = new Response();
                resp.bytes = object.toJSONString().getBytes();
                resp.responseCode = 200;
                return resp;
            }
        });
        server.createContext("/openbmclapi-agent/token", new HandlerWrapper(){
            @Override
            public Response execute(HttpExchange httpExchange) throws IOException {
                JSONObject object = new JSONObject();
                object.put("token", Utils.generateRandomHexString(32));
                object.put("ttl", 1000 * 60 * 60 * 24);
                Response resp = new Response();
                resp.bytes = object.toJSONString().getBytes();
                resp.responseCode = 200;
                return resp;
            }
        });
    }
}
