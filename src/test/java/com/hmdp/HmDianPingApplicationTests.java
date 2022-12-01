package com.hmdp;

import com.hmdp.utils.PasswordEncoder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

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

}
