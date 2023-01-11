package com.hmdp;

import com.hmdp.service.IShopService;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.PasswordEncoder;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class HmDianPingApplicationTests {

    @Test
    void testPasswordEncoder() {
        System.out.println(PasswordEncoder.encode("1234"));
    }

    @Test
    void testDigit() {
        System.out.println((int) 'a');

    }

    @Resource
    private ShopServiceImpl shopService;

    @Test
    void testShop() throws InterruptedException {
        shopService.saveShop2Redis(1L, 10L);
    }

    @Resource
    private RedisIdWorker redisIdWorker;

    private ExecutorService es = Executors.newFixedThreadPool(300);

    @Test
    public void testNextId() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);

        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("test");
                System.out.println(id);
            }
            latch.countDown();
        };
        long start = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - start));
    }

}
