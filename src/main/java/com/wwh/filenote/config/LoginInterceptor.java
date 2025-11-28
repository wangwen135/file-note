package com.wwh.filenote.config;

import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 登录拦截器
 * 用于检查用户是否已登录，未登录则重定向到登录页面
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取当前会话
        HttpSession session = request.getSession();
        
        // 检查是否已登录
        if (session.getAttribute("logged_in") == null) {
            // 未登录，重定向到登录页面
            response.sendRedirect("/login.html");
            return false;
        }
        
        // 已登录，放行
        return true;
    }
}