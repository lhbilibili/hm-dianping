package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @author lh
 * @since 2023/1/13
 */
public class SimpleRedisLock implements ILock {

    private String name;
    private StringRedisTemplate stringRedisTemplate;
    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = "UUID_";

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        String key = KEY_PREFIX + name;
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        Boolean res = stringRedisTemplate.opsForValue().setIfAbsent(key, threadId, timeoutSec, TimeUnit.SECONDS);

        return Boolean.TRUE.equals(res);
    }

    @Override
    public void unLock() {
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        String key = KEY_PREFIX + name;

        String val = stringRedisTemplate.opsForValue().get(key);

        if (threadId.equals(val)) {
            stringRedisTemplate.delete(key);
        }
    }
}
