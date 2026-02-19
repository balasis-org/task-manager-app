package io.github.balasis.taskmanager.context.web.config;

import io.github.balasis.taskmanager.context.web.interceptor.RateLimitInterceptor;
import io.github.balasis.taskmanager.context.web.jwt.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
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
        // rate limiter runs FIRST — catches all requests (incl. /auth/**) by IP
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/h2-console");

        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/**","/h2-console");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
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

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // single-segment SPA paths  (e.g.  /dashboard, /login)
        registry.addViewController("/{path:^(?!api|auth|uploads)[^\\.]*}")
                .setViewName("forward:/index.html");

        // multi-segment SPA paths   (e.g.   /auth/callback, /group/1/task/2)
        registry.addViewController("/{seg1:^(?!api|uploads)[^\\.]*}/{seg2:[^\\.]*}")
                .setViewName("forward:/index.html");

        registry.addViewController("/{seg1:^(?!api|uploads)[^\\.]*}/{seg2:[^\\.]*}/{seg3:[^\\.]*}")
                .setViewName("forward:/index.html");

        registry.addViewController("/{seg1:^(?!api|uploads)[^\\.]*}/{seg2:[^\\.]*}/{seg3:[^\\.]*}/{seg4:[^\\.]*}")
                .setViewName("forward:/index.html");
    }

}
