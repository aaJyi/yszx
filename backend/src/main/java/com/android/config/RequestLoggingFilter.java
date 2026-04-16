package com.android.config;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 请求日志过滤器：记录所有进入后端的 HTTP 请求，用于排查「前端提问但后端无日志」问题
 */
@Slf4j
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest req = (HttpServletRequest) request;
        String method = req.getMethod();
        String uri = req.getRequestURI();
        String query = req.getQueryString();
        String fullUrl = query != null ? uri + "?" + query : uri;
        log.info("[请求进入] {} {} 来自 {}", method, fullUrl, req.getRemoteAddr());
        chain.doFilter(request, response);
    }
}
