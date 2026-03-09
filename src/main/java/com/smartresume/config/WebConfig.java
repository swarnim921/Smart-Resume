package com.smartresume.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Map the root URL ("/") to the static HTML file
        registry.addViewController("/").setViewName("forward:/smart_resume_signin_page.html");
    }
}
