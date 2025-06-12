package com.xzc.usercenter.service.service;


import com.baomidou.mybatisplus.extension.service.IService;

import com.xzc.usercenter.service.dto.UserLoginDTO;
import com.xzc.usercenter.service.dto.UserRegisterDTO;
import com.xzc.usercenter.service.dto.UserUpdateDTO;
import com.xzc.usercenter.service.entity.UserEntity;

import java.util.Map;

/**
 * 用户表
 *
 * @author fuckchao
 * @email 1936002261@qq.com
 * @date 2025-06-12 18:07:16
 */
public interface UserService extends IService<UserEntity> {

    com.xzc.common.utils.PageUtils queryPage(Map<String, Object> params);

    void register(UserRegisterDTO request);

    String login(UserLoginDTO request);

    UserEntity getUserById(Long id);


    void updateUser(UserUpdateDTO user);
}

