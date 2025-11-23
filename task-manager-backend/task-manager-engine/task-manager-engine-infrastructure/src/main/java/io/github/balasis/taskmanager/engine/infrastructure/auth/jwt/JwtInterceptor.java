package io.github.balasis.taskmanager.engine.infrastructure.auth.jwt;

import io.github.balasis.taskmanager.context.base.exception.auth.UnauthenticatedException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor{
    private final CurrentUser currentUser;
    private final JwtService jwtService;

    @Override
    public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler) throws Exception{
        String token = null;
        if (request.getCookies()!=null){
            for (Cookie cookie : request.getCookies()){
                if(cookie.getName().equals("jwt")){
                 token = cookie.getValue();
                }
            }
        }
        if(token == null){
            throw new UnauthenticatedException("No JWT token provided");
        }
        Claims claims = jwtService.extractAllClaims(token);

        currentUser.setUserId(
                ( (Number) claims.get("userId") ).longValue()
        );
        return true;
    }

}
