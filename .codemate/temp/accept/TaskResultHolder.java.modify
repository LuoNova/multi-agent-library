package com.library.agent.dto;

import lombok.Data;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

//任务结果持有者（用于Agent与Controller通信）
@Data
public class TaskResultHolder {
    private String resultJson;      //最终结果JSON
    private CountDownLatch latch = new CountDownLatch(1); //创建了一个"等待1个信号"的锁。

    //等待结果（带超时）
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return latch.await(timeout, unit); //线程阻塞等待
    }

    //设置结果
    public void complete(String result) {
        this.resultJson = result;
        latch.countDown(); //信号减1（变0时唤醒等待线程）
    }
}