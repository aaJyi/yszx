package com.android.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 注册请求日志过滤器，用于排查请求是否到达后端
 */
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter() {
        FilterRegistrationBean<RequestLoggingFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new RequestLoggingFilter());
        bean.addUrlPatterns("/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    /**
     * 接口白名单过滤器：
     * 仅放行小程序使用到的接口，其余返回 404。
     */
    @Bean
    public FilterRegistrationBean<ApiWhitelistFilter> apiWhitelistFilter() {
        FilterRegistrationBean<ApiWhitelistFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new ApiWhitelistFilter());
        bean.addUrlPatterns("/*");
        // 与请求日志过滤器并存即可；即使日志先记录也不影响白名单效果
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return bean;
    }
}
