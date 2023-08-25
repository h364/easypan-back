package com.hh.easypanspringboot.entity.config;

import com.hh.easypanspringboot.entity.constants.Constants;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitmqConfig {
    public static final String NORMAL_EXCHANGE = "normalExchange";
    public static final String DEAD_EXCHANGE = "deadExchange";
    public static final String NORMAL_QUEUE_N = "normalQueue";
    public static final String DEAD_QUEUE_D = "deadQueue";

    @Bean("normalExchange")
    public DirectExchange normalExchange() {
        return new DirectExchange(NORMAL_EXCHANGE);
    }

    @Bean("deadExchange")
    public DirectExchange deadExchange() {
        return new DirectExchange(DEAD_EXCHANGE);
    }

    @Bean("normalQueue")
    public Queue normalQueue() {
        Map<String, Object> map = new HashMap<>();
        map.put("x-dead-letter-exchange", DEAD_EXCHANGE);
        map.put("x-dead-letter-routing-key", Constants.RABBITMQ_ROUTING_KEY_D);
        map.put("x-message-ttl", Constants.TEN_DAYS);

        return QueueBuilder.durable(NORMAL_QUEUE_N).withArguments(map).build();
    }

    @Bean("deadQueue")
    public Queue deadQueue() {
        return QueueBuilder.durable(DEAD_QUEUE_D).build();
    }

    @Bean
    public Binding queueABindN(@Qualifier("normalQueue") Queue queueA, @Qualifier("normalExchange") DirectExchange xExchange) {
        return BindingBuilder.bind(queueA).to(xExchange).with(Constants.RABBITMQ_ROUTING_KEY_N);
    }

    @Bean
    public Binding queueDBindD(@Qualifier("deadQueue") Queue queueD, @Qualifier("deadExchange") DirectExchange yExchange) {
        return BindingBuilder.bind(queueD).to(yExchange).with(Constants.RABBITMQ_ROUTING_KEY_D);
    }
}
