package top.saltwood.everythingAtHome.modules;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class AvroDecoder {
    public final InputStream byteStream;

    public AvroDecoder(InputStream is) {
        this.byteStream = is;
    }

    public static long byteToLong(InputStream is) throws IOException {
        byte b = is.readNBytes(1)[0];
        long n = b & 0x7F;
        long shift = 7;
        while ((b & 0x80) != 0) {
            b = is.readNBytes(1)[0];
            n |= (long) (b & 0x7F) << shift;
            shift += 7;
        }
        return (n >> 1) ^ -(n & 1);
    }

    public static String byteToString(InputStream is) throws IOException {
        long length = byteToLong(is);
        byte[] encodedString = is.readNBytes((int) length);
        return new String(encodedString);
    }

    public long getElements() throws IOException {
        return this.getLong();
    }

    public long getLong() throws IOException {
        return byteToLong(this.byteStream);
    }

    public String getString() throws IOException {
        return new String(this.getBytes());
    }

    public byte[] getBytes() throws IOException {
        long length = byteToLong(this.byteStream);
        return this.byteStream.readNBytes((int) length);
    }

    public boolean getEnd() throws IOException {
        return this.byteStream.read() == 0;
    }
}
