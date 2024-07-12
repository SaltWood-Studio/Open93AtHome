package modules;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskExecutor {
    
    // 使用线程池来执行任务
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    
    // 方法接受多个 Lambda 表达式作为参数，并返回一个 Thread
    public static Thread executeAsync(Runnable... tasks) {
        Thread waitingThread = new Thread(() -> {
            // 提交每个任务到线程池中执行
            for (Runnable task : tasks) {
                executor.submit(task);
            }
        });
        
        waitingThread.start();
        return waitingThread;
    }
}