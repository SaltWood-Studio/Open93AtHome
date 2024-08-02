package top.saltwood.everythingAtHome.modules.storage;

import com.alibaba.fastjson2.JSON;
import top.saltwood.everythingAtHome.modules.Config;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ConfigHelper implements IBaseHelper<Config> {
    private final String name;
    private Config item;
    
    public ConfigHelper(String name) {
        this.name = name;
        this.item = new Config();
    }

    @Override
    public void save() {
        byte[] bytes = JSON.toJSONBytes(this.item);
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
        item = JSON.parseObject(bytes, Config.class);
    }

    @Override
    public Config getItem() {
        return item;
    }

    @Override
    public void setItem(Config item) {
        this.item = item;
    }
}
