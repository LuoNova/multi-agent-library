package com.library.util;

import com.library.agent.dto.TaskResultHolder;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

//Agent任务管理器（修复版：添加任务队列用于Controller与Agent通信）
@Component
public class AgentTaskManager {

    private Map<String, TaskResultHolder> taskMap = new ConcurrentHashMap<>();
    //新增：任务队列（Controller放入，Agent取出）
    private BlockingQueue<TaskRequest> taskQueue = new LinkedBlockingQueue<>();

    //Controller调用：提交新任务
    public String submitTask(Long userId, Long biblioId, Long preferredLibraryId) {
        String taskId = "TASK-" + System.currentTimeMillis();

        //创建结果持有者（用于Controller等待）
        TaskResultHolder holder = new TaskResultHolder();
        taskMap.put(taskId, holder);

        //将任务放入队列（Agent会轮询获取）
        TaskRequest request = new TaskRequest(taskId, userId, biblioId, preferredLibraryId);
        taskQueue.offer(request);

        return taskId;
    }

    //Agent调用：获取待处理任务（非阻塞）
    public TaskRequest pollTask() {
        return taskQueue.poll();
    }

    //Agent调用：提交结果
    public void completeTask(String taskId, String resultJson) {
        TaskResultHolder holder = taskMap.get(taskId);
        if (holder != null) {
            holder.complete(resultJson);
            taskMap.remove(taskId); //清理
        }
    }

    //Controller调用：获取持有者（用于等待）
    public TaskResultHolder getHolder(String taskId) {
        return taskMap.get(taskId);
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