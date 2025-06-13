package com.xzc.usercenter.logging.dao;

import com.xzc.usercenter.logging.entity.LogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志表
 * 
 * @author fuckchao
 * @email 1936002261@qq.com
 * @date 2025-06-13 11:52:18
 */
@Mapper
public interface LogDao extends BaseMapper<LogEntity> {
	
}
