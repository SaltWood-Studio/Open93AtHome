package modules;

import com.alibaba.fastjson2.annotation.JSONField;

public class Config {
    @JSONField(serialize = false)
    public static String version = "1.1.0";
    @JSONField(serialize = false)
    public static String userAgent = "93@home-ctrl/" + version;
    public String filePath = "./files";
}
