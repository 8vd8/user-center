package com.xzc.usercenter.permission.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xzc.usercenter.permission.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户角色关系Mapper接口
 * @author xzc (GitHub: 8vd8)
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
    
    /**
     * 根据用户ID查询角色码
     */
    @Select("SELECT r.role_code FROM user_roles ur " +
            "LEFT JOIN roles r ON ur.role_id = r.role_id " +
            "WHERE ur.user_id = #{userId}")
    String getUserRoleCode(Long userId);
}