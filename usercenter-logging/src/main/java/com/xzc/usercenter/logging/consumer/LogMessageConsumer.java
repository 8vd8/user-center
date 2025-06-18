package com.xzc.usercenter.logging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xzc.common.entity.Detail;
import com.xzc.common.event.OperationLogEvent;
import com.xzc.usercenter.logging.entity.OperationLogsEntity;
import com.xzc.usercenter.logging.service.LogsService;
import io.seata.spring.annotation.GlobalTransactional;
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
    @GlobalTransactional(name = "logging-consume-message", rollbackFor = Exception.class)
    public void onMessage(String message) {
        try {
            log.info("接收到日志消息: {}", message);
            
            // 解析OperationLogEvent
            OperationLogEvent event = objectMapper.readValue(message, OperationLogEvent.class);
            
            // 幂等性检查：先查询是否已经处理过该消息
            if (event.getMsgId() != null && logService.existsByMsgId(event.getMsgId())) {
                log.info("消息已处理过，跳过: msgId={}", event.getMsgId());
                return;
            }
            
            // 转换为OperationLog实体
            OperationLogsEntity operationLog = new OperationLogsEntity();
            operationLog.setUserId(event.getUserId());
            operationLog.setUsername(event.getUsername());
            operationLog.setAction(event.getAction());
            operationLog.setMethod(event.getMethod());
            operationLog.setParams(event.getParams());
            operationLog.setIp(event.getIp());
            operationLog.setLocation(event.getLocation());
            operationLog.setMsgId(event.getMsgId());
            operationLog.setOperationTime(event.getOperationTime());
            operationLog.setServiceSource(event.getServiceSource());
            operationLog.setTraceId(event.getTraceId());
            
            Detail detail = event.getDetail();
            if (detail != null) {
                operationLog.setDetail(objectMapper.writeValueAsString(detail));
            }
            
            // 保存日志
            logService.saveLog(operationLog);
            log.info("日志保存成功: userId={}, action={}, msgId={}, traceId={}", 
                    event.getUserId(), event.getAction(), event.getMsgId(), event.getTraceId());
        } catch (Exception e) {
            log.error("处理日志消息失败: {}", e.getMessage(), e);
            // 注意：RocketMQ消息监听器中不能抛出检查异常
        }
    }
}