import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class StorageHelper<T> {
    private final Class<T> tClass;
    private String name;
    public List<T> elements = new ArrayList<>();
    
    public StorageHelper(String name, Class<T> tClass) {
        this.name = name;
        this.tClass = tClass;
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
        List<JSONObject> objects = JSON.parseObject(bytes, ArrayList.class);
        for (JSONObject element : objects){
            this.elements.add(element.toJavaObject(this.tClass));
        }
    }
}
