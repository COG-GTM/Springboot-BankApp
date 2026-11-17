package com.example.bankapp.controller;

import com.example.bankapp.dto.AuthResponse;
import com.example.bankapp.dto.LoginRequest;
import com.example.bankapp.dto.RegisterRequest;
import com.example.bankapp.model.Account;
import com.example.bankapp.security.JwtUtil;
import com.example.bankapp.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AccountService accountService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthController(AccountService accountService, JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.accountService = accountService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            UserDetails userDetails = accountService.loadUserByUsername(request.getUsername());
            String token = jwtUtil.generateToken(userDetails);

            AuthResponse response = new AuthResponse(token, request.getUsername(), "Login successful");
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Authentication failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            Account account = accountService.registerAccount(request.getUsername(), request.getPassword());
            
            UserDetails userDetails = accountService.loadUserByUsername(account.getUsername());
            String token = jwtUtil.generateToken(userDetails);

            AuthResponse response = new AuthResponse(token, account.getUsername(), "Registration successful");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
