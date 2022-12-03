package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 首先查询redis，有则返回
     * 再去查询数据库，没有报错
     * 存入redis中
     * @param id shop的id
     * @return Result
     */
    @Override
    public Result queryById(Long id) {
        // 缓存穿透
        // Shop shop = queryWithPassThrough(id);

        // 互斥锁解决缓存击穿
        // Shop shop = queryWithMutex(id);

        // 逻辑过期解决缓存击穿
        Shop shop = queryWithLogicalExpire(id);

        if (shop == null) {
            return Result.fail("服务器异常");
        }
        return Result.ok(shop);
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    /**
     * 逻辑过期
     * 查询redis 没命中-->返回null
     * 命中-->去判断是否过期 过期-->新建一个线程去更新缓存
     * 没过期直接返回缓存
     */
    private Shop queryWithLogicalExpire(Long id) {
        String shopKey = RedisConstants.CACHE_SHOP_KEY + id;
        // 查询redis
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        if (StrUtil.isBlank(shopJson)) {
            // 未命中返回null
            return null;
        }

        // 命中
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        // 旧值或新值
        Shop shop = JSONUtil.toBean(data, Shop.class);

        LocalDateTime expireTime = redisData.getExpireTime();
        // 没过期，直接返回
        if (expireTime.isAfter(LocalDateTime.now())) {
            return shop;
        }

        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);

        if (isLock) {
            // 获取到锁
            // double check
            String doubleCheckShopJson = stringRedisTemplate.opsForValue().get(shopKey);

            RedisData doubleCheckRedisData = JSONUtil.toBean(doubleCheckShopJson, RedisData.class);
            LocalDateTime doubleCheckExpireTime = doubleCheckRedisData.getExpireTime();
            if (doubleCheckExpireTime.isAfter(LocalDateTime.now())) {
                JSONObject doubleCheckShop = (JSONObject) doubleCheckRedisData.getData();
                return JSONUtil.toBean(doubleCheckShop, Shop.class);
            }
            // double check检查过期, 再新建一个线程
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    saveShop2Redis(id, 10L);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    unLock(lockKey);
                }
            });
        }
        // 返回旧值
        return shop;
    }

    /**
     * 缓存击穿问题解决方案之互斥锁
     */
    private Shop queryWithMutex(Long id) {
        String shopKey = RedisConstants.CACHE_SHOP_KEY + id;
        // 查询redis
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        if (StrUtil.isNotBlank(shopJson)) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // 检测到空缓存
        if (shopJson != null) {
            return null;
        }

        // 缓存未命中
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        Shop shop = null;
        try {
            // 尝试获取锁
            if (!isLock) {
                // 获取锁失败
                Thread.sleep(50);
                return queryWithMutex(id);
            }

            // 获取锁成功，做Double Check
            String doubleCheckShopJson = stringRedisTemplate.opsForValue().get(shopKey);
            if (StrUtil.isNotBlank(doubleCheckShopJson)) {
                return JSONUtil.toBean(doubleCheckShopJson, Shop.class);
            }
            // 还需要判断是否为空缓存
            if (doubleCheckShopJson != null) {
                return null;
            }

            // doubleCheck的缓存未命中再查询数据库
            shop = this.getById(id);
            Thread.sleep(200); // 做测试用

            // 存入空缓存
            if (shop == null) {
                stringRedisTemplate.opsForValue().set(shopKey, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            // 查询结果存入redis中
            stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 释放锁
            unLock(lockKey);
        }

        return shop;
    }

    /**
     * 缓存穿透
     * @param id shop的id
     * @return 商铺信息
     */
    private Shop queryWithPassThrough(Long id) {
        String shopKey = RedisConstants.CACHE_SHOP_KEY + id;
        // 查询redis
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        if (StrUtil.isNotBlank(shopJson)) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        // 检测到空缓存
        if (shopJson != null) {
            return null;
        }

        // 查询数据库
        Shop shop = this.lambdaQuery().eq(Shop::getId, id).one();

        // 存入空缓存
        if (shop == null) {
            stringRedisTemplate.opsForValue().set(shopKey, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 查询结果存入redis中
        stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return shop;
    }

    /**
     * 利用redis的setnx方法实现一个锁
     * @param key lock的key
     * @return 是否获取到锁
     */
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

    public void saveShop2Redis(Long id, Long expiredSeconds) throws InterruptedException {
        // 查询数据库
        Shop shop = getById(id);
        Thread.sleep(200);
        // 封装过期时间
        RedisData data = new RedisData();
        data.setData(shop);
        data.setExpireTime(LocalDateTime.now().plusSeconds(expiredSeconds));
        // 写入redis
        String shopKey = RedisConstants.CACHE_SHOP_KEY + id;
        stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(data));
    }

    /**
     * 先更新数据库，再删除缓存
     * @param shop 商铺信息
     * @return Result
     */
    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (shop.getId() == null) {
            return Result.fail("商铺id为空");
        }
        this.updateById(shop);

        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + id);
        return Result.ok();
    }
}
