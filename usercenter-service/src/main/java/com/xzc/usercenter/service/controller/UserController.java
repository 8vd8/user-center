package com.xzc.usercenter.service.controller;

import com.xzc.common.utils.R;
import com.xzc.usercenter.service.dto.UserLoginDTO;
import com.xzc.usercenter.service.dto.UserRegisterDTO;
import com.xzc.usercenter.service.dto.UserUpdateDTO;
import com.xzc.usercenter.service.entity.UserEntity;
import com.xzc.usercenter.service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


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



    /**
     * 用户注册
     * 分库分表写入用户表 → RPC调用绑定默认角色
     * todo  → 发送日志消息至MQ
     */
    @PostMapping("/register")
    public R register(@RequestBody UserRegisterDTO request) {
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
    public R login(@RequestBody UserLoginDTO request) {
        try {
            String token = userService.login(request);
            return R.loginSuccess(token);
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/info/{id}")
    public R getUserInfo(@PathVariable Long id) {
        try {
            UserEntity user = userService.getUserById(id);
            return R.userInfo(user);
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/update")
    public R updateUser(@RequestBody UserUpdateDTO user) {
        try {
            userService.updateUser(user);
            return R.updateSuccess();
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }


}
