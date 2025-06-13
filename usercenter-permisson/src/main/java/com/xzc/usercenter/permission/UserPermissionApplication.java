package com.xzc.usercenter.permission;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 用户权限启动类
 * @author fuckchao
 */
@SpringBootApplication
@EnableDiscoveryClient
public class UserPermissionApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserPermissionApplication.class, args);
    }
}