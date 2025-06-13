package com.xzc.usercenter.logging.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xzc.common.utils.PageUtils;
import com.xzc.common.utils.R;
import com.xzc.usercenter.logging.entity.LogEntity;

import java.util.Map;

/**
 * 操作日志表
 *
 * @author fuckchao
 * @email 1936002261@qq.com
 * @date 2025-06-13 11:52:18
 */
public interface LogService extends IService<LogEntity> {

    PageUtils queryPage(Map<String, Object> params);

    R getLogsByUserId(Long userId);

    R getLogsByOperation(String operation);
}

