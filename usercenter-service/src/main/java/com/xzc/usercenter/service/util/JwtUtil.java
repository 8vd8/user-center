
package com.xzc.usercenter.service.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 用于生成和解析JWT令牌
 */
@Component
public class JwtUtil {
    
    // JWT密钥
    private static final String SECRET = "usercenter_jwt_secret_key_2024";
    
    // JWT过期时间（24小时）
    private static final long EXPIRATION = 24 * 60 * 60 * 1000;
    
    // JWT请求头名称
    private static final String HEADER_NAME = "Authorization";
    
    // JWT前缀
    private static final String TOKEN_PREFIX = "Bearer ";
    
    /**
     * 生成JWT令牌
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return JWT令牌
     */
    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .compact();
    }
    
    /**
     * 从请求中获取JWT令牌
     * @param request HTTP请求
     * @return JWT令牌
     */
    public String getTokenFromRequest(HttpServletRequest request) {
        String header = request.getHeader(HEADER_NAME);
        if (header != null && header.startsWith(TOKEN_PREFIX)) {
            return header.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
    
    /**
     * 解析JWT令牌获取Claims
     * @param token JWT令牌
     * @return Claims
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            throw new RuntimeException("JWT令牌解析失败: " + e.getMessage());
        }
    }
    
    /**
     * 从JWT令牌中获取用户ID
     * @param token JWT令牌
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.valueOf(claims.get("userId").toString());
    }
    
    /**
     * 从JWT令牌中获取用户名
     * @param token JWT令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }
    
    /**
     * 从JWT令牌中获取角色ID
     * @param token JWT令牌
     * @return 角色ID
     */
    public Long getRoleIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.valueOf(claims.get("roleId").toString());
    }
    
    /**
     * 验证JWT令牌是否过期
     * @param token JWT令牌
     * @return 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * 验证JWT令牌是否有效
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 从HTTP请求中获取当前登录用户ID
     * @param request HTTP请求
     * @return 当前登录用户ID
     */
    public Long getCurrentUserIdFromRequest(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token == null) {
            throw new RuntimeException("未找到JWT令牌，请先登录");
        }
        
        if (!validateToken(token)) {
            throw new RuntimeException("JWT令牌无效或已过期，请重新登录");
        }
        
        return getUserIdFromToken(token);
    }
}