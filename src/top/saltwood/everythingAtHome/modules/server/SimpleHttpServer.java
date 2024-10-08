package top.saltwood.everythingAtHome.modules.server;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SecureDigestAlgorithm;
import top.saltwood.everythingAtHome.*;
import top.saltwood.everythingAtHome.modules.cluster.ClusterJwt;
import top.saltwood.everythingAtHome.modules.http.HandlerWrapper;
import top.saltwood.everythingAtHome.modules.Logger;

import javax.crypto.SecretKey;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.*;

public class SimpleHttpServer {
    public final static SecureDigestAlgorithm<SecretKey, SecretKey> ALGORITHM = Jwts.SIG.HS512;
    public static SecretKey key = Jwts.SIG.HS512.key().build();
    private final int port;
    public SharedData sharedData;
    private HttpServer server;
    private Thread fileUpdateThread;
    
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
        Logger.logger.logLine("HTTPS server started on port " + this.port);
    }
    
    public void stop() {
        server.stop(0);
    }
    
    private void startHttpServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(this.port), 0);
        createHttpContext();
        
        server.setExecutor(null); // creates a default executor
        server.start();
        Logger.logger.logLine("HTTP server started on port " + this.port);
    }
    
    protected void verifyClusterRequest(HttpExchange exchange) throws Exception {
        String auth = Utils.tryGetInDictionary(exchange.getRequestHeaders(), "Authorization");
        if (auth == null) {
            exchange.sendResponseHeaders(403, Utils.forbiddenTip.length);
            OutputStream os = exchange.getResponseBody();
            os.write(Utils.forbiddenTip);
            os.close();
            exchange.close();
            throw new Exception("Invalid request");
        }
        String jwt = Arrays.stream(auth.split(" ")).reduce((first, second) -> second).orElse(null);
        boolean isValid = Utils.verifyJwt(jwt, key);
        if (!isValid) {
            exchange.sendResponseHeaders(401, Utils.forbiddenTip.length);
            OutputStream os = exchange.getResponseBody();
            os.write(Utils.forbiddenTip);
            os.close();
            exchange.close();
            throw new Exception("Invalid JWT");
        }
    }
    
    private void createHttpContext() {
        this.server.createContext("/", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange exchange) throws Exception {
                exchange.getResponseHeaders().set("Location", "/dashboard/");
                exchange.sendResponseHeaders(302, 0);
            }
        });

        this.server.createContext("/dashboard/", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange exchange) throws Exception {
                String path = exchange.getRequestURI().getPath().substring(11);
                File file = Path.of("./dashboard", path).toFile();
                if (file.isFile()) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        exchange.sendResponseHeaders(200, file.length());
                        OutputStream os = exchange.getResponseBody();
                        byte[] buffer = new byte[2048];
                        while (fis.read(buffer) > 0) {
                            os.write(buffer);
                        }
                        os.flush();
                    }
                } else if (file.isDirectory()) {
                    final String[] defaultFiles = new String[] {
                            "index.html",
                            "index.htm",
                            "index.php"
                    };
                    for (String i : defaultFiles) {
                        File f = Path.of("./dashboard", path, i).toFile();
                        if (f.exists()) {
                            try (FileInputStream fis = new FileInputStream(f)) {
                                exchange.sendResponseHeaders(200, f.length());
                                OutputStream os = exchange.getResponseBody();
                                int bytesRead = 0;
                                byte[] buffer = new byte[2048];
                                while ((bytesRead = fis.read(buffer)) != -1) {
                                    os.write(buffer, 0 ,bytesRead);
                                }
                                os.flush();
                            }
                            break;
                        }
                    }
                }
                byte[] response = ("The requested file \"" + path + "\" was not found on the server.").getBytes();
                exchange.sendResponseHeaders(404, response.length);
                exchange.getResponseBody().write(response);
            }
        });

        this.server.createContext("/" + SharedData.config.getItem().filePath + "/", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange httpExchange) throws Exception {
                String path = httpExchange.getRequestURI().getPath();
                if (sharedData.centerServer.pathToFile.get(path) == null) {
                    byte[] bytes = "The requested url was not found on the server.".getBytes();
                    httpExchange.sendResponseHeaders(404, bytes.length);
                    httpExchange.getResponseBody().write(bytes);
                    return;
                }
                String url = sharedData.centerServer.requestDownload(path);
                if (Math.random() < (1.0 / (sharedData.centerServer.getOnlineClusters().count() + 1)) // 随机到主控
                        || url == null) {
                    try {
                        FileObject file = sharedData.centerServer.pathToFile.get(path);
                        // 主控给文件
                        try (FileInputStream fis = new FileInputStream(Path.of("." + file.path).toString())) {
                            OutputStream stream = httpExchange.getResponseBody();
                            httpExchange.sendResponseHeaders(200, file.size);
                            // 发送文件
                            byte[] buffer = new byte[2048];
                            int len;
                            while ((len = fis.read(buffer)) > -1) {
                                stream.write(buffer, 0, len);
                            }
                            stream.flush();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace(Logger.logger);
                        try {
                            byte[] bytes = ("ERR_INTERNAL_SERVER_ERROR, Exception: " + ex.getMessage()).getBytes();
                            httpExchange.sendResponseHeaders(500, bytes.length);
                            httpExchange.getResponseBody().write(bytes);
                        } catch (Exception ignored) { }
                    }
                } else {
                    httpExchange.getResponseHeaders().set("Location", url);
                    httpExchange.sendResponseHeaders(302, 0);
                }
            }
        });
        this.server.createContext("/robots.txt", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange httpExchange) throws Exception {
                httpExchange.sendResponseHeaders(200, Utils.robotsTip.length);
                httpExchange.getResponseBody().write(Utils.robotsTip);
            }
        });
        this.server.createContext("/openbmclapi-agent/challenge", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange httpExchange) throws Exception {
                httpExchange.getResponseHeaders().set("Content-Type", "application/json");
                Map<String, String> map = Utils.parseBodyToDictionary(httpExchange.getRequestURI().getQuery());
                String id = map.get("clusterId");
                Cluster cluster = sharedData.centerServer.clusters.get(id);
                if (cluster == null) {
                    httpExchange.sendResponseHeaders(404, 0);
                    return;
                }
                if (cluster.isBanned) {
                    httpExchange.sendResponseHeaders(403, 0);
                    return;
                }
                JSONObject object = new JSONObject();
                object.put("challenge", ClusterJwt.generateJwtToken("challenge",
                        1000 * 60 * 60L, key, ALGORITHM, id).compact());
                byte[] bytes = object.toJSONString().getBytes(StandardCharsets.UTF_8);
                httpExchange.sendResponseHeaders(200, bytes.length);
                httpExchange.getResponseBody().write(bytes);
            }
        });
        this.server.createContext("/openbmclapi-agent/token", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange httpExchange) throws IOException {
                httpExchange.getResponseHeaders().set("Content-Type", "application/json");
                Map<String, String> requestObject = Utils.parseBodyToDictionary(new String(httpExchange.getRequestBody().readAllBytes()));
                String id = requestObject.get("clusterId");
                String sign = requestObject.get("signature");
                String challenge = requestObject.get("challenge");
                Cluster cluster = sharedData.centerServer.clusters.get(id);
                if (cluster == null) {
                    httpExchange.sendResponseHeaders(404, 0);
                    return;
                }
                
                String idInJwt = Utils.decodeJwt(challenge, key, "cluster_id");
                if (idInJwt == null || !idInJwt.equals(id)) {
                    httpExchange.sendResponseHeaders(401, 0);
                    return;
                }
                String realSign = Utils.generateSignature(cluster.secret, challenge);
                if (realSign == null || !realSign.equals(sign)) {
                    httpExchange.sendResponseHeaders(401, 0);
                    return;
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
            }
        });
        this.server.createContext("/openbmclapi/configuration", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange httpExchange) throws Exception {
                httpExchange.getResponseHeaders().set("Content-Type", "application/json");
                verifyClusterRequest(httpExchange);
                JSONObject sync = new JSONObject();
                sync.put("source", "center");
                sync.put("concurrency", 10);
                JSONObject object = new JSONObject();
                object.put("sync", sync);
                byte[] bytes = object.toJSONString().getBytes(StandardCharsets.UTF_8);
                httpExchange.sendResponseHeaders(200, bytes.length);
                httpExchange.getResponseBody().write(bytes);
            }
        });
        this.server.createContext("/openbmclapi/download", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange httpExchange) throws Exception {
                verifyClusterRequest(httpExchange);
                String hash = null;
                if (httpExchange.getRequestURI().getPath().startsWith("/openbmclapi/download/")) {
                    hash = httpExchange.getRequestURI().getPath().substring("/openbmclapi/download/".length());
                }
                if (hash == null) {
                    httpExchange.sendResponseHeaders(404, 0);
                    return;
                }
                FileObject file = sharedData.centerServer.hashToFile.get(hash);
                if (file == null) {
                    httpExchange.sendResponseHeaders(404, 0);
                    return;
                }
                httpExchange.sendResponseHeaders(200, file.size);
                OutputStream stream = httpExchange.getResponseBody();
                try (FileInputStream fis = new FileInputStream(Path.of("." + file.path).toString())) {
                    // 发送文件
                    byte[] buffer = new byte[2048];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        stream.write(buffer, 0, len);
                    }
                }
            }
        });
        this.server.createContext("/openbmclapi/files", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange httpExchange) throws Exception {
                verifyClusterRequest(httpExchange);
                Map<String, String> query = Utils.parseBodyToDictionary(httpExchange.getRequestURI().getQuery());
                byte[] bytes;
                if (query.containsKey("lastModified")) {
                    double lastModified = Double.parseDouble(query.get("lastModified"));
                    List<FileObject> objects = sharedData.fileStorageHelper.getItem().stream()
                            .filter(file -> file.lastModified > lastModified)
                            .toList();
                    if (objects.isEmpty() /*|| lastModified == 0 Python 你怎么回事啊，怎么启动请求lastModified=0.0啊，What can I say? */) {
                        httpExchange.sendResponseHeaders(204, -1);
                        return;
                    }
                    bytes = CenterServer.computeAvroBytes(objects);
                } else {
                    bytes = sharedData.centerServer.getAvroBytes();
                }
                httpExchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = httpExchange.getResponseBody();
                os.write(bytes);
                os.close();
            }
        });
        this.server.createContext("/93AtHome/new_cluster", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange httpExchange) throws Exception {
                httpExchange.getResponseHeaders().set("Content-Type", "application/json");
                Utils.verifyToken(sharedData.tokenStorageHelper.getItem(), httpExchange, "permissionRequestAddCluster");
                String request = new String(httpExchange.getRequestBody().readAllBytes());
                JSONObject object = JSONObject.parseObject(request);
                String name = (String) object.get("name");
                int bandwidth = (int) object.get("bandwidth");
                String id = Utils.generateRandomHexString(24);
                String secret = Utils.generateRandomHexString(32);
                Cluster cluster = new Cluster(id, secret, name, bandwidth);
                sharedData.centerServer.clusters.put(id, cluster);
                sharedData.clusterStorageHelper.getItem().add(cluster);
                sharedData.saveAll();
                JSONObject response = new JSONObject();
                response.put("id", id);
                response.put("secret", secret);
                response.put("name", name);
                response.put("bandwidth", bandwidth);
                byte[] message = response.toJSONString().getBytes();
                httpExchange.sendResponseHeaders(200, message.length);
                OutputStream os = httpExchange.getResponseBody();
                os.write(message);
                os.close();
            }
        });
        this.server.createContext("/93AtHome/add_cluster", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange httpExchange) throws Exception {
                httpExchange.getResponseHeaders().set("Content-Type", "application/json");
                Utils.verifyToken(sharedData.tokenStorageHelper.getItem(), httpExchange, "permissionRequestAddCluster");
                String request = new String(httpExchange.getRequestBody().readAllBytes());
                JSONObject object = JSONObject.parseObject(request);
                String name = (String) object.get("name");
                int bandwidth = (int) object.get("bandwidth");
                String id = (String) object.get("id");
                String secret = (String) object.get("secret");
                Cluster cluster = new Cluster(id, secret, name, bandwidth);
                sharedData.centerServer.clusters.put(id, cluster);
                sharedData.clusterStorageHelper.getItem().add(cluster);
                JSONObject response = new JSONObject();
                response.put("id", id);
                response.put("secret", secret);
                response.put("name", name);
                response.put("bandwidth", bandwidth);
                byte[] message = response.toJSONString().getBytes();
                sharedData.saveAll();
                httpExchange.sendResponseHeaders(200, message.length);
                OutputStream os = httpExchange.getResponseBody();
                os.write(message);
                os.close();
            }
        });
        this.server.createContext("/93AtHome/list_cluster", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange httpExchange) throws Exception {
                httpExchange.getResponseHeaders().set("Content-Type", "application/json");
                Utils.verifyToken(sharedData.tokenStorageHelper.getItem(), httpExchange, "permissionRequestListCluster");
                String response = Utils.getClustersJsonArray(sharedData.centerServer.clusters.values()).toJSONString();
                byte[] message = response.getBytes();
                httpExchange.sendResponseHeaders(200, message.length);
                OutputStream os = httpExchange.getResponseBody();
                os.write(message);
                os.close();
            }
        });
        this.server.createContext("/93AtHome/list_online_cluster", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange httpExchange) throws Exception {
                httpExchange.getResponseHeaders().set("Content-Type", "application/json");
                Utils.verifyToken(sharedData.tokenStorageHelper.getItem(), httpExchange, "permissionRequestListCluster");
                String response = Utils.getClustersJsonArray(sharedData.centerServer.getOnlineClusters()).toJSONString();
                byte[] message = response.getBytes();
                httpExchange.sendResponseHeaders(200, message.length);
                OutputStream os = httpExchange.getResponseBody();
                os.write(message);
                os.close();
            }
        });
        this.server.createContext("/93AtHome/rank", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange httpExchange) throws Exception {
                httpExchange.getResponseHeaders().set("Content-Type", "application/json");
                JSONArray array = new JSONArray();
                List<Cluster> clusters = sharedData.clusterStorageHelper.getItem().parallelStream().sorted(Comparator.comparing(Cluster::getTraffics).reversed()).toList();
                for (Cluster cluster : clusters) {
                    JSONObject object = Utils.getJsonObject(cluster);
                    array.add(object);
                }
                String response = array.toJSONString();
                byte[] message = response.getBytes(StandardCharsets.UTF_8);
                httpExchange.sendResponseHeaders(200, message.length);
                OutputStream os = httpExchange.getResponseBody();
                os.write(message);
                os.close();
            }
        });
        this.server.createContext("/93AtHome/remove_cluster", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange httpExchange) throws Exception {
                httpExchange.getResponseHeaders().set("Content-Type", "application/json");
                Utils.verifyToken(sharedData.tokenStorageHelper.getItem(), httpExchange, "permissionRequestRemoveCluster");
                String request = new String(httpExchange.getRequestBody().readAllBytes());
                JSONObject object = JSONObject.parseObject(request);
                String id = (object.get("id") == null) ? null : (String) object.get("id");
                JSONObject response = new JSONObject();
                response.put("id", id);
                response.put("isRemoved", sharedData.clusterStorageHelper.getItem().removeIf(c -> c.id.equals(id)));
                sharedData.saveAll();
                byte[] message = response.toJSONString().getBytes();
                httpExchange.sendResponseHeaders(200, message.length);
                OutputStream os = httpExchange.getResponseBody();
                os.write(message);
                os.close();
            }
        });
        this.server.createContext("/93AtHome/random_file", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange httpExchange) throws Exception {
                httpExchange.getResponseHeaders().set("Content-Type", "application/json");
                FileObject file = sharedData.fileStorageHelper.getItem().get(new Random().nextInt(sharedData.fileStorageHelper.getItem().size()));
                byte[] bytes = JSON.toJSONBytes(file);
                httpExchange.sendResponseHeaders(200, bytes.length);
                httpExchange.getResponseBody().write(bytes);
            }
        });
        this.server.createContext("/93AtHome/add_file", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange httpExchange) throws Exception {
                httpExchange.getResponseHeaders().set("Content-Type", "application/json");
                Utils.verifyToken(sharedData.tokenStorageHelper.getItem(), httpExchange, "permissionRequestAddFile");
                String request = new String(httpExchange.getRequestBody().readAllBytes());
                JSONObject object = JSONObject.parseObject(request);
                String path = (String) object.get("path");
                String hash;
                Path filePath = Path.of("." + path);
                try (FileInputStream fis = new FileInputStream(filePath.toString())) {
                    hash = FileObject.computeHash(fis);
                }
                long size = filePath.toFile().length();
                long lastModified = filePath.toFile().lastModified();
                sharedData.fileStorageHelper.getItem().add(new FileObject(path, hash, size, lastModified));
                sharedData.centerServer.update();
                JSONObject response = new JSONObject();
                response.put("path", path);
                response.put("hash", hash);
                response.put("size", size);
                response.put("lastModified", lastModified);
                byte[] message = response.toJSONString().getBytes();
                sharedData.saveAll();
                httpExchange.sendResponseHeaders(200, message.length);
                OutputStream os = httpExchange.getResponseBody();
                os.write(message);
                os.close();
            }
        });
        this.server.createContext("/93AtHome/remove_file", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange httpExchange) throws Exception {
                httpExchange.getResponseHeaders().set("Content-Type", "application/json");
                Utils.verifyToken(sharedData.tokenStorageHelper.getItem(), httpExchange, "permissionRequestRemoveFile");
                String request = new String(httpExchange.getRequestBody().readAllBytes());
                JSONObject object = JSONObject.parseObject(request);
                String path = (String) object.get("path");
                boolean removed = sharedData.fileStorageHelper.getItem().removeIf(fileObject -> fileObject.path.equals(path));
                sharedData.centerServer.update();
                JSONObject response = new JSONObject();
                response.put("path", path);
                response.put("isRemoved", removed);
                byte[] message = response.toJSONString().getBytes();
                sharedData.saveAll();
                httpExchange.sendResponseHeaders(200, message.length);
                OutputStream os = httpExchange.getResponseBody();
                os.write(message);
                os.close();
            }
        });
        this.server.createContext("/93AtHome/list_file", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange httpExchange) throws Exception {
                httpExchange.getResponseHeaders().set("Content-Type", "application/json");
                Utils.verifyToken(sharedData.tokenStorageHelper.getItem(), httpExchange, "permissionRequestListFile");
                String response = JSON.toJSONString(sharedData.fileStorageHelper.getItem());
                byte[] message = response.getBytes();
                sharedData.saveAll();
                httpExchange.sendResponseHeaders(200, message.length);
                OutputStream os = httpExchange.getResponseBody();
                os.write(message);
                os.close();
            }
        });
        this.server.createContext("/93AtHome/update_files", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange httpExchange) throws Exception {
                Utils.verifyToken(sharedData.tokenStorageHelper.getItem(), httpExchange, "permissionRequestUpdateFiles");
                if (fileUpdateThread != null && fileUpdateThread.isAlive()) {
                    byte[] bytes = Utils.conflictResponse;
                    httpExchange.sendResponseHeaders(409, bytes.length);
                    OutputStream os = httpExchange.getResponseBody();
                    
                    os.write(bytes);
                    os.close();
                    return;
                }
                fileUpdateThread = new Thread(() -> {
                    Utils.updateFiles(sharedData);
                });
                fileUpdateThread.start();
                httpExchange.sendResponseHeaders(204, -1);
            }
        });
        this.server.createContext("/93AtHome/save_all", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange httpExchange) throws Exception {
                Utils.verifyToken(sharedData.tokenStorageHelper.getItem(), httpExchange, "permissionRequestSaveAll");
                sharedData.saveAll();
                byte[] result = "Saved.".getBytes();
                httpExchange.sendResponseHeaders(200, result.length);
                httpExchange.getResponseBody().write(result);
            }
        });
        this.server.createContext("/93AtHome/new_token", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange httpExchange) throws Exception {
                httpExchange.getResponseHeaders().set("Content-Type", "application/json");
                Utils.verifyToken(sharedData.tokenStorageHelper.getItem(), httpExchange, "permissionAll");
                Token token = new Token();
                Map<String, Boolean> body = Utils.parseBodyToDictionary(new String(httpExchange.getRequestBody().readAllBytes()));
                for (Map.Entry<String, Boolean> entry : body.entrySet()) {
                    token.setPermission(entry.getKey(), entry.getValue());
                }
                sharedData.tokenStorageHelper.getItem().add(token);
                sharedData.saveAll();
                JSONObject object = new JSONObject();
                object.put("token", token);
                object.put("permissions", body);
                byte[] bytes = object.toString().getBytes();
                httpExchange.sendResponseHeaders(200, bytes.length);
                httpExchange.getResponseBody().write(bytes);
            }
        });
        this.server.createContext("/93AtHome/today", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange httpExchange) throws Exception {
                httpExchange.getResponseHeaders().set("Content-Type", "application/json");
                // TODO: 做统计，填真的数据
                byte[] fakeResponse = """
                        {
                            "todayBytes": [
                                167598, 165497, 512, 367,
                                87654, 8496, 71865, 76534,
                                7654, 24376, 876959, 10485760,
                                87, 21, 78643, 0,
                                -1, -1, -1, -1,
                                -1, -1, -1, -1,
                            ],
                            "todayHits": [
                                315, 543, 342, 675,
                                465, 213, 435, 876,
                                1145, 1919, 543, 876,
                                231, 564, 886, 0,
                                -1, -1, -1, -1,
                                -1, -1, -1, -1,
                            ],
                            "todayBandwidth": [
                                167598, 165497, 512, 367,
                                87654, 8496, 71865, 76534,
                                7654, 24376, 876959, 10485760,
                                -1, -1, -1, -1,
                                -1, -1, -1, -1,
                            ],
                            "todayOnlines": [
                                1, 2, 0, 3,
                                1, 1, 1, 1,
                                1, 1, 1, 1,
                                2, 1, 1, 0,
                                -1, -1, -1, -1,
                                -1, -1, -1, -1,
                            ]
                        }""".getBytes();
                httpExchange.sendResponseHeaders(200, fakeResponse.length);
                httpExchange.getResponseBody().write(fakeResponse);
            }
        });
        this.server.createContext("/93AtHome/onlines", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange exchange) throws Exception {
                byte[] response = ((Long) sharedData.centerServer.getOnlineClusters().count()).toString().getBytes();
                exchange.sendResponseHeaders(200,response.length);
                exchange.getResponseBody().write(response);
            }
        });
        this.server.createContext("/93AtHome/cluster/", new HandlerWrapper() {
            @Override
            public void execute(HttpExchange exchange) throws Exception {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                String id = exchange.getRequestURI().getPath().substring(18);
                var result = sharedData.statisticsHelper.getItem().get(id);
                Calendar today = Calendar.getInstance();
                JSONObject object = new JSONObject();
                object.put("trafficPerHour", result.getRawBytes()[today.get(Calendar.DAY_OF_MONTH)]);
                object.put("hitsPerHour", result.getRawHits()[today.get(Calendar.DAY_OF_MONTH)]);
                object.put("trafficPerDay", result.getRawBytes());
                object.put("hitsPerDay", result.getRawHits());
                byte[] response = object.toJSONString().getBytes();
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            }
        });
    }
}
