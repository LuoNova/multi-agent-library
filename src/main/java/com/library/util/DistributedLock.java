package com.library.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

//分布式锁工具类（基于JVM内存实现，适用于单机部署）
//如果需要真正的分布式锁，建议使用Redis或Zookeeper实现
@Slf4j
@Component
public class DistributedLock {

    //使用ConcurrentHashMap存储锁对象
    private final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    //获取锁（带超时）
    public boolean tryLock(String lockKey, long timeout, TimeUnit unit) {
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock(true));
        try {
            return lock.tryLock(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取锁被中断: {}", lockKey, e);
            return false;
        }
    }

    //获取锁（无限等待）
    public void lock(String lockKey) {
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock(true));
        lock.lock();
    }

    //释放锁
    public void unlock(String lockKey) {
        ReentrantLock lock = lockMap.get(lockKey);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("释放锁成功: {}", lockKey);
        } else {
            log.warn("尝试释放未持有的锁: {}", lockKey);
        }
    }

    //执行带锁的操作
    public <T> T executeWithLock(String lockKey, long timeout, TimeUnit unit, LockCallback<T> callback) {
        if (!tryLock(lockKey, timeout, unit)) {
            throw new IllegalStateException("获取锁超时: " + lockKey);
        }

        try {
            return callback.execute();
        } finally {
            unlock(lockKey);
        }
    }

    //执行带锁的操作（无限等待）
    public <T> T executeWithLock(String lockKey, LockCallback<T> callback) {
        lock(lockKey);
        try {
            return callback.execute();
        } finally {
            unlock(lockKey);
        }
    }

    //回调接口
    public interface LockCallback<T> {
        T execute();
    }

    //清理闲置的锁
    public void cleanupIdleLocks(long idleTimeoutMillis) {
        lockMap.entrySet().removeIf(entry -> {
            ReentrantLock lock = entry.getValue();
            if (!lock.isLocked()) {
                log.debug("清理闲置锁: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
}
