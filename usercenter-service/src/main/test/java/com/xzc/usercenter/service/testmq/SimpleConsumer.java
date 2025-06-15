package com.xzc.usercenter.service.testmq;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

public class SimpleConsumer {
    public static void main(String[] args) throws Exception {
        // 1. 创建消费者，指定消费组名（不要和其他已有的重复）
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("operation-log-consumer-group");

        // 2. 设置 NameServer 地址（根据你 docker 映射的实际 IP）
        consumer.setNamesrvAddr("127.0.0.1:9876");

        // 3. 设置消费起点（可选）—— 第一次启动时从头开始消费
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);

        // 4. 订阅 Topic 和标签（* 表示接收该 Topic 下所有 tag 的消息）
        consumer.subscribe("operation-log-topic", "*");

        // 5. 注册监听器，处理收到的消息
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    String body = new String(msg.getBody());
                    System.out.println("收到消息: " + body);
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        // 6. 启动消费者
        consumer.start();
        System.out.println("消费者启动成功，等待消息中...");
    }
}