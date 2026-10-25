package com.projectmanagement.projecttaskmanagementsystem.config;

import com.projectmanagement.projecttaskmanagementsystem.CustomUserDetailsService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .authenticationProvider(authenticationProvider())

                .authorizeHttpRequests(auth -> auth

                        // Public pages and static resources
                        .requestMatchers(
                                "/login",
                                "/register-admin",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/error"
                        ).permitAll()

                        // Change password: available to every logged-in role
                        .requestMatchers("/change-password").authenticated()

                        // Admin-only pages
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Employee-only pages
                        .requestMatchers("/employee/**").hasRole("EMPLOYEE")

                        // Manager-only pages and workspace
                        .requestMatchers(
                                "/manager/**",
                                "/projects",
                                "/projects/**",
                                "/tasks",
                                "/tasks/**",
                                "/team",
                                "/team/**"
                        ).hasRole("MANAGER")

                        // All other pages require authentication
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")

                        .successHandler((request, response, authentication) -> {
                            var authorities = authentication.getAuthorities();

                            if (authorities.stream().anyMatch(a ->
                                    a.getAuthority().equals("ROLE_ADMIN"))) {

                                response.sendRedirect("/admin/dashboard");

                            } else if (authorities.stream().anyMatch(a ->
                                    a.getAuthority().equals("ROLE_MANAGER"))) {

                                response.sendRedirect("/manager/dashboard");

                            } else if (authorities.stream().anyMatch(a ->
                                    a.getAuthority().equals("ROLE_EMPLOYEE"))) {

                                response.sendRedirect("/employee/tasks");

                            } else {
                                response.sendRedirect("/login?error");
                            }
                        })

                        .failureUrl("/login?error")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }

}