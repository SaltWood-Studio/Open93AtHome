package top.saltwood.everythingAtHome;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StorageHelper<T> {
    private final Class<T> tClass;
    private final String name;
    public List<T> elements = new ArrayList<>();
    
    public StorageHelper(String name, Class<T> tClass) {
        this.name = name;
        this.tClass = tClass;
    }
    
    public void save() {
        byte[] bytes = JSON.toJSONBytes(this.elements);
        // 读取文件
        try (FileOutputStream fos = new FileOutputStream(name)) {
            fos.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void load() {
        byte[] bytes;
        // 写入到文件
        try (FileInputStream fis = new FileInputStream(name)) {
            bytes = fis.readAllBytes();
        } catch (IOException e) {
            return;
        }
        List<JSONObject> objects = JSON.parseObject(bytes, ArrayList.class);
        for (JSONObject element : objects) {
            this.elements.add(element.toJavaObject(this.tClass));
        }
    }
}
