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

    }
}*/