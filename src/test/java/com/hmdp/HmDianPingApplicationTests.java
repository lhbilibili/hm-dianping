package com.hmdp;

import com.hmdp.service.IShopService;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.PasswordEncoder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

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

}
