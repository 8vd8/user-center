package com.xzc.usercenter.service.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


/**
 * JWT拦截器
 * 用于验证JWT令牌的有效性
 */
//需要转移到网关
@Component
public class JwtInterceptor implements HandlerInterceptor {
    
    // JWT验证已迁移到网关层，这里只需要从请求头中获取用户信息
    
    // 不需要JWT验证的接口路径
    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "service/user/register",
            "service/user/login"
    );
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        
        // 跳过不需要验证的接口
        if (EXCLUDE_PATHS.contains(requestURI)) {
            return true;
        }
        
        // OPTIONS请求直接放行
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        
        try {
            // 从请求头中获取用户信息（由网关层添加）
            String userIdHeader = request.getHeader("X-User-ID");
            String usernameHeader = request.getHeader("X-Username");
            String roleCodeHeader = request.getHeader("X-Role-Code");
            String roleIdHeader = request.getHeader("X-Role-ID");
            
            if (userIdHeader == null || usernameHeader == null) {
                sendErrorResponse(response, 401, "未找到用户信息，请先登录");
                return false;
            }
            
            // 将用户信息存储到请求属性中，方便后续使用
            Long userId = Long.parseLong(userIdHeader);
            String username = usernameHeader;
            String roleCode = roleCodeHeader;
            Long roleId = roleIdHeader != null ? Long.parseLong(roleIdHeader) : null;
            
            request.setAttribute("currentUserId", userId);
            request.setAttribute("currentUsername", username);
            request.setAttribute("currentRoleCode", roleCode);
            if (roleId != null) {
                request.setAttribute("currentRoleId", roleId);
            }
            
            return true;
            
        } catch (Exception e) {
            sendErrorResponse(response, 401, "用户信息验证失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
                "{\"code\":%d,\"msg\":\"%s\",\"data\":null}", 
                status, message
        ));
    }
}