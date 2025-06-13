package com.xzc.usercenter.logging.service.impl;


import com.xzc.common.utils.R;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzc.common.utils.PageUtils;
import com.xzc.common.utils.Query;

import com.xzc.usercenter.logging.dao.LogDao;
import com.xzc.usercenter.logging.entity.LogEntity;
import com.xzc.usercenter.logging.service.LogService;


@Service("logService")
public class LogServiceImpl extends ServiceImpl<LogDao, LogEntity> implements LogService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<LogEntity> page = this.page(
                new Query<LogEntity>().getPage(params),
                new QueryWrapper<LogEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public R getLogsByUserId(Long userId) {
        try {
            QueryWrapper<LogEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId);
            queryWrapper.orderByDesc("create_time");

            List<LogEntity> logs = this.list(queryWrapper);
            return R.ok(logs);
        } catch (Exception e) {
            return R.error("查询用户日志失败: " + e.getMessage());
        }
    }

    @Override
    public R getLogsByOperation(String operation) {
        try {
            QueryWrapper<LogEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("operation", operation);
            queryWrapper.orderByDesc("create_time");
            List<LogEntity> logs = this.list(queryWrapper);
            return R.ok(logs);
        } catch (Exception e) {
            return R.error("查询操作日志失败: " + e.getMessage());
        }
    }

}