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
 * @author xzc
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
     * @param currentUserId 当前用户ID
     * @return 用户列表
     */
    List<UserEntity> queryUsersByPermission(Long currentUserId);

    /**
     * 根据权限获取用户信息
     * @param id 用户ID
     * @param currentUserId 当前用户ID
     * @return 用户信息
     */
    UserEntity getUserByIdWithPermission(Long id, Long currentUserId);

    /**
     * 根据权限更新用户信息
     * @param user 用户更新信息
     * @param currentUserId 当前用户ID
     */
    void updateUserWithPermission(UserUpdateDTO user, Long currentUserId);

    /**
     * 重置密码
     * @param userId 用户ID
     * @param newPassword 新密码
     * @param currentUserId 当前用户ID
     */
    void resetPassword(Long userId, String newPassword, Long currentUserId);
    
    /**
     * 根据角色ID查询用户列表
     * @param currentUserId 当前用户ID
     * @param currentRoleId 当前用户角色ID
     * @return 用户列表
     */
    List<UserEntity> queryUsersByRoleId(Long currentUserId, Long currentRoleId);

    /**
     * 根据角色ID获取用户信息
     * @param id 用户ID
     * @param currentUserId 当前用户ID
     * @param currentRoleId 当前用户角色ID
     * @return 用户信息
     */
    UserEntity getUserByIdWithRoleId(Long id, Long currentUserId, Long currentRoleId);

    /**
     * 根据角色ID更新用户信息
     * @param user 用户更新信息
     * @param currentUserId 当前用户ID
     * @param currentRoleId 当前用户角色ID
     */
    void updateUserWithRoleId(UserUpdateDTO user, Long currentUserId, Long currentRoleId);

    /**
     * 重置密码
     * @param userId 用户ID
     * @param newPassword 新密码
     * @param currentUserId 当前用户ID
     * @param currentRoleId 当前用户角色ID
     */
    void resetPasswordWithRoleId(Long userId, String newPassword, Long currentUserId, Long currentRoleId);
}

