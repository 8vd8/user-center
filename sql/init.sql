CREATE DATABASE IF NOT EXISTS user_center DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE user_center;



-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
                                      `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                                      `username` varchar(50) NOT NULL COMMENT '用户名',
                                      `password` varchar(255) NOT NULL COMMENT '密码',
                                      `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
                                      `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
                                      `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
                                      `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
                                      `status` tinyint(1) DEFAULT '1' COMMENT '状态：1-正常，0-禁用',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `uk_username` (`username`),
                                      UNIQUE KEY `uk_email` (`email`),
                                      KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 权限表
CREATE TABLE IF NOT EXISTS `permission` (
                                            `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '权限ID',
                                            `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                            `role` varchar(50) NOT NULL DEFAULT 'user' COMMENT '角色：user-普通用户，admin-管理员，super_admin-超级管理员',
                                            `permissions` text COMMENT '权限详情JSON',
                                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            PRIMARY KEY (`id`),
                                            UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS `operation_logs` (
                                               `log_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
                                               `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
                                               `username` varchar(50) DEFAULT NULL COMMENT '用户名',
                                               `action` varchar(100) NOT NULL COMMENT '操作类型',
                                               `method` varchar(10) DEFAULT NULL COMMENT '请求方法',
                                               `params` text COMMENT '请求参数',
                                               `ip` varchar(50) DEFAULT NULL COMMENT 'IP地址',
                                               `location` varchar(100) DEFAULT NULL COMMENT '操作地点',
                                               `detail` text COMMENT '操作详情',
                                               `msg_id` varchar(128) NOT NULL COMMENT 'RocketMQ消息唯一标识',
                                               `operation_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
                                               `service_source` varchar(50) DEFAULT NULL COMMENT '服务来源',
                                               `trace_id` varchar(64) DEFAULT NULL COMMENT '链路追踪ID',
                                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                               `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                               PRIMARY KEY (`log_id`),
                                               UNIQUE KEY `uk_msg_id` (`msg_id`),
                                               KEY `idx_user_id` (`user_id`),
                                               KEY `idx_action` (`action`),
                                               KEY `idx_operation_time` (`operation_time`),
                                               KEY `idx_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';