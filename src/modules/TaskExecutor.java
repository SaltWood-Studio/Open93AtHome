/*
package modules;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class TaskExecutor {
    
    // 使用线程池来执行任务
    public final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    public void submit(Runnable... runs){
        for(Runnable runnable : runs){
            try {
                synchronized (executor) {
                    executor.submit(runnable);
                    Thread.sleep(3000);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    public boolean enableTask() throws Exception {
        if (cluster == null) throw new Exception("cluster not found");
        boolean isValid = true;
        Exception exception = null;
        for (int i = 0; i < 8; i++) {
            FileObject file = getFiles().get(new Random().nextInt(getFiles().size()));
            String url = requestDownload(file, cluster);
            try {
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", SharedData.config.config.userAgent)
                        .build();
                OkHttpClient client = new OkHttpClient();
                Response response = client.newCall(request).execute();
                if (!file.hash.equals(FileObject.computeHash(response.body().byteStream()))){
                    isValid = false;
                    break;
                }
            } catch (Exception ex) {
                exception = ex;
                isValid = false;
                break;
            }
        }
        if (isValid) {
            this.onlineClusters.add(cluster);
        } else {
            throw new Exception("Unable to download files from the cluster: " + exception.getMessage());
        }
    }
}*/