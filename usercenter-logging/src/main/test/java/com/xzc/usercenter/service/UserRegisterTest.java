package com.xzc.usercenter.service;

import com.xzc.common.utils.R;
import com.xzc.usercenter.service.controller.UserController;
import com.xzc.usercenter.service.dto.UserRegisterDTO;;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
public class UserRegisterTest {

    @Autowired
    private UserController userController;

    @Test
    public void testRegisterSuccess() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("user" + System.currentTimeMillis()); // 避免重复
        dto.setPassword("abc123");
        dto.setConfirmPassword("abc123");

        R result = userController.register(dto);
        System.out.println(result);

        assert result.getCode() == 0;
        assert "注册成功".equals(result.getMsg());
    }

    @Test
    public void testRegister2() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("admin"); // 假设数据库里已有该用户名
        dto.setPassword("abc123");
        dto.setConfirmPassword("abc123");

        R result = userController.register(dto);
        System.out.println(result);

        assert result.getCode() == 500;
        assert result.getMsg().contains("已存在");
    }
}