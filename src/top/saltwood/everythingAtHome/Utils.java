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
import top.saltwood.everythingAtHome.modules.Config;
import top.saltwood.everythingAtHome.modules.Logger;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {
    public static final byte[] forbiddenTip = "403 Forbidden.".getBytes();
    public static final byte[] robotsTip = """
            User-agent: *
            Disallow: /""".getBytes();
    public static final byte[] conflictResponse = """
                            <!DOCTYPE html>
                            <html lang="zh-CN">
                            <head>
                                <meta charset="UTF-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <title>Conflict!</title>
                                <style>
                                    body {
                                        display: flex;
                                        flex-direction: column;
                                        justify-content: center;
                                        align-items: center;
                                        height: 100vh;
                                        margin: 0;
                                        font-family: Arial, sans-serif;
                                    }
                                    h1 {
                                        font-size: 3em;
                                        margin-bottom: 20px;
                                    }
                                    iframe {
                                        width: 80%;
                                        height: 60%;
                                    }
                                </style>
                            </head>
                            <body>
                                <h1>HTTP status code: 409 Conflict!</h1>
                                <iframe src="https:////player.bilibili.com/player.html?isOutside=true&aid=989089&bvid=BV1xs411Z7vw&cid=1429753&p=1" scrolling="no" border="0" frameborder="no" framespacing="0" allowfullscreen="true"></iframe>
                            </body>
                            </html>""".getBytes();
    
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
            e.printStackTrace(Logger.logger);
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
            Jwt<?, ?> j = Jwts.parser()
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
            e1.printStackTrace(Logger.logger);
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
                    relativePath = "/" + SharedData.config.getItem().filePath + relativePath;
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
                .addHeader("User-Agent", Config.userAgent)
                .build();
        OkHttpClient client = new OkHttpClient();
        Response response = client.newCall(request).execute();
        if (response.body() != null && file.hash.equals(FileObject.computeHash(response.body().byteStream()))) {
            isValid = true;
        }
        response.close();
        return isValid;
    }
    
    public static double measureCluster(Cluster cluster, int size) throws Exception {
        String sign = Utils.getSign("/measure/" + size, cluster);
        String url = "http://" + cluster.ip + ":" + cluster.port + "/measure/" + size + sign;
        double time = Utils.requestForTime(url, 1024L * 1024 * size);
        return size * 8 / (time / 1000);
    }
    
    private static double requestForTime(String url, long size) throws Exception {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", Config.userAgent)
                .build();
        OkHttpClient client = new OkHttpClient();
        long start = System.currentTimeMillis();
        Response response = client.newCall(request).execute();
        if (response.body() != null && response.body().contentLength() != size) {
            response.close();
            throw new Exception("Content-Length less than " + size);
        }
        InputStream is = null;
        if (response.body() != null) {
            is = response.body().byteStream();
            long skippedBytes = is.skip(size);
            if (skippedBytes < size) throw new Exception("Body length less than " + size + ", got " + skippedBytes);
            is.close();
        }
        response.close();
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
        object.put("hits", cluster.hits);
        object.put("bytes", cluster.traffics);
        object.put("pendingHits", cluster.pendingHits);
        object.put("pendingBytes", cluster.pendingTraffics);
        object.put("isOnline", cluster.isOnline);
        object.put("isBanned", cluster.isBanned);
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

    public static <T> T weightedRandom(Stream<T> items, ToIntFunction<T> weightFunction) {
        List<T> itemList = items.toList();
        int[] weights = itemList.stream().mapToInt(weightFunction).toArray();
        int totalWeight = Arrays.stream(weights).sum();

        if (totalWeight == 0) return null;
        int randomValue = new Random().nextInt(totalWeight);

        int currentWeightSum = 0;
        for (int i = 0; i < weights.length; i++) {
            currentWeightSum += weights[i];
            if (randomValue < currentWeightSum) {
                return itemList.get(i);
            }
        }
        return null;
    }

    public static void updateFiles(SharedData sharedData) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(new File(SharedData.config.getItem().filePath));
        processBuilder.command("git", "pull");

        try {
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(Logger.logger);
        }
        Set<String> set = Utils.scanFiles(Path.of(SharedData.config.getItem().filePath).toFile().getAbsolutePath());
        List<FileObject> oldFiles = sharedData.fileStorageHelper.getItem();
        sharedData.fileStorageHelper.setItem(new ArrayList<>());
        for (String file : set) {
            FileObject f;
            try {
                f = new FileObject(file);
            } catch (FileNotFoundException e) {
                sharedData.fileStorageHelper.setItem(oldFiles);
                return;
            }
            sharedData.fileStorageHelper.getItem().add(f);
        }
        sharedData.saveAll();
        List<FileObject> newFiles = new ArrayList<>();
        for (FileObject file : sharedData.fileStorageHelper.getItem()) {
            if (oldFiles.stream().noneMatch(f -> f.hash.equals(file.hash))) {
                newFiles.add(file);
            }
        }
        sharedData.centerServer.getOnlineClusters().forEach(cluster -> {
            for (FileObject object : newFiles) {
                try {
                    Thread.sleep(3000);
                    boolean isValid = cluster.doWardenOnce(object);
                    if (!isValid) {
                        cluster.isOnline = false;
                        break;
                    }
                } catch (Exception e) {
                    cluster.isOnline = false;
                }
            }
        });
    }
}

