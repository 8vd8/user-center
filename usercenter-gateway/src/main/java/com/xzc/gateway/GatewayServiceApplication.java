package com.xzc.gateway;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关服务启动类
 * @author xzc
 */
@SpringBootApplication(exclude ={
DataSourceAutoConfiguration .class,
MybatisPlusAutoConfiguration .class
})
@EnableDiscoveryClient
public class GatewayServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}