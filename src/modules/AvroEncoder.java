package modules;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AvroEncoder {
    public ByteArrayOutputStream byteStream;
    
    public AvroEncoder() {
        byteStream = new ByteArrayOutputStream();
    }
    
    public void setElements(int count) throws IOException {
        this.byteStream.write(this.writeLong(count));
    }
    
    public void setLong(long value) throws IOException {
        this.byteStream.write(this.writeLong(value));
    }
    
    public void setString(String value) throws IOException {
        this.byteStream.write(this.writeString(value));
    }
    
    public byte[] writeLong(long value) throws IOException {
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
    
    public byte[] writeString(String value) throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] bytes = value.getBytes("UTF-8");
        o.write(writeLong(bytes.length));
        o.write(bytes);
        o.close();
        return o.toByteArray();
    }
    
    public void setEnd(){
        this.byteStream.write(0);
    }
}
