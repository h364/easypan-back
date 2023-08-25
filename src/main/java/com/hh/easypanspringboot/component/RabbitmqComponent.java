package com.hh.easypanspringboot.component;

import com.hh.easypanspringboot.entity.constants.Constants;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RabbitmqComponent {
    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private RedisComponent redisComponent;

    public void sendDelFileList2Recycle(String exchange, String routingKey, List<String> delFileList, String token) {
        String sessionInfo = redisComponent.getUserSessionInfo(token);
        MessagePostProcessor messagePostProcessor = msg -> {
            MessageProperties messageProperties = msg.getMessageProperties();
            messageProperties.setContentEncoding("utf-8");
            messageProperties.setExpiration(String.valueOf(Constants.TEN_DAYS));
            messageProperties.setHeader("token", sessionInfo);
            return msg;
        };
        Map<String, List<String>> hashMap = new HashMap<>();
        hashMap.put(token, delFileList);
        rabbitTemplate.convertAndSend(exchange, routingKey, hashMap, messagePostProcessor);
    }
}
