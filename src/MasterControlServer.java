import modules.AvroEncoder;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MasterControlServer {
    private File[] files;
    private byte[] avroBytes;
    private ConcurrentHashMap<String, File> dictionary;
    private ConcurrentHashMap<String, Long> clusterTraffics;
    private ConcurrentHashMap<String, Cluster> clusters;
    private ArrayList<Cluster> onlineClusters;
    
    public MasterControlServer() {
        this.avroBytes = new byte[0];
        this.files = new File[0];
        this.dictionary = new ConcurrentHashMap<>();
        this.clusters = new ConcurrentHashMap<>();
        this.clusterTraffics = new ConcurrentHashMap<>();
        this.onlineClusters = new ArrayList<>();
    }

    public File[] getFiles() {
        return this.files;
    }
    
    public void setFiles(File[] files) throws IOException {
        this.files = files;
        updateDictionary();
        refreshAvroBytes();
    }
    
    public void updateDictionary() {
        synchronized (this.dictionary){
            this.dictionary.clear();
            for (File file : files) {
                this.dictionary.put(file.path, file);
            }
        }
    }
    
    public byte[] getAvroBytes() {
        synchronized (this.avroBytes){
            return this.avroBytes;
        }
    }
    
    public void refreshAvroBytes() throws IOException {
        AvroEncoder encoder = new AvroEncoder();
        synchronized (this.files){
            encoder.setElements(files.length);
            for (File file : files) {
                encoder.setString(file.path);
                encoder.setString(file.hash);
                encoder.setLong(file.size);
                encoder.setLong(file.lastModified);
            }
        }
        encoder.byteStream.close();
        synchronized (this.avroBytes){
            this.avroBytes = encoder.byteStream.toByteArray();
        }
    }
    
    public String requestDownload(String path){
        // 如果 this.files 中有 path 相同的文件
        if (this.dictionary.containsKey(path)){
            File file = this.dictionary.get(path);
            // 选择一个节点
            String id = chooseOneCluster(this.clusters.values());
            Cluster cluster = this.clusters.get(id);
            // 为这个请求计算 sign
            String sign = Utils.getSign(file, cluster);
            // 为选择到的节点加上流量
            this.clusterTraffics.put(id, this.clusterTraffics.get(cluster) + file.size);
            if (sign == null) return null;
            return "https://" + cluster.ip + ":" + this.clusters.get(cluster).port + "/download/" + file.hash + sign;
        }
        else {
            return null;
        }
    }
    
    public String chooseOneCluster(Collection<Cluster> values) {
        // 从 this.onlineClusters 中随机选择一个并返回
        synchronized (this.onlineClusters){
            return this.onlineClusters.get(new Random().nextInt(this.onlineClusters.size())).id;
        }
    }
}
