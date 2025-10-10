package com.portal.universe.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();
        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, configurer -> {
                    configurer.oidc(Customizer.withDefaults());
                })
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                )
                .oauth2ResourceServer(server -> server.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/users/signup").permitAll()
                        .requestMatchers("/login", "/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/users/signup"));

        return http.build();
    }


//
//    @Bean
//    @Order(1)
//    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
//        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();
//        http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher());
//
//        http.with(authorizationServerConfigurer, Customizer.withDefaults());
//        http
//                .authorizeHttpRequests(authorize ->
//                        authorize.anyRequest().authenticated()
//                )
//                .csrf(csrf -> csrf.ignoringRequestMatchers(authorizationServerConfigurer.getEndpointsMatcher()))
//                .exceptionHandling(exceptions -> exceptions
//                        .defaultAuthenticationEntryPointFor(
//                                new LoginUrlAuthenticationEntryPoint("/login"),
//                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
//                        )
//                );
//        return http.build();
//    }
//
//    @Bean
//    @Order(2)
//    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .securityMatcher("/actuator/**")
//                .authorizeHttpRequests(authorize ->
//                        authorize.anyRequest().permitAll()
//                )
//                .csrf(AbstractHttpConfigurer::disable);
//        return http.build();
//    }
//
//    @Bean
//    @Order(3)
//    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
//                        .requestMatchers(HttpMethod.POST, "/api/users/signup").permitAll()
//                        .requestMatchers("/login", "/favicon.ico", "/.well-known/**", "/actuator/**", "/ping", "/force-error").permitAll()
//                        .requestMatchers("/login", "/favicon.ico").permitAll()
//                        .anyRequest().authenticated()
//                )
//                .formLogin(Customizer.withDefaults())
//                .logout(logout -> logout
//                        .invalidateHttpSession(true)
//                        .clearAuthentication(true)
//                        .deleteCookies("JSESSIONID")
//                )
//                .csrf(AbstractHttpConfigurer::disable);
//        return http.build();
//    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}