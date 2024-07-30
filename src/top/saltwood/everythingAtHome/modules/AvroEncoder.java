package top.saltwood.everythingAtHome.modules;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AvroEncoder {
    public final ByteArrayOutputStream byteStream;
    
    public AvroEncoder() {
        byteStream = new ByteArrayOutputStream();
    }
    
    public static byte[] longToByte(long value) throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        long data = (value << 1) ^ (value >> 63);
        while ((data & ~0x7F) != 0) {
            o.write((byte) ((data & 0x7f) | 0x80));
            data >>= 7;
        }
        o.write((byte) (data));
        o.close();
        return o.toByteArray();
    }
    
    public static byte[] stringToByte(String value) throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        o.write(longToByte(bytes.length));
        o.write(bytes);
        o.close();
        return o.toByteArray();
    }
    
    public void setElements(int count) throws IOException {
        this.byteStream.write(longToByte(count));
    }
    
    public void setLong(long value) throws IOException {
        this.byteStream.write(longToByte(value));
    }
    
    public void setString(String value) throws IOException {
        this.byteStream.write(stringToByte(value));
    }
    
    public void setEnd() {
        this.byteStream.write(0x00);
    }
}
