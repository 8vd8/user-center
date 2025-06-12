# 用户权限管理系统

## 项目概述
**项目名称**：用户中心 

**架构类型**：微服务架构

## 核心服务

### 1. 用户服务 (user-service)
- **核心职责**：
  - 用户注册/登录（JWT鉴权）
  - 用户数据管理（ShardingSphere分库分表）
  - 权限服务调用（RPC）
  - 操作日志生产（MQ）

### 2. 权限服务 (permission-service)
- **核心职责**：
  - 用户角色管理（user/admin/super_admin）
  - 提供RPC接口服务
- **接口规范**：
  - 服务类型：纯RPC服务
  - 调用方式：通过OpenFeign调用

### 3. 日志服务 (logging-service)
- **核心职责**：
  - 消费MQ消息（RocketMQ）
  - 异步日志落库
- **接口规范**：
  - 服务类型：MQ消费者
  - 无HTTP接口

## 基础设施配置

- **注册中心**：Nacos  
- **配置中心**：Nacos  
- **服务通信**：OpenFeign  
- **日志消费**：RocketMQ  
- **路由网关**：Spring Cloud Gateway

## 开发者
**开发者签名**：xzc（GitHub: [8vd8](https://github.com/8vd8)）