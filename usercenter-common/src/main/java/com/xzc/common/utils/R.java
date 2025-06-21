/**
 * Copyright (c) 2016-2019 人人开源 All rights reserved.
 *
 * https://www.renren.io
 *
 * 版权所有，侵权必究！
 */

package com.xzc.common.utils;

import org.apache.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一返回结果类
 * 支持用户服务的各种接口返回需求
 *
 * @author xzc
 */
public class R<T> extends HashMap<String, Object> {
	private static final long serialVersionUID = 1L;
	
	// 成功状态码
	public static final int SUCCESS_CODE = 0;
	// 失败状态码
	public static final int ERROR_CODE = 500;
	// 参数错误状态码
	public static final int PARAM_ERROR_CODE = 400;
	// 未授权状态码
	public static final int UNAUTHORIZED_CODE = 401;
	// 禁止访问状态码
	public static final int FORBIDDEN_CODE = 403;
	// 资源不存在状态码
	public static final int NOT_FOUND_CODE = 404;
	
	public R() {
		put("code", SUCCESS_CODE);
		put("msg", "success");
		put("timestamp", System.currentTimeMillis());
	}
	
	// ========== 错误返回方法 ==========
	
	public static <T> R<T> error() {
		return error(ERROR_CODE, "未知异常，请联系管理员");
	}
	
	public static <T> R<T> error(String msg) {
		return error(ERROR_CODE, msg);
	}
	
	public static <T> R<T> error(int code, String msg) {
		R<T> r = new R<>();
		r.put("code", code);
		r.put("msg", msg);
		return r;
	}
	
	// 参数错误
	public static <T> R<T> paramError(String msg) {
		return error(PARAM_ERROR_CODE, msg);
	}
	
	// 未授权
	public static <T> R<T> unauthorized(String msg) {
		return error(UNAUTHORIZED_CODE, msg != null ? msg : "未授权访问");
	}
	
	// 禁止访问
	public static <T> R<T> forbidden(String msg) {
		return error(FORBIDDEN_CODE, msg != null ? msg : "禁止访问");
	}
	
	// 资源不存在
	public static <T> R<T> notFound(String msg) {
		return error(NOT_FOUND_CODE, msg != null ? msg : "资源不存在");
	}
	
	// ========== 成功返回方法 ==========
	
	public static <T> R<T> ok() {
		return new R<>();
	}
	
	public static <T> R<T> ok(String msg) {
		R<T> r = new R<>();
		r.put("msg", msg);
		return r;
	}
	
	public static <T> R<T> ok(Map<String, Object> map) {
		R<T> r = new R<>();
		r.putAll(map);
		return r;
	}
	
	// 返回数据对象
	public static <T> R<T> ok(T data) {
		R<T> r = new R<>();
		r.put("data", data);
		return r;
	}
	
	// 返回数据对象和消息
	public static <T> R<T> ok(T data, String msg) {
		R<T> r = new R<>();
		r.put("data", data);
		r.put("msg", msg);
		return r;
	}
	
	// ========== 用户服务专用方法 ==========
	
	// 登录成功返回token
	public static R<String> loginSuccess(String token) {
		R<String> r = new R<>();
		r.put("token", token);
		r.put("msg", "登录成功");
		return r;
	}
	
	// 注册成功
	public static <T> R<T> registerSuccess() {
		return ok("注册成功");
	}
	
	// 更新成功
	public static <T> R<T> updateSuccess() {
		return ok("更新成功");
	}
	
	// 删除成功
	public static <T> R<T> deleteSuccess() {
		return ok("删除成功");
	}
	
	// 返回用户信息
	public static <T> R<T> userInfo(T user) {
		R<T> r = new R<>();
		r.put("userInfo", user);
		r.put("msg", "获取用户信息成功");
		return r;
	}

    public static<T> R<T> success(T entity) {
		R<T> r = new R<>();
		r.put("msg", entity);
		r.put("code", SUCCESS_CODE);
		return r;
    }

    // ========== 链式调用方法 ==========
	
	public R<T> put(String key, Object value) {
		super.put(key, value);
		return this;
	}
	
	public R<T> data(T data) {
		this.put("data", data);
		return this;
	}
	
	public R<T> msg(String msg) {
		this.put("msg", msg);
		return this;
	}
	
	public R<T> code(int code) {
		this.put("code", code);
		return this;
	}
	
	// ========== 获取方法 ==========
	
	public Integer getCode() {
		return (Integer) this.get("code");
	}
	
	public String getMsg() {
		return (String) this.get("msg");
	}
	
	@SuppressWarnings("unchecked")
	public T getData() {
		return (T) this.get("data");
	}
	
	public Long getTimestamp() {
		return (Long) this.get("timestamp");
	}
	
	// 判断是否成功
	public boolean isSuccess() {
		return SUCCESS_CODE == this.getCode();
	}
	
	// 判断是否失败
	public boolean isError() {
		return !isSuccess();
	}

}
