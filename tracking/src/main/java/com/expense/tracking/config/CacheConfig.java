package com.expense.tracking.config;

import java.time.Duration;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CacheConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Disable caching for JavaScript and CSS files during development
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCacheControl(CacheControl.noCache().mustRevalidate());
        
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCacheControl(CacheControl.noCache().mustRevalidate());
        
        // Allow short caching for other static resources
        registry.addResourceHandler("/icons/**", "/favicon.svg", "/manifest.json")
                .addResourceLocations("classpath:/static/icons/", "classpath:/static/", "classpath:/static/")
                .setCacheControl(CacheControl.maxAge(Duration.ofMinutes(5)));
        
        // No cache for HTML files
        registry.addResourceHandler("/**/*.html")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache().mustRevalidate());
    }
}