package com.hmdp.utils;

/**
 * @author lh
 * @since 2023/1/13
 */
public interface ILock {

    /**
     * 尝试获取锁
     * @param timeoutSec 超时时间
     * @return 是否获取锁成功
     */
    boolean tryLock(long timeoutSec);

    /**
     * 解锁
     */
    void unLock();
}
