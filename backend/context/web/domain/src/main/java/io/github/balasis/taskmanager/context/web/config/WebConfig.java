package io.github.balasis.taskmanager.context.web.config;

import io.github.balasis.taskmanager.context.web.interceptor.RateLimitInterceptor;
import io.github.balasis.taskmanager.context.web.jwt.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    private final JwtInterceptor jwtInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final Environment environment;

    @Value("${app.download-threads:15}")
    private int downloadThreads;

    @Override
    public void addInterceptors(InterceptorRegistry registry){

        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .order(1)
                .excludePathPatterns("/auth/**", "/health", "/actuator/health", "/h2-console");

        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .order(2)
                .excludePathPatterns("/auth/**", "/health", "/actuator/health", "/h2-console", "/");
    }

    // without this Spring spawns unbounded threads per download (SimpleAsyncTaskExecutor)
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(downloadThreads);
        pool.setMaxPoolSize(downloadThreads);
        pool.setQueueCapacity(100); // deep queue so rejections are near-impossible
        pool.setThreadNamePrefix("dl-stream-");
        pool.initialize();

        configurer.setTaskExecutor(pool);
        configurer.setDefaultTimeout(150_000); // sits above TEAM tier cap (120 s)
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        List<String> origins = new ArrayList<>(List.of(
                "https://www.myteamtasks.net",
                "http://localhost:8080",
                "http://127.0.0.1:5500",
                "http://127.0.0.1:8080",
                "http://localhost:5173",
                "http://localhost:5173/",
                "http://localhost:3000",
                "http://localhost:8081"
        ));

        // In dev profiles, automatically allow requests from local-network IPs
        // so developers can test on mobile devices via Vite's network URL.
        if (isDevProfile()) {
            int[] devPorts = {5173, 5174, 5175, 3000, 8081};
            for (String ip : getLocalNetworkIps()) {
                for (int port : devPorts) {
                    origins.add("http://" + ip + ":" + port);
                }
            }
            log.info("CORS: dev-mode network origins added - {}", origins);
        }

        registry.addMapping("/**")
                .allowedOrigins(origins.toArray(String[]::new))
                .allowedMethods("GET","POST","PUT","DELETE","PATCH","OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    private boolean isDevProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.startsWith("dev"));
    }

    /** Returns all non-loopback IPv4 site/local addresses of this machine. */
    private static List<String> getLocalNetworkIps() {
        List<String> ips = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (ni.isLoopback() || !ni.isUp()) continue;
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr.isLoopbackAddress()) continue;
                    if (addr instanceof java.net.Inet4Address) {
                        ips.add(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            // Fallback, no extra origins
        }
        return ips;
    }

}
