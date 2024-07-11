import io.jsonwebtoken.Jwts;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

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
    
    public static String toUrlSafeBase64String(byte[] b) {
        return Base64.getEncoder().encodeToString(b).replace('/', '_').replace('+', '-').replace("=", "");
    }
    
    public static String getSign(File file, Cluster cluster) {
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
            return null;
        }
        long timestamp = System.currentTimeMillis() / 10;
        String e = Long.toString(timestamp, 36);
        byte[] signBytes = sha1.digest((cluster.secret + file.hash + e).getBytes());
        String sign = toUrlSafeBase64String(signBytes);
        return "?s=" + sign + "&e=" + e + "&name=" + file.getName();
    }
}

