package top.saltwood.everythingAtHome.modules.storage;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StorageHelper<T> implements IBaseHelper<List<T>> {
    private final Class<T> tClass;
    private final String name;
    private List<T> item = new ArrayList<>();
    
    public StorageHelper(String name, Class<T> tClass) {
        this.name = name;
        this.tClass = tClass;
    }

    @Override
    public void save() {
        byte[] bytes = JSON.toJSONBytes(this.getItem());
        // 读取文件
        try (FileOutputStream fos = new FileOutputStream(name)) {
            fos.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void load() {
        byte[] bytes;
        // 写入到文件
        try (FileInputStream fis = new FileInputStream(name)) {
            bytes = fis.readAllBytes();
        } catch (IOException e) {
            return;
        }
        var objects = JSON.parseObject(bytes, ArrayList.class);
        for (Object e : objects) {
            JSONObject element = (JSONObject) e;
            this.getItem().add(element.toJavaObject(this.tClass));
        }
    }

    @Override
    public List<T> getItem() {
        return item;
    }

    @Override
    public void setItem(List<T> item) {
        this.item = item;
    }
}
