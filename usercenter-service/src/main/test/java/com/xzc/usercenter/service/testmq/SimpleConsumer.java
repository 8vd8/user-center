package com.xzc.usercenter.service.testmq;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

public class SimpleConsumer {
    public static void main(String[] args) throws Exception {
        // 1. 创建消费者，指定消费组名
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("ConsumerGroupName");
        
        // 2. 设置 Namesrv 地址，跟生产者一样
        consumer.setNamesrvAddr("198.19.249.95:9876");
        
        // 3. 订阅主题和标签，* 表示所有标签
        consumer.subscribe("TestTopic", "*");
        
        // 4. 注册消息监听器，收到消息时回调这里
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    String body = new String(msg.getBody());
                    System.out.println("收到消息: " + body);
                }
                // 消费成功，返回状态
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        
        // 5. 启动消费者
        consumer.start();
        System.out.println("消费者启动成功，等待消息中...");
    }
}