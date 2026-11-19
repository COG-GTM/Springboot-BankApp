package com.example.bankapp.config;

import com.example.bankapp.security.LoginAttemptService;
import com.example.bankapp.security.LoginThrottleFilter;
import com.example.bankapp.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    AccountService accountService;

    @Autowired
    LoginAttemptService loginAttemptService;

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        SimpleUrlAuthenticationSuccessHandler dashboardRedirect = new SimpleUrlAuthenticationSuccessHandler("/dashboard");
        dashboardRedirect.setAlwaysUseDefaultTargetUrl(true);
        SimpleUrlAuthenticationFailureHandler loginErrorRedirect = new SimpleUrlAuthenticationFailureHandler("/login?error");

        http
                .addFilterBefore(new LoginThrottleFilter(loginAttemptService), UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/register").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            loginAttemptService.loginSucceeded(authentication.getName());
                            dashboardRedirect.onAuthenticationSuccess(request, response, authentication);
                        })
                        .failureHandler((request, response, exception) -> {
                            loginAttemptService.loginFailed(request.getParameter("username"), request.getRemoteAddr());
                            loginErrorRedirect.onAuthenticationFailure(request, response, exception);
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())
                );

        return http.build();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth, PasswordEncoder passwordEncoder) throws Exception {
        auth.userDetailsService(accountService).passwordEncoder(passwordEncoder);

    }
}