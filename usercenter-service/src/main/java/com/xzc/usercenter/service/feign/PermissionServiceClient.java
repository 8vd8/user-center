package com.xzc.usercenter.service.feign;

import com.xzc.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 权限服务Feign客户端
 * @author xzc
 */
@FeignClient(name = "usercenter-permission")
public interface PermissionServiceClient {
    
    /**
     * 为用户绑定默认角色
     */
    @PostMapping("/permission/bind-default-role")
    R<String> bindDefaultRole(@RequestParam("userId") Long userId);
    
    /**
     * 检查用户权限
     */
    @PostMapping("/permission/check")
    R<Boolean> checkPermission(@RequestParam("userId") Long userId,
                                   @RequestParam("permission") String permission);

    /**
     * 获取用户角色ID
     */
    @GetMapping("/permission/user-role-id")
    R<Integer> getUserRoleId(@RequestParam("userId") Long userId);

    /**
     * 根据角色ID获取用户ID列表
     */
    @GetMapping("/permission/users-by-role")
    R<List<Long>> getUserIdsByRoleId(@RequestParam("roleId") Integer roleId);
    
    /**
     * 升级用户为管理员
     */
    @PostMapping("/permission/upgrade-to-admin")
    R<String> upgradeToAdmin(@RequestParam("userId") Long userId);
    
    /**
     * 降级用户为普通用户
     */
    @PostMapping("/permission/downgrade-to-user")
    R<String> downgradeToUser(@RequestParam("userId") Long userId);
}