package com.whatiread.config;

import com.whatiread.identity.security.JwtAuthenticationFilter;
import com.whatiread.shared.util.NetworkUtils;
import com.whatiread.shared.web.ApiPaths;
import com.whatiread.shared.web.WebPaths;
import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter,
            Environment environment
    ) throws Exception {
        boolean prodProfile = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            WebPaths.ACTUATOR_HEALTH,
                            WebPaths.ACTUATOR_HEALTH + "/**",
                            ApiPaths.AUTH + "/**",
                            ApiPaths.SETUP + "/**",
                            ApiPaths.STATUS
                    ).permitAll();
                    if (!prodProfile) {
                        auth.requestMatchers(
                                WebPaths.OPENAPI_DOCS + "/**",
                                "/swagger-ui/**",
                                WebPaths.SWAGGER_UI,
                                "/h2-console/**"
                        ).permitAll();
                    }
                    auth.requestMatchers(WebPaths.WS, WebPaths.WS + "/**").permitAll();
                    auth.requestMatchers(WebPaths.ACTUATOR_PROMETHEUS, WebPaths.ACTUATOR_INFO).access((authentication, context) ->
                            new AuthorizationDecision(
                                    NetworkUtils.isInternalNetwork(context.getRequest().getRemoteAddr())
                            ));
                    auth.requestMatchers(HttpMethod.GET, ApiPaths.BOOKS_SEARCH).permitAll();
                    auth.requestMatchers(HttpMethod.GET, ApiPaths.PUBLIC + "/**").permitAll();
                    auth.requestMatchers(ApiPaths.V1 + "/**").authenticated();
                    auth.anyRequest().denyAll();
                })
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
