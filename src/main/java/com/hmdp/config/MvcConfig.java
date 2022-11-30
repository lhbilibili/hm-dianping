package com.hmdp.config;

import com.hmdp.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * <p>
 *
 * </p>
 *
 * @author lh
 * @since 2022/11/30
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/blog/hot",
                        "/user/login",
                        "/user/code",
                        "/shop-type/**",
                        "/shop/**",
                        "/voucher/**",
                        "/upload/**"
                );

    }
}
