package com.hmdp;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.map.MapUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author lh
 * @since 2023/6/8
 */
@SpringBootTest
//@RunWith(SpringJUnit4ClassRunner.class)
public class TestHLL {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 存了12k
     */
    @Test
    public void testHyperLogLog() {
        String[] users = new String[1000];

        int index = 0;

        for (int i = 0; i < 1000000; i++) {
            users[index++] = "user_" + i;

            if (index % 1000 == 0) {
                index = 0;
                stringRedisTemplate.opsForHyperLogLog().add("hll1", users);
            }
        }

        Long size = stringRedisTemplate.opsForHyperLogLog().size("hll1");
        System.out.println("size = " + size);
    }

    /**
     * 存了11M
     */
    @Test
    public void testHashMap() {
        String[] users = new String[1000];

        int index = 0;

        for (int i = 0; i < 1000000; i++) {
            users[index++] = "user_" + i;

            if (index % 1000 == 0){
                index = 0;
                Map<String, String> map = arrToMap(users);
                stringRedisTemplate.opsForHash().putAll("hashuv", map);
            }
        }

        Long size = stringRedisTemplate.opsForHash().size("hashuv");
        System.out.println("size = " + size);
    }

    public Map<String, String> arrToMap(String[] arr) {
        Stream<String> stream = Arrays.stream(arr);

        return stream
                .map(s -> new String[] {s, "1"})
                .collect(Collectors.toMap(s -> s[0], s->s[1]));
    }
}
