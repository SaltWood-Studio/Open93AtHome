import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SecureDigestAlgorithm;
import modules.cluster.ClusterJwt;
import modules.http.HandlerWrapper;
import modules.http.Response;

import javax.crypto.SecretKey;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Arrays;

public class SimpleHttpServer {
    public final static SecureDigestAlgorithm<SecretKey, SecretKey> ALGORITHM = Jwts.SIG.HS512;
    public static SecretKey key = Jwts.SIG.HS512.key().build();
    private final int port;
    public SharedData sharedData;
    private HttpServer server;
    
    public SimpleHttpServer() {
        this(9388);
    }
    
    public SimpleHttpServer(int port) {
        this.port = port;
    }
    
    public void start() {
        start(false);
    }
    
    public void start(boolean forceHttp) {
        try {
            if (forceHttp) {
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
        String auth = Utils.tryGetInDictionary(exchange.getRequestHeaders(), "Authorization");
        if (auth == null) return;
        String jwt = Arrays.stream(auth.split(" ")).reduce((first, second) -> second).orElse(null);
        boolean isValid = Utils.verifyJwt(jwt, key);
        if (!isValid) throw new Exception("Invalid JWT");
    }
    
    private void createHttpContext() {
        this.server.createContext("/", new HandlerWrapper() {
            @Override
            public Response execute(HttpExchange httpExchange) throws Exception {
                if (sharedData.masterControlServer.pathToFile.get(httpExchange.getRequestURI().getPath()) == null) {
                    byte[] bytes = "The requested url was not found on the server.".getBytes();
                    httpExchange.sendResponseHeaders(404, bytes.length);
                    httpExchange.getResponseBody().write(bytes);
                    return null;
                }
                String url = sharedData.masterControlServer.requestDownload(httpExchange.getRequestURI().getPath());
                if (url == null) {
                    byte[] bytes = "Service unavailable.".getBytes();
                    httpExchange.sendResponseHeaders(503, bytes.length);
                    httpExchange.getResponseBody().write(bytes);
                } else {
                    httpExchange.getResponseHeaders().set("Location", url);
                    httpExchange.sendResponseHeaders(302, 0);
                }
                return null;
            }
        });
        this.server.createContext("/openbmclapi-agent/challenge", new HandlerWrapper() {
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
        this.server.createContext("/openbmclapi-agent/token", new HandlerWrapper() {
            @Override
            public Response execute(HttpExchange httpExchange) throws IOException {
                JSONObject requestObject = JSON.parseObject(httpExchange.getRequestBody().readAllBytes());
                String id = requestObject.getString("clusterId");
                String sign = requestObject.getString("signature");
                String challenge = requestObject.getString("challenge");
                Cluster cluster = sharedData.masterControlServer.clusters.get(id);
                if (cluster == null) {
                    httpExchange.sendResponseHeaders(404, 0);
                    return null;
                }
                
                boolean isValid = Utils.verifyJwt(challenge, key);
                String realSign = Utils.generateSignature(cluster.secret, challenge);
                if (realSign != null && (!isValid || !realSign.equals(sign))) {
                    httpExchange.sendResponseHeaders(401, 0);
                    return null;
                }
                
                JSONObject object = new JSONObject();
                ClusterJwt jwt = new ClusterJwt(id, cluster.secret);
                object.put("token", jwt.generateJwtToken());
                object.put("ttl", 1000 * 60 * 60 * 24);
                byte[] bytes = object.toJSONString().getBytes();
                httpExchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = httpExchange.getResponseBody();
                os.write(bytes);
                os.close();
                return null;
            }
        });
        this.server.createContext("/openbmclapi/configuration", new HandlerWrapper() {
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
        this.server.createContext("/openbmclapi/download", new HandlerWrapper() {
            @Override
            public Response execute(HttpExchange httpExchange) throws Exception {
                verifyClusterRequest(httpExchange);
                String hash = null;
                if (httpExchange.getRequestURI().getPath().startsWith("/openbmclapi/download/")) {
                    hash = httpExchange.getRequestURI().getPath().substring("/openbmclapi/download/".length());
                }
                if (hash == null) {
                    httpExchange.sendResponseHeaders(404, 0);
                    return null;
                }
                FileObject file = sharedData.masterControlServer.hashToFile.get(hash);
                if (file == null) {
                    httpExchange.sendResponseHeaders(404, 0);
                    return null;
                }
                httpExchange.sendResponseHeaders(200, file.size);
                OutputStream stream = httpExchange.getResponseBody();
                FileInputStream fis = new FileInputStream(Path.of("./files", file.path).toString());
                // 发送文件
                byte[] buffer = new byte[2048];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    stream.write(buffer, 0, len);
                }
                return null;
            }
        });
        this.server.createContext("/openbmclapi/files", new HandlerWrapper() {
            @Override
            public Response execute(HttpExchange httpExchange) throws Exception {
                verifyClusterRequest(httpExchange);
                byte[] bytes = sharedData.masterControlServer.getAvroBytes();
                httpExchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = httpExchange.getResponseBody();
                os.write(bytes);
                os.close();
                return null;
            }
        });
        this.server.createContext("/93AtHome/new_cluster", new HandlerWrapper() {
            @Override
            public Response execute(HttpExchange httpExchange) throws Exception {
                boolean isInternalRequest = Utils.checkIfInternal(httpExchange.getRemoteAddress().getAddress());
                if (!isInternalRequest) {
                    httpExchange.close();
                }
                String request = new String(httpExchange.getRequestBody().readAllBytes());
                JSONObject object = JSONObject.parseObject(request);
                String name = (String) object.get("name");
                int bandwidth = (int) object.get("bandwidth");
                String id = Utils.generateRandomHexString(24);
                String secret = Utils.generateRandomHexString(32);
                Cluster cluster = new Cluster(id, secret, name, bandwidth);
                sharedData.masterControlServer.clusters.put(id, cluster);
                sharedData.clusterStorageHelper.elements.add(cluster);
                JSONObject response = new JSONObject();
                response.put("id", id);
                response.put("secret", secret);
                response.put("name", name);
                response.put("bandwidth", bandwidth);
                byte[] message = response.toJSONString().getBytes();
                saveAll();
                httpExchange.sendResponseHeaders(200, message.length);
                OutputStream os = httpExchange.getResponseBody();
                os.write(message);
                os.close();
                return null;
            }
        });
        this.server.createContext("/93AtHome/add_cluster", new HandlerWrapper() {
            @Override
            public Response execute(HttpExchange httpExchange) throws Exception {
                boolean isInternalRequest = Utils.checkIfInternal(httpExchange.getRemoteAddress().getAddress());
                if (!isInternalRequest) {
                    httpExchange.close();
                }
                String request = new String(httpExchange.getRequestBody().readAllBytes());
                JSONObject object = JSONObject.parseObject(request);
                String name = (String) object.get("name");
                int bandwidth = (int) object.get("bandwidth");
                String id = (String) object.get("id");
                String secret = (String) object.get("secret");
                Cluster cluster = new Cluster(id, secret, name, bandwidth);
                sharedData.masterControlServer.clusters.put(id, cluster);
                sharedData.clusterStorageHelper.elements.add(cluster);
                JSONObject response = new JSONObject();
                response.put("id", id);
                response.put("secret", secret);
                response.put("name", name);
                response.put("bandwidth", bandwidth);
                byte[] message = response.toJSONString().getBytes();
                saveAll();
                httpExchange.sendResponseHeaders(200, message.length);
                OutputStream os = httpExchange.getResponseBody();
                os.write(message);
                os.close();
                return null;
            }
        });
        this.server.createContext("/93AtHome/add_file", new HandlerWrapper() {
            @Override
            public Response execute(HttpExchange httpExchange) throws Exception {
                boolean isInternalRequest = Utils.checkIfInternal(httpExchange.getRemoteAddress().getAddress());
                if (!isInternalRequest) {
                    httpExchange.close();
                }
                String request = new String(httpExchange.getRequestBody().readAllBytes());
                JSONObject object = JSONObject.parseObject(request);
                String path = (String) object.get("path");
                String hash = null;
                String filePath = Path.of("./files", path).toString();
                try (FileInputStream fis = new FileInputStream(filePath)){
                    hash = FileObject.computeHash(fis);
                } catch (Exception e) {
                    throw e;
                }
                Long size = new File(filePath).length();
                Long lastModified = 0L;
                sharedData.fileStorageHelper.elements.add(new FileObject(path, hash, size, lastModified));
                sharedData.masterControlServer.update();
                saveAll();
                JSONObject response = new JSONObject();
                response.put("path", path);
                response.put("hash", hash);
                response.put("size", size);
                response.put("lastModified", lastModified);
                byte[] message = response.toJSONString().getBytes();
                saveAll();
                httpExchange.sendResponseHeaders(200, message.length);
                OutputStream os = httpExchange.getResponseBody();
                os.write(message);
                os.close();
                return null;
            }
        });
        this.server.createContext("/93AtHome/remove_file", new HandlerWrapper() {
            @Override
            public Response execute(HttpExchange httpExchange) throws Exception {
                boolean isInternalRequest = Utils.checkIfInternal(httpExchange.getRemoteAddress().getAddress());
                if (!isInternalRequest) {
                    httpExchange.close();
                }
                String request = new String(httpExchange.getRequestBody().readAllBytes());
                JSONObject object = JSONObject.parseObject(request);
                String path = (String) object.get("path");
                boolean removed = sharedData.fileStorageHelper.elements.removeIf(fileObject -> fileObject.path.equals(path));
                sharedData.masterControlServer.update();
                saveAll();
                JSONObject response = new JSONObject();
                response.put("path", path);
                response.put("isRemoved", removed);
                byte[] message = response.toJSONString().getBytes();
                saveAll();
                httpExchange.sendResponseHeaders(200, message.length);
                OutputStream os = httpExchange.getResponseBody();
                os.write(message);
                os.close();
                return null;
            }
        });
        this.server.createContext("/93AtHome/list_file", new HandlerWrapper() {
            @Override
            public Response execute(HttpExchange httpExchange) throws Exception {
                boolean isInternalRequest = Utils.checkIfInternal(httpExchange.getRemoteAddress().getAddress());
                if (!isInternalRequest) {
                    httpExchange.close();
                }
                String response = JSON.toJSONString(sharedData.fileStorageHelper.elements);
                byte[] message = response.getBytes();
                saveAll();
                httpExchange.sendResponseHeaders(200, message.length);
                OutputStream os = httpExchange.getResponseBody();
                os.write(message);
                os.close();
                return null;
            }
        });
    }
    
    public void saveAll(){
        sharedData.clusterStorageHelper.save();
        sharedData.fileStorageHelper.save();
    }
}
