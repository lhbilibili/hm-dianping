package com.hmdp.config;

import com.hmdp.utils.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author lh
 * @since 2023/4/18
 */
@Configuration
public class DirectRabbitConfig {
//    @Bean
//    public Queue orderQueue() {
//        return new Queue(RabbitMQConstants.ORDER_QUEUE_NAME);
//    }
//
//    @Bean
//    public FanoutExchange orderFanoutExchange() {
//        return new FanoutExchange(RabbitMQConstants.ORDER_EXCHANGE_NAME);
//    }
//
//    @Bean
//    public Binding bindDirect() {
//        return BindingBuilder
//                .bind(orderQueue())
//                .to(orderFanoutExchange());
//    }
}
