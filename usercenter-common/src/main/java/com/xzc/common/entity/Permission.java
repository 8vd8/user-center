package com.xzc.common.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 权限实体类
 * @author xzc
 */
@Data
public class Permission {
    private Long id;
    private Long userId;
    private String role; // user/admin/super_admin
    private String permissions; // JSON格式存储具体权限
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}