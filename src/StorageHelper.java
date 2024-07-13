import com.alibaba.fastjson2.JSON;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class StorageHelper<T> {
    public List<T> elements = new ArrayList<>();
    private String name;
    
    public StorageHelper(String name) {
        this.name = name;
    }
    
    public void save(){
        byte[] bytes = JSON.toJSONBytes(this.elements);
        // 写入到文件 clusters.dat
        try (FileOutputStream fos = new FileOutputStream(name)) {
            fos.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void load(){
        byte[] bytes;
        // 写入到文件 clusters.dat
        try (FileInputStream fis = new FileInputStream(name)) {
            bytes = fis.readAllBytes();
        } catch (IOException e) {
            return;
        }
        this.elements = JSON.parseObject(bytes, ArrayList.class);
    }
}
