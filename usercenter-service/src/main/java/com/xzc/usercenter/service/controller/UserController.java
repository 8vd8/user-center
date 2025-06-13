package com.xzc.usercenter.service.controller;

import com.xzc.common.utils.PageUtils;
import com.xzc.common.utils.R;
import com.xzc.usercenter.service.dto.PasswordResetDTO;
import com.xzc.usercenter.service.dto.UserLoginDTO;
import com.xzc.usercenter.service.dto.UserRegisterDTO;
import com.xzc.usercenter.service.dto.UserUpdateDTO;
import com.xzc.usercenter.service.entity.UserEntity;
import com.xzc.usercenter.service.service.UserService;
import com.xzc.usercenter.service.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 用户表
 *
 * @author fuckchao
 * @email 1936002261@qq.com
 * @date 2025-06-12 18:07:16
 */
@RestController
@RequestMapping("service/user")
public class UserController {
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 从JWT令牌中获取当前登录用户ID
     * @param request HTTP请求
     * @return 当前登录用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        // 优先从拦截器设置的请求属性中获取（性能更好）
        Long userId = (Long) request.getAttribute("currentUserId");
        if (userId != null) {
            return userId;
        }
        // 兜底方案：直接解析JWT令牌
        return jwtUtil.getCurrentUserIdFromRequest(request);
    }



    /**
     * 用户注册
     * 分库分表写入用户表 → RPC调用绑定默认角色→ 发送日志消息至MQ
     */
    @PostMapping("/register")
    public R<String> register(@RequestBody UserRegisterDTO request) {
        try {
            userService.register(request);
            return R.registerSuccess();
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public R<String> login(@RequestBody UserLoginDTO request) {
        try {
            String token = userService.login(request);
            return R.loginSuccess(token);
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

//
//    /**
//     * 更新用户信息
//     */
//    @PutMapping("/update")
//    public R<String> updateUser(@Valid @RequestBody UserUpdateDTO user) {
//        try {
//            userService.updateUser(user);
//            return R.ok("用户信息更新成功");
//        } catch (Exception e) {
//            return R.error("用户信息更新失败: " + e.getMessage());
//        }
//    }

    /**
     * 查询用户列表
     * 普通用户仅自己
     * 管理员所有普通用户
     * 超管全部
     */
    @GetMapping("/users")
    public R<List<UserEntity>> getUserList(HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            List<UserEntity> users = userService.queryUsersByPermission(currentUserId);
            return R.ok(users);
        } catch (Exception e) {
            return R.error("获取用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 查询特定用户信息
     * 普通用户仅能查询自己，管理员能查询普通用户，超管能查询所有人
     */
    @GetMapping("/user/{targetUserId}")
    public R<UserEntity> getUserInfoWithPermission(@PathVariable Long targetUserId,
                                                   HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            UserEntity user = userService.getUserByIdWithPermission(targetUserId, currentUserId);
            return R.ok(user);
        } catch (Exception e) {
            return R.error("获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 修改用户信息 - 根据权限限制
     * 普通用户改自己，管理员改普通用户，超管改所有
     */
    @PutMapping("/user/{uerId}")
    public R<String> updateUserWithPermission(@PathVariable Long userId,
                                    @Valid @RequestBody UserUpdateDTO user,
                                    HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            user.setId(userId); // 确保更新的是路径参数中的用户ID
            userService.updateUserWithPermission(user, currentUserId);
            return R.ok("用户信息更新成功");
        } catch (Exception e) {
            return R.error("用户信息更新失败: " + e.getMessage());
        }
    }

    /**
     * 密码重置
     * 普通用户重置自己，管理员重置普通用户，超管重置所有人
     */
    @PostMapping("/reset-password")
    public R<String> resetPassword(@Valid @RequestBody PasswordResetDTO passwordResetDTO,
                                   HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            userService.resetPassword(passwordResetDTO.getUserId(), 
                                    passwordResetDTO.getNewPassword(), 
                                    currentUserId);
            return R.ok("密码重置成功");
        } catch (Exception e) {
            return R.error("密码重置失败: " + e.getMessage());
        }
    }


}
