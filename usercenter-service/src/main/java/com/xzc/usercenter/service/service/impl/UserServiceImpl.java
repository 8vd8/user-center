package com.xzc.usercenter.service.service.impl;


import com.alibaba.nacos.client.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzc.common.utils.PageUtils;
import com.xzc.common.utils.Query;
import com.xzc.common.utils.R;
import com.xzc.usercenter.service.dao.UserDao;
import com.xzc.usercenter.service.dto.UserLoginDTO;
import com.xzc.usercenter.service.dto.UserRegisterDTO;
import com.xzc.usercenter.service.dto.UserUpdateDTO;
import com.xzc.usercenter.service.entity.UserEntity;
import com.xzc.usercenter.service.service.UserService;
import com.xzc.usercenter.service.feign.PermissionServiceClient;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserDao, UserEntity> implements UserService {


    @Autowired
    private PermissionServiceClient permissionServiceClient;
    
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    private String salt = "FUCK";
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<UserEntity> page;
        page = this.page(
                new Query<UserEntity>().getPage(params),
                new QueryWrapper<UserEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void register(UserRegisterDTO request) {
        String username = request.getUsername();
        String password = request.getPassword();

        // 校验：用户名不能为空
        if (StringUtils.isBlank(username)) {
            throw new RuntimeException("用户名不能为空");
        }

        // 校验：用户名格式（字母、数字、下划线）和长度
        if (!username.matches("^[a-zA-Z0-9_]{4,10}$")) {
            throw new RuntimeException("用户名格式非法，应为4-10位字母、数字或下划线");
        }

        // 校验：密码长度
        if (password.length() < 6 || password.length() > 18) {
            throw new RuntimeException("密码长度必须在6~18位之间");
        }

        // 校验：密码不能包含特殊字符（只允许字母和数字）
        if (!password.matches("^[a-zA-Z0-9]+$")) {
            throw new RuntimeException("密码不能包含特殊字符，只能是字母和数字");
        }

        String confirmPassword = request.getConfirmPassword();
        // 校验：确认密码一致
        if (!password.equals(confirmPassword) ){
            throw new RuntimeException("两次密码输入不一致");
        }

        // 检查用户名是否已存在
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        UserEntity existUser = this.getOne(queryWrapper);
        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建新用户 -> 防止并发
        UserEntity user = new UserEntity();
        BeanUtils.copyProperties(request, user);
        // 使用加盐的密码
        user.setPassword(DigestUtils.md5DigestAsHex((password + salt).getBytes()));
        user.setStatus(1);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());

        this.save(user);


        // RPC调用权限服务绑定默认角色
        try {
            permissionServiceClient.bindDefaultRole(user.getId());
        } catch (Exception e) {
            // 记录日志但不影响用户注册流程
            System.err.println("绑定默认角色失败: " + e.getMessage());
        }

        // 发送操作日志到MQ
        try {
            String clientIp = getClientIp();
            String logData = String.format("{\"username\":\"%s\",\"email\":\"%s\"}", 
                user.getUsername(), user.getEmail());
            
            // 构建日志消息
            String logMessage = String.format(
                "{\"userId\":%d,\"operation\":\"REGISTER\",\"ip\":\"%s\",\"data\":\"%s\",\"timestamp\":\"%s\"}",
                user.getId(), clientIp, logData, new Date().toString()
            );
            
            rocketMQTemplate.convertAndSend("operation-log-topic", logMessage);
        } catch (Exception e) {
            // 记录日志但不影响用户注册流程
            System.err.println("发送MQ消息失败: " + e.getMessage());
        }
    }

    @Override
    public String login(UserLoginDTO request) {
        // 查询用户
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", request.getUsername());
        UserEntity user = this.getOne(queryWrapper);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证密码
        String encryptedPassword = DigestUtils.md5DigestAsHex((request.getPassword()+salt).getBytes());
        if (!user.getPassword().equals(encryptedPassword)) {
            throw new RuntimeException("密码错误");
        }

        if (user.getStatus() == 0) {
            throw new RuntimeException("用户已被禁用");
        }

        //todo: 生成JWT token
        return R.ok().put("user", user).toString();
    }

    @Override
    public UserEntity getUserById(Long id) {
        try {
            UserEntity user = this.getById(id);
            return user;
        } catch (NullPointerException e) {
            throw new RuntimeException("没有此id账户");
        }
    }

    @Override
    public void updateUser(UserUpdateDTO userUpdateDTO) {
        // 校验：用户ID不能为空
        if (userUpdateDTO.getId() == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        // 查询用户是否存在
        UserEntity existUser = this.getById(userUpdateDTO.getId());
        if (existUser == null) {
            throw new RuntimeException("用户不存在");
        }

        // 校验用户名格式（如果要更新用户名）
        if (!StringUtils.isBlank(userUpdateDTO.getUsername())) {
            if (!userUpdateDTO.getUsername().matches("^[a-zA-Z0-9_]{4,10}$")) {
                throw new RuntimeException("用户名格式非法，应为4-10位字母、数字或下划线");
            }
            
            // 检查用户名是否已被其他用户使用
            QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("username", userUpdateDTO.getUsername())
                       .ne("id", userUpdateDTO.getId());
            UserEntity duplicateUser = this.getOne(queryWrapper);
            if (duplicateUser != null) {
                throw new RuntimeException("用户名已存在");
            }
        }

        // 校验密码格式（如果要更新密码）
        if (!StringUtils.isBlank(userUpdateDTO.getPassword())) {
            if (userUpdateDTO.getPassword().length() < 6 || userUpdateDTO.getPassword().length() > 18) {
                throw new RuntimeException("密码长度必须在6~18位之间");
            }
            if (!userUpdateDTO.getPassword().matches("^[a-zA-Z0-9]+$")) {
                throw new RuntimeException("密码不能包含特殊字符，只能是字母和数字");
            }
        }

        // 创建更新实体
        UserEntity updateUser = new UserEntity();
        updateUser.setId(userUpdateDTO.getId());
        
        // 只更新非空字段
        if (!StringUtils.isBlank(userUpdateDTO.getUsername())) {
            updateUser.setUsername(userUpdateDTO.getUsername());
        }
        if (!StringUtils.isBlank(userUpdateDTO.getPassword())) {
            // 使用加盐的密码
            updateUser.setPassword(DigestUtils.md5DigestAsHex((userUpdateDTO.getPassword() + salt).getBytes()));
        }
        if (!StringUtils.isBlank(userUpdateDTO.getEmail())) {
            updateUser.setEmail(userUpdateDTO.getEmail());
        }
        if (!StringUtils.isBlank(userUpdateDTO.getPhone())) {
            updateUser.setPhone(userUpdateDTO.getPhone());
        }
        if (!StringUtils.isBlank(userUpdateDTO.getNickname())) {
            updateUser.setNickname(userUpdateDTO.getNickname());
        }
        
        // 设置更新时间
        updateUser.setUpdateTime(new Date());
        
        // 执行更新
        this.updateById(updateUser);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
                    return xForwardedFor.split(",")[0];
                }
                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
                    return xRealIp;
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            System.err.println("获取客户端IP失败: " + e.getMessage());
        }
        return "unknown";
    }

}