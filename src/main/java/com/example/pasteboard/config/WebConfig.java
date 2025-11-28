package com.example.pasteboard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 用于注册拦截器和配置Web相关设置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册登录拦截器
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/**") // 拦截所有路径
                .excludePathPatterns("/login", "/logout") // 排除登录和登出路径
                .excludePathPatterns("/login.html"); // 排除静态资源
    }
}