package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * <p>
 *
 * </p>
 *
 * @author lh
 * @since 2022/12/3
 */
@Component
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 设置key过期时间
     */
    public void set(String key, Object val, Long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(val), timeout, unit);
    }

    /**
     * 设置逻辑过期时间
     */
    public void setWithExpire(String key, Object val, Long timeout, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(val);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(timeout)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 缓存穿透
     *
     * @param keyPrefix  redis中key的前缀
     * @param id         要查数据的id
     * @param type       数据的类型
     * @param dbFallback 执行查询的方法
     * @param timeout    过期时间
     * @param unit       时间单位
     * @param <R>        数据类型
     * @param <ID>       id的类型
     * @return 返回数据
     */
    public <R, ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long timeout, TimeUnit unit) {
        String key = keyPrefix + id;
        // 查询redis
        String dataJson = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(dataJson)) {
            return JSONUtil.toBean(dataJson, type);
        }

        // 检测到空缓存
        if (dataJson != null) {
            return null;
        }

        // 查询数据库
        R r = dbFallback.apply(id);

        // 存入空缓存
        if (r == null) {
            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 查询结果存入redis中
        this.set(key, r, timeout, unit);
        return r;
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public <R, ID> R queryWithLogicalExpire(
            String keyPrefix, String lockKeyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long timeout, TimeUnit unit) {

        String key = keyPrefix + id;
        // 查询redis
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(json)) {
            // 未命中返回null
            return null;
        }

        // 命中
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        // 旧值或新值
        R r = JSONUtil.toBean(data, type);

        LocalDateTime expireTime = redisData.getExpireTime();
        // 没过期，直接返回
        if (expireTime.isAfter(LocalDateTime.now())) {
            return r;
        }

        String lockKey = lockKeyPrefix + id;
        boolean isLock = tryLock(lockKey);

        if (isLock) {
            // 获取到锁
            // double check
            String doubleCheckShopJson = stringRedisTemplate.opsForValue().get(key);

            RedisData doubleCheckRedisData = JSONUtil.toBean(doubleCheckShopJson, RedisData.class);
            LocalDateTime doubleCheckExpireTime = doubleCheckRedisData.getExpireTime();
            if (doubleCheckExpireTime.isAfter(LocalDateTime.now())) {
                JSONObject doubleCheckShop = (JSONObject) doubleCheckRedisData.getData();
                return JSONUtil.toBean(doubleCheckShop, type);
            }
            // double check检查过期, 再新建一个线程
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    R r1 = dbFallback.apply(id);
                    Thread.sleep(200); // TODO 测试用
                    this.setWithExpire(key, r1, timeout, unit);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    unLock(lockKey);
                }
            });
        }
        // 返回旧值
        return r;
    }

    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);

        return BooleanUtil.isTrue(flag);
    }

    /**
     * 解锁
     * @param key 需要解锁lock的key
     */
    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }
}
