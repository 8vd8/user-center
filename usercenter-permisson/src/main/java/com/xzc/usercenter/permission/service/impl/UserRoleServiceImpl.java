package com.xzc.usercenter.permission.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzc.usercenter.permission.entity.Role;
import com.xzc.usercenter.permission.entity.UserRole;
import com.xzc.usercenter.permission.mapper.RoleMapper;
import com.xzc.usercenter.permission.mapper.UserRoleMapper;
import com.xzc.usercenter.permission.service.UserRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 权限服务实现类
 * @author xzc (GitHub: 8vd8)
 */
@Service
public class UserRoleServiceImpl  extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {


    @Resource
    private RoleMapper roleMapper;

    /**
     * 绑定默认角色
     * @param userId
     */
    @Override
    @Transactional
    public void bindDefaultRole(Long userId) {
        // 检查用户是否已有角色
        QueryWrapper<UserRole> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        UserRole existUserRole = this.getOne(queryWrapper);

        if (existUserRole == null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(2); // 普通用户角色ID
            this.save(userRole);
            Role role = new Role();
            role.setRoleId(2);
            role.setRoleCode("user");
            roleMapper.insert(role);
        }
    }

    /**
     * 根据用户id查询对应的角色权限
     * @param userId
     * @return
     */
    @Override
    public String getUserRoleCode(Long userId) {
        UserRole userRole = this.getById(userId);
        Integer roleId = userRole.getRoleId();
        QueryWrapper<Role> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_id", roleId);
        String roleCode = null;
        try {
            roleCode = roleMapper.selectOne(queryWrapper).getRoleCode();
        } catch (NullPointerException e) {
            throw new RuntimeException("此号没有用户权限");
        }
        return roleCode;
    }

    @Override
    @Transactional
    public void upgradeToAdmin(Long userId) {
        // 更新用户角色为管理员（role_id = 3）
        QueryWrapper<UserRole> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        UserRole userRole = this.getOne(queryWrapper);
        if (userRole == null) {
            throw new RuntimeException("此用户没有用户权限");
        }
        userRole.setRoleId(3); // 管理员角色ID
        this.updateById(userRole);
        QueryWrapper<Role> queryWrapper2 = new QueryWrapper<>();
        queryWrapper2.eq("role_id", userRole.getRoleId());
        Role role = roleMapper.selectOne(queryWrapper2);
        if (role == null) {
            throw new RuntimeException("此用户没有用户权限?");
        }
        role.setRoleCode("admin");
        roleMapper.updateById(role);
    }

    @Override
    @Transactional
    public void downgradeToUser(Long userId) {
        // 降级用户角色为普通用户（role_id = 2）
        QueryWrapper<UserRole> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        UserRole userRole = this.getOne(queryWrapper);
        QueryWrapper<Role> queryWrapper2 = new QueryWrapper<>();
        queryWrapper2.eq("role_id", userRole.getRoleId());
        if (userRole != null) {
            userRole.setRoleId(2); // 普通用户角色ID
            this.updateById(userRole);
            Role role =roleMapper.selectOne(queryWrapper2);
            role.setRoleCode("user");
        }
    }
}