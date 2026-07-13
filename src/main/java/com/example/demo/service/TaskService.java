package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池使用示例 Service
 *
 * 演示四种使用方式：
 * 1. @Async 注解（最简洁）
 * 2. 直接注入线程池手动提交任务
 * 3. CompletableFuture 异步编排
 * 4. 查询线程池状态
 */
@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final ThreadPoolTaskExecutor taskExecutor;
    private final ThreadPoolTaskExecutor ioThreadPool;
    private final ThreadPoolExecutor nativeThreadPool;

    // 注入三个不同的线程池
    public TaskService(
            @Qualifier("taskExecutor") ThreadPoolTaskExecutor taskExecutor,
            @Qualifier("ioThreadPool") ThreadPoolTaskExecutor ioThreadPool,
            @Qualifier("nativeThreadPool") ThreadPoolExecutor nativeThreadPool) {
        this.taskExecutor = taskExecutor;
        this.ioThreadPool = ioThreadPool;
        this.nativeThreadPool = nativeThreadPool;
    }

    // ==================== 方式一：@Async 注解（最常用） ====================

    /**
     * 无返回值的异步任务
     * @Async 注解的方法会在线程池中异步执行
     */
    @Async("taskExecutor")
    public void asyncNoReturn(String taskName) {
        log.info("[@Async 无返回值] 任务 [{}] 开始执行，线程: {}",
                taskName, Thread.currentThread().getName());
        try {
            Thread.sleep(2000); // 模拟耗时操作
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("[@Async 无返回值] 任务 [{}] 执行完成", taskName);
    }

    /**
     * 有返回值的异步任务
     * 返回 Future 或 CompletableFuture
     */
    @Async("taskExecutor")
    public CompletableFuture<String> asyncWithReturn(String taskName) {
        log.info("[@Async 有返回值] 任务 [{}] 开始执行，线程: {}",
                taskName, Thread.currentThread().getName());

        String result;
        try {
            Thread.sleep(1500);
            result = taskName + " -> 处理成功";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result = taskName + " -> 被中断";
        }

        log.info("[@Async 有返回值] 任务 [{}] 执行完成", taskName);
        return CompletableFuture.completedFuture(result);
    }

    // ==================== 方式二：手动提交任务 ====================

    /**
     * 通过注入的线程池手动提交 Runnable 任务
     */
    public void executeRunnable(String taskName) {
        ioThreadPool.execute(() -> {
            log.info("[execute Runnable] 任务 [{}] 开始执行，线程: {}",
                    taskName, Thread.currentThread().getName());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            log.info("[execute Runnable] 任务 [{}] 执行完成", taskName);
        });
    }

    /**
     * 通过注入的线程池手动提交 Callable 任务，返回 Future
     */
    public Future<String> submitCallable(String taskName) {
        return ioThreadPool.submit(() -> {
            log.info("[submit Callable] 任务 [{}] 开始执行，线程: {}",
                    taskName, Thread.currentThread().getName());
            Thread.sleep(1000);
            log.info("[submit Callable] 任务 [{}] 执行完成", taskName);
            return taskName + " -> Callable 返回结果";
        });
    }

    // ==================== 方式三：CompletableFuture 异步编排 ====================

    /**
     * CompletableFuture 链式调用
     * 场景：下单 → 扣库存 → 发通知
     */
    public CompletableFuture<String> chainTask(String orderId) {
        return CompletableFuture
                // 步骤1：创建订单
                .supplyAsync(() -> {
                    log.info("[步骤1] 创建订单 [{}]，线程: {}",
                            orderId, Thread.currentThread().getName());
                    sleep(500);
                    return orderId;
                }, taskExecutor)
                // 步骤2：扣减库存
                .thenApplyAsync(id -> {
                    log.info("[步骤2] 扣减库存 [{}]，线程: {}",
                            id, Thread.currentThread().getName());
                    sleep(800);
                    return id + " -> 库存已扣减";
                }, taskExecutor)
                // 步骤3：发送通知
                .thenApplyAsync(result -> {
                    log.info("[步骤3] 发送通知 [{}]，线程: {}",
                            result, Thread.currentThread().getName());
                    sleep(300);
                    return result + " -> 通知已发送";
                }, taskExecutor);
    }

    // ==================== 方式四：批量任务并行处理 ====================

    /**
     * 批量并行处理 + 等待所有结果
     */
    public List<String> batchProcess(List<String> items) {
        log.info("[批量处理] 开始处理 {} 个任务", items.size());

        // 每个元素提交一个异步任务
        List<CompletableFuture<String>> futures = items.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> {
                    log.info("处理 [{}] 开始，线程: {}",
                            item, Thread.currentThread().getName());
                    sleep(1000);
                    return item + " ✅";
                }, ioThreadPool))
                .toList();

        // 等待所有任务完成，收集结果
        List<String> results = futures.stream()
                .map(CompletableFuture::join)  // join() 等待完成并获取结果
                .toList();

        log.info("[批量处理] 全部完成，结果: {}", results);
        return results;
    }

    /**
     * 使用 allOf 等待所有任务完成
     */
    public void batchWithAllOf() {
        CompletableFuture<String> task1 = CompletableFuture.supplyAsync(() -> {
            sleep(1000);
            return "Task1 完成";
        }, taskExecutor);

        CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> {
            sleep(2000);
            return "Task2 完成";
        }, taskExecutor);

        CompletableFuture<String> task3 = CompletableFuture.supplyAsync(() -> {
            sleep(1500);
            return "Task3 完成";
        }, taskExecutor);

        // allOf 等待所有完成
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(task1, task2, task3);

        allFutures.thenRun(() -> {
            try {
                log.info("全部完成! {} | {} | {}",
                        task1.get(), task2.get(), task3.get());
            } catch (Exception e) {
                log.error("获取结果失败", e);
            }
        });

        allFutures.join(); // 阻塞等待
    }

    // ==================== 方式五：使用原生 ThreadPoolExecutor ====================

    public void useNativePool(String taskName) {
        nativeThreadPool.execute(() -> {
            log.info("[原生线程池] 任务 [{}] 开始，线程: {}",
                    taskName, Thread.currentThread().getName());
            sleep(1000);
            log.info("[原生线程池] 任务 [{}] 完成", taskName);
        });
    }

    // ==================== 线程池监控 ====================

    /**
     * 查看线程池状态（可用于监控端点）
     */
    public java.util.Map<String, Object> getPoolStats() {
        java.util.Map<String, Object> stats = new java.util.LinkedHashMap<>();

        // 主线程池
        stats.put("taskExecutor", buildPoolInfo(taskExecutor));
        // IO 线程池
        stats.put("ioThreadPool", buildPoolInfo(ioThreadPool));
        // 原生线程池
        stats.put("nativeThreadPool", buildNativePoolInfo(nativeThreadPool));

        return stats;
    }

    private java.util.Map<String, Object> buildPoolInfo(ThreadPoolTaskExecutor executor) {
        java.util.Map<String, Object> info = new java.util.LinkedHashMap<>();
        info.put("corePoolSize", executor.getCorePoolSize());        // 核心线程数
        info.put("maxPoolSize", executor.getMaxPoolSize());          // 最大线程数
        info.put("poolSize", executor.getPoolSize());                // 当前线程数
        info.put("activeCount", executor.getActiveCount());          // 活跃线程数
        info.put("queueSize", executor.getThreadPoolExecutor()       // 队列中等待的任务数
                .getQueue().size());
        info.put("completedTasks", executor.getThreadPoolExecutor()  // 已完成任务数
                .getCompletedTaskCount());
        info.put("queueRemainingCapacity", executor.getThreadPoolExecutor()
                .getQueue().remainingCapacity());
        return info;
    }

    private java.util.Map<String, Object> buildNativePoolInfo(ThreadPoolExecutor executor) {
        java.util.Map<String, Object> info = new java.util.LinkedHashMap<>();
        info.put("corePoolSize", executor.getCorePoolSize());
        info.put("maxPoolSize", executor.getMaximumPoolSize());
        info.put("poolSize", executor.getPoolSize());
        info.put("activeCount", executor.getActiveCount());
        info.put("queueSize", executor.getQueue().size());
        info.put("completedTasks", executor.getCompletedTaskCount());
        return info;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
