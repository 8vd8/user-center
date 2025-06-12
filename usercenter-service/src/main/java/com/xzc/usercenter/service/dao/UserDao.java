package com.xzc.usercenter.service.dao;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xzc.usercenter.service.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表
 * 
 * @author fuckchao
 * @email 1936002261@qq.com
 * @date 2025-06-12 18:07:16
 */
@Mapper
public interface UserDao extends BaseMapper<UserEntity> {
	
}
