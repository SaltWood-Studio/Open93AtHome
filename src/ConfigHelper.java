import com.alibaba.fastjson2.JSON;
import modules.Config;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ConfigHelper {
    private final String name;
    public Config config;
    
    public ConfigHelper(String name) {
        this.name = name;
        this.config = new Config();
    }
    
    public void save() {
        byte[] bytes = JSON.toJSONBytes(this.config);
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
        config = JSON.parseObject(bytes, Config.class);
    }
}
