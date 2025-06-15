package com.xzc.usercenter.logging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xzc.common.entity.Detail;
import com.xzc.common.event.OperationLogEvent;
import com.xzc.usercenter.logging.entity.OperationLogsEntity;
import com.xzc.usercenter.logging.service.LogsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;

/**
 * 日志消息消费者
 * @author xzc (GitHub: 8vd8)
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "operation-log-topic", consumerGroup = "logging-service-consumer")
public class LogMessageConsumer implements RocketMQListener<String> {

    @PostConstruct
    public void init() {
        log.info("LogMessageConsumer 初始化完成");
    }
    @Autowired
    private LogsService logService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public void onMessage(String message) {
        try {
            log.info("接收到日志消息: {}", message);
            
            // 解析OperationLogEvent
            OperationLogEvent event = objectMapper.readValue(message, OperationLogEvent.class);
            
            // 转换为OperationLog实体
            OperationLogsEntity operationLog = new OperationLogsEntity();
            operationLog.setUserId(event.getUserId());
            operationLog.setAction(event.getAction());
            operationLog.setIp(event.getIp());
            Detail detail = event.getDetail();
            operationLog.setDetail(objectMapper.writeValueAsString(detail));
            
            // 保存日志
            logService.saveLog(operationLog);
            log.info("日志保存成功: userId={}, action={}", event.getUserId(), event.getAction());
        } catch (Exception e) {
            log.error("处理日志消息失败: {}", e.getMessage(), e);
        }
    }
}