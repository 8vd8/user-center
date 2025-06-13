package com.xzc.usercenter.logging.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


import com.xzc.common.entity.OperationLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.xzc.usercenter.logging.entity.LogEntity;
import com.xzc.usercenter.logging.service.LogService;
import com.xzc.common.utils.PageUtils;
import com.xzc.common.utils.R;



/**
 * 操作日志表
 *
 * @author fuckchao
 * @email 1936002261@qq.com
 * @date 2025-06-13 11:52:18
 */
@RestController
@RequestMapping("logging/log")
public class LogController {
    @Autowired
    private LogService logService;

    /**
     * 根据用户ID查询操作日志
     */
    @GetMapping("/user/{userId}")
    public R getLogsByUserId(@PathVariable Long userId) {
        return logService.getLogsByUserId(userId);
    }

    /**
     * 根据操作类型查询日志
     */
    @GetMapping("/operation/{operation}")
    public R getLogsByOperation(@PathVariable String operation) {
        return logService.getLogsByOperation(operation);
    }

}
