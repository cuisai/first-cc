package com.example.demo.controller;

import com.example.demo.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * 线程池演示 Controller
 * 提供各种 REST API 来触发线程池任务
 */
@RestController
@RequestMapping("/api/task")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // ==================== @Async 异步调用 ====================

    /**
     * 触发 @Async 无返回值任务
     * GET /api/task/async-no-return?name=任务1
     */
    @GetMapping("/async-no-return")
    public String asyncNoReturn(@RequestParam String name) {
        taskService.asyncNoReturn(name);
        return "异步任务 [" + name + "] 已提交，不等待结果";
    }

    /**
     * 触发 @Async 有返回值任务
     * GET /api/task/async-with-return?name=任务A
     */
    @GetMapping("/async-with-return")
    public String asyncWithReturn(@RequestParam String name) {
        CompletableFuture<String> future = taskService.asyncWithReturn(name);
        try {
            // get() 会阻塞等待结果
            String result = future.get();
            return "异步任务结果: " + result;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            return "异步任务异常: " + e.getMessage();
        }
    }

    // ==================== 手动提交任务 ====================

    /**
     * 手动提交 Runnable
     * GET /api/task/execute?name=任务X
     */
    @GetMapping("/execute")
    public String execute(@RequestParam String name) {
        taskService.executeRunnable(name);
        return "Runnable 任务 [" + name + "] 已提交";
    }

    /**
     * 手动提交 Callable
     * GET /api/task/submit?name=任务Y
     */
    @GetMapping("/submit")
    public String submit(@RequestParam String name) throws Exception {
        Future<String> future = taskService.submitCallable(name);
        return "Callable 任务结果: " + future.get();
    }

    // ==================== CompletableFuture 链式编排 ====================

    /**
     * 链式异步任务
     * GET /api/task/chain?orderId=ORDER001
     */
    @GetMapping("/chain")
    public String chain(@RequestParam String orderId) throws Exception {
        CompletableFuture<String> future = taskService.chainTask(orderId);
        return "链式任务结果: " + future.get();
    }

    // ==================== 批量处理 ====================

    /**
     * 批量并行处理
     * POST /api/task/batch
     * Body: ["item1", "item2", "item3", "item4", "item5"]
     */
    @PostMapping("/batch")
    public List<String> batch(@RequestBody List<String> items) {
        return taskService.batchProcess(items);
    }

    /**
     * allOf 批量等待
     * GET /api/task/batch-allOf
     */
    @GetMapping("/batch-allOf")
    public String batchAllOf() {
        taskService.batchWithAllOf();
        return "allOf 批量任务全部完成";
    }

    // ==================== 原生线程池 ====================

    /**
     * 使用原生 ThreadPoolExecutor
     * GET /api/task/native?name=原生任务
     */
    @GetMapping("/native")
    public String useNative(@RequestParam String name) {
        taskService.useNativePool(name);
        return "原生线程池任务 [" + name + "] 已提交";
    }

    // ==================== 线程池监控 ====================

    /**
     * 查看所有线程池状态
     * GET /api/task/stats
     */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return taskService.getPoolStats();
    }
}
