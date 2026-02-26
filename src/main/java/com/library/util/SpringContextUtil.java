package com.library.util;

import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

//Agent获取SpringBean的工具类，修复循环依赖风险
@Component
public class SpringContextUtil implements ApplicationContextAware {

    //使用ReentrantLock保证线程安全
    private static final ReentrantLock lock = new ReentrantLock();
    private static ApplicationContext applicationContext;
    //检查ApplicationContext是否已初始化
    @Getter
    private static volatile boolean initialized = false;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        lock.lock();
        try {
            applicationContext = context;
            initialized = true;
            System.out.println("SpringContextUtil initialized: " + context);
        } finally {
            lock.unlock();
        }
    }

    //获取ApplicationContext，支持等待初始化完成
    public static ApplicationContext getApplicationContext() {
        return getApplicationContextWithTimeout(30, TimeUnit.SECONDS);
    }

    //带超时等待的ApplicationContext获取方法，避免Agent在Spring未初始化完成时启动
    public static ApplicationContext getApplicationContextWithTimeout(long timeout, TimeUnit unit) {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = unit.toMillis(timeout);

        while (!initialized) {
            if (System.currentTimeMillis() - startTime > timeoutMillis) {
                throw new IllegalStateException("ApplicationContext初始化超时。请检查SpringContextUtil是否被扫描，以及Agent是否在Spring完全初始化后启动。当前等待时间: " + timeout + " " + unit);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("等待ApplicationContext初始化时被中断", e);
            }
        }

        lock.lock();
        try {
            if (applicationContext == null) {
                throw new IllegalStateException("ApplicationContext为null，初始化可能失败");
            }
            return applicationContext;
        } finally {
            lock.unlock();
        }
    }

    //安全获取Bean，支持等待初始化
    public static <T> T getBean(Class<T> clazz) {
        return getApplicationContext().getBean(clazz);
    }

    //安全获取Bean（指定名称和类型），支持等待初始化
    public static <T> T getBean(String name, Class<T> clazz) {
        return getApplicationContext().getBean(name, clazz);
    }
}