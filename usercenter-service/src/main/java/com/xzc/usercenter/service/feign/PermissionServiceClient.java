package com.xzc.usercenter.service.feign;

import com.baomidou.mybatisplus.extension.api.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 权限服务Feign客户端
 * @author xzc
 */
@FeignClient(name = "usercenter-permisson")
public interface PermissionServiceClient {
    
    /**
     * 为用户绑定默认角色
     */
    @PostMapping("/permission/bind-default-role")
    R<String> bindDefaultRole(@RequestParam("userId") Long userId);
    
    /**
     * 检查用户权限
     */
    @PostMapping("/permission/check")
    R<Boolean> checkPermission(@RequestParam("userId") Long userId,
                                   @RequestParam("permission") String permission);
}