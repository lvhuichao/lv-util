package com.lv.util;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.Setter;

/**
 * 令牌桶算法实现
 * <p>
 * 一秒钟内最多可以获取 maxPermit * 2 个permit.
 * 比如，开始时刻（或者其他某个permits满了的时刻），permits是满的，等于maxPermit，此时瞬间来了maxPermit个请求全部成功；
 * 之后一秒又请求maxPermit个。总共两倍的maxPermit。
 *
 * @author: lvhuichao
 * @date: 2020/10/29
 */
public class RateLimiter {

    private final int maxPermit;
    private final AtomicInteger permits;
    private final int interval;
    private volatile long lastTimeUpdatePermit;

    /**
     * @param permitPerSecond 每秒钟生成的总令牌数
     */
    public RateLimiter(int permitPerSecond) {
        this.maxPermit = permitPerSecond;
        this.permits = new AtomicInteger(maxPermit);
        this.interval = 1000 / permitPerSecond;
        this.lastTimeUpdatePermit = System.currentTimeMillis();
    }

    public boolean acquire() {
        updatePermit();
        return doAcquire();
    }

    public boolean doAcquire() {
        MutableBoolean mutableBoolean = new MutableBoolean();
        permits.getAndUpdate(operand -> {
            if (operand > 0) {
                mutableBoolean.setValue(true);
                return operand - 1;
            } else {
                mutableBoolean.setValue(false);
                return operand;
            }
        });
        return mutableBoolean.value;
    }

    private void updatePermit() {
        long totalInterval;
        long now;
        // try to update permit in CAS.
        while ((totalInterval = ((now = System.currentTimeMillis()) - lastTimeUpdatePermit)) > interval) {
            long count = totalInterval / interval;
            int old = permits.get();
            if (permits.compareAndSet(old, (int) Math.min(maxPermit, old + count))) {
                if (permits.get() == maxPermit) {
                    this.lastTimeUpdatePermit = System.currentTimeMillis();
                } else {
                    this.lastTimeUpdatePermit = now + interval * count;
                }
                break;
            }
        }
    }

    private static class MutableBoolean {
        @Setter
        private boolean value;
    }
}
