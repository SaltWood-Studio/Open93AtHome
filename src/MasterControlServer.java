import com.github.luben.zstd.Zstd;
import modules.AvroEncoder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class MasterControlServer {
    public final ConcurrentHashMap<String, File> dictionary;
    public final ConcurrentHashMap<String, Long> clusterTraffics;
    public final ConcurrentHashMap<String, Cluster> clusters;
    public final ArrayList<Cluster> onlineClusters;
    public SharedData sharedData;
    private File[] files;
    private byte[] avroBytes;
    
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
        synchronized (this.dictionary) {
            this.dictionary.clear();
            for (File file : files) {
                this.dictionary.put(file.path, file);
            }
        }
    }
    
    public byte[] getAvroBytes() {
        synchronized (this.avroBytes) {
            return this.avroBytes;
        }
    }
    
    public void refreshAvroBytes() throws IOException {
        AvroEncoder encoder = new AvroEncoder();
        synchronized (this.files) {
            encoder.setElements(files.length);
            for (File file : files) {
                encoder.setString(file.path);
                encoder.setString(file.hash);
                encoder.setLong(file.size);
                encoder.setLong(file.lastModified);
            }
        }
        encoder.byteStream.close();
        synchronized (this.avroBytes) {
            this.avroBytes = Zstd.compress(encoder.byteStream.toByteArray());
        }
    }
    
    public String requestDownload(String path) {
        // 如果 this.files 中有 path 相同的文件
        if (this.dictionary.containsKey(path)) {
            File file = this.dictionary.get(path);
            // 选择一个节点
            Cluster cluster = chooseOneCluster();
            if (cluster == null) return null;
            return requestDownload(file, cluster);
        } else {
            return null;
        }
    }
    
    public String requestDownload(File file, Cluster cluster) {
        // 为这个请求计算 sign
        String sign = Utils.getSign(file, cluster);
        // 为选择到的节点加上流量
        this.clusterTraffics.put(cluster.id, this.clusterTraffics.get(cluster) + file.size);
        if (sign == null) return null;
        return "https://" + cluster.ip + ":" + this.clusters.get(cluster).port + "/download/" + file.hash + sign;
    }
    
    public Cluster chooseOneCluster() {
        // 从 this.onlineClusters 中随机选择一个并返回
        synchronized (this.onlineClusters) {
            if (this.onlineClusters.size() == 0) return null;
            return this.onlineClusters.get(new Random().nextInt(this.onlineClusters.size()));
        }
    }
    
    public void tryEnable(String id) throws Exception {
        Cluster cluster = this.clusters.get(id);
        if (cluster == null) throw new Exception("cluster not found");
        Runnable[] runs = new Runnable[8];
        for (int i = 0; i < 8; i++) {
            File file = this.files[new Random().nextInt(files.length)];
            String url = requestDownload(file, cluster);
            Runnable lambda = () -> {
                try {
                    Request request = new Request.Builder()
                            .url(url)
                            .addHeader("User-Agent", "93@home-ctrl/1.0")
                            .build();
                    OkHttpClient client = new OkHttpClient();
                    Response response = client.newCall(request).execute();
                    file.compareHash(response.body().bytes());
                } catch (Exception ex) {
                }
            };
            runs[i] = lambda;
        }
        boolean isValid = Arrays.stream(sharedData.executor.getResult(runs)).allMatch(result -> (boolean) result);
        if (isValid){
            this.onlineClusters.add(cluster);
        }
    }
}
