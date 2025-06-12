package com.xzc.common.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体类
 * @author xzc
 */
@Data
public class OperationLog {
    private Long id;
    private Long userId;
    private String username;
    private String operation; // 操作类型
    private String method; // 请求方法
    private String params; // 请求参数
    private String ip; // IP地址
    private LocalDateTime createTime;
}