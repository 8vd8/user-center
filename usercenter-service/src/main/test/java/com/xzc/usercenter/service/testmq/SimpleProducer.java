package com.xzc.usercenter.service.testmq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;

public class SimpleProducer {
    public static void main(String[] args) throws Exception {
        // 1. 创建生产者实例，指定生产者组名
        DefaultMQProducer producer = new DefaultMQProducer("operation-log-producer-group");

        // 2. 设置 NameServer 地址
        producer.setNamesrvAddr("127.0.0.1:9876");

        // 3. 启动生产者
        producer.start();

        // 4. 创建消息（Topic, Tag, 内容）
        Message msg = new Message("operation-log-topic", "TagA", "Hello RocketMQ".getBytes());

        // 5. 发送消息
        producer.send(msg);
        // 6. 关闭生产者
        producer.shutdown();

        System.out.println("消息发送成功！");
    }
}