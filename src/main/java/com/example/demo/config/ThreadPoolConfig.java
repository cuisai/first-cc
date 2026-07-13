package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置类
 *
 * 核心概念：
 * - corePoolSize:    核心线程数，线程池一直保持的线程数量（即使空闲）
 * - maxPoolSize:     最大线程数，队列满了之后能扩展到的最大线程数
 * - queueCapacity:   队列容量，用于存放等待执行的任务
 * - keepAliveSeconds:超过核心线程数的空闲线程存活时间
 *
 * 执行流程：
 * 1. 任务进来 → 先使用核心线程
 * 2. 核心线程满了 → 放入队列
 * 3. 队列满了 → 创建新线程，最多到 maxPoolSize
 * 4. 全部满了 → 触发拒绝策略（RejectedExecutionHandler）
 */
@Configuration
@EnableAsync  // 开启 Spring 异步支持，使 @Async 注解生效
public class ThreadPoolConfig {

    // ==================== 方式一：用于 @Async 注解的默认线程池 ====================

    /**
     * Spring 默认异步任务执行器
     * 当使用 @Async 注解时，默认使用这个线程池
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // ---------- 核心参数 ----------
        executor.setCorePoolSize(5);             // 核心线程数
        executor.setMaxPoolSize(10);             // 最大线程数
        executor.setQueueCapacity(100);          // 阻塞队列容量
        executor.setKeepAliveSeconds(60);        // 空闲线程存活时间（秒）

        // ---------- 线程工厂 ----------
        executor.setThreadNamePrefix("async-task-");  // 线程名前缀，方便排查问题

        // ---------- 拒绝策略 ----------
        // CallerRunsPolicy: 当线程池满了，由调用者线程执行任务（推荐，不会丢任务）
        // AbortPolicy:       直接抛异常 RejectedExecutionException（默认）
        // DiscardPolicy:     直接丢弃任务（不推荐）
        // DiscardOldestPolicy: 丢弃队列中最旧的任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // ---------- 优雅关闭 ----------
        executor.setWaitForTasksToCompleteOnShutdown(true);  // 关闭时等待任务完成
        executor.setAwaitTerminationSeconds(60);             // 最多等 60 秒

        executor.initialize();
        return executor;
    }

    // ==================== 方式二：自定义专用线程池（用于特定业务场景） ====================

    /**
     * IO 密集型任务线程池
     * 适用于：数据库查询、远程 API 调用、文件读写等
     * 线程数公式：CPU 核心数 * 2（经验值）
     */
    @Bean(name = "ioThreadPool")
    public ThreadPoolTaskExecutor ioThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("io-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * CPU 密集型任务线程池
     * 适用于：大量计算、加密解密、数据处理等
     * 线程数公式：CPU 核心数 + 1（经验值）
     */
    @Bean(name = "cpuThreadPool")
    public ThreadPoolTaskExecutor cpuThreadPool() {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(cpuCores);
        executor.setMaxPoolSize(cpuCores + 1);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("cpu-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    // ==================== 方式三：使用 Java 原生的 ThreadPoolExecutor ====================

    /**
     * 原生 ThreadPoolExecutor（如果不需要 Spring 封装）
     */
    @Bean(name = "nativeThreadPool")
    public ThreadPoolExecutor nativeThreadPool() {
        return new ThreadPoolExecutor(
                3,                              // corePoolSize
                6,                              // maxPoolSize
                30L,                            // keepAliveTime
                java.util.concurrent.TimeUnit.SECONDS,
                new java.util.concurrent.LinkedBlockingQueue<>(50),   // 有界队列
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
