package io.github.balasis.taskmanager.context.web.config;

import io.github.balasis.taskmanager.context.web.interceptor.RateLimitInterceptor;
import io.github.balasis.taskmanager.context.web.jwt.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final JwtInterceptor jwtInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry){
        // JWT runs FIRST — populates CurrentUser for authenticated endpoints
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .order(1)
                .excludePathPatterns("/auth/**","/h2-console");

        // Rate limiter runs SECOND — uses userId from CurrentUser (skips public endpoints)
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .order(2)
                .excludePathPatterns("/auth/**", "/h2-console", "/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "https://www.myteamtasks.net",
                        "http://localhost:8080",
                        "http://127.0.0.1:5500",
                        "http://127.0.0.1:8080",
                        "http://localhost:5173",
                        "http://localhost:5173/",
                        "http://localhost:3000",
                        "http://localhost:8081")
                .allowedMethods("GET","POST","PUT","DELETE","PATCH","OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }



}
