package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.service.impl.UserServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

/**
 * @author lh
 * @since 2023/4/9
 * 保存多个token在redis中以做多人秒杀压测
 */
@Component
public class MutilToken {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UserServiceImpl userService;

    private StringJoiner tokens = new StringJoiner(",");


    public void createMutilToken() {

        for (int i = 0; i < 300; i++) {
            String token = UUID.randomUUID().toString(true);
            tokens.add(token);
            String phone = "132" + RandomUtil.randomNumbers(8);
            User user = userService.createUserWithPhone(phone);

            String tokenKey = RedisConstants.LOGIN_USER_KEY + token;

            UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);

            Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                    CopyOptions.create()
                            .setIgnoreNullValue(true)
                            .setFieldValueEditor((fieldKey, fieldValue) -> fieldValue.toString()));

            stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
            // 设置token有效期
            stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.SECONDS);
        }

        try {
            saveFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("执行完毕！！");
    }

    private void saveFile() throws IOException {
        FileOutputStream fileOutputStream = null;
        File file = new File("D:\\user\\lhbilibili\\token.txt");
        if (!file.exists()) {
            file.createNewFile();
        }

        fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(tokens.toString().getBytes(StandardCharsets.UTF_8));
        fileOutputStream.flush();
        fileOutputStream.close();
    }

}
