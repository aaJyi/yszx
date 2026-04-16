package com.android.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 将 club-division/img 目录映射为 /club-division-img/** 静态资源。
 */
@Configuration
public class ClubDivisionImgResourceConfig implements WebMvcConfigurer {

    @Value("${club.division.img.dir:}")
    private String imgDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (imgDir == null || imgDir.isBlank()) {
            return;
        }
        Path path = Paths.get(imgDir).toAbsolutePath().normalize();
        String location = path.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/club-division-img/**")
                .addResourceLocations(location);
    }
}
