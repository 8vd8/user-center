package com.xzc.usercenter.service.cache;

import com.xzc.common.utils.R;
import com.xzc.usercenter.service.feign.PermissionServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 权限缓存服务
 * 提供本地缓存的权限检查功能，减少远程调用
 */
@Slf4j
@Service
public class PermissionCacheService {

    @Autowired
    private PermissionServiceClient permissionServiceClient;

    // 本地缓存：用户ID -> 角色权限码
    private final ConcurrentHashMap<Long, String> userRoleCache = new ConcurrentHashMap<>();
    
    // 本地缓存：用户ID -> 角色ID
    private final ConcurrentHashMap<Long, Integer> userRoleIdCache = new ConcurrentHashMap<>();
    
    // 缓存过期时间映射：用户ID -> 过期时间戳
    private final ConcurrentHashMap<Long, Long> cacheExpireTime = new ConcurrentHashMap<>();
    
    // 缓存过期时间（毫秒）- 默认5分钟
    private static final long CACHE_EXPIRE_TIME = 5 * 60 * 1000;
    
    // 定时清理任务
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    
    public PermissionCacheService() {
        // 启动定时清理过期缓存任务，每分钟执行一次
        cleanupExecutor.scheduleAtFixedRate(this::cleanExpiredCache, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * 获取用户角色权限码（优先从缓存获取）
     * @param userId 用户ID
     * @return 角色权限码
     */
    public String getUserRoleCode(Long userId) {
        if (userId == null) {
            return null;
        }
        
        // 检查缓存是否存在且未过期
        String cachedRoleCode = getCachedRoleCode(userId);
        if (cachedRoleCode != null) {
            log.debug("从缓存获取用户权限: userId={}, roleCode={}", userId, cachedRoleCode);
            return cachedRoleCode;
        }
        
        // 缓存未命中，从远程服务获取
        try {
            R<Integer> result = permissionServiceClient.getUserRoleId(userId);
            if (result != null && result.getCode() == 200 && result.getData() != null) {
                String roleCode = String.valueOf(result.getData());
                Integer roleId = result.getData();
                // 更新缓存
                putCache(userId, roleCode, roleId);
                log.info("从远程服务获取用户权限并缓存: userId={}, roleCode={}, roleId={}", userId, roleCode, roleId);
                return roleCode;
            }
            return "USER"; // 默认权限
        } catch (Exception e) {
            log.error("获取用户权限失败: userId={}, error={}", userId, e.getMessage());
            // 返回默认权限
            return "USER";
        }
    }
    
    /**
     * 获取用户角色ID（优先从缓存获取）
     * @param userId 用户ID
     * @return 角色ID
     */
    public Integer getUserRoleId(Long userId) {
        if (userId == null) {
            return null;
        }
        
        // 检查缓存是否存在且未过期
        Integer cachedRoleId = getCachedRoleId(userId);
        if (cachedRoleId != null) {
            log.debug("从缓存获取用户角色ID: userId={}, roleId={}", userId, cachedRoleId);
            return cachedRoleId;
        }
        
        // 缓存未命中，从远程服务获取
        try {
            R<Integer> result = permissionServiceClient.getUserRoleId(userId);
            if (result != null && result.getCode() ==  R.SUCCESS_CODE && result.getData() != null) {
                String roleCode = String.valueOf(result.getData());
                Integer roleId = result.getData();
                // 更新缓存
                putCache(userId, roleCode, roleId);
                log.info("从远程服务获取用户角色ID并缓存: userId={}, roleId={}", userId, roleId);
                return roleId;
            }
            return 2; // 默认为普通用户
        } catch (Exception e) {
            log.error("获取用户角色ID失败: userId={}, error={}", userId, e.getMessage());
            // 返回默认角色ID
            return 2;
        }
    }
    
    /**
     * 从缓存获取角色权限码
     * @param userId 用户ID
     * @return 角色权限码，如果缓存不存在或已过期则返回null
     */
    private String getCachedRoleCode(Long userId) {
        Long expireTime = cacheExpireTime.get(userId);
        if (expireTime == null || System.currentTimeMillis() > expireTime) {
            // 缓存不存在或已过期
            removeFromCache(userId);
            return null;
        }
        return userRoleCache.get(userId);
    }
    
    /**
     * 从缓存获取角色ID
     * @param userId 用户ID
     * @return 角色ID，如果缓存不存在或已过期则返回null
     */
    private Integer getCachedRoleId(Long userId) {
        Long expireTime = cacheExpireTime.get(userId);
        if (expireTime == null || System.currentTimeMillis() > expireTime) {
            // 缓存不存在或已过期
            removeFromCache(userId);
            return null;
        }
        return userRoleIdCache.get(userId);
    }
    
    /**
     * 将权限码和角色ID放入缓存
     * @param userId 用户ID
     * @param roleCode 角色权限码
     * @param roleId 角色ID
     */
    private void putCache(Long userId, String roleCode, Integer roleId) {
        userRoleCache.put(userId, roleCode);
        userRoleIdCache.put(userId, roleId);
        cacheExpireTime.put(userId, System.currentTimeMillis() + CACHE_EXPIRE_TIME);
    }
    
    /**
     * 从缓存中移除用户权限信息
     * @param userId 用户ID
     */
    public void removeFromCache(Long userId) {
        userRoleCache.remove(userId);
        userRoleIdCache.remove(userId);
        cacheExpireTime.remove(userId);
        log.debug("移除用户权限缓存: userId={}", userId);
    }
    
    /**
     * 刷新用户权限缓存
     * @param userId 用户ID
     * @return 最新的角色权限码
     */
    public String refreshUserRoleCode(Long userId) {
        // 先移除旧缓存
        removeFromCache(userId);
        // 重新获取并缓存
        return getUserRoleCode(userId);
    }
    
    /**
     * 清理过期的缓存条目
     */
    private void cleanExpiredCache() {
        long currentTime = System.currentTimeMillis();
        AtomicInteger cleanedCount = new AtomicInteger(0);
        
        // 遍历过期时间映射，清理过期条目
        cacheExpireTime.entrySet().removeIf(entry -> {
            if (currentTime > entry.getValue()) {
                Long userId = entry.getKey();
                userRoleCache.remove(userId);
                userRoleIdCache.remove(userId);
                cleanedCount.incrementAndGet();
                return true;
            }
            return false;
        });
        
        if (cleanedCount.get() > 0) {
            log.info("清理过期权限缓存: 清理数量={}, 剩余缓存数量={}", cleanedCount.get(), userRoleCache.size());
        }
    }
    
    /**
     * 获取缓存统计信息
     * @return 缓存统计信息
     */
    public String getCacheStats() {
        return String.format("权限缓存统计 - 总数量: %d, 有效数量: %d", 
                userRoleCache.size(), 
                cacheExpireTime.entrySet().stream()
                        .mapToInt(entry -> System.currentTimeMillis() <= entry.getValue() ? 1 : 0)
                        .sum());
    }
    
    /**
     * 清空所有缓存
     */
    public void clearAllCache() {
        userRoleCache.clear();
        userRoleIdCache.clear();
        cacheExpireTime.clear();
        log.info("清空所有权限缓存");
    }
}