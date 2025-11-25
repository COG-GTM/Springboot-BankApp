package com.example.bankapp.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationFailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, 
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        
        String username = request.getParameter("username");
        String errorMessage;
        
        if (exception instanceof BadCredentialsException) {
            logger.error("Authentication failed - bad credentials for user: {}", username);
            errorMessage = "Invalid username or password";
        } else if (exception instanceof DisabledException) {
            logger.error("Authentication failed - account disabled for user: {}", username);
            errorMessage = "Your account has been disabled";
        } else if (exception instanceof LockedException) {
            logger.error("Authentication failed - account locked for user: {}", username);
            errorMessage = "Your account has been locked";
        } else {
            logger.error("Authentication failed for user: {} - Error: {}", username, exception.getMessage());
            errorMessage = "Authentication failed. Please try again.";
        }
        
        String encodedMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        response.sendRedirect("/login?error=true&message=" + encodedMessage);
    }
}
