package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
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

        // 解决缓存击穿
        Shop shop = queryWithMutex(id);

        if (shop == null) {
            return Result.fail("服务器异常");
        }
        return Result.ok(shop);
    }

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
        Shop shop = null;
        try {
            // 尝试获取锁
            if (!tryLock(lockKey)) {
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
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1");

        return BooleanUtil.isTrue(flag);
    }

    /**
     * 解锁
     * @param key 需要解锁lock的key
     */
    private void unLock(String key) {
        stringRedisTemplate.delete(key);
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
