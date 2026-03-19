package io.github.balasis.taskmanager.engine.core.bootstrap;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.github.balasis.taskmanager.engine.infrastructure.bootstrap.StartupGate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

// returns 503 to all requests until StartupGate says the app is ready.
// prevents race conditions where a request arrives before DefaultImageBootstrap
// or DataLoader have finished. health probes and h2-console are exempted
// so Azure can still check liveness during boot.
@Component
public class StartupBlockingFilter implements Filter {

    private final StartupGate startupGate;

    public StartupBlockingFilter(StartupGate startupGate) {
        this.startupGate = startupGate;
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain) throws ServletException, IOException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (!startupGate.isReady() && !shouldSkip(request)) {
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Service is starting up. Please try again shortly.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkip(HttpServletRequest request) {
        String path = request.getRequestURI();

        if (path == null) {
            return false;
        }

        return path.startsWith("/actuator/health")
                || path.equals("/health")
                || path.startsWith("/h2-console")
                || path.equals("/error");
    }
}
