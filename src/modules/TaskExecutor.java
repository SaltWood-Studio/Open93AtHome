package modules;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskExecutor {
    
    // 使用线程池来执行任务
    public final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    // 方法接受多个 Lambda 表达式作为参数，并返回一个 Thread
    public Thread executeAsync(Runnable... tasks) {
        Thread waitingThread = new Thread(() -> {
            // 提交每个任务到线程池中执行
            for (Runnable task : tasks) {
                try {
                    executor.submit(task).wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        
        waitingThread.start();
        return waitingThread;
    }
}