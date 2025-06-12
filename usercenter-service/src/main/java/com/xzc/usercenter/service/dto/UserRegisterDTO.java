package com.xzc.usercenter.service.dto;

import lombok.Data;

/**
 * 用户注册请求DTO
 * @author xzc
 */
@Data
public class UserRegisterDTO {
    private String username;
    private String password;
    private String email;
    private String phone;
    private String nickname;
    private String confirmPassword;


}