package top.saltwood.everythingAtHome.modules;

import com.alibaba.fastjson2.annotation.JSONField;

public class Config {
    @JSONField(serialize = false)
    public static final String version = "1.1.4";
    @JSONField(serialize = false)
    public static final String userAgent = "93@home-ctrl/" + version;
    public String filePath = "./files";
    public Boolean debug = false;
}
