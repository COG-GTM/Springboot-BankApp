package com.example.bankapp.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleAccountNotFoundException(AccountNotFoundException ex, 
                                                  HttpServletRequest request, 
                                                  Model model) {
        logger.error("Account not found error: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        model.addAttribute("error", ex.getMessage());
        model.addAttribute("status", HttpStatus.NOT_FOUND.value());
        return "error";
    }

    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInsufficientFundsException(InsufficientFundsException ex, 
                                                    HttpServletRequest request, 
                                                    Model model) {
        logger.error("Insufficient funds error: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        model.addAttribute("error", ex.getMessage());
        model.addAttribute("status", HttpStatus.BAD_REQUEST.value());
        return "error";
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleDuplicateUsernameException(DuplicateUsernameException ex, 
                                                    HttpServletRequest request, 
                                                    Model model) {
        logger.error("Duplicate username error: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        model.addAttribute("error", ex.getMessage());
        model.addAttribute("status", HttpStatus.CONFLICT.value());
        return "error";
    }

    @ExceptionHandler(InvalidAmountException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInvalidAmountException(InvalidAmountException ex, 
                                                HttpServletRequest request, 
                                                Model model) {
        logger.error("Invalid amount error: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        model.addAttribute("error", ex.getMessage());
        model.addAttribute("status", HttpStatus.BAD_REQUEST.value());
        return "error";
    }

    @ExceptionHandler(InvalidInputException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInvalidInputException(InvalidInputException ex, 
                                               HttpServletRequest request, 
                                               Model model) {
        logger.error("Invalid input error: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        model.addAttribute("error", ex.getMessage());
        model.addAttribute("status", HttpStatus.BAD_REQUEST.value());
        return "error";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleValidationException(MethodArgumentNotValidException ex, 
                                             HttpServletRequest request, 
                                             Model model) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        logger.error("Validation error: {} - Path: {}", errorMessage, request.getRequestURI());
        model.addAttribute("error", errorMessage);
        model.addAttribute("status", HttpStatus.BAD_REQUEST.value());
        return "error";
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBindException(BindException ex, 
                                       HttpServletRequest request, 
                                       Model model) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        logger.error("Binding error: {} - Path: {}", errorMessage, request.getRequestURI());
        model.addAttribute("error", errorMessage);
        model.addAttribute("status", HttpStatus.BAD_REQUEST.value());
        return "error";
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDeniedException(AccessDeniedException ex, 
                                               HttpServletRequest request, 
                                               Model model) {
        logger.error("Access denied error: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        model.addAttribute("error", "You do not have permission to access this resource");
        model.addAttribute("status", HttpStatus.FORBIDDEN.value());
        return "error";
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public String handleAuthenticationException(AuthenticationException ex, 
                                                 HttpServletRequest request, 
                                                 Model model) {
        logger.error("Authentication error: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        model.addAttribute("error", "Authentication failed. Please check your credentials.");
        model.addAttribute("status", HttpStatus.UNAUTHORIZED.value());
        return "error";
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public String handleBadCredentialsException(BadCredentialsException ex, 
                                                 HttpServletRequest request, 
                                                 Model model) {
        logger.error("Bad credentials error: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        model.addAttribute("error", "Invalid username or password");
        model.addAttribute("status", HttpStatus.UNAUTHORIZED.value());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex, 
                                          HttpServletRequest request, 
                                          Model model) {
        logger.error("Unexpected error occurred: {} - Path: {} - Exception: {}", 
                     ex.getMessage(), request.getRequestURI(), ex.getClass().getName(), ex);
        model.addAttribute("error", "An unexpected error occurred. Please try again later.");
        model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return "error";
    }
}
