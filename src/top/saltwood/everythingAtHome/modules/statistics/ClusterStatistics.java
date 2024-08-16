package top.saltwood.everythingAtHome.modules.statistics;

import java.util.Calendar;

public class ClusterStatistics {
    private Object _lock = new Object();
    private long[][] bytes = new long[24][31];
    private long[][] hits = new long[24][31];

    public ClusterStatistics() {

    }

    public void add(long traffic) {
        synchronized (_lock) {
            final Calendar instance = Calendar.getInstance();
            this.bytes[instance.get(Calendar.DAY_OF_MONTH)][instance.get(Calendar.HOUR_OF_DAY)] += traffic;
            this.hits[instance.get(Calendar.DAY_OF_MONTH)][instance.get(Calendar.HOUR_OF_DAY)] += 1;
        }
    }

    public void add(long hits, long traffic) {
        synchronized (_lock) {
            final Calendar instance = Calendar.getInstance();
            this.bytes[instance.get(Calendar.DAY_OF_MONTH)][instance.get(Calendar.HOUR_OF_DAY)] += traffic;
            this.hits[instance.get(Calendar.DAY_OF_MONTH)][instance.get(Calendar.HOUR_OF_DAY)] += hits;
        }
    }

    public long getBytes(int dayOfMonth, int hour){
        return this.bytes[dayOfMonth][hour];
    }

    public long getHits(int dayOfMonth, int hour){
        return this.hits[dayOfMonth][hour];
    }

    public void setBytes(int dayOfMonth, int hour, long value){
        this.bytes[dayOfMonth][hour] = value;
    }

    public void setHits(int dayOfMonth, int hour, long value){
        this.hits[dayOfMonth][hour] = value;
    }

    public long[][] getRawBytes(){
        return this.bytes;
    }

    public long[][] getRawHits(){
        return this.hits;
    }
}
