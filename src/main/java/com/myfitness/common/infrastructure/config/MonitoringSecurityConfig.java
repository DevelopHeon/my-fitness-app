package com.myfitness.common.infrastructure.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;

@Configuration(proxyBeanMethods = false)
@Profile("monitoring")
public class MonitoringSecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain monitoringSecurityFilterChain(
            HttpSecurity http,
            @Value("${app.monitoring.password:}") String password) throws Exception {
        if (password.isBlank()) {
            throw new IllegalStateException("monitoring requires app.monitoring.password");
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager(
                User.withUsername("prometheus")
                        .password(encoder.encode(password))
                        .roles("MONITORING")
                        .build());
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return http
                .securityMatcher("/actuator/prometheus")
                .authenticationManager(new ProviderManager(List.of(provider)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("MONITORING"))
                .exceptionHandling(errors -> errors.accessDeniedHandler(
                        (request, response, exception) -> response.setStatus(401)))
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
