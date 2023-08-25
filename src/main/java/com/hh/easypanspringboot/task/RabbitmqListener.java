package com.hh.easypanspringboot.task;

import com.hh.easypanspringboot.service.FileInfoService;
import com.hh.easypanspringboot.utils.JWTUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class RabbitmqListener {

    @Resource
    private FileInfoService fileInfoService;

    @RabbitListener(queues = "deadQueue")
    public void receiveMsg(Message message, @Payload Map<String, List<String>> hashMap) {
        String token = message.getMessageProperties().getHeader("token").toString();
        String userId = JWTUtils.resolveToken(token);
        List<String> delFileList = hashMap.get(token);
        fileInfoService.delFileBatch(userId, String.join(",", delFileList), false);
    }
}
