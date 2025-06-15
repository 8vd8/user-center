package com.xzc.usercenter.service.dto;

import lombok.Data;

/**
 * 用户注册请求DTO
 * @author xzc
 */
@Data
public class UserUpdateDTO {
    //用于填更新请求的id，不能进行修改
    private Long id;
    private String username;
    private String password;
    private String email;
    private String phone;
    private String nickname;
    private Long updateRoleId;


}