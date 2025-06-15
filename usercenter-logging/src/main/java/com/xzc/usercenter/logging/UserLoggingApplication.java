package com.xzc.usercenter.logging;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 用户权限启动类
 * @author fuckchao
 */
@SpringBootApplication
@EnableDiscoveryClient
//@MapperScan("com.xzc.usercenter.logging.mapper")
public class UserLoggingApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserLoggingApplication.class, args);
    }
}