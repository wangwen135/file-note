package com.wwh.filebox.security;

import com.wwh.filebox.config.GroupConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 登录拦截器
 * 用于检查用户是否已登录，未登录则根据配置决定是否创建匿名会话或重定向到登录页面
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private GroupConfig groupConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取当前会话
        HttpSession session = request.getSession();
        
        // 检查是否已登录
        if (session.getAttribute("logged_in") == null) {
            // 未登录，检查是否启用了匿名访问
            GroupConfig.Anonymous anonymousConfig = groupConfig.getAnonymous();
            if (anonymousConfig != null && anonymousConfig.isEnabled()) {
                // 启用了匿名访问，为用户创建匿名会话
                session.setAttribute("username", "anonymous");
                session.setAttribute("groupName", anonymousConfig.getName());
                session.setAttribute("storageDir", anonymousConfig.getDirectory());
                session.setAttribute("role", anonymousConfig.getRole());
                session.setAttribute("logged_in", true);
                session.setAttribute("is_anonymous", true);
                // 匿名用户，放行
                return true;
            } else {
                // 未启用匿名访问，重定向到登录页面
                response.sendRedirect("/login.html");
                return false;
            }
        }
        
        // 已登录，放行
        return true;
    }
}