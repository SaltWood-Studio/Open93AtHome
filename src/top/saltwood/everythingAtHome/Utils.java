package top.saltwood.everythingAtHome;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultClaims;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

public class Utils {
    public static final byte[] forbiddenTip = "403 Forbidden.".getBytes();
    public static final byte[] robotsTip = """
            User-agent: *
            Disallow: /""".getBytes();
    
    public static String generateRandomHexString(int length) {
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder hexString = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            int randomNumber = secureRandom.nextInt(16);
            hexString.append(Integer.toHexString(randomNumber));
        }
        
        return hexString.toString();
    }
    
    public static String generateSignature(String clusterSecret, String challenge) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(clusterSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(challenge.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash).toLowerCase();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    public static boolean verifyJwt(String jwt, SecretKey key) {
        boolean isValid;
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parse(jwt);
            isValid = true;
        } catch (Exception e) {
            isValid = false;
        }
        return isValid;
    }
    
    public static String decodeJwt(String jwt, SecretKey key, String header) {
        try {
            Jwt j = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parse(jwt);
            return ((DefaultClaims) j.getPayload()).get(header).toString();
        } catch (Exception e) {
            return null;
        }
    }
    
    public static String toUrlSafeBase64String(byte[] b) {
        return Base64.getEncoder().encodeToString(b).replace('/', '_').replace('+', '-').replace("=", "");
    }
    
    public static String getSign(FileObject file, Cluster cluster) {
        return getSign(file.hash, cluster);
    }
    
    public static String getSign(String path, Cluster cluster) {
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
            return null;
        }
        long timestamp = System.currentTimeMillis() + 5 * 60 * 1000;
        String e = Long.toString(timestamp, 36);
        byte[] signBytes = sha1.digest((cluster.secret + path + e).getBytes());
        String sign = toUrlSafeBase64String(signBytes);
        return "?s=" + sign + "&e=" + e;
    }
    
    public static String getISOTime() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(new Date());
    }
    
    public static String tryGetInDictionary(Headers dictionary, String key) {
        List<String> result = dictionary.get(key);
        if (result != null) return result.get(result.size() - 1);
        result = dictionary.get(key.toLowerCase());
        if (result != null) return result.get(result.size() - 1);
        result = dictionary.get(key.toUpperCase());
        if (result != null) return result.get(result.size() - 1);
        return null;
    }
    
    public static Map parseBodyToDictionary(String string) {
        if (string == null || string.isEmpty()) return new HashMap();
        if (string.startsWith("{")) {
            return JSONObject.parseObject(string).toJavaObject(Map.class);
        } else {
            Map<String, String> dictionary = new HashMap<>();
            String[] params = string.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    dictionary.put(URLDecoder.decode(keyValue[0], Charset.defaultCharset()), URLDecoder.decode(keyValue[1], Charset.defaultCharset()));
                }
            }
            return dictionary;
        }
    }
    
    /**
     * 扫描指定目录，返回该目录下所有文件的路径集合
     *
     * @param directoryPath 要扫描的目录路径
     * @return 包含所有文件相对路径的 Set 集合
     */
    public static Set<String> scanFiles(String directoryPath) {
        Set<String> filePaths = new HashSet<>();
        File directory = new File(directoryPath);
        if (directory.exists() && directory.isDirectory()) {
            scanDirectory(directory, filePaths, directoryPath);
        }
        return filePaths;
    }
    
    /**
     * 递归扫描目录及其子目录，收集所有文件的相对路径
     *
     * @param directory 当前扫描的目录
     * @param filePaths 存储文件路径的 Set 集合
     * @param rootPath  根目录路径，用于计算相对路径
     */
    private static void scanDirectory(File directory, Set<String> filePaths, String rootPath) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                // 忽略以点开头的文件或目录
                if (file.getName().startsWith(".")) {
                    continue;
                }
                if (file.isFile()) {
                    // 计算相对于根目录的路径
                    String relativePath = file.getAbsolutePath().substring(rootPath.length()).replace(File.separator, "/");
                    if (!relativePath.startsWith("/")) {
                        relativePath = "/" + relativePath;
                    }
                    filePaths.add(relativePath);
                } else if (file.isDirectory()) {
                    // 递归扫描子目录
                    scanDirectory(file, filePaths, rootPath);
                }
            }
        }
    }
    
    public static boolean checkCluster(String url, FileObject file) throws IOException {
        boolean isValid = false;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", SharedData.config.config.userAgent)
                .build();
        OkHttpClient client = new OkHttpClient();
        Response response = client.newCall(request).execute();
        if (file.hash.equals(FileObject.computeHash(response.body().byteStream()))) {
            isValid = true;
        }
        response.close();
        return isValid;
    }
    
    public static double measureCluster(Cluster cluster, int size) throws IOException {
        String sign = Utils.getSign("/measure/" + size, cluster);
        String url = "http://" + cluster.ip + ":" + cluster.port + "/measure/" + size + "?sign=" + sign;
        double time = Utils.requestForTime(url, 1024L * 1024 * size);
        return (size * 8 * 1000) / time;
    }
    
    private static double requestForTime(String url, long size) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", SharedData.config.config.userAgent)
                .build();
        OkHttpClient client = new OkHttpClient();
        long start = System.currentTimeMillis();
        Response response = client.newCall(request).execute();
        if (response.body().contentLength() != size) {
            return -1;
        }
        return (System.currentTimeMillis() - start);
    }
    
    public static <T> T random(List<T> list) {
        if (list.isEmpty()) {
            return null;
        }
        return list.get(new Random().nextInt(list.size()));
    }
    
    public static String getUrl(FileObject file, Cluster cluster, String sign) {
        return "http://" + cluster.ip + ":" + cluster.port + "/download/" + file.hash + sign;
    }
    
    public static void verifyToken(Collection<Token> elements, HttpExchange httpExchange, String permission) throws Exception {
        boolean isAuthorized = elements.stream().anyMatch(t -> t.verifyPermission(httpExchange, permission));
        if (!isAuthorized) {
            httpExchange.sendResponseHeaders(403, forbiddenTip.length);
            OutputStream os = httpExchange.getResponseBody();
            os.write(forbiddenTip);
            os.close();
            httpExchange.close();
            throw new Exception("Forbidden");
        }
    }
    
    public static JSONObject getJsonObject(Cluster cluster) {
        JSONObject object = new JSONObject();
        object.put("id", cluster.id);
        object.put("name", cluster.name);
        object.put("bandwidth", cluster.bandwidth);
        object.put("measureBandwidth", cluster.measureBandwidth);
        object.put("bytes", cluster.traffics);
        object.put("pendingBytes", cluster.pendingTraffics);
        object.put("hits", cluster.hits);
        object.put("pendingHits", cluster.pendingHits);
        return object;
    }
    
    public static JSONArray getClustersJsonArray(Collection<Cluster> clusters) {
        JSONArray array = new JSONArray();
        for (Cluster cluster : clusters) {
            array.add(getJsonObject(cluster));
        }
        return array;
    }
    
    public static JSONArray getClustersJsonArray(Stream<Cluster> clusters) {
        JSONArray array = new JSONArray();
        clusters.forEach(cluster -> array.add(getJsonObject(cluster)));
        return array;
    }
}

