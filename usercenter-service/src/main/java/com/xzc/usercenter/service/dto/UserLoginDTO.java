package com.xzc.usercenter.service.dto;

import lombok.Data;

/**
 * 用户登录请求DTO
 * @author xzc
 */
@Data
public class UserLoginDTO {
    private String username;
    private String password;
}