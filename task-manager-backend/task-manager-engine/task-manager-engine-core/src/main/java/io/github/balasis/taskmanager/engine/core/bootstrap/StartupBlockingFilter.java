package io.github.balasis.taskmanager.engine.core.bootstrap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class StartupBlockingFilter extends OncePerRequestFilter {

    private final StartupGate startupGate;

    public StartupBlockingFilter(StartupGate startupGate) {
        this.startupGate = startupGate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!startupGate.isReady() && !shouldNotFilter(request)) {
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Service is starting up. Please try again shortly.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        if (path == null) {
            return false;
        }

        return path.startsWith("/actuator/health")
                || path.startsWith("/h2-console")
                || path.equals("/error");
    }
}
