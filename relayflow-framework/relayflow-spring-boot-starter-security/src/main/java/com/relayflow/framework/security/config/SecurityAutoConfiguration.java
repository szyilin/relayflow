package com.relayflow.framework.security.config;

import com.relayflow.framework.security.core.JwtTokenService;
import com.relayflow.framework.security.core.PermissionAuthoritiesLoader;
import com.relayflow.framework.security.core.TokenRevocationStore;
import com.relayflow.framework.security.filter.AdminPortalAuthorizationFilter;
import com.relayflow.framework.security.filter.JwtAuthenticationFilter;
import com.relayflow.framework.security.handler.RelayflowAccessDeniedHandler;
import com.relayflow.framework.security.handler.RelayflowAuthenticationEntryPoint;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@AutoConfiguration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
@Import(JwtTokenService.class)
public class SecurityAutoConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtTokenService jwtTokenService,
            ObjectProvider<PermissionAuthoritiesLoader> permissionAuthoritiesLoader,
            ObjectProvider<TokenRevocationStore> tokenRevocationStore) {
        return new JwtAuthenticationFilter(jwtTokenService, permissionAuthoritiesLoader, tokenRevocationStore);
    }

    @Bean
    public AdminPortalAuthorizationFilter adminPortalAuthorizationFilter() {
        return new AdminPortalAuthorizationFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AdminPortalAuthorizationFilter adminPortalAuthorizationFilter)
            throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new RelayflowAuthenticationEntryPoint())
                        .accessDeniedHandler(new RelayflowAccessDeniedHandler()))
                .authorizeHttpRequests(registry -> registry
                        .requestMatchers(
                                "/admin-api/system/auth/login",
                                "/admin-api/system/tenant/default",
                                "/app-api/system/auth/register",
                                "/app-api/system/member-invite/preview",
                                "/app-api/system/member-invite/accept",
                                "/app-api/infra/file/public/**",
                                "/infra/ws"
                        ).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(adminPortalAuthorizationFilter, JwtAuthenticationFilter.class);
        return http.build();
    }
}
