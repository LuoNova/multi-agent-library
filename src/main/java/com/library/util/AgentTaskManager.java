package com.library.util;

import com.library.agent.dto.TaskResultHolder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

//Agent任务管理器（修复版：添加任务队列用于Controller与Agent通信，增强并发安全）
@Slf4j
@Component
public class AgentTaskManager {

    //使用ConcurrentHashMap保证线程安全
    private final Map<String, TaskResultHolder> taskMap = new ConcurrentHashMap<>();
    //任务队列（Controller放入，Agent取出），使用LinkedBlockingQueue保证线程安全
    private final BlockingQueue<TaskRequest> taskQueue = new LinkedBlockingQueue<>();

    //使用ReentrantLock保护关键操作
    private final ReentrantLock taskLock = new ReentrantLock();

    //使用原子计数器生成唯一任务ID，防止并发冲突
    private final java.util.concurrent.atomic.AtomicLong taskIdCounter = new java.util.concurrent.atomic.AtomicLong(0);

    //Controller调用：提交新任务（线程安全）
    public String submitTask(Long userId, Long biblioId, Long preferredLibraryId) {
        taskLock.lock();
        try {
            //使用原子计数器生成唯一任务ID，避免时间戳冲突
            String taskId = "TASK-" + System.currentTimeMillis() + "-" + taskIdCounter.incrementAndGet();

            //创建结果持有者（用于Controller等待）
            TaskResultHolder holder = new TaskResultHolder();
            taskMap.put(taskId, holder);

            //将任务放入队列（Agent会轮询获取）
            TaskRequest request = new TaskRequest(taskId, userId, biblioId, preferredLibraryId);
            boolean offered = taskQueue.offer(request);

            if (!offered) {
                log.error("任务队列已满，无法提交任务: {}", taskId);
                taskMap.remove(taskId);
                throw new IllegalStateException("任务队列已满");
            }

            log.info("任务提交成功: {}", taskId);
            return taskId;
        } finally {
            taskLock.unlock();
        }
    }

    //Agent调用：获取待处理任务（非阻塞，线程安全）
    public TaskRequest pollTask() {
        return taskQueue.poll();
    }

    //Agent调用：获取待处理任务（带超时，线程安全）
    public TaskRequest pollTask(long timeout, TimeUnit unit) throws InterruptedException {
        return taskQueue.poll(timeout, unit);
    }

    //Agent调用：提交结果（线程安全）
    public void completeTask(String taskId, String resultJson) {
        taskLock.lock();
        try {
            TaskResultHolder holder = taskMap.get(taskId);
            if (holder != null) {
                holder.complete(resultJson);
                taskMap.remove(taskId); //清理
                log.info("任务完成: {}", taskId);
            } else {
                log.warn("任务不存在或已完成: {}", taskId);
            }
        } finally {
            taskLock.unlock();
        }
    }

    //Controller调用：获取持有者（用于等待，线程安全）
    public TaskResultHolder getHolder(String taskId) {
        return taskMap.get(taskId);
    }

    //清理超时任务，防止内存泄漏
    public void cleanupExpiredTasks(long timeoutMillis) {
        taskLock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            taskMap.entrySet().removeIf(entry -> {
                TaskResultHolder holder = entry.getValue();
                if (holder.isExpired(currentTime, timeoutMillis)) {
                    log.warn("清理超时任务: {}", entry.getKey());
                    return true;
                }
                return false;
            });
        } finally {
            taskLock.unlock();
        }
    }

    //获取当前任务队列大小
    public int getQueueSize() {
        return taskQueue.size();
    }

    //获取当前活跃任务数
    public int getActiveTaskCount() {
        return taskMap.size();
    }

    //内部类：任务请求
    @Data
    @AllArgsConstructor
    public static class TaskRequest {
        private String taskId;
        private Long userId;
        private Long biblioId;
        private Long preferredLibraryId;
    }
}