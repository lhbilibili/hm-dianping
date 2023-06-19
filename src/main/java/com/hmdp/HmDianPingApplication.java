package com.hmdp;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@MapperScan("com.hmdp.mapper")
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@Slf4j
@PropertySource("classpath:preheat.yml")
public class HmDianPingApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(HmDianPingApplication.class, args);
        ConfigurableEnvironment environment = context.getEnvironment();

        boolean isPreheat = Boolean.parseBoolean(environment.getProperty("my.is-preheat"));
        // 缓存预热
        if (isPreheat) {
            IShopService shopService = context.getBean(IShopService.class);
            StringRedisTemplate stringRedisTemplate = context.getBean(StringRedisTemplate.class);
            List<Shop> shops = shopService.query().list();

            for (Shop shop : shops) {
                String key = RedisConstants.CACHE_SHOP_KEY + shop.getId();
                RedisData redisData = new RedisData();
                redisData.setData(shop);
                redisData.setExpireTime(LocalDateTime.now().plusSeconds(TimeUnit.SECONDS.toSeconds(10L)));
                stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
            }
            log.debug("预热完成，预热数量{}", shops.size());
        }
    }

}
