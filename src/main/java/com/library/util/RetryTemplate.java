package com.library.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

//重试模板工具类，为关键操作提供重试机制
@Slf4j
@Component
public class RetryTemplate {

    //执行带重试的操作
    public <T> T executeWithRetry(RetryCallback<T> callback,
                                  RetryConfig retryConfig) throws Exception {
        int attempt = 0;
        Throwable lastException = null;

        while (attempt <= retryConfig.getMaxAttempts()) {
            attempt++;
            try {
                log.debug("重试尝试 {}/{}", attempt, retryConfig.getMaxAttempts());
                T result = callback.execute();
                if (attempt > 1) {
                    log.info("重试成功，尝试次数: {}", attempt);
                }
                return result;
            } catch (Exception e) {
                lastException = e;
                log.warn("重试尝试 {}/{} 失败: {}", attempt, retryConfig.getMaxAttempts(), e.getMessage());

                //检查是否应该重试
                if (attempt >= retryConfig.getMaxAttempts() ||
                        !retryConfig.getRetryPredicate().shouldRetry(e)) {
                    break;
                }

                //检查是否需要等待
                if (retryConfig.getWaitInterval() > 0) {
                    try {
                        long waitTime = calculateWaitTime(attempt, retryConfig);
                        log.debug("等待 {} 毫秒后重试", waitTime);
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试等待被中断", ie);
                    }
                }
            }
        }

        log.error("重试失败，已达到最大尝试次数: {}", retryConfig.getMaxAttempts());
        throw new RetryExhaustedException("重试失败，已达到最大尝试次数: " + retryConfig.getMaxAttempts(), lastException);
    }

    //计算等待时间（支持指数退避）
    private long calculateWaitTime(int attempt, RetryConfig retryConfig) {
        if (retryConfig.isExponentialBackoff()) {
            //指数退避：waitInterval * 2^(attempt-1)
            return retryConfig.getWaitInterval() * (long) Math.pow(2, attempt - 1);
        } else {
            //固定间隔
            return retryConfig.getWaitInterval();
        }
    }

    //回调接口
    public interface RetryCallback<T> {
        T execute() throws Exception;
    }

    //重试条件接口
    public interface RetryPredicate {
        boolean shouldRetry(Throwable throwable);
    }

    //重试配置类
    public static class RetryConfig {
        private int maxAttempts = 3;
        private long waitInterval = 1000; //毫秒
        private boolean exponentialBackoff = false;
        private RetryPredicate retryPredicate = this::defaultRetryPredicate;

        //默认重试条件：重试所有异常
        private boolean defaultRetryPredicate(Throwable throwable) {
            return true;
        }

        public static RetryConfig create() {
            return new RetryConfig();
        }

        public RetryConfig maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public RetryConfig waitInterval(long waitInterval) {
            this.waitInterval = waitInterval;
            return this;
        }

        public RetryConfig exponentialBackoff(boolean exponentialBackoff) {
            this.exponentialBackoff = exponentialBackoff;
            return this;
        }

        public RetryConfig retryPredicate(RetryPredicate retryPredicate) {
            this.retryPredicate = retryPredicate;
            return this;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public long getWaitInterval() {
            return waitInterval;
        }

        public boolean isExponentialBackoff() {
            return exponentialBackoff;
        }

        public RetryPredicate getRetryPredicate() {
            return retryPredicate;
        }
    }

    //重试耗尽异常
    public static class RetryExhaustedException extends Exception {
        public RetryExhaustedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
