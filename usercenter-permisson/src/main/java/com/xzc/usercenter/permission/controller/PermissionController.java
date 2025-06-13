package com.xzc.usercenter.permission.controller;


import com.xzc.usercenter.permission.service.UserRoleService;
import com.xzc.common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 权限控制器
 * @author xzc (GitHub: 8vd8)
 */
@RestController
@RequestMapping("/permission")
public class PermissionController {

    @Autowired
    private UserRoleService userRoleService;


    /**
     * 绑定默认角色（普通用户）
     */
    @PostMapping("/bind-default-role")
    public R bindDefaultRole(@RequestParam("userId") Long userId) {
        try {
            userRoleService.bindDefaultRole(userId);
            return R.ok("默认角色绑定成功");
        } catch (Exception e) {
            return R.error("默认角色绑定失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户角色码
     */
    @GetMapping("/user-role-code")
    public String getUserRoleCode(@RequestParam("userId") Long userId) {
        return userRoleService.getUserRoleCode(userId);
    }

    /**
     * 升级用户为管理员
     */
    @PostMapping("/upgrade-to-admin")
    public void upgradeToAdmin(@RequestParam("userId") Long userId) {
        userRoleService.upgradeToAdmin(userId);
    }

    /**
     * 降级用户为普通角色
     */
    @PostMapping("/downgrade-to-user")
    public void downgradeToUser(@RequestParam("userId") Long userId) {
        userRoleService.downgradeToUser(userId);
    }
}