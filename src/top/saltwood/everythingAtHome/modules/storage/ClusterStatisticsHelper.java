package top.saltwood.everythingAtHome.modules.storage;

import top.saltwood.everythingAtHome.Cluster;
import top.saltwood.everythingAtHome.SharedData;
import top.saltwood.everythingAtHome.modules.AvroDecoder;
import top.saltwood.everythingAtHome.modules.AvroEncoder;
import top.saltwood.everythingAtHome.modules.statistics.ClusterStatistics;

import javax.naming.OperationNotSupportedException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

public class ClusterStatisticsHelper implements IBaseHelper<Dictionary<String, ClusterStatistics>> {
    private Hashtable<String, ClusterStatistics> statistics;
    private final Collection<Cluster> clusters;

    public ClusterStatisticsHelper(Collection<Cluster> clusters) {
        this.statistics = new Hashtable<>();
        this.clusters = clusters;
    }

    @Override
    public void save() throws Exception {
        for (Cluster cluster : this.clusters) {
            try (FileOutputStream fos = new FileOutputStream(Path.of("./statistics", cluster.id).toFile())) {
                AvroEncoder encoder = new AvroEncoder();
                encoder.setLong(this.statistics.size());
                for (var set : this.statistics.entrySet()) {
                    encoder.setString(set.getKey());
                    for (var oneDayBytes : set.getValue().getRawBytes()){
                        for (var bytes : oneDayBytes){
                            encoder.setLong(bytes);
                        }
                    }
                    for (var oneDayHits : set.getValue().getRawHits()){
                        for (var hits : oneDayHits){
                            encoder.setLong(hits);
                        }
                    }
                    encoder.setEnd();
                }
                encoder.setEnd();
                encoder.byteStream.close();
                fos.write(encoder.byteStream.toByteArray());
            }
        }
    }

    @Override
    public void load() throws Exception {
        for (Cluster cluster : this.clusters) {
            try (FileInputStream fis = new FileInputStream(Path.of("./statistics", cluster.id).toFile())){
                this.statistics = new Hashtable<>();
                AvroDecoder decoder = new AvroDecoder(fis);
                long length = decoder.getLong();
                for (long i = 0; i < length; i++) {
                    String key = decoder.getString();
                    ClusterStatistics value = new ClusterStatistics();
                    for (int day = 0; day < 31; day++){
                        for (int hour = 0; day < 24; day++){
                            value.setBytes(day, hour, decoder.getLong());
                        }
                    }
                    for (int day = 0; day < 31; day++){
                        for (int hour = 0; day < 24; day++){
                            value.setHits(day, hour, decoder.getLong());
                        }
                    }
                    if (!decoder.getEnd()) throw new Exception("Invalid statistic data.");
                    this.statistics.put(key, value);
                }
                decoder.getEnd();
            }
        }
    }

    @Override
    public Dictionary<String, ClusterStatistics> getItem() {
        return this.statistics;
    }

    @Override
    public void setItem(Dictionary<String, ClusterStatistics> item) throws OperationNotSupportedException {
        throw new OperationNotSupportedException("Cannot setItem");
    }
}
