package com.xzc.usercenter.service.testmq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;

public class SimpleProducer {
    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("ProducerGroupName");
        producer.setNamesrvAddr("198.19.249.95:9876");
        producer.start();
        Message msg = new Message("TestTopic", "TagA", "Hello RocketMQ".getBytes());
        producer.send(msg);
        producer.shutdown();
    }
}