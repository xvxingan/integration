package com.github.xvxingan.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * @author xuxingan
 */
public class ExecutorServiceUtil {
    private static ThreadFactory threadFactory = new ThreadFactory() {
        private Integer seq = 0;
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("自定义线程池-"+ (seq++));
            return thread;
        }
    };

    private static ExecutorService cachedThreadPool = Executors.newCachedThreadPool(threadFactory);

    private static ExecutorService singleThreadPool = Executors.newSingleThreadExecutor(threadFactory);

    private static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10,threadFactory);

    public static ExecutorService getCachedExecutorService(){
        return cachedThreadPool;
    }

    public static ScheduledExecutorService getScheduledExecutorService(){
        return scheduledExecutorService;
    }
    public static ExecutorService getSingeExecutorService(){
        return singleThreadPool;
    }
}
