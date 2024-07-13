import com.alibaba.fastjson2.JSONObject;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultClaims;

import com.sun.net.httpserver.Headers;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Utils {
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
            return ((DefaultClaims)j.getPayload()).get(header).toString();
        } catch (Exception e) {
            return null;
        }
    }
    
    public static String toUrlSafeBase64String(byte[] b) {
        return Base64.getEncoder().encodeToString(b).replace('/', '_').replace('+', '-').replace("=", "");
    }
    
    public static String getSign(FileObject file, Cluster cluster) {
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
            return null;
        }
        long timestamp = System.currentTimeMillis() / 10 + 5 * 60 * 100;
        String e = Long.toString(timestamp, 36);
        byte[] signBytes = sha1.digest((cluster.secret + file.hash + e).getBytes());
        String sign = toUrlSafeBase64String(signBytes);
        return "?s=" + sign + "&e=" + e + "&name=" + file.getName();
    }
    
    public static boolean checkIfInternal(InetAddress address) {
        if (address.isLoopbackAddress()) return true;
        if (address.isLinkLocalAddress()) return true;
        if (address.isSiteLocalAddress()) return true;
        byte[] bytes = address.getAddress();
        
        final byte b0 = bytes[0];
        final byte b1 = bytes[1];
        //10.x.x.x/8
        final byte SECTION_1 = 0x0A;
        //172.16.x.x/12
        final byte SECTION_2 = (byte) 0xAC;
        final byte SECTION_3 = (byte) 0x10;
        final byte SECTION_4 = (byte) 0x1F;
        //192.168.x.x/16
        final byte SECTION_5 = (byte) 0xC0;
        final byte SECTION_6 = (byte) 0xA8;
        switch (b0) {
            case SECTION_1:
                return true;
            case SECTION_2:
                if (b1 >= SECTION_3 && b1 <= SECTION_4) {
                    return true;
                }
            case SECTION_5:
                switch (b1) {
                    case SECTION_6:
                        return true;
                }
            default:
                return false;
        }
    }
    
    public static String getISOTime(){
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = df.format(new Date());
        return timestamp;
    }
    
    public static String tryGetInDictionary(Headers dictionary, String key) {
        List<String> result = dictionary.get(key);
        if (result != null) return result.get(result.size() - 1);
        result = dictionary.get(key.toString().toLowerCase());
        if (result != null) return result.get(result.size() - 1);
        result = dictionary.get(key.toString().toUpperCase());
        if (result != null) return result.get(result.size() - 1);
        return null;
    }
    
    public static Map<String, String> parseBodyToDictionary(String string) {
        if (string.startsWith("{")){
            return JSONObject.parseObject(string).toJavaObject(Map.class);
        } else {
            Map<String, String> dictionary = new HashMap<>();
            String[] params = string.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    dictionary.put(URLDecoder.decode(keyValue[0]), URLDecoder.decode(keyValue[1]));
                }
            }
            return dictionary;
        }
    }
}

