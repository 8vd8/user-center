package com.xzc.usercenter.service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import com.alibaba.nacos.client.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzc.common.exception.BusinessException;
import com.xzc.common.exception.ForbiddenException;
import com.xzc.common.exception.NotFoundException;
import com.xzc.common.exception.UnauthorizedException;
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
import com.xzc.usercenter.service.cache.PermissionCacheService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import io.seata.spring.annotation.GlobalTransactional;

import javax.servlet.http.HttpServletRequest;
import com.xzc.usercenter.service.utils.JwtUtil;
import java.util.*;

@Slf4j
@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserDao, UserEntity> implements UserService {


    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PermissionServiceClient permissionServiceClient;
    
    @Autowired
    private PermissionCacheService permissionCacheService;
    
    @Autowired
private RocketMQTemplate rocketMQTemplate;

@Autowired
private com.xzc.usercenter.service.service.ReliableMessageService reliableMessageService;

    @Autowired
    private JwtUtil jwtUtil;

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
    @GlobalTransactional(name = "user-register", rollbackFor = Exception.class)
    public void register(UserRegisterDTO request) {
        String username = request.getUsername();
        String password = request.getPassword();

        // 校验：用户名不能为空
        if (StringUtils.isBlank(username)) {
            throw new BusinessException("用户名不能为空");
        }

        // 校验：用户名格式（字母、数字、下划线）和长度
        if (!username.matches("^[a-zA-Z0-9_]{4,10}$")) {
            throw new BusinessException("用户名格式非法，应为4-10位字母、数字或下划线");
        }

        // 校验：密码长度
        if (password.length() < 6 || password.length() > 18) {
            throw new BusinessException("密码长度必须在6~18位之间");
        }

        // 校验：密码不能包含特殊字符（只允许字母和数字）
        if (!password.matches("^[a-zA-Z0-9]+$")) {
            throw new BusinessException("密码不能包含特殊字符，只能是字母和数字");
        }

        String confirmPassword = request.getConfirmPassword();
        // 校验：确认密码一致
        if (!password.equals(confirmPassword) ){
            throw new BusinessException("两次密码输入不一致");
        }

        // 检查用户名是否已存在
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        UserEntity existUser = this.getOne(queryWrapper);
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        // 创建新用户 -> 防止并发
        UserEntity user = new UserEntity();
        BeanUtils.copyProperties(request, user);
        // 使用加盐的密码
        user.setPassword(DigestUtils.md5Hex((password + salt).getBytes()));
        user.setStatus(1);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        user.setAvatar("default website");
        user.setNickname("user"+username);

        this.save(user);


        // RPC调用权限服务绑定默认角色
        try {
            permissionServiceClient.bindDefaultRole(user.getId());
        } catch (Exception e) {
            // 记录日志但不影响用户注册流程
           log.error("绑定默认角色失败: " + e.getMessage());
           throw new BusinessException("绑定默认角色失败");
        }

        // 发送操作日志到MQ（使用可靠消息服务）
        try {
            Map<String, Object> detail = new HashMap<>();

            detail.put("username", username);
            String email = user.getEmail();
            if(email== null||email.equals("")){
                email = "test@qq.com";
            }
            detail.put("email", email);

            Map<String, Object> message = new HashMap<>();
            message.put("userId", user.getId());
            message.put("action", "REGISTER");
            message.put("ip", getClientIp());
            message.put("detail", detail);

            String json = objectMapper.writeValueAsString(message);
            // 使用可靠消息服务发送操作日志到MQ
            reliableMessageService.sendMessage("operation-log-topic", json);

            log.info("操作日志发送成功: " + json);
        } catch (Exception e) {
            // 记录日志但不影响用户注册流程
            log.error("发送MQ消息失败: " + e.getMessage(), e);
            throw new BusinessException("发送MQ消息失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String login(UserLoginDTO request) {
        // 查询用户
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", request.getUsername());
        UserEntity user = this.getOne(queryWrapper);

        if (user == null) {
            throw new NotFoundException("用户不存在");
        }

        // 验证密码
        String encryptedPassword = DigestUtils.md5Hex((request.getPassword()+salt).getBytes());
        if (!user.getPassword().equals(encryptedPassword)) {
            throw new BusinessException("密码错误");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException("用户已被禁用");
        }

        // 获取用户角色权限码和角色ID（使用缓存）
        String roleCode = permissionCacheService.getUserRoleCode(user.getId());
        Integer roleId = permissionCacheService.getUserRoleId(user.getId());
        if (roleCode == null || roleId == null) {
            log.warn("获取用户角色权限码或角色ID失败，使用默认权限: userId={}", user.getId());
            roleCode = "USER"; // 默认权限
            roleId = 2; // 默认为普通用户
        }
        
        // 生成包含权限码和角色ID的JWT token
        String token = jwtUtil.generateTokenWithRoleAndId(user.getId(), user.getUsername(), roleCode, roleId);
        return "Bearer " + token;
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
            throw new BusinessException("用户ID不能为空");
        }

        // 查询用户是否存在
        UserEntity existUser = this.getById(userUpdateDTO.getId());
        if (existUser == null) {
            throw new NotFoundException("用户不存在");
        }

        // 校验用户名格式（如果要更新用户名）
        if (!StringUtils.isBlank(userUpdateDTO.getUsername())) {
            if (!userUpdateDTO.getUsername().matches("^[a-zA-Z0-9_]{4,10}$")) {
                throw new BusinessException("用户名格式非法，应为4-10位字母、数字或下划线");
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
            updateUser.setPassword(DigestUtils.md5Hex((userUpdateDTO.getPassword() + salt).getBytes()));
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

    @Override
    public List<UserEntity> queryUsersByPermission(Long currentUserId) {
        try {
            // 获取当前用户角色ID
            R<Integer> roleResult = permissionServiceClient.getUserRoleId(currentUserId);
            if (roleResult.getCode() ==  R.ERROR_CODE) {
                throw new RuntimeException("获取用户权限失败");
            }
            Integer roleId = roleResult.getData();
            
            if (roleId == 1) {
                // 超管：可以查看所有用户
                return this.list();
            } else if (roleId == 3) {
                // 管理员：只能查看普通用户
                //这里是远程调用，直接返回R，后期好维护
                R<List<Long>> userIdsResult = permissionServiceClient.getUserIdsByRoleId(2);
                if (userIdsResult.getCode() != 0) {
                    throw new RuntimeException("获取普通用户列表失败");
                }
                List<Long> userIds = userIdsResult.getData();
                if (userIds.isEmpty()) {
                    return new ArrayList<>();
                }
                return new ArrayList<>(this.listByIds(userIds));
            } else if (roleId == 2) {
                // 普通用户：只能看自己
                UserEntity user = this.getById(currentUserId);
                return user != null ? Arrays.asList(user) : new ArrayList<>();
            } else {
                throw new ForbiddenException("无效的角色权限");
            }
        } catch (Exception e) {
            throw new RuntimeException("查询用户列表失败: " + e.getMessage());
        }
    }

    @Override
    public UserEntity getUserByIdWithPermission(Long id, Long currentUserId) {
        try {
            // 获取当前用户角色ID
            R<Integer> roleResult = permissionServiceClient.getUserRoleId(currentUserId);
            if (roleResult.getCode() ==R.ERROR_CODE) {
                throw new RuntimeException("获取用户权限失败");
            }
            Integer roleId = roleResult.getData();
            
            // 获取目标用户信息
            UserEntity targetUser = this.getById(id);
            if (targetUser == null) {
                throw new RuntimeException("用户不存在");
            }
            
            // 权限校验
            if (roleId == 1) {
                // 超管：可以查看所有用户
                return targetUser;
            } else if (roleId == 3) {
                // 管理员：只能查看普通用户
                R<Integer> targetRoleResult = permissionServiceClient.getUserRoleId(id);
                if (targetRoleResult.getCode() == R.ERROR_CODE) {
                    throw new ForbiddenException("目标用户没有权限");
                }
                Integer targetRoleId = targetRoleResult.getData();
                if (targetRoleId != 2) {
                    throw new ForbiddenException("权限不足，无法查看该用户信息");
                }
                return targetUser;
            } else if (roleId == 2) {
                // 普通用户：只能查看自己
                if (!currentUserId.equals(id)) {
                    throw new ForbiddenException("权限不足，只能查看自己的信息");
                }
                return targetUser;
            } else {
                throw new RuntimeException("无效的角色权限");
            }
        } catch (Exception e) {
            throw new BusinessException("获取用户信息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateUserWithPermission(UserUpdateDTO updateUser, Long currentUserId) {
        try {
            // 获取当前用户角色ID-》等级
            R<Integer> roleResult = permissionServiceClient.getUserRoleId(currentUserId);
            if (roleResult.getCode() == R.ERROR_CODE) {
                throw new RuntimeException("获取用户权限失败");
            }
            Integer roleId = roleResult.getData();
            //目标用户的id
            Long targetUserId = updateUser.getId();
            Long updateRoleId = updateUser.getUpdateRoleId();
            if(updateRoleId == 1){
                throw new RuntimeException("超级管理员是唯一的，无法升级");
            }

            R<Integer> userRoleR = permissionServiceClient.getUserRoleId(targetUserId);
            if(userRoleR.getCode() == R.ERROR_CODE){
                throw new RuntimeException("更新的用户没有权限");
            }
           if (roleId == 3) {
                // 管理员：只能修改普通用户
                R<Integer> targetRoleResult = permissionServiceClient.getUserRoleId(targetUserId);
                if (targetRoleResult.getCode() == R.ERROR_CODE) {
                    throw new RuntimeException("获取目标用户权限失败");
                }
                Integer targetRoleId = targetRoleResult.getData();
                if (targetRoleId != 2) {
                    throw new RuntimeException("权限不足，只能修改普通用户信息");
                }
            } else if (roleId == 2) {
                // 普通用户：只能修改自己
                if (!currentUserId.equals(targetUserId)) {
                    throw new RuntimeException("权限不足，只能修改自己的信息");
                }
            } else if (roleId == 1) {
                // 超级管理员：可以升级/降级用户角色
                if (updateRoleId != null) {
                    Integer currentRoleId = userRoleR.getData();
                    // 如果当前角色和目标角色不同，则进行升级或降级
                    if (!currentRoleId.equals(updateRoleId.intValue())) {
                        if (updateRoleId == 3) {
                            // 升级为管理员
                            permissionServiceClient.upgradeToAdmin(targetUserId);
                            // 刷新用户权限缓存
                            permissionCacheService.refreshUserRoleCode(targetUserId);
                            log.info("用户 {} 已被升级为管理员，并刷新权限缓存", targetUserId);
                        } else if (updateRoleId == 2) {
                            // 降级为普通用户
                            permissionServiceClient.downgradeToUser(targetUserId);
                            // 刷新用户权限缓存
                            permissionCacheService.refreshUserRoleCode(targetUserId);
                            log.info("用户 {} 已被降级为普通用户，并刷新权限缓存", targetUserId);
                        }
                    }
                }
            } else {
                throw new RuntimeException("无效的角色权限");
            }
            
            // 执行更新
            this.updateUser(updateUser);
        } catch (Exception e) {
            throw new RuntimeException("更新用户信息失败: " + e.getMessage());
        }
    }

    @Override
    public void resetPassword(Long userId, String newPassword, Long currentUserId) {
        try {
            // 获取当前用户角色ID
            R<Integer> roleResult = permissionServiceClient.getUserRoleId(currentUserId);
            if (roleResult.getCode() != 0) {
                throw new RuntimeException("获取用户权限失败");
            }
            Integer roleId = roleResult.getData();
            
            // 权限校验
            if (roleId == 1) {
                // 超管：可以重置所有用户密码
            } else if (roleId == 3) {
                // 管理员：只能重置普通用户密码
                R<Integer> targetRoleResult = permissionServiceClient.getUserRoleId(userId);
                if (targetRoleResult.getCode() != 0) {
                    throw new RuntimeException("获取目标用户权限失败");
                }
                Integer targetRoleId = targetRoleResult.getData();
                if (targetRoleId != 2) {
                    throw new RuntimeException("权限不足，只能重置普通用户密码");
                }
            } else if (roleId == 2) {
                // 普通用户：只能重置自己的密码
                if (!currentUserId.equals(userId)) {
                    throw new RuntimeException("权限不足，只能重置自己的密码");
                }
            } else {
                throw new RuntimeException("无效的角色权限");
            }
            
            // 获取用户信息
            UserEntity user = this.getById(userId);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            
            // 加密新密码
            String encryptedPassword = DigestUtils.sha256Hex(newPassword + salt);
            user.setPassword(encryptedPassword);
            
            // 更新密码
            this.updateById(user);
        } catch (Exception e) {
            throw new RuntimeException("重置密码失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<UserEntity> queryUsersByRoleId(Long currentUserId, Long currentRoleId) {
        try {
            Integer roleId = currentRoleId.intValue();
            
            if (roleId == 1) {
                // 超管：可以查看所有用户
                return this.list();
            } else if (roleId == 3) {
                // 管理员：只能查看普通用户
                //这里是远程调用，直接返回R，后期好维护
                R<List<Long>> userIdsResult = permissionServiceClient.getUserIdsByRoleId(2);
                if (userIdsResult.getCode() != 0) {
                    throw new RuntimeException("获取普通用户列表失败");
                }
                List<Long> userIds = userIdsResult.getData();
                if (userIds.isEmpty()) {
                    return new ArrayList<>();
                }
                return new ArrayList<>(this.listByIds(userIds));
            } else if (roleId == 2) {
                // 普通用户：只能看自己
                UserEntity user = this.getById(currentUserId);
                return user != null ? Arrays.asList(user) : new ArrayList<>();
            } else {
                throw new RuntimeException("无效的角色权限");
            }
        } catch (Exception e) {
            throw new RuntimeException("查询用户列表失败: " + e.getMessage());
        }
    }

    @Override
    public UserEntity getUserByIdWithRoleId(Long id, Long currentUserId, Long currentRoleId) {
        try {
            Integer roleId = currentRoleId.intValue();
            
            // 获取目标用户信息
            UserEntity targetUser = this.getById(id);
            if (targetUser == null) {
                throw new RuntimeException("用户不存在");
            }
            
            // 权限校验
            if (roleId == 1) {
                // 超管：可以查看所有用户
                return targetUser;
            } else if (roleId == 3) {
                // 管理员：只能查看普通用户
                R<Integer> targetRoleResult = permissionServiceClient.getUserRoleId(id);
                if (targetRoleResult.getCode() == R.ERROR_CODE) {
                    throw new RuntimeException("目标用户没有权限");
                }
                Integer targetRoleId = targetRoleResult.getData();
                if (targetRoleId != 2) {
                    throw new RuntimeException("权限不足，无法查看该用户信息");
                }
                return targetUser;
            } else if (roleId == 2) {
                // 普通用户：只能查看自己
                if (!currentUserId.equals(id)) {
                    throw new RuntimeException("权限不足，只能查看自己的信息");
                }
                return targetUser;
            } else {
                throw new RuntimeException("无效的角色权限");
            }
        } catch (Exception e) {
            throw new RuntimeException("获取用户信息失败: " + e.getMessage());
        }
    }

    @Override
    public void updateUserWithRoleId(UserUpdateDTO updateUser, Long currentUserId, Long currentRoleId) {
        try {
            Integer roleId = currentRoleId.intValue();
            //目标用户的id
            Long targetUserId = updateUser.getId();
            Long updateRoleId = updateUser.getUpdateRoleId();
            if(updateRoleId == 1){
                throw new RuntimeException("超级管理员是唯一的，无法升级");
            }

            R<Integer> userRoleR = permissionServiceClient.getUserRoleId(targetUserId);
            if(userRoleR.getCode() == R.ERROR_CODE){
                throw new RuntimeException("更新的用户没有权限");
            }
           if (roleId == 3) {
                // 管理员：只能修改普通用户
                R<Integer> targetRoleResult = permissionServiceClient.getUserRoleId(targetUserId);
                if (targetRoleResult.getCode() == R.ERROR_CODE) {
                    throw new RuntimeException("获取目标用户权限失败");
                }
                Integer targetRoleId = targetRoleResult.getData();
                if (targetRoleId != 2) {
                    throw new RuntimeException("权限不足，只能修改普通用户信息");
                }
            } else if (roleId == 2) {
                // 普通用户：只能修改自己
                if (!currentUserId.equals(targetUserId)) {
                    throw new RuntimeException("权限不足，只能修改自己的信息");
                }
            } else if (roleId == 1) {
                // 超级管理员：可以升级/降级用户角色
                if (updateRoleId != null) {
//                    Integer currentRoleId2 = userRoleR.getData();
                    // 如果当前角色和目标角色不同，则进行升级或降级
                    if (!currentRoleId.equals(updateRoleId.intValue())) {
                        if (updateRoleId == 3) {
                            // 升级为管理员
                            permissionServiceClient.upgradeToAdmin(targetUserId);
                            // 刷新用户权限缓存
                            permissionCacheService.refreshUserRoleCode(targetUserId);
                            log.info("用户 {} 已被升级为管理员，并刷新权限缓存", targetUserId);
                        } else if (updateRoleId == 2) {
                            // 降级为普通用户
                            permissionServiceClient.downgradeToUser(targetUserId);
                            // 刷新用户权限缓存
                            permissionCacheService.refreshUserRoleCode(targetUserId);
                            log.info("用户 {} 已被降级为普通用户，并刷新权限缓存", targetUserId);
                        }
                    }
                }
            } else {
                throw new RuntimeException("无效的角色权限");
            }
            
            // 执行更新
            this.updateUser(updateUser);
        } catch (Exception e) {
            throw new RuntimeException("更新用户信息失败: " + e.getMessage());
        }
    }

    @Override
    public void resetPasswordWithRoleId(Long userId, String newPassword, Long currentUserId, Long currentRoleId) {
        try {
            Integer roleId = currentRoleId.intValue();
            
            // 权限校验
            if (roleId == 1) {
                // 超管：可以重置所有用户密码
            } else if (roleId == 3) {
                // 管理员：只能重置普通用户密码
                R<Integer> targetRoleResult = permissionServiceClient.getUserRoleId(userId);
                if (targetRoleResult.getCode() != 0) {
                    throw new RuntimeException("获取目标用户权限失败");
                }
                Integer targetRoleId = targetRoleResult.getData();
                if (targetRoleId != 2) {
                    throw new RuntimeException("权限不足，只能重置普通用户密码");
                }
            } else if (roleId == 2) {
                // 普通用户：只能重置自己的密码
                if (!currentUserId.equals(userId)) {
                    throw new RuntimeException("权限不足，只能重置自己的密码");
                }
            } else {
                throw new RuntimeException("无效的角色权限");
            }
            
            // 获取用户信息
            UserEntity user = this.getById(userId);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            
            // 加密新密码
            String encryptedPassword = DigestUtils.sha256Hex(newPassword + salt);
            user.setPassword(encryptedPassword);
            
            // 更新密码
            this.updateById(user);
        } catch (Exception e) {
            throw new RuntimeException("重置密码失败: " + e.getMessage());
        }
    }

}