package top.saltwood.everythingAtHome.modules.storage;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import io.jsonwebtoken.Jwts;
import top.saltwood.everythingAtHome.modules.AvroDecoder;
import top.saltwood.everythingAtHome.modules.AvroEncoder;
import top.saltwood.everythingAtHome.modules.Config;
import java.nio.file.Path;

public class SecretKeyHelper implements IBaseHelper<SecretKey> {
    private SecretKey key = Jwts.SIG.HS512.key().build();
    private final String directory;

    public SecretKeyHelper(String directory) {
        this.directory = directory;
    }

    @Override
    public void save() throws Exception {
        try (FileOutputStream fos = new FileOutputStream(Path.of(directory, "secret.key").toFile())) {
            byte[] encodedKey = this.key.getEncoded();
            String algorithm = this.key.getAlgorithm();
            AvroEncoder encoder = new AvroEncoder();
            encoder.setBytes(encodedKey);
            encoder.setString(algorithm);
            encoder.byteStream.close();
            fos.write(encoder.byteStream.toByteArray());
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void load() throws Exception {
        byte[] bytes;
        byte[] keyBytes;
        String algorithm;
        try (FileInputStream fis = new FileInputStream(Path.of(directory, "secret.key").toFile())) {
            AvroDecoder decoder = new AvroDecoder(fis);
            keyBytes = decoder.getBytes();
            algorithm = decoder.getString();
        } catch (Exception ignored) {
            return;
        }
        this.key = new SecretKeySpec(keyBytes, 0, keyBytes.length, algorithm);
    }

    @Override
    public SecretKey getItem() {
        return this.key;
    }

    @Override
    public void setItem(SecretKey item) {
        this.key = item;
    }
}
