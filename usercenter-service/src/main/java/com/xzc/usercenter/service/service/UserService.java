package com.xzc.usercenter.service.service;


import com.baomidou.mybatisplus.extension.service.IService;

import com.xzc.usercenter.service.dto.UserLoginDTO;
import com.xzc.usercenter.service.dto.UserRegisterDTO;
import com.xzc.usercenter.service.dto.UserUpdateDTO;
import com.xzc.usercenter.service.entity.UserEntity;

import java.util.List;
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

    /**
     * 根据权限查询用户列表
     */
    List<UserEntity> queryUsersByPermission(Long currentUserId);

    /**
     * 根据权限获取用户信息
     */
    UserEntity getUserByIdWithPermission(Long id, Long currentUserId);

    /**
     * 根据权限更新用户信息
     */
    void updateUserWithPermission(UserUpdateDTO user, Long currentUserId);

    /**
     * 重置用户密码
     */
    void resetPassword(Long userId, String newPassword, Long currentUserId);
}

