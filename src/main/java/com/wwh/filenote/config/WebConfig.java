package com.wwh.filenote.config;

import com.wwh.filenote.security.LoginInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 用于注册拦截器和配置Web相关设置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public LoginInterceptor loginInterceptor() {
        return new LoginInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册登录拦截器
        registry.addInterceptor(loginInterceptor())
                .addPathPatterns("/**") // 拦截所有路径
                .excludePathPatterns("/favicon.ico") // 排除网站图标
                .excludePathPatterns("/login", "/logout", "/api/anonymous-config") // 排除登录和登出接口
                .excludePathPatterns("/login.html") // 排除登录页面
                .excludePathPatterns("/images/**");// 排除图片资源
    }
}