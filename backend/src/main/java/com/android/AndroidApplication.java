package com.android;

import com.android.config.UserContextInterceptor;
import com.android.service.IAdminHealthDataQueryService;
import com.android.service.IRawHealthDataService;
import com.android.service.ITUserService;
import com.android.service.impl.AdminHealthDataQueryServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@MapperScan("com.android.mapper")
@EnableAsync
@EnableScheduling
public class AndroidApplication implements WebMvcConfigurer {

    @Bean
    public UserContextInterceptor userContextInterceptorConfig() {
        return new UserContextInterceptor();
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(clientHttpRequestFactory());
        
        // 配置ObjectMapper支持LocalDateTime序列化
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 配置消息转换器
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);
        restTemplate.getMessageConverters().add(0, converter);
        
        return restTemplate;
    }

    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 连接超时30秒
        factory.setReadTimeout(900000); // 读取超时900秒（15分钟），智能体服务需要查询健康档案、分析数据并生成响应，需要较长时间
        return factory;
    }

    /**
     * 管理端健康数据查询服务显式注册（避免 IDE 增量编译/扫描异常导致 Bean 缺失）。
     */
    @Bean
    @Primary
    public IAdminHealthDataQueryService adminHealthDataQueryService(
            ITUserService tUserService,
            IRawHealthDataService rawHealthDataService) {
        return new AdminHealthDataQueryServiceImpl(tUserService, rawHealthDataService);
    }

    public static void main(String[] args) {
        SpringApplication.run(AndroidApplication.class, args);
    }
    
    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userContextInterceptorConfig())
                .addPathPatterns("/**")
                .excludePathPatterns("/t-user/login", "/t-user/register");
    }
}
