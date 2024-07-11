import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.luben.zstd.Zstd;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SecureDigestAlgorithm;
import modules.AvroEncoder;
import modules.cluster.ClusterJwt;
import modules.http.HandlerWrapper;
import modules.http.Response;

import javax.crypto.SecretKey;
import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SimpleHttpServer {
    private final int port;
    public static SecretKey key = Jwts.SIG.HS512.key().build();
    private HttpServer server;
    public final static SecureDigestAlgorithm<SecretKey, SecretKey> ALGORITHM = Jwts.SIG.HS512;

    public SimpleHttpServer(int port){
        this.port = port;
    }

    public void start(){
        start(false);
    }

    public void start(boolean forceHttp) {
        try {
            if (forceHttp){
                startHttpServer();
                return;
            }
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
        this.server = httpsServer;
        createHttpContext();

        httpsServer.setExecutor(null); // creates a default executor
        httpsServer.start();
        System.out.println("HTTPS server started on port " + this.port);
    }

    public void stop() {
        server.stop(0);
    }

    private void startHttpServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(this.port), 0);
        createHttpContext();

        server.setExecutor(null); // creates a default executor
        server.start();
        System.out.println("HTTP server started on port " + this.port);
    }

    protected void verifyClusterRequest(HttpExchange exchange) throws Exception {
        String auth = exchange.getRequestHeaders().get("Authorization").stream()
                .reduce((first, second) -> second).orElse(null);
        if (auth == null) return;
        String jwt = Arrays.stream(auth.split(" ")).reduce((first, second) -> second).orElse(null);
        boolean isValid = Utils.verifyJwt(jwt, key);
        if (!isValid) throw new Exception("Invalid JWT");
    }

    private void createHttpContext(){
        this.server.createContext("/openbmclapi-agent/challenge", new HandlerWrapper(){
            @Override
            public Response execute(HttpExchange httpExchange) {
                String id = httpExchange.getRequestURI().getQuery();
                System.out.println(id);
                JSONObject object = new JSONObject();
                object.put("challenge", ClusterJwt.generateJwtToken("challenge",
                                1000 * 60 * 60L, key, ALGORITHM, id).compact());
                httpExchange.getResponseHeaders().add("Content-Type", "application/json");
                Response resp = new Response();
                resp.bytes = object.toJSONString().getBytes();
                resp.responseCode = 200;
                return resp;
            }
        });
        this.server.createContext("/openbmclapi-agent/token", new HandlerWrapper(){
            @Override
            public Response execute(HttpExchange httpExchange) throws IOException {
                JSONObject requestObject = JSON.parseObject(httpExchange.getRequestBody().readAllBytes());
                String id = requestObject.getString("clusterId");
                String sign = requestObject.getString("signature");
                String challenge = requestObject.getString("challenge");

                boolean isValid = Utils.verifyJwt(challenge, key);
                // TODO: 存储、读取 cluster 数据
                String realSign = Utils.generateSignature("secret", challenge);

                if (realSign != null && (!isValid || !realSign.equals(sign))) {
                    Response resp = new Response();
                    resp.responseCode = 401;
                    return resp;
                }

                JSONObject object = new JSONObject();
                ClusterJwt cluster = new ClusterJwt(id, "secret");
                object.put("token", cluster.generateJwtToken());
                object.put("ttl", 1000 * 60 * 60 * 24);
                Response resp = new Response();
                resp.bytes = object.toJSONString().getBytes();
                resp.responseCode = 200;
                return resp;
            }
        });
        this.server.createContext("/openbmclapi/configuration", new HandlerWrapper(){
            @Override
            public Response execute(HttpExchange httpExchange) throws Exception {
                verifyClusterRequest(httpExchange);
                JSONObject sync = new JSONObject();
                sync.put("source", "center");
                sync.put("concurrency", 10);
                JSONObject object = new JSONObject();
                object.put("sync", sync);
                Response resp = new Response();
                resp.bytes = object.toJSONString().getBytes();
                resp.responseCode = 200;
                return resp;
            }
        });
        this.server.createContext("/openbmclapi/files", new HandlerWrapper(){
            @Override
            public Response execute(HttpExchange httpExchange) throws Exception {
                File[] files = new File[1];// TODO: read records
                files[0] = new File("/path/to/file", "hash", 1L, 0L);
                AvroEncoder encoder = new AvroEncoder();
                encoder.setElements(files.length);
                for (File file : files) {
                    encoder.setString(file.path);
                    encoder.setString(file.hash);
                    encoder.setLong(file.size);
                    encoder.setLong(file.lastModified);
                }
                encoder.byteStream.close();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                outputStream.write(Zstd.compress(encoder.byteStream.toByteArray()));
                Response resp = new Response();
                resp.bytes = outputStream.toByteArray();
                return resp;
            }
        });
    }
}
