package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @author lh
 * @since 2023/1/11
 */
@Component
public class RedisIdWorker {
    /**
     * id时间戳的开始时间, 2023.1.1
     */
    private static final long BEGIN_TIMESTAMP = 1672531200L;

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String keyPrefix) {
        // 时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowTimeStamp = now.toEpochSecond(ZoneOffset.UTC);
        long stamp = nowTimeStamp - BEGIN_TIMESTAMP;

        // redis的自增id
        String nowDate = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count = stringRedisTemplate.opsForValue().increment("inc:" + keyPrefix + ":" + nowDate);

        // 拼接
        return stamp << 32 | count;
    }
}
