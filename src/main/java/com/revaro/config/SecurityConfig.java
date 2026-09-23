package com.revaro.config;

import com.revaro.security.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final int REMEMBER_ME_SECONDS = 7 * 24 * 60 * 60;

    private final UserDetailsServiceImpl userDetailsService;
    private final String rememberMeKey;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService,
                          @Value("${app.remember-me-key}") String rememberMeKey) {
        this.userDetailsService = userDetailsService;
        this.rememberMeKey = rememberMeKey;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/img/**", "/error").permitAll()
                .requestMatchers("/", "/events/{id}", "/leaderboard", "/about", "/contact").permitAll()
                .requestMatchers("/sitemap.xml", "/robots.txt").permitAll()
                .requestMatchers("/login", "/register", "/auth/forgot-password", "/auth/reset-password").permitAll()
                .requestMatchers("/profile/edit").authenticated()
                .requestMatchers("/profile/{username}", "/api/users/{username}/preview").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .logoutSuccessUrl("/")
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .key(rememberMeKey)
                .tokenValiditySeconds(REMEMBER_ME_SECONDS)
            )
            .exceptionHandling(ex -> ex.accessDeniedHandler(this::handleAccessDenied));

        return http.build();
    }

    // When a session expires (or the app redeploys) its CSRF token goes with it, so the next
    // form post used to fail with a raw 403. Send people back to sign in instead.
    private void handleAccessDenied(HttpServletRequest request, HttpServletResponse response,
                                    AccessDeniedException e) throws IOException {
        if (e instanceof CsrfException) {
            response.sendRedirect(request.getContextPath() + "/login?expired");
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }
}
