package com.cliq24.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/app.js", "/style.css", "/logo.PNG",
                                "/privacy.html", "/terms.html", "/data-deletion.html",
                                "/auth/google", "/oauth2/**", "/login/**", "/error",
                                "/api/social-accounts/Facebook", "/api/social-accounts/facebook/callback",
                                "/api/social-accounts/Instagram", "/api/social-accounts/instagram/callback",
                                "/api/social-accounts/LinkedIn", "/api/social-accounts/linkedin/callback",
                                "/api/social-accounts/TikTok", "/api/social-accounts/tiktok/callback",
                                "/api/social-accounts/Snapchat", "/api/social-accounts/snapchat/callback",
                                "/uploads/**", "/*.png", "/*.jpg", "/*.css", "/*.js", "/*.html").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2LoginSuccessHandler)
                .failureUrl("/?error=oauth_failed")
            );
        return http.build();
    }

      @Bean
      public CorsConfigurationSource corsConfigurationSource() {
          CorsConfiguration configuration = new CorsConfiguration();
          configuration.setAllowedOrigins(Arrays.asList("*"));
          configuration.setAllowedMethods(Arrays.asList("*"));
          configuration.setAllowedHeaders(Arrays.asList("*"));
          UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
          source.registerCorsConfiguration("/**", configuration);
          return source;
      }
  }