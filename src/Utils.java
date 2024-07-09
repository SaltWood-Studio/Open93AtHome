import java.security.SecureRandom;

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
}

