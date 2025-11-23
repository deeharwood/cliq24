package com.cliq24.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded profile pictures
        registry.addResourceHandler("/uploads/profile-pictures/**")
                .addResourceLocations("file:uploads/profile-pictures/")
                .setCachePeriod(3600); // Cache for 1 hour

        // Serve static resources from classpath:/static/
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0); // Disable caching for development
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Forward root to index.html
        registry.addViewController("/").setViewName("forward:/index.html");
    }
}
